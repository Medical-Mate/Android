package com.mist.medicalmate.auth.ui

import com.mist.medicalmate.auth.data.AuthRepository
import com.mist.medicalmate.auth.data.AuthResult
import com.mist.medicalmate.auth.data.Session
import com.mist.medicalmate.core.network.ApiErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        advanceUntilIdle()

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

        advanceUntilIdle()

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

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `오프라인이어도 로그인 화면으로 보낸다`() = runTest {
        val viewModel =
            SessionViewModel(FakeAuthRepository(restoreResult = AuthResult.NetworkUnavailable))

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `로그인을 마치면 SignedIn으로 올린다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        viewModel.onSignedIn(onboardingRequired = true)

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = true), viewModel.uiState.value)
    }

    @Test
    fun `로그아웃하면 SignedOut이 되고 Repository 로그아웃이 호출된다`() = runTest {
        val repository =
            FakeAuthRepository(
                restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
            )
        val viewModel = SessionViewModel(repository)

        viewModel.logout()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
        assertEquals(AccountActionState.Idle, viewModel.accountAction.value)
        assertTrue(repository.logoutCalled)
    }

    @Test
    fun `탈퇴에 성공하면 SignedOut이 된다`() = runTest {
        val repository =
            FakeAuthRepository(
                restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
                withdrawResult = AuthResult.Success(Session(onboardingRequired = false)),
            )
        val viewModel = SessionViewModel(repository)

        viewModel.withdraw()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
        assertEquals(AccountActionState.Idle, viewModel.accountAction.value)
    }

    @Test
    fun `탈퇴에 실패하면 로그인 상태를 유지하고 실패를 알린다`() = runTest {
        val repository =
            FakeAuthRepository(
                restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
                withdrawResult =
                AuthResult.Rejected(code = ApiErrorCode.INTERNAL, requestId = "req_test"),
            )
        val viewModel = SessionViewModel(repository)

        viewModel.withdraw()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = false), viewModel.uiState.value)
        assertEquals(AccountActionState.WithdrawFailed, viewModel.accountAction.value)
    }

    @Test
    fun `오프라인이면 탈퇴가 실패로 남는다`() = runTest {
        val repository =
            FakeAuthRepository(
                restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
                withdrawResult = AuthResult.NetworkUnavailable,
            )
        val viewModel = SessionViewModel(repository)

        viewModel.withdraw()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = false), viewModel.uiState.value)
        assertEquals(AccountActionState.WithdrawFailed, viewModel.accountAction.value)
    }

    @Test
    fun `탈퇴 실패를 확인하면 다시 시도할 수 있게 Idle로 돌아간다`() = runTest {
        val repository =
            FakeAuthRepository(
                restoreResult = AuthResult.Success(Session(onboardingRequired = false)),
                withdrawResult = AuthResult.NetworkUnavailable,
            )
        val viewModel = SessionViewModel(repository)

        viewModel.withdraw()
        viewModel.onAccountActionFailureAcknowledged()

        assertEquals(AccountActionState.Idle, viewModel.accountAction.value)
    }

    @Test
    fun `최소 노출 시간이 지나기 전에는 Checking이다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        advanceTimeBy(500)

        assertEquals(SessionUiState.Checking, viewModel.uiState.value)
    }

    @Test
    fun `복구가 늦게 끝나도 그 사이 옮겨간 상태를 덮지 않는다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        viewModel.onSignedIn(onboardingRequired = false)
        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = false), viewModel.uiState.value)
    }

    @Test
    fun `온보딩이 필요한 로그인은 그 표시를 들고 있다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        viewModel.onSignedIn(onboardingRequired = true)

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = true), viewModel.uiState.value)
    }

    @Test
    fun `온보딩을 마치면 로그인 상태는 그대로 두고 표시만 내린다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))
        viewModel.onSignedIn(onboardingRequired = true)

        viewModel.onOnboardingCompleted()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedIn(onboardingRequired = false), viewModel.uiState.value)
    }

    @Test
    fun `로그인 상태가 아니면 온보딩 완료는 아무것도 바꾸지 않는다`() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreResult = null))

        viewModel.onOnboardingCompleted()

        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `세션이 화면 밖에서 끝나면 로그인 화면으로 보낸다`() = runTest {
        val repository = FakeAuthRepository(restoreResult = AuthResult.Success(Session(false)))
        val viewModel = SessionViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SessionUiState.SignedIn)

        repository.dropSession()
        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `복구 중에는 세션이 비어 있어도 상태를 바꾸지 않는다`() = runTest {
        val repository = FakeAuthRepository(restoreResult = null)
        val viewModel = SessionViewModel(repository)

        repository.dropSession()

        assertEquals(SessionUiState.Checking, viewModel.uiState.value)
    }

    private class FakeAuthRepository(
        private val restoreResult: AuthResult?,
        private val withdrawResult: AuthResult = AuthResult.Success(Session(onboardingRequired = false)),
    ) : AuthRepository {
        var logoutCalled: Boolean = false
            private set

        /** 세션이 화면 밖에서 끝나는 것을 흉내 내려면 값을 흘려 넣어야 한다. */
        private val sessionFlow = MutableStateFlow(true)

        override val hasSession: Flow<Boolean> = sessionFlow

        fun dropSession() {
            sessionFlow.value = false
        }

        override suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult =
            AuthResult.Success(Session(onboardingRequired = false))

        override suspend fun restoreSession(): AuthResult? = restoreResult

        override suspend fun logout() {
            logoutCalled = true
        }

        override suspend fun withdraw(): AuthResult = withdrawResult

        override suspend fun clearSession() = Unit
    }
}
