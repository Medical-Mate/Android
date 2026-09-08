package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
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
class IntakeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음은 아픈 부위 단계다`() {
        val viewModel = IntakeViewModel()

        assertEquals(IntakeStep.BODY_PART, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun `아픈 부위를 지나면 부위가 정해지고 첫 마디가 붙는다`() {
        val viewModel = IntakeViewModel()

        viewModel.onNext()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertEquals("복부", state.bodyPart)
        assertEquals(1, state.messages.size)
        assertEquals(IntakeMessage.Sender.AI, state.messages.first().sender)
    }

    @Test
    fun `보내면 환자 마디가 붙고 기다린 뒤 AI가 답한다`() = runTest {
        val viewModel = IntakeViewModel()
        viewModel.onNext()

        viewModel.onDraftChange("한 3주쯤 됐어요")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.awaitingReply)
        assertEquals("", viewModel.uiState.value.draft)
        assertEquals(IntakeMessage.Sender.PATIENT, viewModel.uiState.value.messages.last().sender)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.awaitingReply)
        assertEquals(3, state.messages.size)
        assertEquals(IntakeMessage.Sender.AI, state.messages.last().sender)
    }

    @Test
    fun `빈 글은 보내지지 않는다`() {
        val viewModel = IntakeViewModel()
        viewModel.onNext()

        viewModel.onDraftChange("   ")
        viewModel.onSend()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.awaitingReply)
    }

    @Test
    fun `물어볼 것이 남지 않으면 문답이 끝난 것으로 표시된다`() = runTest {
        val viewModel = IntakeViewModel()
        viewModel.onNext()

        repeat(4) {
            viewModel.onDraftChange("답")
            viewModel.onSend()
            advanceUntilIdle()
        }

        assertTrue(viewModel.uiState.value.chatFinished)
    }

    @Test
    fun `마이크는 대기와 듣는 중을 오간다`() {
        val viewModel = IntakeViewModel()

        viewModel.onMicClick()
        assertEquals(MedicalMateVoiceState.LISTENING, viewModel.uiState.value.voice)

        viewModel.onMicClick()
        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `입력 방식을 바꾸면 음성은 대기로 돌아간다`() {
        val viewModel = IntakeViewModel()
        viewModel.onInputModeChange(IntakeInputMode.VOICE)
        viewModel.onMicClick()

        viewModel.onInputModeChange(IntakeInputMode.TEXT)

        assertEquals(IntakeInputMode.TEXT, viewModel.uiState.value.inputMode)
        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `통증 강도를 고른다`() {
        val viewModel = IntakeViewModel()

        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_5)

        assertEquals(MedicalMateSeverity.LEVEL_5, viewModel.uiState.value.severity)
    }

    @Test
    fun `질문을 적어 넣고 지운다`() {
        val viewModel = IntakeViewModel()

        viewModel.onQuestionDraftChange("검사를 받아야 하나요?")
        viewModel.onAddQuestion()
        viewModel.onQuestionDraftChange("진통제를 계속 먹어도 되나요?")
        viewModel.onAddQuestion()

        assertEquals(2, viewModel.uiState.value.questions.size)
        assertEquals("", viewModel.uiState.value.questionDraft)

        viewModel.onRemoveQuestion(0)

        assertEquals(listOf("진통제를 계속 먹어도 되나요?"), viewModel.uiState.value.questions)
    }

    @Test
    fun `빈 질문은 들어가지 않고 없는 자리를 지워도 그대로다`() {
        val viewModel = IntakeViewModel()

        viewModel.onQuestionDraftChange("  ")
        viewModel.onAddQuestion()
        viewModel.onRemoveQuestion(3)

        assertEquals(emptyList<String>(), viewModel.uiState.value.questions)
    }

    @Test
    fun `네 단계를 지나면 흐름이 끝난다`() {
        val viewModel = IntakeViewModel()

        viewModel.onNext()
        viewModel.onNext()
        assertEquals(IntakeStep.SEVERITY, viewModel.uiState.value.step)
        viewModel.onNext()
        assertEquals(IntakeStep.QUESTIONS, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.completed)

        viewModel.onNext()

        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `뒤로 가면 앞 단계로 돌아가고 답이 남아 있다`() {
        val viewModel = IntakeViewModel()
        viewModel.onNext()
        viewModel.onNext()
        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_2)

        viewModel.onBack()

        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)
        assertEquals(MedicalMateSeverity.LEVEL_2, viewModel.uiState.value.severity)
    }
}
