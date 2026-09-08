package com.mist.medicalmate.profile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSetupViewModelTest {
    @Test
    fun `처음은 복용약 단계다`() {
        val viewModel = ProfileSetupViewModel()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun `칩을 누르면 들어가고 다시 누르면 빠진다`() {
        val viewModel = ProfileSetupViewModel()

        viewModel.onOptionToggle("혈압약")
        assertEquals(setOf("혈압약"), viewModel.uiState.value.answer.chosen)

        viewModel.onOptionToggle("혈압약")
        assertEquals(emptySet<String>(), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `아무것도 고르지 않아도 다음 단계로 갈 수 있다`() {
        val viewModel = ProfileSetupViewModel()

        viewModel.onNext()

        assertEquals(ProfileSetupStep.CONDITIONS, viewModel.uiState.value.step)
        assertEquals(emptySet<String>(), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `단계를 넘어갔다 돌아와도 답이 남아 있다`() {
        val viewModel = ProfileSetupViewModel()
        viewModel.onOptionToggle("혈압약")

        viewModel.onNext()
        viewModel.onOptionToggle("고혈압")
        viewModel.onBack()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
        assertEquals(setOf("혈압약"), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `첫 단계에서 뒤로 가면 아무 일도 일어나지 않는다`() {
        val viewModel = ProfileSetupViewModel()

        viewModel.onBack()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
    }

    @Test
    fun `마지막 단계까지 가면 완료가 된다`() {
        val viewModel = ProfileSetupViewModel()

        viewModel.onNext()
        viewModel.onNext()
        assertEquals(ProfileSetupStep.ALLERGIES, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.completed)

        viewModel.onNext()

        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `단계 번호는 1부터 시작하고 마지막만 마지막이다`() {
        assertEquals(1, ProfileSetupStep.MEDICATIONS.number)
        assertEquals(3, ProfileSetupStep.ALLERGIES.number)
        assertEquals(3, ProfileSetupStep.total)
        assertFalse(ProfileSetupStep.MEDICATIONS.isLast)
        assertTrue(ProfileSetupStep.ALLERGIES.isLast)
    }
}
