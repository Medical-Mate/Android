package com.mist.medicalmate.auth.ui

import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import com.mist.medicalmate.auth.data.Session
import com.mist.medicalmate.core.network.ApiErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `저장된 토큰이 없으면 SignedOut이다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `복구에 성공하면 SignedIn이다`() = runTest {
        val viewModel =
            SessionViewModel(
                FakeAuthRepository(
                    restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
                ),
            )

        assertEquals(SessionUiState.SignedIn(onboardingRequired = false), viewModel.uiState.value)
    }

    @Test
    fun `토큰이 거절되면 SignedOut이다`() = runTest {
        val viewModel =
            SessionViewModel(
                FakeAuthRepository(
                    restoreResult =
                    AuthResult.Rejected(
                        code = ApiErrorCode.UNAUTHORIZED,
                        requestId = "req_test",
                    ),
                ),
            )

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `오프라인이어도 로그인 화면으로 보낸다`() = runTest {
        val viewModel =
            SessionViewModel(FakeAuthRepository(restoreResult = AuthResult.NetworkUnavailable))

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `로그인을 마치면 SignedIn으로 올린다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        viewModel.onSignedIn(onboardingRequired = true)

        assertEquals(SessionUiState.SignedIn(onboardingRequired = true), viewModel.uiState.value)
    }

    private class FakeAuthRepository(private val restoreResult: AuthResult?) : AuthRepository {
        override suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult =
            AuthResult.Success(Session(onboardingRequired = false))

        override suspend fun restoreSession(): AuthResult? = restoreResult

        override suspend fun clearSession() = Unit
    }
}
