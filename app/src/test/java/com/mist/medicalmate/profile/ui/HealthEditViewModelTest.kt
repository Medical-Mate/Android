package com.mist.medicalmate.profile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthEditViewModelTest {
    @Test
    fun `처음에는 비어 있고 불러오면 저장된 값이 들어온다`() {
        val viewModel = HealthEditViewModel()

        assertEquals(emptySet<String>(), viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))

        viewModel.load()

        assertEquals(setOf("혈압약", "진통제"), viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))
        assertEquals(setOf("고혈압"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
        assertEquals(setOf("페니실린"), viewModel.uiState.value.chosenIn(ProfileSetupStep.ALLERGIES))
    }

    @Test
    fun `누르면 고르고 다시 누르면 뺀다`() {
        val viewModel = loaded()

        viewModel.onOptionClick(ProfileSetupStep.CONDITIONS, "당뇨")
        assertEquals(setOf("고혈압", "당뇨"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))

        viewModel.onOptionClick(ProfileSetupStep.CONDITIONS, "당뇨")
        assertEquals(setOf("고혈압"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
    }

    @Test
    fun `갈래마다 따로 담긴다`() {
        val viewModel = loaded()

        viewModel.onOptionClick(ProfileSetupStep.MEDICATIONS, "위장약")

        assertTrue("위장약" in viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))
        assertFalse("위장약" in viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
    }

    @Test
    fun `직접 추가를 누르면 그 갈래가 열린다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)

        assertEquals(ProfileSetupStep.ALLERGIES, viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `열린 갈래를 다시 누르면 닫힌다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)

        assertNull(viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `다른 갈래를 누르면 그쪽으로 옮겨가고 적던 것은 사라진다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onAddClick(ProfileSetupStep.MEDICATIONS)

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `추가하면 목록에 붙고 고른 것으로 표시된다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        val state = viewModel.uiState.value
        assertEquals(listOf("메밀"), state.extrasIn(ProfileSetupStep.ALLERGIES))
        assertTrue("메밀" in state.chosenIn(ProfileSetupStep.ALLERGIES))
        assertEquals("", state.draft)
    }

    @Test
    fun `앞뒤 공백은 잘라낸다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("  메밀  ")
        viewModel.onDraftSubmit()

        assertEquals(listOf("메밀"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    @Test
    fun `빈 값은 넣지 않는다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("   ")
        assertFalse(viewModel.uiState.value.canAddDraft)

        viewModel.onDraftSubmit()

        assertEquals(emptyList<String>(), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    @Test
    fun `같은 것을 두 번 추가하지 않는다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        assertEquals(listOf("메밀"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `열지 않고 추가를 눌러도 아무 일이 없다`() {
        val viewModel = loaded()

        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        assertEquals(emptyList<String>(), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    private fun loaded() = HealthEditViewModel().apply { load() }
}
