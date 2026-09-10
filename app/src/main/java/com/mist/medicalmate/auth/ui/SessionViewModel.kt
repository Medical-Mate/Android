package com.mist.medicalmate.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SessionUiState {
    /** 저장된 토큰으로 세션을 복구하는 중. */
    data object Checking : SessionUiState

    data object SignedOut : SessionUiState

    data class SignedIn(val onboardingRequired: Boolean) : SessionUiState
}

/**
 * 로그아웃·탈퇴 진행 상태. 세션 상태와 분리한다.
 *
 * 탈퇴가 실패하면 로그인 상태는 그대로 유지되어야 하고, 실패 사실만 따로 알려야
 * 하기 때문이다. 한 상태에 섞으면 "로그인됨 + 탈퇴 실패"를 표현할 수 없다.
 */
sealed interface AccountActionState {
    data object Idle : AccountActionState

    data object InProgress : AccountActionState

    /** 탈퇴가 실패했다. 계정은 그대로 남아 있다. */
    data object WithdrawFailed : AccountActionState
}

/**
 * 앱 진입 시 로그인 화면과 홈 중 어디로 보낼지 결정하고, 로그아웃·탈퇴를 수행한다.
 *
 * 저장된 refresh 토큰이 있으면 서버에 교환을 시도한다. 저장된 토큰이 없거나 서버가
 * 거절하면 로그인 화면으로 보낸다.
 *
 * **서버에 물어보지 못한 것은 로그아웃이 아니다.** 응답이 늦거나 연결이 안 되면 토큰이
 * 살아 있다고 보고 들어간다. 저장소가 토큰을 지우는 것은 서버가 거절했을 때뿐이라
 * (`DefaultAuthRepository.restoreSession`) 그 경우에만 확실히 못 쓰는 토큰이다. 정말
 * 만료됐다면 다음 인증 요청의 401이 재발급을 시도하고, 그것도 거절되면 저장소가 비면서
 * [observeSession]이 로그인 화면으로 보낸다.
 */
@HiltViewModel
class SessionViewModel
@Inject
constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<SessionUiState>(SessionUiState.Checking)
    val uiState: StateFlow<SessionUiState> = mutableUiState.asStateFlow()

    private val mutableAccountAction = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val accountAction: StateFlow<AccountActionState> = mutableAccountAction.asStateFlow()

    init {
        restore()
        observeSession()
    }

    fun onSignedIn(onboardingRequired: Boolean) {
        mutableUiState.value = SessionUiState.SignedIn(onboardingRequired)
    }

    /**
     * 온보딩을 마쳤다. 세션은 그대로 두고 온보딩 필요 표시만 내린다.
     *
     * 로그인 상태가 아니면 아무것도 하지 않는다. 이 메서드로 로그인이 되어서는 안 된다.
     *
     * 지금은 온보딩 인트로(1a-2)의 시작하기가 부르지만, 신상정보 입력(1b-1~1b-4)이 생기면
     * 그 흐름의 끝으로 옮겨야 한다. 인트로만 보고 넘어간 사람은 아직 신상정보를 넣지 않았다.
     */
    fun onOnboardingCompleted() {
        if (mutableUiState.value is SessionUiState.SignedIn) {
            mutableUiState.value = SessionUiState.SignedIn(onboardingRequired = false)
        }
    }

    /**
     * 로그아웃. 서버 호출 결과와 무관하게 로그인 화면으로 보낸다.
     * Repository가 로컬 토큰과 카카오 세션을 반드시 정리한다.
     */
    fun logout() {
        mutableAccountAction.value = AccountActionState.InProgress
        viewModelScope.launch {
            authRepository.logout()
            mutableAccountAction.value = AccountActionState.Idle
            mutableUiState.value = SessionUiState.SignedOut
        }
    }

    /** 회원탈퇴. 성공했을 때만 로그인 화면으로 보낸다. */
    fun withdraw() {
        mutableAccountAction.value = AccountActionState.InProgress
        viewModelScope.launch {
            when (authRepository.withdraw()) {
                is AuthResult.Success -> {
                    mutableAccountAction.value = AccountActionState.Idle
                    mutableUiState.value = SessionUiState.SignedOut
                }

                // 계정이 그대로 남아 있으므로 로그인 상태를 유지한다.
                AuthResult.NetworkUnavailable, is AuthResult.Rejected ->
                    mutableAccountAction.value = AccountActionState.WithdrawFailed
            }
        }
    }

    fun onAccountActionFailureAcknowledged() {
        mutableAccountAction.value = AccountActionState.Idle
    }

    /**
     * 세션이 화면 밖에서 끝나면 로그인 화면으로 보낸다.
     *
     * 토큰 재발급이 거절되면 저장소가 비워진다. 그 자리는 OkHttp 스레드라 화면을 옮길 수
     * 없어서, 저장소가 비는 것을 신호로 삼는다.
     *
     * 로그인된 상태에서만 움직인다. 복구 중(Checking)에는 저장된 토큰이 없는 것이 정상이고,
     * 이미 로그아웃된 상태라면 옮길 곳이 없다.
     */
    private fun observeSession() {
        viewModelScope.launch {
            authRepository.hasSession.collect { hasSession ->
                if (!hasSession && mutableUiState.value is SessionUiState.SignedIn) {
                    mutableUiState.value = SessionUiState.SignedOut
                }
            }
        }
    }

    /**
     * 복구를 시작하고, 최소 노출 시간이 지난 뒤에 결과를 반영한다.
     *
     * 복구와 대기를 동시에 돌린다. 그래서 걸리는 시간은 둘 중 긴 쪽이다. 순서대로 하면
     * 복구가 느린 날에 스플래시가 [MIN_SPLASH_MILLIS]만큼 더 길어진다.
     *
     * 대기를 두는 이유는 저장된 토큰이 없을 때 복구가 즉시 끝나서 스플래시(1a-1)가 한 프레임만
     * 스쳤다 사라지기 때문이다. 화면이 번쩍인 것으로 읽힌다.
     *
     * **[MAX_SPLASH_MILLIS]를 넘기면 답을 기다리지 않고 넘어간다.** 서버가 잠들어 있으면
     * 재발급 응답이 읽기 제한(60초)까지 오지 않는데, 그동안 스플래시만 떠 있어서 앱이 멈춘
     * 것으로 보인다.
     *
     * 상한을 넘겼을 때 가는 곳은 로그인 화면이 **아니다.** 기다리다 만 것은 서버가 거절한
     * 것과 다르고, 저장된 토큰도 그대로 남아 있다. 로그인 화면으로 보내면 멀쩡한 세션을 두고
     * 다시 로그인을 시키는 셈이다. 클래스 주석의 규칙을 그대로 따른다.
     *
     * 늦게 도착한 결과는 버린다. 아래의 `Checking` 확인이 그 일을 한다. 화면을 보고 있는
     * 사람을 갑자기 다른 곳으로 옮기면 그 사이에 한 일과 부딪힌다. 그래도 늦게 온 거절은
     * 반영된다. 저장소가 비면서 [observeSession]이 받는다.
     */
    private fun restore() {
        viewModelScope.launch {
            val restored = async { authRepository.restoreSession() }
            delay(MIN_SPLASH_MILLIS)

            // 상한과 최소 노출의 차이만큼만 더 기다린다. 결과를 [RestoreOutcome]으로 감싸는
            // 이유는 시간이 다 됐을 때의 null과 저장된 토큰이 없을 때의 null이 겹치기
            // 때문이다. 둘은 가는 곳이 다르다.
            val outcome = withTimeoutOrNull(MAX_SPLASH_MILLIS - MIN_SPLASH_MILLIS) { RestoreOutcome(restored.await()) }
            val next =
                when {
                    // 시간이 다 됐다. 아직 답을 모르는 것이라 토큰을 믿고 들어간다.
                    outcome == null -> keepGoingWithStoredToken()

                    // 저장된 토큰이 없다. 복구할 세션이 애초에 없다.
                    outcome.result == null -> SessionUiState.SignedOut

                    else -> outcome.result.toSessionState()
                }

            // 기다리는 동안 화면이 이미 옮겨갔다면 복구 결과를 버린다. 대기를 두면서
            // 생긴 창이다. 늦게 도착한 결과가 그 사이의 로그인이나 로그아웃을 덮으면
            // 사용자가 방금 한 일이 되돌려진다.
            if (mutableUiState.value == SessionUiState.Checking) {
                mutableUiState.value = next
            }
        }
    }

    /**
     * 복구 결과가 가리키는 세션 상태.
     *
     * 연결이 안 된 것을 로그아웃으로 읽지 않는다. 이유는 클래스 주석에 있다.
     */
    private fun AuthResult.toSessionState(): SessionUiState = when (this) {
        is AuthResult.Success -> SessionUiState.SignedIn(session.onboardingRequired)
        AuthResult.NetworkUnavailable -> keepGoingWithStoredToken()
        is AuthResult.Rejected -> SessionUiState.SignedOut
    }

    /**
     * 서버에 물어보지 못했을 때 들어가는 상태.
     *
     * 온보딩 필요 여부를 false로 두는 것은 임의로 정한 값이 아니다. `refresh` 응답이 이 값을
     * 항상 false로 주므로(`TokenResponse` 주석) 복구가 성공했더라도 같은 값이 들어온다.
     * 온보딩을 보여줄지는 `profile/data/OnboardingStore`의 기기 기록이 따로 판단한다.
     */
    private fun keepGoingWithStoredToken(): SessionUiState = SessionUiState.SignedIn(onboardingRequired = false)

    /** 시간 값은 시험이 그대로 읽는다. 시험이 6000을 다시 적으면 값을 바꿀 때 한쪽만 바뀐다. */
    internal companion object {
        /**
         * 스플래시 최소 노출 시간.
         *
         * Figma에 값이 없어서 정한 값이다. 브랜드 스플래시의 통상 범위인 1~2초에서 위쪽을
         * 골랐다. 태그라인을 읽는 데만 1초 가까이 걸리고, 읽고 나서도 로고가 남아 있는 편이
         * 진입으로 자연스럽다. 2초를 넘기면 앱이 느리다는 인상이 생기므로 여기가 상한이다.
         *
         * 복구가 이보다 오래 걸리면 그쪽을 기다린다. 이 값이 더해지는 것이 아니다.
         *
         * 시스템 스플래시의 1초 제한과는 무관하다. 이 화면은 우리가 그린다.
         */
        const val MIN_SPLASH_MILLIS = 2_000L

        /**
         * 스플래시 최대 노출 시간.
         *
         * 세션 복구를 여기까지만 기다린다. 백엔드가 Render 무료 티어라 유휴 상태에서 첫
         * 요청의 응답이 수십 초 걸리는데, 그동안 브랜드 화면만 떠 있으면 앱이 멈춘 것으로
         * 읽힌다.
         *
         * **응답 시간에 맞춰 정한 값이 아니다.** 실기기에서 `POST /api/auth/refresh`를 재보니
         * 깨어 있는 서버가 9.2초, 잠든 서버는 90초에도 답이 없었다. 답을 덮으려면 스플래시가
         * 10초를 넘어야 해서 그쪽은 택하지 않았다. 6초는 브랜드 화면을 보여줄 수 있는 한계로
         * 잡은 값이고, 답은 늦게 와도 반영된다.
         */
        const val MAX_SPLASH_MILLIS = 6_000L
    }
}

/**
 * 복구 결과를 감싸는 껍데기.
 *
 * `restoreSession()`은 저장된 토큰이 없으면 null을 주고, [withTimeoutOrNull]도 시간이 다
 * 되면 null을 준다. 감싸지 않으면 두 null이 같은 값이 되는데 가는 곳이 서로 다르다.
 */
private class RestoreOutcome(val result: AuthResult?)
