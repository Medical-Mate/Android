package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.model.MAX_BRIEF_CARD_QUESTIONS
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionMessage
import com.mist.medicalmate.intake.data.IntakeSessionStatus
import com.mist.medicalmate.intake.data.IntakeSide
import com.mist.medicalmate.intake.data.IntakeTurn
import com.mist.medicalmate.intake.data.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 증상 정리를 말로 하는 것(#284). */
@OptIn(ExperimentalCoroutinesApi::class)
class IntakeVoiceTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 말 풀이

    @Test
    fun `좌우와 부위를 말하면 그 자리를 고른다`() {
        val pick = pickBodyPartByVoice("오른쪽 어깨가 아파요", null) as BodyVoicePick.Picked

        assertEquals("SUR:051", pick.selection.zoneId)
        assertEquals(BodyMapSide.RIGHT, pick.selection.side)
    }

    @Test
    fun `좌우가 갈리는 부위를 좌우 없이 말하면 어느 쪽인지 묻는다`() {
        val ask = pickBodyPartByVoice("어깨가 결려요", null) as BodyVoicePick.AskSide

        assertEquals(setOf(BodyMapSide.LEFT, BodyMapSide.RIGHT), ask.choices.map { it.side }.toSet())
    }

    @Test
    fun `큰 부위만 말하면 그 부위로 들어간다`() {
        val focus = pickBodyPartByVoice("왼쪽 다리가 아파요", null) as BodyVoicePick.Focus
        assertEquals(BodyMapSelection("ANC:014", side = BodyMapSide.LEFT), focus.anchor)

        assertTrue(pickBodyPartByVoice("머리가 아파요", null) is BodyVoicePick.Focus)
        // 다리를 짚어 둔 채 "무릎"이면 그 다리의 무릎이다.
        val knee = pickBodyPartByVoice("무릎", focus.anchor) as BodyVoicePick.Picked
        assertEquals(BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT), knee.selection)
    }

    @Test
    fun `별칭과 구역 없는 부위도 찾고 부위가 아닌 말은 고르지 않는다`() {
        // 명치는 좌우 점이 따로 있다.
        assertTrue(pickBodyPartByVoice("명치가 쓰려요", null) is BodyVoicePick.AskSide)
        val spot = pickBodyPartByVoice("오른쪽 명치가 쓰려요", null) as BodyVoicePick.Picked
        assertEquals("SUR:031", spot.selection.zoneId)
        assertEquals(
            BodyMapSelection("ANC:010"),
            (pickBodyPartByVoice("온몸이 쑤셔요", null) as BodyVoicePick.Picked).selection,
        )
        assertEquals(BodyVoicePick.None, pickBodyPartByVoice("감기 걸린 것 같아", null))
    }

    @Test
    fun `읽어 준 목록에서 몇 번째로 고른다`() {
        // 큰 부위 목록의 다섯 번째는 팔이다. 좌우를 말하지 않아 어느 쪽인지 묻는다.
        assertEquals(listOf("머리", "목", "가슴", "배", "팔"), bodyVoiceAnchors().take(5).map { it.second })
        assertTrue(pickBodyPartByVoice("다섯 번째", null) is BodyVoicePick.AskSide)

        val arm = BodyMapSelection("ANC:013", side = BodyMapSide.RIGHT)
        assertEquals(listOf("어깨", "위팔", "팔꿈치", "아래팔", "손목", "손"), bodyVoiceZones("ANC:013").map { it.second })
        val elbow = pickBodyPartByVoice("세 번째요", arm) as BodyVoicePick.Picked
        assertEquals(BodyMapSelection("ANC:013", "SUR:061", BodyMapSide.RIGHT), elbow.selection)
        assertEquals("SUR:072", (pickBodyPartByVoice("마지막 거", arm) as BodyVoicePick.Picked).selection.zoneId)
    }

    @Test
    fun `아픈 정도를 낱말과 점수와 단계로 고른다`() {
        assertEquals(MedicalMateSeverity.LEVEL_4, parseSeverityVoice("많이 아파요"))
        assertEquals(MedicalMateSeverity.LEVEL_5, parseSeverityVoice("견디기 힘들어요"))
        assertEquals(MedicalMateSeverity.LEVEL_1, parseSeverityVoice("조금 불편해요"))
        assertEquals(MedicalMateSeverity.LEVEL_4, parseSeverityVoice("7점"))
        assertEquals(MedicalMateSeverity.LEVEL_2, parseSeverityVoice("10점 중에 3점이요"))
        assertEquals(MedicalMateSeverity.LEVEL_2, parseSeverityVoice("2단계"))
        assertNull(parseSeverityVoice("그냥 그래요"))
    }

    @Test
    fun `묻는 말은 질문으로 넣고 질문이라는 말이 있어야 지우고 고친다`() {
        assertEquals(QuestionVoice.Add("약을 계속 먹어도 되나요"), parseQuestionVoice("약을 계속 먹어도 되나요?"))
        assertEquals(QuestionVoice.Add("운동해도 될까요"), parseQuestionVoice("질문 추가, 운동해도 될까요"))
        assertEquals(1, (parseQuestionVoice("두 번째 질문 지워 줘") as QuestionVoice.Remove).ordinal)
        assertEquals(
            QuestionVoice.Edit(0, "술 마셔도 되나요"),
            parseQuestionVoice("첫 번째 질문 고쳐, 술 마셔도 되나요"),
        )
        assertNull(parseQuestionVoice("오늘 날씨 좋다"))
    }

    @Test
    fun `다음은 짧게 말할 때만이다`() {
        assertTrue(isNextVoice("다음"))
        assertTrue(isNextVoice("다음으로 넘어가 줘"))
        assertFalse(isNextVoice("다음 주에 병원 다시 오라고 했어요"))
        assertTrue(isBackVoice("뒤로 가"))
    }

    @Test
    fun `정리 완료에서 카드와 병원을 고른다`() {
        assertEquals(IntakeDoneChoice.CARD, parseDoneVoice("카드 만들어 줘"))
        assertEquals(IntakeDoneChoice.HOSPITAL, parseDoneVoice("병원부터 찾을래"))
        assertNull(parseDoneVoice("음"))
    }

    // 화면 동작

    @Test
    fun `부위를 말해 고르고 다음이라고 하면 문답을 연다`() {
        val repository = VoiceSessionRepository()
        val viewModel = IntakeViewModel(repository, FakeSpeechToText())

        val reply = viewModel.handle("오른쪽 어깨가 아파요") as VoiceReply.Done
        assertEquals(R.string.voice_intake_picked, reply.message)
        assertEquals(listOf("오른쪽 어깨"), reply.args)
        assertEquals(BodyMapSelection("ANC:013", side = BodyMapSide.RIGHT), viewModel.uiState.value.bodyMap.focus)

        viewModel.handle("다음")
        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)
        assertEquals(listOf("SUR:051"), repository.startedCodes)
    }

    @Test
    fun `큰 부위로 들어가면 그 안에 고를 수 있는 곳을 읽어 준다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())
        assertEquals(R.string.voice_intake_ask_part, viewModel.uiState.value.voiceQuestion()?.text)

        val reply = viewModel.handle("오른쪽 팔이 아파요") as VoiceReply.Done

        assertEquals(R.string.voice_intake_part_focus, reply.message)
        assertEquals(R.string.voice_intake_ask_zone, reply.question?.text)
        assertEquals(listOf("오른쪽 팔", "· 어깨\n· 위팔\n· 팔꿈치\n· 아래팔\n· 손목\n· 손"), reply.question?.args)

        // 모르겠다고 하면 다시 읽어 준다.
        val again = viewModel.handle("잘 모르겠어요") as VoiceReply.Done
        assertEquals(reply.question, again.question)
        viewModel.handle("두 번째")
        assertEquals("SUR:055", viewModel.uiState.value.bodyMap.selection?.zoneId)
    }

    @Test
    fun `반대라고 하면 좌우를 바꾼다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())
        viewModel.handle("왼쪽 팔이 아파요")

        viewModel.handle("반대쪽")
        assertEquals(BodyMapSelection("ANC:013", side = BodyMapSide.RIGHT), viewModel.uiState.value.bodyMap.focus)

        // 고른 부위가 있으면 그 반대쪽을 고른다.
        viewModel.handle("손목")
        val reply = viewModel.handle("반대편이요") as VoiceReply.Done
        assertEquals(
            BodyMapSelection("ANC:013", "SUR:071", BodyMapSide.LEFT),
            viewModel.uiState.value.bodyMap.selection,
        )
        assertEquals(listOf("왼쪽 손목"), reply.args)
    }

    @Test
    fun `좌우가 없는 곳에서 반대라고 하면 바꾸지 않는다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())
        viewModel.handle("머리가 아파요")

        val reply = viewModel.handle("반대") as VoiceReply.Done

        assertEquals(R.string.voice_intake_flip_none, reply.message)
    }

    @Test
    fun `아픈 정도를 물을 때 고를 말을 읽어 준다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inChat()
        viewModel.handle("어제부터요")

        val reply = viewModel.handle("다음") as VoiceReply.Done

        assertEquals(R.string.voice_intake_ask_severity, reply.question?.text)
        assertEquals("· LEVEL_1" + "\n· LEVEL_2\n· LEVEL_3\n· LEVEL_4\n· LEVEL_5", reply.question?.args?.get(1))
        val again = viewModel.handle("어떻게 말해야 할지 모르겠어요") as VoiceReply.Done
        assertEquals(R.string.voice_intake_severity_list, again.message)
        assertEquals(reply.question, again.question)
    }

    @Test
    fun `어느 쪽인지 물은 뒤에는 좌우만 말해도 고른다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())

        val asked = viewModel.handle("어깨가 아파요") as VoiceReply.Done
        assertEquals(R.string.voice_intake_part_side, asked.message)
        viewModel.handle("왼쪽이요")

        assertEquals(
            BodyMapSelection("ANC:013", "SUR:051", BodyMapSide.LEFT),
            viewModel.uiState.value.bodyMap.selection,
        )
    }

    @Test
    fun `부위를 고르기 전의 다음과 첫 화면의 뒤로는 하지 않는다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())

        assertEquals(R.string.voice_intake_part_first, (viewModel.handle("다음") as VoiceReply.Done).message)
        // 증상 정리를 나가는 것은 전역 "뒤로"가 한다.
        assertEquals(VoiceReply.NotMine, viewModel.handle("뒤로"))
    }

    @Test
    fun `문답에서 한 말은 말로 한 답으로 보낸다`() {
        val repository = VoiceSessionRepository()
        val viewModel = IntakeViewModel(repository, FakeSpeechToText()).inChat()

        val reply = viewModel.handle("사흘 전부터 아팠어요") as VoiceReply.Done

        assertEquals(R.string.voice_intake_sent, reply.message)
        assertEquals("사흘 전부터 아팠어요", repository.sentText)
        assertTrue(repository.sentByVoice)
    }

    @Test
    fun `문답이 끝나기 전의 다음은 넘어가지 않고 끝난 뒤에는 넘어간다`() {
        val repository = VoiceSessionRepository()
        val viewModel = IntakeViewModel(repository, FakeSpeechToText()).inChat()

        assertEquals(R.string.voice_intake_still_asking, (viewModel.handle("다음") as VoiceReply.Done).message)
        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)

        repository.ends = true
        viewModel.handle("어제부터요")
        viewModel.handle("다음")
        assertEquals(IntakeStep.SEVERITY, viewModel.uiState.value.step)
    }

    @Test
    fun `아픈 정도를 말로 고르고 뒤로라고 하면 문답으로 돌아간다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inChat()
        viewModel.handle("어제부터요")
        viewModel.handle("다음")

        val reply = viewModel.handle("많이 아파요") as VoiceReply.Done

        assertEquals(MedicalMateSeverity.LEVEL_4, viewModel.uiState.value.severity)
        assertEquals(listOf("LEVEL_4"), reply.args)
        viewModel.handle("뒤로")
        assertEquals(IntakeStep.SYMPTOM_CHAT, viewModel.uiState.value.step)
    }

    @Test
    fun `질문을 말로 넣고 지우고 고치고 다 됐으면 마친다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inQuestions()

        viewModel.handle("약을 계속 먹어도 되나요")
        viewModel.handle("운동해도 될까요")
        viewModel.handle("두 번째 질문 고쳐, 수영해도 될까요")
        assertEquals(listOf("약을 계속 먹어도 되나요", "수영해도 될까요"), viewModel.uiState.value.questions)

        viewModel.handle("첫 번째 질문 지워 줘")
        assertEquals(listOf("수영해도 될까요"), viewModel.uiState.value.questions)

        viewModel.handle("다음")
        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `질문이 다 찼으면 넣지 않고 알린다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inQuestions()
        repeat(MAX_BRIEF_CARD_QUESTIONS) { viewModel.handle("${it}번 약 먹어도 되나요") }

        val reply = viewModel.handle("하나 더 물어봐도 되나요") as VoiceReply.Done

        assertEquals(R.string.voice_intake_question_full, reply.message)
        assertEquals(MAX_BRIEF_CARD_QUESTIONS, viewModel.uiState.value.questions.size)
    }

    // 말로 바뀐 자리 보이기

    @Test
    fun `부위를 고르거나 옮기면 인체도를 보인다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(), FakeSpeechToText())

        viewModel.handle("왼쪽 무릎이 아파요")
        val picked = viewModel.uiState.value.voiceFocus
        assertEquals(listOf(IntakeFocusTarget.Board), picked?.targets)

        viewModel.handle("반대")
        assertNotEquals(picked, viewModel.uiState.value.voiceFocus)
        assertEquals(listOf(IntakeFocusTarget.Board), viewModel.uiState.value.voiceFocus?.targets)
    }

    @Test
    fun `아픈 정도를 고르면 슬라이더를 보인다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inChat()
        viewModel.handle("어제부터요")
        viewModel.handle("다음")

        viewModel.handle("7점")

        assertEquals(listOf(IntakeFocusTarget.Severity), viewModel.uiState.value.voiceFocus?.targets)
    }

    @Test
    fun `질문을 넣거나 고치면 그 줄을 지우면 목록 머리를 보인다`() {
        val viewModel = IntakeViewModel(VoiceSessionRepository(ends = true), FakeSpeechToText()).inQuestions()

        viewModel.handle("약을 계속 먹어도 되나요")
        viewModel.handle("운동해도 될까요")
        assertEquals(listOf(IntakeFocusTarget.Question(1)), viewModel.uiState.value.voiceFocus?.targets)

        viewModel.handle("첫 번째 질문 고쳐, 술 마셔도 되나요")
        assertEquals(listOf(IntakeFocusTarget.Question(0)), viewModel.uiState.value.voiceFocus?.targets)

        viewModel.handle("두 번째 질문 지워 줘")
        assertEquals(listOf(IntakeFocusTarget.Questions), viewModel.uiState.value.voiceFocus?.targets)
    }

    private fun IntakeViewModel.handle(text: String): VoiceReply = voiceCommands.handle(
        text,
        IntakeVoiceRoutes(next = { onNext("라벨") }, severityLabel = { it.name }),
    )

    private fun IntakeViewModel.inChat(): IntakeViewModel = apply {
        handle("오른쪽 명치가 아파요")
        handle("다음")
    }

    private fun IntakeViewModel.inQuestions(): IntakeViewModel = inChat().apply {
        handle("어제부터요")
        handle("다음")
        handle("다음")
    }
}

/** 말로 보낸 답과 연 세션을 기록한다. */
private class VoiceSessionRepository(var ends: Boolean = false) : SessionRepository {
    var startedCodes: List<String>? = null
    var sentText: String? = null
    var sentByVoice = false

    override suspend fun start(siteCode: String, side: IntakeSide?, siteText: String?): ApiResult<IntakeSession> {
        startedCodes = listOf(siteCode)
        return ApiResult.Success(session.copy(messages = listOf(IntakeSessionMessage(1, false, "언제부터요?"))))
    }

    override suspend fun load(sessionId: Long): ApiResult<IntakeSession> = ApiResult.Success(session)

    override suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn> {
        sentText = text
        sentByVoice = byVoice
        return ApiResult.Success(
            IntakeTurn(
                ended = ends,
                messages = listOf(IntakeSessionMessage(1, false, "언제부터요?"), IntakeSessionMessage(2, true, text)),
                answered = 1,
                total = 4,
            ),
        )
    }

    override suspend fun setSeverity(sessionId: Long, level: Int, label: String): ApiResult<IntakeSession> =
        ApiResult.Success(session)

    override suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession> =
        ApiResult.Success(session)

    private companion object {
        val session =
            IntakeSession(
                id = 7,
                status = IntakeSessionStatus.IN_PROGRESS,
                siteCodes = listOf("SUR:031"),
                siteText = "명치",
                answered = 0,
                total = 4,
                messages = emptyList(),
            )
    }
}
