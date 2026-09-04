package com.mist.medicalmate.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `Content에는 인사말에 쓸 이름과 배너가 있다`() {
        val viewModel = HomeViewModel()

        viewModel.refresh()
        val content = viewModel.uiState.value as HomeUiState.Content

        assertEquals("서연", content.userName)
        assertNotNull(content.notice)
    }

    @Test
    fun `저장된 카드는 확정과 진행 중을 모두 담는다`() {
        val viewModel = HomeViewModel()

        viewModel.refresh()
        val content = viewModel.uiState.value as HomeUiState.Content
        val statuses = content.savedCards.map { it.status }

        assertTrue(statuses.contains(SavedCardSummary.Status.CONFIRMED))
        assertTrue(statuses.contains(SavedCardSummary.Status.DRAFT))
    }

    @Test
    fun `카드 id는 서로 다르다`() {
        val viewModel = HomeViewModel()

        viewModel.refresh()
        val content = viewModel.uiState.value as HomeUiState.Content
        val ids = content.savedCards.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `날짜는 java time 타입이라 포맷이 화면단에 남는다`() {
        val viewModel = HomeViewModel()

        viewModel.refresh()
        val content = viewModel.uiState.value as HomeUiState.Content

        assertEquals(LocalDate.of(2026, 6, 20), content.savedCards.first().writtenOn)
    }
}
