package com.mist.medicalmate.visit.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VisitNoteViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 비어 있어 저장할 수 없다`() {
        val state = viewModel().uiState.value

        assertEquals(VisitHeadline(LocalDate.of(2026, 9, 12)), state.visit)
        assertEquals("", state.note)
        assertFalse(state.canSave)
    }

    @Test
    fun `고른 병원과 카드가 머리말에 온다`() {
        // 앞 화면에서 고른 값이다. 픽스처를 보여주면 방금 고른 곳과 다른 병원이 적힌다.
        val viewModel = viewModel()

        viewModel.load(clinic = "가톨릭대학교 성빈센트병원", cardTitle = "갈비뼈 · 일주일")

        assertEquals(
            VisitHeadline(
                visitedOn = LocalDate.of(2026, 9, 12),
                clinic = "가톨릭대학교 성빈센트병원",
                cardTitle = "갈비뼈 · 일주일",
            ),
            viewModel.uiState.value.visit,
        )
    }

    @Test
    fun `머리말을 다시 채워도 적던 글은 남는다`() {
        // 화면이 다시 조합되면 이 호출이 한 번 더 온다. 그때 적던 글이 사라지면 안 된다.
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.load(clinic = "가톨릭대학교 성빈센트병원", cardTitle = null)

        assertEquals("위염이라고 하셨어요", viewModel.uiState.value.note)
    }

    @Test
    fun `적으면 저장할 수 있다`() {
        val viewModel = viewModel()

        viewModel.onNoteChange("위염이라고 하셨어요")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `공백만 적으면 저장할 수 없다`() {
        val viewModel = viewModel()

        viewModel.onNoteChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `정리하는 동안은 저장을 막는다`() = runTest {
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onOrganizeClick()

        assertTrue(viewModel.uiState.value.organizing)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `정리가 끝나면 다듬은 문장이 들어온다`() = runTest {
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onOrganizeClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.organizing)
        assertEquals(PREVIEW_VISIT_NOTE, state.note)
        assertTrue(state.canSave)
    }

    @Test
    fun `정리 중에 다시 눌러도 한 번만 돈다`() = runTest {
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onOrganizeClick()
        viewModel.onOrganizeClick()
        advanceUntilIdle()

        assertEquals(PREVIEW_VISIT_NOTE, viewModel.uiState.value.note)
    }

    private fun viewModel() =
        VisitNoteViewModel(Clock.fixed(Instant.parse("2026-09-12T01:00:00Z"), ZoneId.of("Asia/Seoul")))
}
