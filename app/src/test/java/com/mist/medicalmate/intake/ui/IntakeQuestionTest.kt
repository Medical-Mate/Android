package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.model.IntakeStep
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

/** 문답이 끝난 자리의 추천 질문 고르기(1c-D~D4, #310). 대역과 헬퍼는 [IntakeViewModelTest] 파일에 있다. */
@OptIn(ExperimentalCoroutinesApi::class)
class IntakeQuestionTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `문답이 끝난 턴의 추천이 질문 줄로 선다`() {
        // 추천 질문 고르기가 따로 단계가 아니라 문답 끝에 이어진다(#310). 처음에는 아무것도 체크하지 않는다.
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()

        viewModel.finishChat()

        val state = viewModel.uiState.value
        assertTrue(state.chatFinished)
        assertEquals(threeCandidates, state.questionRows.map { it.text })
        assertTrue(state.questionRows.none { it.selected || it.custom })
        assertEquals(emptyList<String>(), state.questions)
    }

    @Test
    fun `끝난 뒤에 또 보내도 체크가 사라지지 않는다`() {
        // 끝난 문답에 또 보내도 서버는 같은 후보를 싣는다. 덮으면 그 사이의 체크가 사라진다.
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()
        viewModel.finishChat()
        viewModel.question.onToggle(0)

        viewModel.finishChat()

        assertEquals(listOf("검사를 받아야 하나요?"), viewModel.uiState.value.questions)
    }

    @Test
    fun `추천을 눌러 담고 다시 눌러 뺀다`() {
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()
        viewModel.finishChat()

        viewModel.question.onToggle(2)
        viewModel.question.onToggle(0)

        // 누른 차례가 아니라 화면 차례다. 진료실에서 위에서부터 묻는다.
        assertEquals(listOf("검사를 받아야 하나요?", "언제 다시 와야 하나요?"), viewModel.uiState.value.questions)

        viewModel.question.onToggle(0)

        assertEquals(listOf("언제 다시 와야 하나요?"), viewModel.uiState.value.questions)
    }

    @Test
    fun `빈 칸에 적은 질문은 체크된 채 맨 아래에 붙는다`() {
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()
        viewModel.finishChat()

        viewModel.question.onAddClick()
        assertTrue(viewModel.uiState.value.writingQuestion)
        viewModel.onDraftChange("공복으로 가야 하나요?")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(IntakeQuestion("공복으로 가야 하나요?", selected = true, custom = true), state.questionRows.last())
        assertFalse(state.writingQuestion)
        assertEquals("", state.draft)
    }

    @Test
    fun `질문을 적는 중에 보내면 문답으로 보내지 않는다`() {
        // 문답은 끝났다. 빈 칸에 적은 글이 AI에게 답으로 가면 안 된다.
        val repository = FakeSessionRepository(ends = true, candidates = threeCandidates)
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.finishChat()

        viewModel.question.onAddClick()
        viewModel.onDraftChange("공복으로 가야 하나요?")
        viewModel.onSend()

        assertEquals("그게 다예요", repository.sentText)
    }

    @Test
    fun `질문을 적다가 뒤로 가면 적던 것만 거둔다`() {
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()
        viewModel.finishChat()
        viewModel.question.onAddClick()
        viewModel.onDraftChange("공복으로")

        viewModel.onBack()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertFalse(state.writingQuestion)
        assertEquals("", state.draft)
        assertEquals(3, state.questionRows.size)
    }

    @Test
    fun `빈 질문은 붙지 않고 없는 줄을 눌러도 그대로다`() {
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = threeCandidates))
        viewModel.openChatStep()
        viewModel.finishChat()

        viewModel.question.onAddClick()
        viewModel.onDraftChange("  ")
        viewModel.onSend()
        viewModel.question.onToggle(7)

        assertEquals(3, viewModel.uiState.value.questionRows.size)
        assertEquals(emptyList<String>(), viewModel.uiState.value.questions)
    }

    @Test
    fun `질문은 다섯 개까지만 담기고 차면 빈 칸이 열리지 않는다`() {
        // 서버 상한과 같다(Backend#132). 넘치는 목록을 보내면 거절되고 카드에 질문이 하나도 안 남았다(#264).
        val six = (1..6).map { "질문 $it" }
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true, candidates = six))
        viewModel.openChatStep()
        viewModel.finishChat()

        repeat(6) { viewModel.question.onToggle(it) }
        viewModel.question.onAddClick()

        val state = viewModel.uiState.value
        assertEquals(5, state.questions.size)
        assertFalse(state.questionRows[5].selected)
        assertTrue(state.questionsFull)
        assertFalse(state.writingQuestion)

        // 푸는 것은 된다. 그러면 다시 담을 수 있다.
        viewModel.question.onToggle(0)
        viewModel.question.onToggle(5)

        assertEquals(listOf("질문 2", "질문 3", "질문 4", "질문 5", "질문 6"), viewModel.uiState.value.questions)
    }

    @Test
    fun `문답을 마칠 때 체크한 질문만 화면 차례로 보낸다`() {
        val repository = FakeSessionRepository(ends = true, candidates = threeCandidates)
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.finishChat()
        viewModel.question.onToggle(1)
        viewModel.question.onAddClick()
        viewModel.onDraftChange("공복으로 가야 하나요?")
        viewModel.onSend()

        viewModel.onNext("")

        assertEquals(listOf("진통제 계속 먹어도 되나요?", "공복으로 가야 하나요?"), repository.sentQuestions)
    }
}
