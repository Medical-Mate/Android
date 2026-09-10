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
    fun `앵커를 짚는 것은 확대일 뿐 고른 것이 아니다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:004@CENTER")

        val state = viewModel.uiState.value
        assertTrue(state.bodyMap.pickingZone)
        assertEquals(null, state.bodyMap.selection)
        assertFalse(state.canLeaveBodyPart)
        assertEquals(IntakeStep.BODY_PART, state.step)
    }

    @Test
    fun `구역을 고른 뒤에도 확대 화면에 머문다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:014@LEFT")

        viewModel.bodyMap.onDotClick("SUR:091@LEFT")

        val bodyMap = viewModel.uiState.value.bodyMap
        // 고른 점과 이름을 볼 수 있어야 한다. 앵커 화면으로 튕기면 둘 다 못 본다.
        assertTrue(bodyMap.pickingZone)
        assertEquals("ANC:014", bodyMap.focus?.anchorId)
        assertEquals("SUR:091", bodyMap.selection?.zoneId)
    }

    @Test
    fun `확대한 앵커의 좌우가 고른 구역의 좌우에 덮이지 않는다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:001@CENTER")

        viewModel.bodyMap.onDotClick("SUR:002@LEFT")

        val bodyMap = viewModel.uiState.value.bodyMap
        // 제목이 "왼쪽 머리 어디가 아프세요?"가 되면 안 된다
        assertEquals("머리", bodyMap.focus?.title())
        assertEquals("왼쪽 눈", bodyMap.selection?.title())
    }

    @Test
    fun `고른 구역은 확대 화면과 목록에서 같은 값으로 표시된다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")

        val bodyMap = viewModel.uiState.value.bodyMap
        val anchor = requireNotNull(bodyMap.anchor)
        val focus = requireNotNull(bodyMap.focus)

        val litDot = bodyMapZoneDots(anchor, focus.side, bodyMap.selection).single { it.selected }
        val checkedRow = bodyMapZoneChoices(anchor, focus.side).single { it == bodyMap.selection }

        assertEquals(litDot.label, checkedRow.title())
    }

    @Test
    fun `다른 구역을 누르면 앞서 고른 것을 대신한다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")

        viewModel.bodyMap.onDotClick("SUR:097@LEFT")

        assertEquals("SUR:097", viewModel.uiState.value.bodyMap.selection?.zoneId)
    }

    @Test
    fun `확대를 닫아도 고른 부위는 남는다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")

        viewModel.bodyMap.onFocusClear()

        val state = viewModel.uiState.value
        assertFalse(state.bodyMap.pickingZone)
        assertEquals("SUR:091", state.bodyMap.selection?.zoneId)
        assertTrue(state.canLeaveBodyPart)
    }

    @Test
    fun `목록에서 앵커 줄을 누르면 확대만 되고 골라지지 않는다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onAnchorFocus(BodyMapSelection("ANC:013", side = BodyMapSide.RIGHT))

        val bodyMap = viewModel.uiState.value.bodyMap
        assertTrue(bodyMap.pickingZone)
        assertEquals(null, bodyMap.selection)
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

        viewModel.bodyMap.onSideAnchorSelect("ANC:011")

        assertTrue(viewModel.uiState.value.canLeaveBodyPart)

        viewModel.onNext()

        assertEquals("피부", viewModel.uiState.value.bodyPart)
    }

    @Test
    fun `구역 단계에서 뒤로 가면 앵커 화면으로 돌아간다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:001@CENTER")

        viewModel.onBack()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.BODY_PART, state.step)
        assertEquals(null, state.bodyMap.focus)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `앞뒤를 바꿔도 고른 부위는 남는다`() {
        val viewModel = IntakeViewModel()
        viewModel.bodyMap.onSideAnchorSelect("ANC:010")

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
    fun `부위 이름에 받침이 있으면 조사가 이가 된다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")
        viewModel.onNext()

        // 물음이 "왼쪽 무릎가 얼마나"가 되지 않아야 한다
        assertEquals("왼쪽 무릎이", viewModel.uiState.value.bodyPartSubject)
    }

    @Test
    fun `받침이 없으면 조사가 가가 된다`() {
        val viewModel = IntakeViewModel()

        viewModel.bodyMap.onPartSelect(BodyMapSelection("ANC:001", "SUR:004"))
        viewModel.onNext()

        assertEquals("코가", viewModel.uiState.value.bodyPartSubject)
        assertTrue(viewModel.uiState.value.messages.first().text.startsWith("코가 불편하시군요"))
    }

    @Test
    fun `괄호로 끝나는 이름은 마지막 한글 음절로 조사를 정한다`() {
        assertEquals("가슴 옆(갈비)가", withSubjectParticle("가슴 옆(갈비)"))
        assertEquals("윗배(명치)가", withSubjectParticle("윗배(명치)"))
        assertEquals("목 안(목구멍)이", withSubjectParticle("목 안(목구멍)"))
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
