package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import com.mist.medicalmate.card.ui.BriefCardHospital
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 브리핑 카드 읽고 쓰기.
 *
 * 카드는 `DRAFT`로 만들어지고 [confirm]이 따로 있다. 그것이 이 도메인의 임시저장이다.
 */
interface CardRepository {
    /** 문답을 카드로 만든다. 1c-5의 "브리핑 카드 만들기"가 여기로 온다. */
    /**
     * 문답으로 카드를 만든다.
     *
     * [clinic]은 1m-B에서 고른 진료받을 병원이다. 건너뛰었으면 null이고 병원 없이 만들어진다.
     */
    suspend fun createFromSession(sessionId: Long, clinic: BriefCardHospital? = null): ApiResult<BriefCard>

    suspend fun cards(): ApiResult<List<CardListItem>>

    suspend fun card(cardId: Long): ApiResult<BriefCard>

    /**
     * 고친 값을 보낸다.
     *
     * **응답의 카드로 갈아타야 한다.** 확정된 카드를 고치면 서버가 새 버전을 만들고 id가
     * 달라진다. 옛 id를 들고 있으면 다음 수정이 엉뚱한 카드로 간다.
     */
    /** [clinic]은 1e-1의 `변경`으로 고른 병원이다. 안 바꿨으면 null이라 보내지 않는다. */
    suspend fun update(
        cardId: Long,
        axes: List<AxisEdit>,
        questions: List<String>,
        clinic: BriefCardHospital? = null,
    ): ApiResult<BriefCard>

    /**
     * 카드를 지운다. 문답과 진료 기록이 함께 지워지고 일정은 연결만 끊긴다. 되돌릴 수 없다.
     */
    suspend fun delete(cardId: Long): ApiResult<Unit>

    /**
     * 여러 장을 지우고 **실제로 지워진 id만** 돌려준다.
     *
     * 한 번에 지우는 API가 없어서 한 장씩 부른다. 일부가 실패해도 나머지는 계속 지운다.
     * 하나 실패했다고 멈추면 이미 지운 것과 화면이 어긋나고, 다시 누르면 지운 것을 또
     * 부르게 된다.
     */
    suspend fun deleteAll(cardIds: Set<String>): Set<String>

    suspend fun confirm(cardId: Long): ApiResult<BriefCard>
}

internal class DefaultCardRepository
@Inject
constructor(private val api: CardApi, private val json: Json) :
    CardRepository {
    override suspend fun createFromSession(sessionId: Long, clinic: BriefCardHospital?): ApiResult<BriefCard> =
        apiCall(json) { api.createFromSession(sessionId, GenerateCardRequest(clinic.toRequest())) }
            .map { it.toBriefCard() }

    override suspend fun cards(): ApiResult<List<CardListItem>> =
        apiCall(json) { api.cards() }.map { list -> list.map { it.toListItem() } }

    override suspend fun card(cardId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.card(cardId) }.map { it.toBriefCard() }

    /**
     * 바뀐 축과 질문만 보낸다.
     *
     * 제목과 진료과는 서버가 수정을 받지 않는다. 안 바뀐 축을 함께 보내지 않는 이유는 그것이
     * PATCH이기 때문이다. 보낸 것만 바뀐다.
     */
    override suspend fun update(
        cardId: Long,
        axes: List<AxisEdit>,
        questions: List<String>,
        clinic: BriefCardHospital?,
    ): ApiResult<BriefCard> = apiCall(json) {
        api.update(
            cardId,
            UpdateCardRequest(
                axes = axes.map { AxisEditRequest(axis = it.axis, value = it.value) }.takeIf { it.isNotEmpty() },
                questions = questions,
                clinic = clinic.toRequest(),
            ),
        )
    }.map { it.toBriefCard() }

    override suspend fun delete(cardId: Long): ApiResult<Unit> = apiCall(json) { api.delete(cardId) }

    override suspend fun deleteAll(cardIds: Set<String>): Set<String> = cardIds
        .mapNotNull { id -> id.toLongOrNull()?.takeIf { delete(it) is ApiResult.Success }?.let { id } }
        .toSet()

    override suspend fun confirm(cardId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.confirm(cardId) }.map { it.toBriefCard() }
}

/** 고친 축 하나. 서버가 `{"axis":"onset","value":"3주 전"}`으로 받는다. */
data class AxisEdit(val axis: String, val value: String)

/**
 * 목록의 카드 한 줄. 본문은 담기지 않는다.
 *
 * [visited]가 진료를 마쳤는지다. [confirmed]와 다른 축이라 따로 둔다. 문서가 "진료 완료"
 * 뱃지는 [visited]로 판단하라고 적었다. 병원명은 선택 입력이라 진료를 마쳤어도 비어 있을
 * 수 있어서 그것으로 판단하면 안 된다.
 */
data class CardListItem(
    val id: String,
    val title: String,
    val confirmed: Boolean,
    val visited: Boolean,
    val clinic: String?,
    val writtenOn: LocalDate,
)

/**
 * 목록 한 줄.
 *
 * 서버가 `title`을 아직 내려주지 않는다. 대신 환자가 말한 원문이 오는데 길 수 있어서 줄인다.
 * 목록의 줄은 한 줄짜리라 넘치면 잘리기만 하고 무엇인지 알 수 없게 된다.
 */
internal fun CardSummaryResponse.toListItem() = CardListItem(
    id = cardId.toString(),
    title = (title ?: chiefComplaint)?.shorten().orEmpty(),
    confirmed = status == "CONFIRMED",
    visited = visited,
    // 목록 줄의 병원은 "진료받을 병원"이다(Backend#101). `clinicName`은 진료를 받은 병원이라
    // 진료 전 카드는 비어 있다. 시안 1j-4의 `09.04 작성 · 서울OO병원 내과`가 이 값이다.
    clinic = clinic?.name?.takeIf { it.isNotBlank() } ?: clinicName,
    writtenOn = OffsetDateTime.parse(createdAt).toLocalDate(),
)

/** 화면의 병원을 요청 모양으로. 없으면 보내지 않는다. */
private fun BriefCardHospital?.toRequest(): ClinicRequest? =
    this?.let { ClinicRequest(name = it.name, address = it.address) }

/** 목록 제목의 길이 상한. 넘으면 말줄임을 붙인다. */
private fun String.shorten(): String = if (length <= TITLE_MAX) this else take(TITLE_MAX).trimEnd() + "…"

private const val TITLE_MAX = 24
