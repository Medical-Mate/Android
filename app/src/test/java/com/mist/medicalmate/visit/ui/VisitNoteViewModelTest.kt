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
        val state = VisitNoteViewModel().uiState.value

        assertEquals(previewVisitHeadline, state.visit)
        assertEquals("", state.note)
        assertFalse(state.canSave)
    }

    @Test
    fun `적으면 저장할 수 있다`() {
        val viewModel = VisitNoteViewModel()

        viewModel.onNoteChange("위염이라고 하셨어요")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `공백만 적으면 저장할 수 없다`() {
        val viewModel = VisitNoteViewModel()

        viewModel.onNoteChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `정리하는 동안은 저장을 막는다`() = runTest {
        val viewModel = VisitNoteViewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onOrganizeClick()

        assertTrue(viewModel.uiState.value.organizing)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `정리가 끝나면 다듬은 문장이 들어온다`() = runTest {
        val viewModel = VisitNoteViewModel()
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
        val viewModel = VisitNoteViewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onOrganizeClick()
        viewModel.onOrganizeClick()
        advanceUntilIdle()

        assertEquals(PREVIEW_VISIT_NOTE, viewModel.uiState.value.note)
    }
}
