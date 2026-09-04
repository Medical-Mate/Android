package com.mist.medicalmate.auth.ui

import androidx.lifecycle.ViewModel
import com.mist.medicalmate.auth.data.KakaoLoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그인 화면 상태 보유자.
 *
 * SDK 호출은 Activity Context가 필요해 UI 계층이 담당하고, 이 클래스는 결과만 받는다.
 * Context 의존이 없어 상태 전이를 JVM 테스트로 검증할 수 있다.
 */
@HiltViewModel
class LoginViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

    fun onLoginStarted() {
        mutableUiState.value = LoginUiState.InProgress
    }

    fun onLoginResult(result: KakaoLoginResult) {
        mutableUiState.value =
            when (result) {
                is KakaoLoginResult.Success -> LoginUiState.Authenticated(result.accessToken)
                // 사용자가 스스로 닫은 것은 오류가 아니므로 처음 상태로 돌린다.
                KakaoLoginResult.Cancelled -> LoginUiState.Idle
                is KakaoLoginResult.Failure -> LoginUiState.Failed
            }
    }

    /** 오류 안내를 닫고 다시 시도할 수 있게 되돌린다. */
    fun onFailureAcknowledged() {
        if (mutableUiState.value == LoginUiState.Failed) {
            mutableUiState.value = LoginUiState.Idle
        }
    }
}
