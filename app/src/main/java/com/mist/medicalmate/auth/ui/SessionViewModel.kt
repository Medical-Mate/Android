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

    /**
     * 서버에 물어보지 못해 로그인 여부를 모른다.
     *
     * 가는 곳은 [SignedOut]과 같은 로그인 화면이다. 홈으로 보내면 로그인되지 않았을 수도
     * 있는 사람에게 자기 기록인 척하는 화면을 보여주게 된다. 그래도 상태를 나눠 두는 이유는
     * 로그인 화면이 왜 다시 로그인해야 하는지 알려야 하기 때문이다. 스스로 로그아웃한 사람과
     * 자동 로그인이 실패한 사람은 같은 화면에서 다른 것을 알아야 한다.
     */
    data object RestoreFailed : SessionUiState
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
 * **서버에 물어보지 못한 것과 서버가 거절한 것을 나눈다.** 응답이 늦거나 연결이 안 되면
 * [SessionUiState.RestoreFailed]다. 두 경우 모두 로그인 화면으로 가지만, 그쪽은 왜 다시
 * 로그인해야 하는지 문구로 알려준다.
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
     * 복구에 실패한 상태([SessionUiState.RestoreFailed])도 함께 본다. 토큰이 남아 있다고 보고
     * 안내 문구를 띄우는 상태인데, 저장소가 비었다면 그 전제가 깨진 것이다. 문구를 내리고
     * 평범한 로그인 화면으로 돌린다.
     *
     * 복구 중(Checking)에는 움직이지 않는다. 저장된 토큰이 없는 것이 정상인 시점이라 여기서
     * 옮기면 복구 결과보다 먼저 화면을 정해버린다. 이미 로그아웃된 상태라면 옮길 곳이 없다.
     */
    private fun observeSession() {
        viewModelScope.launch {
            authRepository.hasSession.collect { hasSession ->
                if (hasSession) return@collect
                val current = mutableUiState.value
                if (current is SessionUiState.SignedIn || current == SessionUiState.RestoreFailed) {
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
     * 상한을 넘기면 로그인 화면으로 보내되 [SessionUiState.RestoreFailed]로 구분한다. 기다리다
     * 만 것은 서버가 거절한 것과 다르고 저장된 토큰도 그대로 남아 있다. 그 사실을 로그인
     * 화면이 문구로 알려준다.
     *
     * **기다리기를 그만두면 요청도 끊는다.** 그대로 두면 응답이 읽기 제한까지 살아 있어서, 그
     * 사이 사용자가 카카오 로그인을 마치면 재발급 응답이 뒤늦게 도착해 방금 받은 토큰을 덮는다.
     * 서버가 refresh 토큰을 회전시키므로 어느 쪽이 살아남는지도 서버 구현에 달렸다. 어차피
     * 결과를 쓰지 않는 요청이라 끊는 편이 분명하다.
     *
     * 늦게 도착한 결과는 버린다. 아래의 `Checking` 확인이 그 일을 한다. 화면을 보고 있는
     * 사람을 갑자기 다른 곳으로 옮기면 그 사이에 한 일과 부딪힌다.
     */
    private fun restore() {
        viewModelScope.launch {
            val restored = async { authRepository.restoreSession() }
            delay(MIN_SPLASH_MILLIS)

            // 남은 시간만큼만 더 기다린다. 결과를 [RestoreOutcome]으로 감싸는 이유는 시간이
            // 다 됐을 때의 null과 저장된 토큰이 없을 때의 null이 겹치기 때문이다. 둘은 가는
            // 곳이 다르다.
            val outcome =
                withTimeoutOrNull(MAX_SPLASH_MILLIS - MIN_SPLASH_MILLIS) { RestoreOutcome(restored.await()) }
            val next =
                when {
                    // 시간이 다 됐다. 답을 모르는 채로 로그아웃이라고 단정하지 않는다.
                    outcome == null -> {
                        restored.cancel()
                        SessionUiState.RestoreFailed
                    }

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
        AuthResult.NetworkUnavailable -> SessionUiState.RestoreFailed
        is AuthResult.Rejected -> SessionUiState.SignedOut
    }

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
         * 잡은 값이다. 넘기면 기다리기를 그만두고 로그인 화면으로 보낸다.
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
