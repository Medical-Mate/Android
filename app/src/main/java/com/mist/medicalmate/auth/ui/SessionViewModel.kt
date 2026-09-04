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
 * 앱 진입 시 로그인 화면과 홈 중 어디로 보낼지 결정한다.
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

    init {
        restore()
    }

    fun onSignedIn(onboardingRequired: Boolean) {
        mutableUiState.value = SessionUiState.SignedIn(onboardingRequired)
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
