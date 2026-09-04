package com.mist.medicalmate.auth.ui

import com.mist.medicalmate.auth.data.KakaoLoginResult
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginViewModelTest {
    @Test
    fun `처음 상태는 Idle이다`() {
        assertEquals(LoginUiState.Idle, LoginViewModel().uiState.value)
    }

    @Test
    fun `로그인을 시작하면 InProgress가 된다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginStarted()

        assertEquals(LoginUiState.InProgress, viewModel.uiState.value)
    }

    @Test
    fun `성공하면 받은 토큰을 그대로 담은 Authenticated가 된다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginStarted()
        viewModel.onLoginResult(KakaoLoginResult.Success("token-abc"))

        assertEquals(LoginUiState.Authenticated("token-abc"), viewModel.uiState.value)
    }

    @Test
    fun `취소는 실패가 아니라 처음 상태로 돌아간다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginStarted()
        viewModel.onLoginResult(KakaoLoginResult.Cancelled)

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `실패하면 Failed가 된다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginStarted()
        viewModel.onLoginResult(KakaoLoginResult.Failure(IllegalStateException("boom")))

        assertEquals(LoginUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `실패를 확인하면 다시 시도할 수 있게 Idle로 돌아간다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginResult(KakaoLoginResult.Failure(IllegalStateException("boom")))
        viewModel.onFailureAcknowledged()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `Authenticated 상태에서 실패 확인은 상태를 바꾸지 않는다`() {
        val viewModel = LoginViewModel()

        viewModel.onLoginResult(KakaoLoginResult.Success("token-abc"))
        viewModel.onFailureAcknowledged()

        assertEquals(LoginUiState.Authenticated("token-abc"), viewModel.uiState.value)
    }
}
