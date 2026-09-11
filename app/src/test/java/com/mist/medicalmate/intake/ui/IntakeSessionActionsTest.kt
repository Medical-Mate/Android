package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionMessage
import com.mist.medicalmate.intake.data.IntakeSessionStatus
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
 * 홈의 "이어서 하기"가 [IntakeSessionActions.restore]로 들어오고, 부위를 고르고 다음을
 * 누르면 [IntakeSessionActions.start]가 세션을 연다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntakeSessionActionsTest {
    @Test
    fun `부위를 고르면 그 코드로 세션을 연다`() = runTest {
        val repository = RecordingRepository()
        val actions = actions(repository)

        actions.start(BodyMapSelection(anchorId = "ANC:004", zoneId = "SUR:031"), "명치")

        assertEquals(listOf("SUR:031"), repository.startedCodes)
        assertEquals("명치", repository.startedText)
    }

    @Test
    fun `구역이 없는 부위는 앵커 코드를 보낸다`() = runTest {
        val repository = RecordingRepository()

        actions(repository).start(BodyMapSelection(anchorId = "ANC:002"), "목")

        assertEquals(listOf("ANC:002"), repository.startedCodes)
    }

    @Test
    fun `세션을 열면 id가 상태에 남는다`() = runTest {
        // 이 id가 임시저장의 열쇠다. 없으면 앱을 나갔다 와도 이어 할 자리가 없다.
        var state = IntakeUiState()
        actions(RecordingRepository(), update = { state = it(state) })
            .start(BodyMapSelection(anchorId = "ANC:004", zoneId = "SUR:031"), "명치")

        assertEquals(7L, state.sessionId)
    }

    @Test
    fun `세션 만들기가 실패해도 문답은 막지 않는다`() = runTest {
        var state = IntakeUiState(step = IntakeStep.SYMPTOM_CHAT)
        val failing = FixedRepository(ApiResult.NetworkUnavailable(IOException()))

        actions(failing, update = { state = it(state) })
            .start(BodyMapSelection(anchorId = "ANC:004"), "명치")

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

    private fun TestScope.actions(
        repository: SessionRepository,
        update: (((IntakeUiState) -> IntakeUiState)) -> Unit = {},
    ) = IntakeSessionActions(repository, TestScope(UnconfinedTestDispatcher(testScheduler)), update)

    private class RecordingRepository : SessionRepository {
        var startedCodes: List<String>? = null
        var startedText: String? = null

        override suspend fun start(siteCodes: List<String>, siteText: String?): ApiResult<IntakeSession> {
            startedCodes = siteCodes
            startedText = siteText
            return ApiResult.Success(savedSession)
        }

        override suspend fun load(sessionId: Long): ApiResult<IntakeSession> = ApiResult.Success(savedSession)

        override suspend fun send(sessionId: Long, text: String, byVoice: Boolean) =
            ApiResult.Success(IntakeTurn(ended = false, messages = emptyList(), answered = 0, total = 0))

        override suspend fun setSeverity(sessionId: Long, level: Int, label: String) = ApiResult.Success(savedSession)

        override suspend fun setQuestions(sessionId: Long, questions: List<String>) = ApiResult.Success(savedSession)
    }

    private class FixedRepository(private val result: ApiResult<IntakeSession>) : SessionRepository {
        override suspend fun start(siteCodes: List<String>, siteText: String?) = result

        override suspend fun load(sessionId: Long) = result

        override suspend fun send(sessionId: Long, text: String, byVoice: Boolean) =
            ApiResult.Success(IntakeTurn(ended = false, messages = emptyList(), answered = 0, total = 0))

        override suspend fun setSeverity(sessionId: Long, level: Int, label: String) = result

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
    }
}
