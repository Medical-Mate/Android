package com.mist.medicalmate.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeViewModelTest {
    @Test
    fun `처음 상태는 Loading이다`() {
        assertEquals(HomeUiState.Loading, HomeViewModel().uiState.value)
    }

    @Test
    fun `refresh하면 Content가 된다`() {
        val viewModel = HomeViewModel()

        viewModel.refresh()

        assertTrue(viewModel.uiState.value is HomeUiState.Content)
    }

    @Test
    fun `Content에는 헤더에 쓸 머리글자와 알림 여부가 있다`() {
        val content = refreshedContent()

        assertEquals("김", content.userInitial)
        assertTrue(content.hasUnreadNotification)
    }

    @Test
    fun `오늘의 한 줄은 지난 진료 이후 경과를 담는다`() {
        val todayLine = refreshedContent().todayLine

        assertTrue(todayLine is HomeTodayLine.SinceLastVisit)
        assertEquals(12, (todayLine as HomeTodayLine.SinceLastVisit).daysSinceLastVisit)
        assertNotNull(todayLine.nextVisit)
    }

    @Test
    fun `작성 중이던 문답의 진행 정도가 총 단계보다 작다`() {
        val resume = refreshedContent().resume

        assertNotNull(resume)
        requireNotNull(resume)
        assertTrue(resume.answeredSteps < resume.totalSteps)
    }

    @Test
    fun `저장된 카드는 확정과 진행 중을 모두 담는다`() {
        val statuses = refreshedContent().savedCards.map { it.status }

        assertTrue(statuses.contains(SavedCardSummary.Status.CONFIRMED))
        assertTrue(statuses.contains(SavedCardSummary.Status.DRAFT))
    }

    @Test
    fun `카드 id는 서로 다르다`() {
        val ids = refreshedContent().savedCards.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `확정 전 카드에는 병원이 없다`() {
        val draft =
            refreshedContent().savedCards.first { it.status == SavedCardSummary.Status.DRAFT }

        assertNull(draft.clinic)
    }

    @Test
    fun `날짜는 java time 타입이라 포맷이 화면단에 남는다`() {
        val content = refreshedContent()

        assertEquals(LocalDate.of(2026, 9, 4), content.savedCards.first().writtenOn)
        assertEquals(LocalDate.of(2026, 9, 12), content.upcoming.first().date)
    }

    private fun refreshedContent(): HomeUiState.Content {
        val viewModel = HomeViewModel()
        viewModel.refresh()
        return viewModel.uiState.value as HomeUiState.Content
    }
}
