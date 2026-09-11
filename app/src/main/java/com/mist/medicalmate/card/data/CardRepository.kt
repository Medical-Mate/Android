package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
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
    suspend fun createFromSession(sessionId: Long): ApiResult<BriefCard>

    suspend fun cards(): ApiResult<List<CardListItem>>

    suspend fun card(cardId: Long): ApiResult<BriefCard>

    /**
     * 고친 값을 보낸다.
     *
     * **응답의 카드로 갈아타야 한다.** 확정된 카드를 고치면 서버가 새 버전을 만들고 id가
     * 달라진다. 옛 id를 들고 있으면 다음 수정이 엉뚱한 카드로 간다.
     */
    suspend fun update(cardId: Long, questions: List<String>): ApiResult<BriefCard>

    suspend fun confirm(cardId: Long): ApiResult<BriefCard>

    /**
     * 진료실에서 보여주는 화면.
     *
     * 확정한 카드만 열린다. 여는 순간이 전달 시각으로 기록되므로, 그냥 카드 조회로 대신하면
     * 서버에 "보여줬다"가 남지 않는다.
     */
    suspend fun handoff(cardId: Long): ApiResult<BriefCard>
}

internal class DefaultCardRepository
@Inject
constructor(private val api: CardApi, private val json: Json) :
    CardRepository {
    override suspend fun createFromSession(sessionId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.createFromSession(sessionId) }.map { it.toBriefCard() }

    override suspend fun cards(): ApiResult<List<CardListItem>> =
        apiCall(json) { api.cards() }.map { list -> list.map { it.toListItem() } }

    override suspend fun card(cardId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.card(cardId) }.map { it.toBriefCard() }

    /**
     * 지금은 질문 목록만 보낸다.
     *
     * 카드 본문(`onset`·`pattern` 등)은 서버가 고정 필드에서 가변 목록으로 바꾸는 중이라
     * 보낼 모양이 정해지지 않았다. 질문은 그 변경과 무관한 `List<String>`이라 먼저 붙였다.
     */
    override suspend fun update(cardId: Long, questions: List<String>): ApiResult<BriefCard> =
        apiCall(json) { api.update(cardId, UpdateCardRequest(questions = questions)) }.map { it.toBriefCard() }

    override suspend fun confirm(cardId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.confirm(cardId) }.map { it.toBriefCard() }

    override suspend fun handoff(cardId: Long): ApiResult<BriefCard> =
        apiCall(json) { api.handoff(cardId) }.map { it.toBriefCard(cardId) }
}

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

private fun CardSummaryResponse.toListItem() = CardListItem(
    id = cardId.toString(),
    title = title.orEmpty(),
    confirmed = status == "CONFIRMED",
    visited = visited,
    clinic = clinicName,
    writtenOn = OffsetDateTime.parse(createdAt).toLocalDate(),
)
