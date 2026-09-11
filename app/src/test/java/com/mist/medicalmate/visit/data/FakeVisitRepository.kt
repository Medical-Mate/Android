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
) : VisitRepository {
    var requestedId: Long? = null
    var createCount = 0
    var cardId: Long? = null
    var request: NewVisit? = null

    override suspend fun visits(): ApiResult<List<VisitListItem>> = list

    override suspend fun visit(visitId: Long): ApiResult<Visit> {
        requestedId = visitId
        return detail
    }

    override suspend fun create(cardId: Long, visit: NewVisit): ApiResult<Visit> {
        createCount += 1
        this.cardId = cardId
        request = visit
        return saved
    }

    companion object {
        const val SAVED_ID = "77"

        val EMPTY_VISIT =
            Visit(
                id = "1",
                cardId = null,
                clinic = null,
                visitedOn = null,
                whatWasDone = null,
                result = null,
                prescription = null,
                rawNote = null,
            )

        /** 연결이 없는 경우. 저장이 나가지 않았는지 보는 데 쓴다. */
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
