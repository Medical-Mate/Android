package com.mist.medicalmate.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
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
    }

    fun onSignedIn(onboardingRequired: Boolean) {
        mutableUiState.value = SessionUiState.SignedIn(onboardingRequired)
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

    private fun restore() {
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = authRepository.restoreSession()) {
                    is AuthResult.Success ->
                        SessionUiState.SignedIn(result.session.onboardingRequired)

                    // 저장된 토큰이 없거나(null) 복구에 실패한 경우 모두 로그인 화면으로.
                    null, AuthResult.NetworkUnavailable, is AuthResult.Rejected ->
                        SessionUiState.SignedOut
                }
        }
    }
}
