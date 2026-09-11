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
)

enum class IntakeSessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }

data class IntakeSessionMessage(val seq: Long, val fromPatient: Boolean, val text: String)

private fun SessionResponse.toSession() = IntakeSession(
    id = sessionId,
    status = statusOf(status),
    siteCodes = siteCodes,
    siteText = siteText,
    answered = progress?.current ?: 0,
    total = progress?.total ?: 0,
    messages = messages.map { IntakeSessionMessage(seq = it.seq, fromPatient = it.role == ROLE_USER, text = it.text) },
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
