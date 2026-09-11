package com.mist.medicalmate.home.ui

import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.home.data.HomeRepository
import com.mist.medicalmate.home.data.HomeSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음 상태는 Loading이다`() {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `refresh하면 받은 값이 Content가 된다`() {
        val viewModel = viewModel()

        viewModel.refresh()

        val content = viewModel.uiState.value as HomeUiState.Content
        assertEquals("김", content.userInitial)
        assertEquals(snapshot.resume, content.resume)
        assertEquals(snapshot.savedCards, content.savedCards)
    }

    @Test
    fun `읽지 않은 알림은 늘 꺼져 있다`() {
        // 서버에 대응하는 값이 없다. 알림 목록 API도 없다.
        val viewModel = viewModel()

        viewModel.refresh()

        assertFalse((viewModel.uiState.value as HomeUiState.Content).hasUnreadNotification)
    }

    @Test
    fun `오늘 날짜를 저장소에 넘긴다`() {
        // D-day와 "며칠 지났어요"가 이 날짜로 계산된다. 시스템 시계를 직접 읽으면 검증할 수 없다.
        var received: LocalDate? = null
        val viewModel = viewModel(
            repository = { today ->
                received = today
                ApiResult.Success(snapshot)
            },
        )

        viewModel.refresh()

        assertEquals(LocalDate.of(2026, 9, 11), received)
    }

    @Test
    fun `서버가 거절하면 Failed가 된다`() {
        val viewModel = viewModel(
            repository = { ApiResult.Rejected(ApiErrorCode.UNKNOWN, null, null, retryable = false) },
        )

        viewModel.refresh()

        assertEquals(HomeUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `연결이 없으면 Failed가 된다`() {
        val viewModel = viewModel(repository = { ApiResult.NetworkUnavailable(IOException()) })

        viewModel.refresh()

        assertEquals(HomeUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `다시 부르면 Loading을 거쳐 간다`() {
        // 화면으로 돌아올 때마다 부른다. 옛 값이 남아 있으면 바뀐 진행도가 늦게 보인다.
        val states = mutableListOf<HomeUiState>()
        val viewModel = viewModel()
        viewModel.refresh()

        val before = viewModel.uiState.value
        viewModel.refresh()
        states += before

        assertTrue(states.first() is HomeUiState.Content)
        assertTrue(viewModel.uiState.value is HomeUiState.Content)
    }

    private fun viewModel(
        repository: suspend (LocalDate) -> ApiResult<HomeSnapshot> = {
            ApiResult.Success(snapshot)
        },
    ) = HomeViewModel(
        repository = object : HomeRepository {
            override suspend fun load(today: LocalDate) = repository(today)
        },
        clock = Clock.fixed(Instant.parse("2026-09-11T09:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    private companion object {
        val snapshot =
            HomeSnapshot(
                userInitial = "김",
                todayLine = HomeTodayLine.SinceLastVisit(
                    daysSinceLastVisit = 12,
                    nextVisit = LocalDate.of(2026, 9, 12),
                ),
                resume = HomeResume(intakeId = "7", symptomTitle = "복부", answeredSteps = 2, totalSteps = 4),
                savedCards =
                listOf(
                    SavedCardSummary(
                        id = "1",
                        title = "복부 통증 · 3주",
                        visited = true,
                        writtenOn = LocalDate.of(2026, 9, 4),
                        clinic = "서울OO병원 내과",
                    ),
                ),
                upcoming = emptyList(),
            )
    }
}
