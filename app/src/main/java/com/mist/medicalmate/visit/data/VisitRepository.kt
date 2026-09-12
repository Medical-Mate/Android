package com.mist.medicalmate.visit.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** 진료 후 기록 읽고 쓰기. */
interface VisitRepository {
    suspend fun visits(): ApiResult<List<VisitListItem>>

    suspend fun visit(visitId: Long): ApiResult<Visit>

    /**
     * 진료 후 기록을 남긴다.
     *
     * @param cardId 확정한 카드여야 한다. 카드 하나에 기록 하나다.
     */
    suspend fun create(cardId: Long, visit: NewVisit): ApiResult<Visit>
}

/**
 * 남길 기록.
 *
 * 모두 선택이다. [rawNote]만 적어도 저장된다. 병원을 막 나온 사람에게 필수 입력을 요구하면
 * 아무것도 안 남는다.
 */
data class NewVisit(
    val clinicName: String? = null,
    val visitedOn: LocalDate? = null,
    val whatWasDone: String? = null,
    val result: String? = null,
    val prescription: String? = null,
    val rawNote: String? = null,
)

internal class DefaultVisitRepository
@Inject
constructor(private val api: VisitApi, private val json: Json) :
    VisitRepository {
    override suspend fun visits(): ApiResult<List<VisitListItem>> =
        apiCall(json) { api.visits() }.map { list -> list.map { it.toListItem() } }

    override suspend fun visit(visitId: Long): ApiResult<Visit> =
        apiCall(json) { api.visit(visitId) }.map { it.toVisit() }

    override suspend fun create(cardId: Long, visit: NewVisit): ApiResult<Visit> = apiCall(json) {
        api.create(
            cardId,
            CreateVisitRequest(
                clinicName = visit.clinicName,
                visitedOn = visit.visitedOn?.toString(),
                whatWasDone = visit.whatWasDone,
                result = visit.result,
                prescription = visit.prescription,
                rawNote = visit.rawNote,
            ),
        )
    }.map { it.toVisit() }
}

/** 목록의 기록 한 줄. 원문은 담기지 않는다. */
data class VisitListItem(
    val id: String,
    /**
     * 이 기록이 매달린 카드.
     *
     * 기록 목록(1j-1)에서 지우면 서버가 지우는 것이 그 카드다. 기록만 지우는 API가 없다.
     */
    val cardId: Long?,
    val cardTitle: String,
    val clinic: String?,
    val visitedOn: LocalDate,
)

/**
 * 기록 하나.
 *
 * 서버가 한 것·결과·처방 셋으로 고정해서 준다. 카드의 축과 달리 가변이 아니다.
 */
data class Visit(
    val id: String,
    val cardId: Long?,
    val clinic: String?,
    val visitedOn: LocalDate?,
    val whatWasDone: String?,
    val result: String?,
    val prescription: String?,
    val rawNote: String?,
)

private fun VisitSummaryResponse.toListItem() = VisitListItem(
    id = visitId.toString(),
    cardId = cardId,
    cardTitle = cardTitle.orEmpty(),
    clinic = clinicName,
    visitedOn = LocalDate.parse(visitedOn),
)

private fun VisitResponse.toVisit() = Visit(
    id = visitId.toString(),
    cardId = cardId,
    clinic = clinicName,
    visitedOn = visitedOn?.let(LocalDate::parse),
    whatWasDone = whatWasDone,
    result = result,
    prescription = prescription,
    rawNote = rawNote,
)
