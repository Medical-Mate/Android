package com.mist.medicalmate.intake.data

import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 응답을 화면 값으로 옮기는 부분.
 *
 * 4단계(1i)가 AI 후보를 목록에 채워 두고 환자가 고치게 한다. 후보와 확정을 섞는 규칙이라
 * 화면이 아니라 여기서 정한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {
    @Test
    fun `적어 둔 질문이 없으면 AI 후보를 채운다`() = runTest {
        val session =
            load(
                SessionResponse(
                    sessionId = 1,
                    questionCandidates =
                    listOf(
                        QuestionCandidateResponse(text = "정밀 검사를 받아야 하나요?", rank = 1),
                        QuestionCandidateResponse(text = "지금 진통제 계속 먹어도 되나요?", rank = 2),
                    ),
                ),
            )

        assertEquals(
            listOf("정밀 검사를 받아야 하나요?", "지금 진통제 계속 먹어도 되나요?"),
            session.questions,
        )
    }

    @Test
    fun `후보는 rank 차례로 놓는다`() = runTest {
        // 서버가 정렬해 준다고 했지만 순서를 믿지 않는다. 맵이 아니라 배열이어도 마찬가지다.
        val session =
            load(
                SessionResponse(
                    sessionId = 1,
                    questionCandidates =
                    listOf(
                        QuestionCandidateResponse(text = "나중 것", rank = 5),
                        QuestionCandidateResponse(text = "먼저 것", rank = 1),
                    ),
                ),
            )

        assertEquals(listOf("먼저 것", "나중 것"), session.questions)
    }

    @Test
    fun `한 번이라도 적었으면 후보로 덮지 않는다`() = runTest {
        // 덮으면 환자가 지운 질문이 되살아난다.
        val session =
            load(
                SessionResponse(
                    sessionId = 1,
                    questions = listOf("내가 적은 것"),
                    questionCandidates = listOf(QuestionCandidateResponse(text = "AI가 고른 것", rank = 1)),
                ),
            )

        assertEquals(listOf("내가 적은 것"), session.questions)
    }

    @Test
    fun `후보도 적어 둔 것도 없으면 비어 있다`() = runTest {
        assertTrue(load(SessionResponse(sessionId = 1)).questions.isEmpty())
    }

    private suspend fun load(response: SessionResponse): IntakeSession {
        val repository =
            DefaultSessionRepository(
                api =
                object : SessionApi {
                    override suspend fun start(request: StartSessionRequest) = response

                    override suspend fun session(sessionId: Long) = response

                    override suspend fun sendMessage(sessionId: Long, request: SendMessageRequest): TurnResponse =
                        error("이 시험이 부르지 않는다")

                    override suspend fun putSeverity(sessionId: Long, request: SeverityRequest) = response

                    override suspend fun putQuestions(sessionId: Long, request: QuestionsRequest) = response
                },
                json = Json,
            )
        return (repository.load(1) as ApiResult.Success).value
    }
}
