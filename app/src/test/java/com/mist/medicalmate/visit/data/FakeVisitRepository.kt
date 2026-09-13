package com.mist.medicalmate.visit.data

import com.mist.medicalmate.core.network.ApiResult
import java.io.IOException

/**
 * 진료 기록 저장소 대역.
 *
 * 세 자리의 응답을 따로 정한다. 목록·상세·저장을 보는 화면이 각각이고, 한 화면을 시험할 때
 * 나머지 둘은 무엇이 오든 상관이 없다.
 *
 * 저장 요청은 [request]에 남긴다. 무엇을 보냈는지가 이 대역을 쓰는 시험의 관심사다.
 */
internal class FakeVisitRepository(
    private val list: ApiResult<List<VisitListItem>> = ApiResult.Success(emptyList()),
    private val detail: ApiResult<Visit> = ApiResult.Success(EMPTY_VISIT),
    private val saved: ApiResult<Visit> = ApiResult.Success(EMPTY_VISIT.copy(id = SAVED_ID)),
    /**
     * id마다 다른 상세.
     *
     * 1k가 이번 기록과 직전 기록을 함께 읽어서 둘이 달라야 한다. 여기 없는 id는 [detail]로
     * 떨어진다.
     */
    private val details: Map<Long, ApiResult<Visit>> = emptyMap(),
) : VisitRepository {
    var requestedId: Long? = null
    var createCount = 0
    var cardId: Long? = null
    var request: NewVisit? = null

    /** 지워 달라고 받은 id. 기록 삭제가 카드가 아니라 기록으로 나가는지 보는 데 쓴다. */
    val deletedIds = mutableListOf<String>()

    /** 지우지 못할 id. 일부만 실패하는 경우를 만든다. */
    var deleteFails: Set<String> = emptySet()

    /** 물어본 차례대로. 몇 번 불렀는지가 관심사인 시험이 있다. */
    val requestedIds = mutableListOf<Long>()

    override suspend fun visits(): ApiResult<List<VisitListItem>> = list

    override suspend fun visit(visitId: Long): ApiResult<Visit> {
        requestedId = visitId
        requestedIds += visitId
        return details[visitId] ?: detail
    }

    override suspend fun create(cardId: Long, visit: NewVisit): ApiResult<Visit> {
        createCount += 1
        this.cardId = cardId
        request = visit
        return saved
    }

    override suspend fun delete(visitId: Long): ApiResult<Unit> {
        deletedIds += visitId.toString()
        return if (visitId.toString() in deleteFails) OFFLINE else ApiResult.Success(Unit)
    }

    override suspend fun deleteAll(visitIds: Set<String>): Set<String> = visitIds
        .mapNotNull { id -> id.toLongOrNull()?.takeIf { delete(it) is ApiResult.Success }?.let { id } }
        .toSet()

    companion object {
        const val SAVED_ID = "77"

        val EMPTY_VISIT =
            Visit(
                id = "1",
                cardId = null,
                clinic = null,
                visitedOn = null,
                items = emptyList(),
                followUp = null,
                patientNotes = emptyList(),
                rawNote = null,
            )

        /** 연결이 없는 경우. 저장이 나가지 않았는지 보는 데 쓴다. */
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
