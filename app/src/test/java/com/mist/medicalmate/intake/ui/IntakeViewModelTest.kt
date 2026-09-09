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

/**
 * 부위를 고르지 않으면 문답이 열리지 않는다. 문답 이후를 보는 시험은 이 헬퍼로 1단계를
 * 지난다. 어느 부위를 고르는지는 그 시험들과 무관하다.
 */
private fun IntakeViewModel.openChatStep() {
    bodyMap.onDotClick("ANC:004@CENTER")
    bodyMap.onDotClick("SUR:031@RIGHT")
    onNext()
}

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
    fun `부위를 고르지 않으면 다음으로 가지 않는다`() {
        val viewModel = IntakeViewModel()

        viewModel.onNext()

        assertEquals(IntakeStep.BODY_PART, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canLeaveBodyPart)
    }

    @Test
    fun `앵커만 짚으면 구역이 남아 아직 넘어갈 수 없다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:004@CENTER")

        val state = viewModel.uiState.value
        assertTrue(state.bodyMap.pickingZone)
        assertFalse(state.canLeaveBodyPart)
        assertEquals(IntakeStep.BODY_PART, state.step)
    }

    @Test
    fun `구역까지 고르면 부위가 정해지고 첫 마디에 그 이름이 들어간다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")
        viewModel.onNext()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertEquals("왼쪽 무릎", state.bodyPart)
        assertEquals(1, state.messages.size)
        assertEquals(IntakeMessage.Sender.AI, state.messages.first().sender)
        assertTrue(state.messages.first().text.startsWith("왼쪽 무릎이 불편하시군요"))
    }

    @Test
    fun `전신과 피부는 구역이 없어 칩만 누르면 정해진다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onSideAnchorClick("ANC:011")

        assertTrue(viewModel.uiState.value.canLeaveBodyPart)

        viewModel.onNext()

        assertEquals("피부", viewModel.uiState.value.bodyPart)
    }

    @Test
    fun `구역 단계에서 뒤로 가면 앵커 선택으로 돌아간다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:001@CENTER")

        viewModel.onBack()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.BODY_PART, state.step)
        assertEquals(null, state.bodyMap.selection)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `앞뒤를 바꿔도 고른 부위는 남는다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onSideAnchorClick("ANC:010")

        viewModel.bodyMap.onViewChange(BodyMapView.BACK)

        val state = viewModel.uiState.value
        assertEquals(BodyMapView.BACK, state.bodyMap.view)
        assertEquals("ANC:010", state.bodyMap.selection?.anchorId)
    }

    @Test
    fun `목록과 인체도를 오가도 고른 부위는 남는다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onPartSelect(BodyMapSelection("ANC:003", "SUR:022", BodyMapSide.RIGHT))

        viewModel.bodyMap.onListModeToggle()

        assertTrue(viewModel.uiState.value.bodyMap.byList)
        assertEquals("SUR:022", viewModel.uiState.value.bodyMap.selection?.zoneId)

        viewModel.bodyMap.onListModeToggle()

        assertFalse(viewModel.uiState.value.bodyMap.byList)
    }

    @Test
    fun `보내면 환자 마디가 붙고 기다린 뒤 AI가 답한다`() = runTest {
        val viewModel = IntakeViewModel()
        viewModel.openChatStep()

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
        viewModel.openChatStep()

        viewModel.onDraftChange("   ")
        viewModel.onSend()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.awaitingReply)
    }

    @Test
    fun `물어볼 것이 남지 않으면 문답이 끝난 것으로 표시된다`() = runTest {
        val viewModel = IntakeViewModel()
        viewModel.openChatStep()

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

        viewModel.openChatStep()
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
        viewModel.openChatStep()
        viewModel.onNext()
        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_2)

        viewModel.onBack()

        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)
        assertEquals(MedicalMateSeverity.LEVEL_2, viewModel.uiState.value.severity)
    }
}
