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
 * 저장된 refresh 토큰이 있으면 서버에 교환을 시도한다. 없거나 실패하면 로그인
 * 화면으로 보낸다. 실패 사유로 화면을 나누지 않는다. 어느 쪽이든 사용자가 할 일은
 * 다시 로그인하는 것뿐이다.
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
     */
    private fun restore() {
        viewModelScope.launch {
            val restored = async { authRepository.restoreSession() }
            delay(MIN_SPLASH_MILLIS)

            val next =
                when (val result = restored.await()) {
                    is AuthResult.Success ->
                        SessionUiState.SignedIn(result.session.onboardingRequired)

                    // 저장된 토큰이 없거나(null) 복구에 실패한 경우 모두 로그인 화면으로.
                    null, AuthResult.NetworkUnavailable, is AuthResult.Rejected ->
                        SessionUiState.SignedOut
                }

            // 기다리는 동안 화면이 이미 옮겨갔다면 복구 결과를 버린다. 대기를 두면서
            // 생긴 창이다. 늦게 도착한 결과가 그 사이의 로그인이나 로그아웃을 덮으면
            // 사용자가 방금 한 일이 되돌려진다.
            if (mutableUiState.value == SessionUiState.Checking) {
                mutableUiState.value = next
            }
        }
    }

    private companion object {
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
    }
}
