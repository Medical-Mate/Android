package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionMessage
import com.mist.medicalmate.intake.data.IntakeSessionStatus
import com.mist.medicalmate.intake.data.IntakeSeverity
import com.mist.medicalmate.intake.data.IntakeSide
import com.mist.medicalmate.intake.data.IntakeTurn
import com.mist.medicalmate.intake.data.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 임시저장을 여닫는 부분.
 *
 * 홈의 "이어서 하기"가 [IntakeSessionActions.restore]로 들어오고, 강도를 고르고 다음을
 * 누르면 [IntakeSessionActions.start]가 세션을 연다(#310).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntakeSessionActionsTest {
    @Test
    fun `부위를 고르면 그 코드로 세션을 연다`() = runTest {
        val repository = RecordingRepository()
        val actions = actions(repository)

        actions.start(BodyMapSelection(anchorId = "ANC:004", zoneId = "SUR:031"), "명치", SEVERITY)

        assertEquals(listOf("SUR:031"), repository.startedCodes)
        assertEquals("명치", repository.startedText)
    }

    @Test
    fun `세션을 열 때 고른 강도를 함께 보낸다`() = runTest {
        // 강도가 문답 앞으로 왔다(Backend#169).
        val repository = RecordingRepository()

        actions(repository).start(BodyMapSelection(anchorId = "ANC:002"), "목", IntakeSeverity(5, "견디기 힘들어요"))

        assertEquals(IntakeSeverity(5, "견디기 힘들어요"), repository.startedSeverity)
    }

    @Test
    fun `구역이 없는 부위는 앵커 코드를 보낸다`() = runTest {
        val repository = RecordingRepository()

        actions(repository).start(BodyMapSelection(anchorId = "ANC:002"), "목", SEVERITY)

        assertEquals(listOf("ANC:002"), repository.startedCodes)
    }

    @Test
    fun `세션을 열면 id가 상태에 남는다`() = runTest {
        // 이 id가 임시저장의 열쇠다. 없으면 앱을 나갔다 와도 이어 할 자리가 없다.
        var state = IntakeUiState()
        actions(RecordingRepository(), update = { state = it(state) })
            .start(BodyMapSelection(anchorId = "ANC:004", zoneId = "SUR:031"), "명치", SEVERITY)

        assertEquals(7L, state.sessionId)
    }

    @Test
    fun `세션 만들기가 실패해도 문답은 막지 않는다`() = runTest {
        var state = IntakeUiState(step = IntakeStep.SYMPTOM_CHAT)
        val failing = FixedRepository(ApiResult.NetworkUnavailable(IOException()))

        actions(failing, update = { state = it(state) })
            .start(BodyMapSelection(anchorId = "ANC:004"), "명치", SEVERITY)

        assertNull(state.sessionId)
        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertFalse(state.restoreFailed)
    }

    @Test
    fun `이어서 하면 주고받은 말이 되살아난다`() = runTest {
        var state = IntakeUiState()

        actions(FixedRepository(ApiResult.Success(savedSession)), update = { state = it(state) }).restore(7)

        assertEquals(
            listOf("언제부터 그러셨어요?", "3주쯤 됐어요"),
            state.messages.map { it.text },
        )
        assertEquals(
            listOf(IntakeMessage.Sender.AI, IntakeMessage.Sender.PATIENT),
            state.messages.map { it.sender },
        )
    }

    @Test
    fun `이어서 하면 문답 단계로 간다`() = runTest {
        // 세션이 있다는 것은 부위를 이미 골랐다는 뜻이다. 다시 고르게 하지 않는다.
        var state = IntakeUiState()

        actions(FixedRepository(ApiResult.Success(savedSession)), update = { state = it(state) }).restore(7)

        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertEquals("명치", state.bodyPart)
        assertEquals(7L, state.sessionId)
    }

    @Test
    fun `강도를 고른 문답도 3단계로 돌아간다`() = runTest {
        // 세션은 강도를 고른 뒤에 생긴다. 강도 단계로 되돌려 다시 고르게 할 이유가 없다.
        var state = IntakeUiState()
        val past = savedSession.copy(status = IntakeSessionStatus.COMPLETED, severityLevel = 4)

        actions(FixedRepository(ApiResult.Success(past)), update = { state = it(state) }).restore(7)

        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
    }

    @Test
    fun `고른 강도가 눈금에 얹힌다`() = runTest {
        // 돌아간 단계만 맞고 눈금이 기본값이면 고른 적 없는 값이 저장된다.
        var state = IntakeUiState()
        val past = savedSession.copy(status = IntakeSessionStatus.COMPLETED, severityLevel = 4)

        actions(FixedRepository(ApiResult.Success(past)), update = { state = it(state) }).restore(7)

        assertEquals(MedicalMateSeverity.LEVEL_4, state.severity)
    }

    @Test
    fun `문답이 끝났으면 추천 질문 고르기로 돌아간다`() = runTest {
        // 질문 고르기에서 나간 문답이다(Backend#173의 ended).
        var state = IntakeUiState()

        actions(FixedRepository(ApiResult.Success(withCandidates)), update = { state = it(state) }).restore(7)

        assertEquals(IntakeStep.SYMPTOM_CHAT, state.step)
        assertTrue(state.chatFinished)
    }

    @Test
    fun `추천 줄을 세우고 저장해 둔 것만 체크한다`() = runTest {
        var state = IntakeUiState()

        actions(FixedRepository(ApiResult.Success(withCandidates)), update = { state = it(state) }).restore(7)

        assertEquals(
            listOf(
                IntakeQuestion("검사를 받아야 하나요?", selected = true),
                IntakeQuestion("진통제 계속 먹어도 되나요?", selected = false),
                IntakeQuestion("공복으로 가야 하나요?", selected = true, custom = true),
            ),
            state.questionRows,
        )
    }

    @Test
    fun `마디 id는 서버의 seq를 쓴다`() = runTest {
        // 목록의 key다. 이어 답할 때 새로 붙는 마디와 겹치면 안 된다.
        var state = IntakeUiState()

        actions(FixedRepository(ApiResult.Success(savedSession)), update = { state = it(state) }).restore(7)

        assertEquals(listOf(1L, 2L), state.messages.map { it.id })
    }

    @Test
    fun `서버에 마디가 없으면 첫 물음을 다시 연다`() = runTest {
        // 답을 보내는 경로가 없어 대화가 서버에 쌓이지 않는다. 그대로 두면 빈 화면이 열린다.
        var state = IntakeUiState()
        val empty = savedSession.copy(messages = emptyList())

        actions(FixedRepository(ApiResult.Success(empty)), update = { state = it(state) }).restore(7)

        assertEquals(1, state.messages.size)
        assertEquals(IntakeMessage.Sender.AI, state.messages.single().sender)
        assertTrue(state.messages.single().text.startsWith("명치가"))
    }

    @Test
    fun `이어 답한 마디는 불러온 마디와 id가 겹치지 않는다`() = runTest {
        var state = IntakeUiState()
        actions(FixedRepository(ApiResult.Success(savedSession)), update = { state = it(state) }).restore(7)

        val added = state.newMessage(IntakeMessage.Sender.PATIENT, "더 있어요")

        assertEquals(3L, added.id)
    }

    @Test
    fun `불러오지 못하면 이유를 남긴다`() = runTest {
        var state = IntakeUiState()
        val rejected = ApiResult.Rejected(ApiErrorCode.UNKNOWN, null, null, retryable = false)

        actions(FixedRepository(rejected), update = { state = it(state) }).restore(7)

        assertTrue(state.restoreFailed)
        assertFalse(state.restoring)
    }

    @Test
    fun `강도를 다시 고르면 그 값만 보낸다`() = runTest {
        val repository = RecordingRepository()

        actions(repository).saveSeverity(IntakeUiState(sessionId = 7), IntakeSeverity(2, "은근히 아파요"))

        assertEquals(IntakeSeverity(2, "은근히 아파요"), repository.savedSeverity)
    }

    @Test
    fun `세션이 없으면 강도를 보내지 않는다`() = runTest {
        val repository = RecordingRepository()

        actions(repository).saveSeverity(IntakeUiState(), IntakeSeverity(2, "은근히 아파요"))

        assertNull(repository.savedSeverity)
    }

    private fun TestScope.actions(
        repository: SessionRepository,
        update: (((IntakeUiState) -> IntakeUiState)) -> Unit = {},
    ) = IntakeSessionActions(repository, TestScope(UnconfinedTestDispatcher(testScheduler)), update)

    private class RecordingRepository : SessionRepository {
        var startedCodes: List<String>? = null
        var startedText: String? = null
        var startedSeverity: IntakeSeverity? = null
        var savedSeverity: IntakeSeverity? = null

        override suspend fun start(
            siteCode: String,
            side: IntakeSide?,
            siteText: String?,
            severity: IntakeSeverity,
        ): ApiResult<IntakeSession> {
            startedCodes = listOf(siteCode)
            startedText = siteText
            startedSeverity = severity
            return ApiResult.Success(savedSession)
        }

        override suspend fun load(sessionId: Long): ApiResult<IntakeSession> = ApiResult.Success(savedSession)

        override suspend fun send(sessionId: Long, text: String, byVoice: Boolean) =
            ApiResult.Success(IntakeTurn(ended = false, messages = emptyList(), answered = 0, total = 0))

        override suspend fun setSeverity(sessionId: Long, severity: IntakeSeverity): ApiResult<IntakeSession> {
            savedSeverity = severity
            return ApiResult.Success(savedSession)
        }

        override suspend fun setQuestions(sessionId: Long, questions: List<String>) = ApiResult.Success(savedSession)
    }

    private class FixedRepository(private val result: ApiResult<IntakeSession>) : SessionRepository {
        override suspend fun start(siteCode: String, side: IntakeSide?, siteText: String?, severity: IntakeSeverity) =
            result

        override suspend fun load(sessionId: Long) = result

        override suspend fun send(sessionId: Long, text: String, byVoice: Boolean) =
            ApiResult.Success(IntakeTurn(ended = false, messages = emptyList(), answered = 0, total = 0))

        override suspend fun setSeverity(sessionId: Long, severity: IntakeSeverity) = result

        override suspend fun setQuestions(sessionId: Long, questions: List<String>) = result
    }

    private companion object {
        val savedSession =
            IntakeSession(
                id = 7,
                status = IntakeSessionStatus.IN_PROGRESS,
                siteCodes = listOf("SUR:031"),
                siteText = "명치",
                answered = 1,
                total = 4,
                messages =
                listOf(
                    IntakeSessionMessage(seq = 1, fromPatient = false, text = "언제부터 그러셨어요?"),
                    IntakeSessionMessage(seq = 2, fromPatient = true, text = "3주쯤 됐어요"),
                ),
            )

        /** 문답이 끝나 추천이 실린 세션. 추천 하나와 직접 적은 하나를 체크해 저장해 뒀다. */
        val withCandidates =
            savedSession.copy(
                status = IntakeSessionStatus.COMPLETED,
                severityLevel = 3,
                questionCandidates = listOf("검사를 받아야 하나요?", "진통제 계속 먹어도 되나요?"),
                questions = listOf("검사를 받아야 하나요?", "공복으로 가야 하나요?"),
            )

        val SEVERITY = IntakeSeverity(3, "꽤 아파요")
    }
}
