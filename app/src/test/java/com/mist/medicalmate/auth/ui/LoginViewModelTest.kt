package com.mist.medicalmate.auth.ui

import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import com.mist.medicalmate.auth.data.KakaoLoginResult
import com.mist.medicalmate.auth.data.Session
import com.mist.medicalmate.core.network.ApiErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음 상태는 Idle이다`() {
        assertEquals(LoginUiState.Idle, viewModel().uiState.value)
    }

    @Test
    fun `로그인을 시작하면 InProgress가 된다`() {
        val viewModel = viewModel()

        viewModel.onLoginStarted()

        assertEquals(LoginUiState.InProgress, viewModel.uiState.value)
    }

    @Test
    fun `취소는 실패가 아니라 처음 상태로 돌아간다`() {
        val viewModel = viewModel()

        viewModel.onLoginStarted()
        viewModel.onLoginResult(KakaoLoginResult.Cancelled)

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `카카오 단계에서 실패하면 KAKAO 갈래가 된다`() {
        val viewModel = viewModel()

        viewModel.onLoginResult(KakaoLoginResult.Failure(IllegalStateException("boom")))

        assertEquals(LoginUiState.Failed(LoginFailure.KAKAO), viewModel.uiState.value)
    }

    @Test
    fun `서버 교환이 성공하면 onboardingRequired를 그대로 담는다`() = runTest {
        val viewModel =
            viewModel(
                FakeAuthRepository(AuthResult.Success(Session(onboardingRequired = true))),
            )

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        assertEquals(
            LoginUiState.Authenticated(onboardingRequired = true),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `서버 교환에 넘기는 값은 카카오 액세스 토큰이다`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        assertEquals("kakao-token", repository.lastKakaoAccessToken)
    }

    @Test
    fun `네트워크가 닿지 않으면 NETWORK 갈래가 된다`() = runTest {
        val viewModel = viewModel(FakeAuthRepository(AuthResult.NetworkUnavailable))

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        assertEquals(LoginUiState.Failed(LoginFailure.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `서버가 인증을 거절하면 SERVER 갈래가 된다`() = runTest {
        val viewModel = viewModel(FakeAuthRepository(rejected(ApiErrorCode.UNAUTHORIZED)))

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        assertEquals(LoginUiState.Failed(LoginFailure.SERVER), viewModel.uiState.value)
    }

    @Test
    fun `서버가 상위 서비스에 닿지 못한 경우는 NETWORK 갈래로 묶는다`() = runTest {
        val viewModel = viewModel(FakeAuthRepository(rejected(ApiErrorCode.UPSTREAM_TIMEOUT)))

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        assertEquals(LoginUiState.Failed(LoginFailure.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `실패를 확인하면 다시 시도할 수 있게 Idle로 돌아간다`() {
        val viewModel = viewModel()

        viewModel.onLoginResult(KakaoLoginResult.Failure(IllegalStateException("boom")))
        viewModel.onFailureAcknowledged()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `로그인 완료를 처리하면 Idle로 돌아간다`() = runTest {
        val viewModel = viewModel()

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))
        viewModel.onAuthenticationHandled()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `소비하지 않으면 Authenticated가 그대로 남아 재진입 시 다시 흘러나간다`() = runTest {
        val viewModel = viewModel()

        viewModel.onLoginResult(KakaoLoginResult.Success("kakao-token"))

        // 이 ViewModel은 Activity 스코프라 로그인 화면을 떠나도 살아 있다. 이 상태가
        // 남아 있으면 로그아웃 뒤 로그인 화면이 열리자마자 다시 홈으로 튕긴다.
        assertEquals(
            LoginUiState.Authenticated(onboardingRequired = false),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `Idle 상태에서 로그인 완료 처리를 불러도 상태가 바뀌지 않는다`() {
        val viewModel = viewModel()

        viewModel.onAuthenticationHandled()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    private fun rejected(code: ApiErrorCode) = AuthResult.Rejected(code = code, requestId = "req_test")

    private fun viewModel(repository: AuthRepository = FakeAuthRepository()) = LoginViewModel(repository)

    private class FakeAuthRepository(
        private val result: AuthResult = AuthResult.Success(Session(onboardingRequired = false)),
    ) : AuthRepository {
        var lastKakaoAccessToken: String? = null
            private set

        override val hasSession: Flow<Boolean> = flowOf(false)

        override suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult {
            lastKakaoAccessToken = kakaoAccessToken
            return result
        }

        override suspend fun restoreSession(): AuthResult? = null

        override suspend fun logout() = Unit

        override suspend fun withdraw(): AuthResult = AuthResult.Success(Session(onboardingRequired = false))

        override suspend fun clearSession() = Unit
    }
}
