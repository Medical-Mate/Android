package com.mist.medicalmate.intake.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/**
 * 문답 세션 읽고 쓰기.
 *
 * 임시저장이 여기 있다. [start]가 만든 세션을 서버가 들고 있고 [load]가 대화와 진행도를
 * 돌려준다. 홈의 "이어서 하기"가 [load]로 들어온다.
 */
interface SessionRepository {
    /**
     * 짚은 부위로 문답을 시작한다.
     *
     * @param siteCodes 구역까지 골랐으면 `SUR:*` 하나, 앵커까지면 `ANC:*` 하나다. 서버가
     *   목록으로 받는 것은 다중 선택을 열어두려는 것이고 우리는 한 곳만 고른다.
     * @param siteText 사람이 읽는 이름. 문답 첫 문장에 그대로 들어간다.
     */
    suspend fun start(siteCodes: List<String>, siteText: String?): ApiResult<IntakeSession>

    suspend fun load(sessionId: Long): ApiResult<IntakeSession>

    /**
     * 환자 발화를 보내고 다음 질문을 받는다.
     *
     * @param byVoice 음성으로 말했는지. 오디오는 보내지 않고 그 사실만 기록된다.
     */
    suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn>

    /** 3단계의 통증 강도. 표시 문구도 함께 보낸다. 서버가 카피를 들고 있지 않다. */
    suspend fun setSeverity(sessionId: Long, level: Int, label: String): ApiResult<IntakeSession>

    /** 4단계의 질문. 목록을 통째로 보낸다. */
    suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession>
}

internal class DefaultSessionRepository
@Inject
constructor(private val api: SessionApi, private val json: Json) :
    SessionRepository {
    override suspend fun start(siteCodes: List<String>, siteText: String?): ApiResult<IntakeSession> =
        apiCall(json) { api.start(StartSessionRequest(siteCodes = siteCodes, siteText = siteText)) }
            .map { it.toSession() }

    override suspend fun load(sessionId: Long): ApiResult<IntakeSession> =
        apiCall(json) { api.session(sessionId) }.map { it.toSession() }

    override suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn> = apiCall(json) {
        api.sendMessage(sessionId, SendMessageRequest(text = text, inputMethod = if (byVoice) "STT" else "TEXT"))
    }.map { it.toTurn() }

    override suspend fun setSeverity(sessionId: Long, level: Int, label: String): ApiResult<IntakeSession> =
        apiCall(json) { api.putSeverity(sessionId, SeverityRequest(level = level, label = label)) }
            .map { it.toSession() }

    override suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession> =
        apiCall(json) { api.putQuestions(sessionId, QuestionsRequest(questions)) }.map { it.toSession() }
}

/**
 * 서버가 들고 있는 문답 하나.
 *
 * [answered]와 [total]을 그대로 둔다. 화면이 "4단계 중 2단계"를 만들고, 이어서 할 때
 * 어느 단계로 돌아갈지도 이 숫자가 정한다.
 */
data class IntakeSession(
    val id: Long,
    val status: IntakeSessionStatus,
    val siteCodes: List<String>,
    val siteText: String?,
    val answered: Int,
    val total: Int,
    val messages: List<IntakeSessionMessage>,
    /** 3단계에서 고른 강도. 1~5 서열척도다. */
    val severityLevel: Int? = null,
    /** 4단계에 적어 둔 질문. 최대 셋이다. */
    val questions: List<String> = emptyList(),
)

enum class IntakeSessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }

/**
 * 한 턴의 결과.
 *
 * [messages]에 대화 전체가 들어 있다. 화면이 그것으로 다시 그리면 세션을 또 조회하지
 * 않아도 된다.
 *
 * [ended]가 서면 문답이 끝난 것이다. 그 뒤에 또 보내도 오류가 아니라 마지막 문장만
 * 돌아온다. 네트워크가 끊긴 사이에 끝났을 수 있어서 서버가 그렇게 열어 뒀다.
 */
data class IntakeTurn(val ended: Boolean, val messages: List<IntakeSessionMessage>, val answered: Int, val total: Int)

data class IntakeSessionMessage(val seq: Long, val fromPatient: Boolean, val text: String)

private fun TurnResponse.toTurn() = IntakeTurn(
    ended = ended,
    messages = messages.map { it.toMessage() },
    answered = progress?.current ?: 0,
    total = progress?.total ?: 0,
)

private fun MessageResponse.toMessage() = IntakeSessionMessage(seq = seq, fromPatient = role == ROLE_USER, text = text)

private fun SessionResponse.toSession() = IntakeSession(
    id = sessionId,
    status = statusOf(status),
    siteCodes = siteCodes,
    siteText = siteText,
    answered = progress?.current ?: 0,
    total = progress?.total ?: 0,
    messages = messages.map { it.toMessage() },
    severityLevel = severity?.level,
    // 환자가 아직 아무것도 적지 않았으면 AI 후보를 채워 둔다. 시안 1i가 추천 질문 셋을
    // 목록에 넣어 두고 ×로 지우게 한다. 한 번이라도 손댔으면 그 결과가 정본이다 — 후보로
    // 덮으면 지운 질문이 되살아난다.
    questions = questions.ifEmpty { questionCandidates.sortedBy { it.rank }.map { it.text } },
)

/**
 * 모르는 값은 진행 중으로 본다.
 *
 * 서버가 상태를 늘렸을 때 세션을 못 여는 것보다, 열어 두고 진행도로 판단하는 쪽이 낫다.
 * 임시저장을 잃는 것이 더 큰 손해다.
 */
private fun statusOf(value: String?): IntakeSessionStatus = when (value) {
    "COMPLETED" -> IntakeSessionStatus.COMPLETED
    "ABANDONED" -> IntakeSessionStatus.ABANDONED
    else -> IntakeSessionStatus.IN_PROGRESS
}

private const val ROLE_USER = "USER"
