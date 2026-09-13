package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.speech.SpeechChunk
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionMessage
import com.mist.medicalmate.intake.data.IntakeSessionStatus
import com.mist.medicalmate.intake.data.IntakeTurn
import com.mist.medicalmate.intake.data.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 세션 저장소를 세운 ViewModel.
 *
 * 세션 만들기는 여기서 늘 성공한다. 실패해도 문답이 그대로 진행된다는 것은
 * [IntakeSessionActionsTest]가 본다.
 */
private fun intakeViewModel(
    repository: SessionRepository = FakeSessionRepository(),
    speech: FakeSpeechToText = FakeSpeechToText(),
) = IntakeViewModel(repository, speech)

/** 부른 것을 기록만 한다. 시험마다 무엇을 보냈는지 확인할 수 있다. */
private class FakeSessionRepository(
    private val session: IntakeSession = savedSession,
    private val startFails: Boolean = false,
    ends: Boolean = false,
) : SessionRepository {
    var startedCodes: List<String>? = null
    var startedText: String? = null

    override suspend fun start(siteCodes: List<String>, siteText: String?): ApiResult<IntakeSession> {
        startedCodes = siteCodes
        startedText = siteText
        return if (startFails) ApiResult.NetworkUnavailable(java.io.IOException()) else ApiResult.Success(session)
    }

    override suspend fun load(sessionId: Long): ApiResult<IntakeSession> = ApiResult.Success(session)

    var sentText: String? = null
    var sentByVoice: Boolean = false
    var severityLevel: Int? = null
    var severityLabel: String? = null
    var sentQuestions: List<String>? = null
    var turnEnded: Boolean = ends

    override suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn> {
        sentText = text
        sentByVoice = byVoice
        return ApiResult.Success(
            IntakeTurn(
                ended = turnEnded,
                messages = savedSession.messages + IntakeSessionMessage(99, true, text) +
                    IntakeSessionMessage(100, false, "다음 질문"),
                answered = 1,
                total = 4,
            ),
        )
    }

    override suspend fun setSeverity(sessionId: Long, level: Int, label: String): ApiResult<IntakeSession> {
        severityLevel = level
        severityLabel = label
        return ApiResult.Success(savedSession)
    }

    override suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession> {
        sentQuestions = questions
        return ApiResult.Success(savedSession)
    }
}

private val savedSession =
    IntakeSession(
        id = 7,
        status = IntakeSessionStatus.IN_PROGRESS,
        siteCodes = listOf("SUR:031"),
        siteText = "명치",
        answered = 1,
        total = 4,
        messages = emptyList(),
    )

/**
 * 부위를 고르지 않으면 문답이 열리지 않는다. 문답 이후를 보는 시험은 이 헬퍼로 1단계를
 * 지난다. 어느 부위를 고르는지는 그 시험들과 무관하다.
 */
private fun IntakeViewModel.openChatStep() {
    bodyMap.onDotClick("ANC:004@CENTER")
    bodyMap.onDotClick("SUR:031@RIGHT")
    onNext("꽤 아파요")
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
        val viewModel = intakeViewModel()

        assertEquals(IntakeStep.BODY_PART, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun `부위를 고르지 않으면 다음으로 가지 않는다`() {
        val viewModel = intakeViewModel()

        viewModel.onNext("꽤 아파요")

        assertEquals(IntakeStep.BODY_PART, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canLeaveBodyPart)
    }

    @Test
    fun `앵커를 짚는 것은 확대일 뿐 고른 것이 아니다`() {
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:004@CENTER")

        val state = viewModel.uiState.value
        assertTrue(state.bodyMap.pickingZone)
        assertEquals(null, state.bodyMap.selection)
        assertFalse(state.canLeaveBodyPart)
        assertEquals(IntakeStep.BODY_PART, state.step)
    }

    @Test
    fun `구역을 고른 뒤에도 확대 화면에 머문다`() {
        val viewModel = intakeViewModel()
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
        val viewModel = intakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:001@CENTER")

        viewModel.bodyMap.onDotClick("SUR:002@LEFT")

        val bodyMap = viewModel.uiState.value.bodyMap
        // 제목이 "왼쪽 머리 어디가 아프세요?"가 되면 안 된다
        assertEquals("머리", bodyMap.focus?.title())
        assertEquals("왼쪽 눈", bodyMap.selection?.title())
    }

    @Test
    fun `고른 구역은 확대 화면과 목록에서 같은 값으로 표시된다`() {
        val viewModel = intakeViewModel()
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
        val viewModel = intakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")

        viewModel.bodyMap.onDotClick("SUR:097@LEFT")

        assertEquals("SUR:097", viewModel.uiState.value.bodyMap.selection?.zoneId)
    }

    @Test
    fun `확대를 닫아도 고른 부위는 남는다`() {
        val viewModel = intakeViewModel()
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
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onAnchorFocus(BodyMapSelection("ANC:013", side = BodyMapSide.RIGHT))

        val bodyMap = viewModel.uiState.value.bodyMap
        assertTrue(bodyMap.pickingZone)
        assertEquals(null, bodyMap.selection)
    }

    @Test
    fun `구역까지 고르면 부위가 정해지고 첫 마디에 그 이름이 들어간다`() {
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")
        viewModel.onNext("꽤 아파요")

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertEquals("왼쪽 무릎", state.bodyPart)
        assertEquals(1, state.messages.size)
        assertEquals(IntakeMessage.Sender.AI, state.messages.first().sender)
        assertTrue(state.messages.first().text.startsWith("왼쪽 무릎이 불편하시군요"))
    }

    @Test
    fun `전신과 피부는 구역이 없어 칩만 누르면 정해진다`() {
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onSideAnchorSelect("ANC:011")

        assertTrue(viewModel.uiState.value.canLeaveBodyPart)

        viewModel.onNext("꽤 아파요")

        assertEquals("피부", viewModel.uiState.value.bodyPart)
    }

    @Test
    fun `구역 단계에서 뒤로 가면 앵커 화면으로 돌아간다`() {
        val viewModel = intakeViewModel()
        viewModel.bodyMap.onDotClick("ANC:001@CENTER")

        viewModel.onBack()

        val state = viewModel.uiState.value
        assertEquals(IntakeStep.BODY_PART, state.step)
        assertEquals(null, state.bodyMap.focus)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `앞뒤를 바꿔도 고른 부위는 남는다`() {
        val viewModel = intakeViewModel()
        viewModel.bodyMap.onSideAnchorSelect("ANC:010")

        viewModel.bodyMap.onViewChange(BodyMapView.BACK)

        val state = viewModel.uiState.value
        assertEquals(BodyMapView.BACK, state.bodyMap.view)
        assertEquals("ANC:010", state.bodyMap.selection?.anchorId)
    }

    @Test
    fun `목록과 인체도를 오가도 고른 부위는 남는다`() {
        val viewModel = intakeViewModel()
        viewModel.bodyMap.onPartSelect(BodyMapSelection("ANC:003", "SUR:022", BodyMapSide.RIGHT))

        viewModel.bodyMap.onListModeToggle()

        assertTrue(viewModel.uiState.value.bodyMap.byList)
        assertEquals("SUR:022", viewModel.uiState.value.bodyMap.selection?.zoneId)

        viewModel.bodyMap.onListModeToggle()

        assertFalse(viewModel.uiState.value.bodyMap.byList)
    }

    @Test
    fun `보내면 환자 마디가 먼저 붙고 입력이 비워진다`() = runTest {
        // 서버를 기다렸다 한꺼번에 그리면 방금 누른 것이 사라진 것처럼 보인다.
        val viewModel = intakeViewModel()
        viewModel.openChatStep()

        viewModel.onDraftChange("한 3주쯤 됐어요")
        viewModel.onSend()

        assertEquals("", viewModel.uiState.value.draft)
        assertFalse(viewModel.uiState.value.awaitingReply)
        assertEquals(IntakeMessage.Sender.AI, viewModel.uiState.value.messages.last().sender)
    }

    @Test
    fun `빈 글은 보내지지 않는다`() {
        val viewModel = intakeViewModel()
        viewModel.openChatStep()

        viewModel.onDraftChange("   ")
        viewModel.onSend()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.awaitingReply)
    }

    @Test
    fun `서버가 끝났다고 하면 문답이 끝난 것으로 표시된다`() = runTest {
        // 끝을 앱이 세지 않는다. 서버가 `ended`로 알린다.
        val viewModel = intakeViewModel(FakeSessionRepository(ends = true))
        viewModel.openChatStep()

        viewModel.onDraftChange("답")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.chatFinished)
    }

    @Test
    fun `음성으로 바꾸면 바로 듣기 시작한다`() {
        // 누른 사람은 패널이 뜨자마자 말한다. 한 번 더 눌러야 듣기 시작하면 그 사이에 한 말이
        // 사라진다. 아직 아무것도 못 들었을 뿐이라 문구는 "말씀해 주세요"다.
        val viewModel = intakeViewModel(speech = FakeSpeechToText(keepOpen = true))

        viewModel.onVoiceMode()

        assertEquals(IntakeInputMode.VOICE, viewModel.uiState.value.inputMode)
        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `말소리가 들어오면 듣고 있어요가 된다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Partial("배가")), keepOpen = true)
        val viewModel = intakeViewModel(speech = speech)

        viewModel.onVoiceMode()

        assertEquals(MedicalMateVoiceState.LISTENING, viewModel.uiState.value.voice)
    }

    @Test
    fun `패널의 마이크를 누르면 멈춘다`() {
        // 다 말했을 때 누르는 자리다.
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Partial("배가")), keepOpen = true)
        val viewModel = intakeViewModel(speech = speech)
        viewModel.onVoiceMode()

        viewModel.onMicClick()

        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `받아쓴 글이 입력칸에 들어간다`() {
        // 보내는 것은 환자가 한다. 말이 끝나자마자 나가면 잘못 알아들은 것을 고칠 자리가 없다.
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Final("배가 아파요")), keepOpen = true)
        val viewModel = intakeViewModel(speech = speech)

        viewModel.onVoiceMode()

        assertEquals("배가 아파요", viewModel.uiState.value.draft)
    }

    @Test
    fun `부분 결과는 앞선 것을 갈아끼운다`() {
        // 말하는 중에 계속 갱신되는 값이다. 이어 붙이면 같은 말이 여러 번 쌓인다.
        val speech =
            FakeSpeechToText(
                chunks = listOf(SpeechChunk.Partial("배가"), SpeechChunk.Partial("배가 아파요")),
                keepOpen = true,
            )
        val viewModel = intakeViewModel(speech = speech)

        viewModel.onVoiceMode()

        assertEquals("배가 아파요", viewModel.uiState.value.draft)
    }

    @Test
    fun `적던 글 뒤에 붙는다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Final("아파요")), keepOpen = true)
        val viewModel = intakeViewModel(speech = speech)
        viewModel.onDraftChange("배가 ")

        viewModel.onVoiceMode()

        assertEquals("배가 아파요", viewModel.uiState.value.draft)
    }

    @Test
    fun `못 알아들어도 적던 글은 그대로다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Failed))
        val viewModel = intakeViewModel(speech = speech)
        viewModel.onDraftChange("배가 아파요")

        viewModel.onVoiceMode()

        assertEquals("배가 아파요", viewModel.uiState.value.draft)
        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `마이크 권한을 거부하면 왜 안 되는지 적는다`() {
        val viewModel = intakeViewModel()

        viewModel.onMicDenied()

        assertEquals(MedicalMateVoiceState.DENIED, viewModel.uiState.value.voice)
    }

    @Test
    fun `쓸 수 없는 기기에서는 마이크를 그리지 않는다`() {
        val viewModel = intakeViewModel(speech = FakeSpeechToText(usable = false))

        viewModel.checkVoice()

        assertFalse(viewModel.uiState.value.voiceAvailable)
    }

    @Test
    fun `쓸 수 있으면 마이크를 그린다`() {
        val viewModel = intakeViewModel(speech = FakeSpeechToText(usable = true))

        viewModel.checkVoice()

        assertTrue(viewModel.uiState.value.voiceAvailable)
    }

    @Test
    fun `입력 방식을 바꾸면 음성은 대기로 돌아간다`() {
        val viewModel = intakeViewModel()
        viewModel.onInputModeChange(IntakeInputMode.VOICE)
        viewModel.onMicClick()

        viewModel.onInputModeChange(IntakeInputMode.TEXT)

        assertEquals(IntakeInputMode.TEXT, viewModel.uiState.value.inputMode)
        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `부위 이름에 받침이 있으면 조사가 이가 된다`() {
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onDotClick("ANC:014@LEFT")
        viewModel.bodyMap.onDotClick("SUR:091@LEFT")
        viewModel.onNext("꽤 아파요")

        // 물음이 "왼쪽 무릎가 얼마나"가 되지 않아야 한다
        assertEquals("왼쪽 무릎이", viewModel.uiState.value.bodyPartSubject)
    }

    @Test
    fun `받침이 없으면 조사가 가가 된다`() {
        val viewModel = intakeViewModel()

        viewModel.bodyMap.onPartSelect(BodyMapSelection("ANC:001", "SUR:004"))
        viewModel.onNext("꽤 아파요")

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
        val viewModel = intakeViewModel()

        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_5)

        assertEquals(MedicalMateSeverity.LEVEL_5, viewModel.uiState.value.severity)
    }

    @Test
    fun `질문을 적어 넣고 지운다`() {
        val viewModel = intakeViewModel()

        viewModel.question.onDraftChange("검사를 받아야 하나요?")
        viewModel.question.onAdd()
        viewModel.question.onDraftChange("진통제를 계속 먹어도 되나요?")
        viewModel.question.onAdd()

        assertEquals(2, viewModel.uiState.value.questions.size)
        assertEquals("", viewModel.uiState.value.questionDraft)

        viewModel.question.onRemove(0)

        assertEquals(listOf("진통제를 계속 먹어도 되나요?"), viewModel.uiState.value.questions)
    }

    @Test
    fun `빈 질문은 들어가지 않고 없는 자리를 지워도 그대로다`() {
        val viewModel = intakeViewModel()

        viewModel.question.onDraftChange("  ")
        viewModel.question.onAdd()
        viewModel.question.onRemove(3)

        assertEquals(emptyList<String>(), viewModel.uiState.value.questions)
    }

    @Test
    fun `네 단계를 지나면 흐름이 끝난다`() {
        val viewModel = intakeViewModel()

        viewModel.openChatStep()
        viewModel.onNext("꽤 아파요")
        assertEquals(IntakeStep.SEVERITY, viewModel.uiState.value.step)
        viewModel.onNext("꽤 아파요")
        assertEquals(IntakeStep.QUESTIONS, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.completed)

        viewModel.onNext("꽤 아파요")

        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `뒤로 가면 앞 단계로 돌아가고 답이 남아 있다`() {
        val viewModel = intakeViewModel()
        viewModel.openChatStep()
        viewModel.onNext("꽤 아파요")
        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_2)

        viewModel.onBack()

        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)
        assertEquals(MedicalMateSeverity.LEVEL_2, viewModel.uiState.value.severity)
    }

    @Test
    fun `보낸 말을 서버에 넘긴다`() {
        val repository = FakeSessionRepository()
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onDraftChange("3주 전부터요")

        viewModel.onSend()

        assertEquals("3주 전부터요", repository.sentText)
        assertFalse(repository.sentByVoice)
    }

    @Test
    fun `음성으로 말했으면 그 사실만 함께 보낸다`() {
        // 오디오는 보내지 않는다. 녹음은 서버에 저장되지 않는다.
        val repository = FakeSessionRepository()
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onInputModeChange(IntakeInputMode.VOICE)
        viewModel.onDraftChange("식후에 쓰려요")

        viewModel.onSend()

        assertTrue(repository.sentByVoice)
    }

    @Test
    fun `응답이 준 대화 전체로 갈아 끼운다`() {
        // 우리가 붙인 줄과 서버가 센 줄이 어긋나면 안 된다.
        val repository = FakeSessionRepository()
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onDraftChange("3주 전부터요")

        viewModel.onSend()

        assertEquals("다음 질문", viewModel.uiState.value.messages.last().text)
        assertFalse(viewModel.uiState.value.awaitingReply)
    }

    @Test
    fun `세션이 없으면 보내지 않는다`() {
        // 세션 만들기가 실패한 경우다. 보낼 곳이 없다.
        val repository = FakeSessionRepository(startFails = true)
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onDraftChange("3주 전부터요")

        viewModel.onSend()

        assertNull(repository.sentText)
    }

    @Test
    fun `강도 단계를 떠날 때 고른 값과 문구를 보낸다`() {
        // 서버가 카피를 들고 있지 않다. 표시 문구는 앱이 보낸다.
        val repository = FakeSessionRepository()
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onNext("꽤 아파요")
        viewModel.onSeverityChange(MedicalMateSeverity.LEVEL_2)

        viewModel.onNext("조금 아파요")

        assertEquals(2, repository.severityLevel)
        assertEquals("조금 아파요", repository.severityLabel)
    }

    @Test
    fun `질문 단계를 떠날 때 목록을 통째로 보낸다`() {
        val repository = FakeSessionRepository()
        val viewModel = intakeViewModel(repository)
        viewModel.openChatStep()
        viewModel.onNext("꽤 아파요")
        viewModel.onNext("꽤 아파요")
        viewModel.question.onDraftChange("검사를 받아야 하나요?")
        viewModel.question.onAdd()

        viewModel.onNext("꽤 아파요")

        assertEquals(listOf("검사를 받아야 하나요?"), repository.sentQuestions)
    }
}
