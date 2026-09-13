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

    /** 기록 하나를 지운다. 카드는 남는다. */
    suspend fun delete(visitId: Long): ApiResult<Unit>

    /**
     * 여러 건을 지우고 **실제로 지워진 id만** 돌려준다.
     *
     * 한 번에 지우는 API가 없어서 한 건씩 부른다. 일부가 실패해도 나머지는 계속 지운다.
     * 하나 실패했다고 멈추면 이미 지운 것과 화면이 어긋나고, 다시 누르면 지운 것을 또
     * 부르게 된다.
     */
    suspend fun deleteAll(visitIds: Set<String>): Set<String>

    /**
     * 적어 둔 메모를 항목으로 나눈다. 저장하지 않는다.
     *
     * @param labels 직전 결과의 분류. 있으면 함께 보낸다. 안 보내면 AI 모델을 다시 부른다.
     */
    suspend fun classify(
        memo: String,
        visitedOn: LocalDate?,
        clinic: String?,
        labels: Map<String, String>?,
    ): ApiResult<VisitClassification>
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
    /** 적은 항목. 축 id와 값만 보낸다. 상태와 출처는 서버가 정한다. */
    val items: List<NewVisitItem> = emptyList(),
    val followUp: VisitFollowUp? = null,
    /** 어느 항목에도 들어가지 않은 문장. */
    val patientNotes: List<String> = emptyList(),
    val rawNote: String? = null,
)

data class NewVisitItem(val axis: String, val value: String)

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
                axes = visit.items.map { VisitAxisRequest(axis = it.axis, value = it.value) },
                followUp =
                visit.followUp?.let {
                    FollowUpRequest(
                        date = it.date.toString(),
                        text = it.text,
                        approximate = it.approximate,
                    )
                },
                patientNotes = visit.patientNotes,
                rawNote = visit.rawNote,
            ),
        )
    }.map { it.toVisit() }

    override suspend fun delete(visitId: Long): ApiResult<Unit> = apiCall(json) { api.delete(visitId) }

    override suspend fun deleteAll(visitIds: Set<String>): Set<String> = visitIds
        .mapNotNull { id -> id.toLongOrNull()?.takeIf { delete(it) is ApiResult.Success }?.let { id } }
        .toSet()

    override suspend fun classify(
        memo: String,
        visitedOn: LocalDate?,
        clinic: String?,
        labels: Map<String, String>?,
    ): ApiResult<VisitClassification> = apiCall(json) {
        api.classify(
            ClassifyMemoRequest(
                memo = memo,
                visitedOn = visitedOn?.toString(),
                clinicName = clinic,
                labels = labels?.takeIf { it.isNotEmpty() },
            ),
        )
    }.map { it.toClassification() }
}

/**
 * 메모를 나눈 결과.
 *
 * [labels]를 들고 있다가 다시 나눌 때 돌려준다. 그것이 AI 모델을 다시 부르지 않게 하는
 * 유일한 방법이다.
 */
data class VisitClassification(
    val items: List<VisitItem>,
    val followUp: VisitFollowUp?,
    val patientNotes: List<String>,
    val labels: Map<String, String>,
)

/** 목록의 기록 한 줄. 원문은 담기지 않는다. */
data class VisitListItem(
    val id: String,
    /**
     * 이 기록이 매달린 카드.
     *
     * **없을 수 있다.** 카드를 지워도 기록은 남고 그때 연결만 끊긴다. 줄의 제목은 [cardTitle]로
     * 그린다 — 카드를 만들 때 박아둔 값이라 연결이 끊겨도 남아 있다.
     */
    val cardId: Long?,
    val cardTitle: String,
    val clinic: String?,
    val visitedOn: LocalDate,
    /** 다시 오라고 들은 날. 달력에 점을 찍는 데 쓴다. */
    val followUpDate: LocalDate? = null,
)

/**
 * 기록 하나.
 *
 * **항목이 가변이다**(#178). 브리핑 카드처럼 축 목록이고 AI가 축을 늘려도 실린다. 값이 없는
 * 항목은 줄이 없다.
 */
data class Visit(
    val id: String,
    val cardId: Long?,
    val clinic: String?,
    val visitedOn: LocalDate?,
    val items: List<VisitItem>,
    val followUp: VisitFollowUp?,
    val patientNotes: List<String>,
    val rawNote: String?,
)

/** 기록의 한 줄. [axis]는 서버 축 id이고 [label]은 화면에 적는 이름이다. */
data class VisitItem(val axis: String, val label: String, val value: String)

/**
 * 다시 오라고 들은 날.
 *
 * [approximate]면 환자가 "2주 뒤"처럼 범위로 말한 것이다. 화면이 "9월 27일 전후"로 적는다.
 * 정확한 날짜처럼 그리면 그날이 아니면 안 되는 것으로 읽힌다.
 *
 * **이 값으로 일정이 생기지 않는다.** 서버가 저장만 하고, 캘린더에 올리는 것은 환자가 보고
 * 정하는 흐름(1r-2-A)이다. AI가 날짜를 잘못 뽑을 수 있어서 조용히 일정이 생기면 안 된다.
 */
data class VisitFollowUp(val date: LocalDate, val text: String?, val approximate: Boolean)
