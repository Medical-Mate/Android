package com.mist.medicalmate.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import com.mist.medicalmate.auth.data.KakaoLoginResult
import com.mist.medicalmate.core.network.ApiErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 로그인 화면 상태 보유자.
 *
 * 카카오 SDK 호출은 Activity Context가 필요해 UI 계층이 담당하고, 이 클래스는
 * 그 결과를 받아 서버 토큰 교환까지 진행한다. Context 의존이 없어 상태 전이를
 * JVM 테스트로 검증할 수 있다.
 *
 * Repository가 실패를 예외가 아니라 [AuthResult]로 주므로 여기에 try/catch가 없다.
 */
@HiltViewModel
class LoginViewModel
@Inject
constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

    fun onLoginStarted() {
        mutableUiState.value = LoginUiState.InProgress
    }

    fun onLoginResult(result: KakaoLoginResult) {
        when (result) {
            is KakaoLoginResult.Success -> exchangeToken(result.accessToken)
            // 사용자가 스스로 닫은 것은 오류가 아니므로 처음 상태로 돌린다.
            KakaoLoginResult.Cancelled -> mutableUiState.value = LoginUiState.Idle
            is KakaoLoginResult.Failure ->
                mutableUiState.value = LoginUiState.Failed(LoginFailure.KAKAO)
        }
    }

    /**
     * 로그인 완료를 호출자가 처리한 뒤 부른다.
     *
     * 이 ViewModel은 Activity 스코프라 로그인 화면을 떠나도 살아 있다. 상태를
     * 되돌리지 않으면 로그아웃 뒤 로그인 화면이 다시 열릴 때 예전 [LoginUiState.Authenticated]가
     * 그대로 흘러나가 곧바로 홈으로 되돌아간다.
     */
    fun onAuthenticationHandled() {
        if (mutableUiState.value is LoginUiState.Authenticated) {
            mutableUiState.value = LoginUiState.Idle
        }
    }

    /** 오류 안내를 닫고 다시 시도할 수 있게 되돌린다. */
    fun onFailureAcknowledged() {
        if (mutableUiState.value is LoginUiState.Failed) {
            mutableUiState.value = LoginUiState.Idle
        }
    }

    private fun exchangeToken(kakaoAccessToken: String) {
        mutableUiState.value = LoginUiState.ExchangingToken
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = authRepository.loginWithKakao(kakaoAccessToken)) {
                    is AuthResult.Success ->
                        LoginUiState.Authenticated(result.session.onboardingRequired)

                    AuthResult.NetworkUnavailable ->
                        LoginUiState.Failed(LoginFailure.NETWORK)

                    is AuthResult.Rejected -> LoginUiState.Failed(result.toFailure())
                }
        }
    }
}

/**
 * 서버가 상위 서비스에 닿지 못한 경우(카카오 검증 실패가 아니라 통신 실패)는
 * 사용자가 할 일이 "잠시 후 다시"라서 네트워크 갈래로 묶는다.
 */
private fun AuthResult.Rejected.toFailure(): LoginFailure = when (code) {
    ApiErrorCode.UPSTREAM_ERROR, ApiErrorCode.UPSTREAM_TIMEOUT -> LoginFailure.NETWORK
    else -> LoginFailure.SERVER
}
