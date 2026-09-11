package com.mist.medicalmate.card.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 브리핑 카드 API. `/v3/api-docs`의 `/api/cards`와 `/api/me/cards` 기준이다.
 *
 * 카드는 `DRAFT`로 만들어지고 [confirm]이 따로 있다. 그것이 이 도메인의 임시저장이다.
 */
internal interface CardApi {
    /** 문답을 카드로 만든다. 검증에 걸린 필드는 `UNKNOWN`으로 저장되고 이름이 온다. */
    @POST("api/sessions/{sessionId}/card")
    suspend fun createFromSession(@Path("sessionId") sessionId: Long): CardResponse

    /** 목록에는 본문이 없다. 상세는 [card]로 본다. */
    @GET("api/me/cards")
    suspend fun cards(): List<CardSummaryResponse>

    @GET("api/cards/{cardId}")
    suspend fun card(@Path("cardId") cardId: Long): CardResponse

    /**
     * 보낸 필드만 바뀐다.
     *
     * 확정된 카드는 고치지 않고 이어받은 새 버전이 만들어진다. 응답의 `cardId`와 `version`이
     * 달라지므로 호출자가 그 값으로 갈아타야 한다. 의사가 이미 본 카드가 뒤바뀌면 안 된다.
     */
    @PATCH("api/cards/{cardId}")
    suspend fun update(@Path("cardId") cardId: Long, @Body request: UpdateCardRequest): CardResponse

    /** 이미 확정한 카드를 다시 확정하면 400이다. */
    @POST("api/cards/{cardId}/confirm")
    suspend fun confirm(@Path("cardId") cardId: Long): CardResponse

    /**
     * 진료실에서 의사에게 보여주는 화면.
     *
     * 확정한 카드만 열린다. 초안이면 400이다. 여는 순간이 전달 시각으로 기록된다.
     */
    @GET("api/cards/{cardId}/handoff")
    suspend fun handoff(@Path("cardId") cardId: Long): HandoffResponse
}

@Serializable
internal data class CardSummaryResponse(
    val cardId: Long,
    val title: String? = null,
    val status: String? = null,
    val visited: Boolean = false,
    val clinicName: String? = null,
    val createdAt: String,
)

/**
 * @param version 확정 뒤 수정할 때마다 오른다. [parentCardId]가 이어받은 앞 카드다.
 * @param rejectedFields 검증에 걸려 `UNKNOWN`으로 저장된 필드 이름. 카드 만들기 자체는
 *   성공한다. 통째로 실패시키면 환자가 답한 문답이 날아간다.
 */
@Serializable
internal data class CardResponse(
    val cardId: Long,
    val status: String? = null,
    val version: Int = 1,
    val parentCardId: Long? = null,
    val sessionId: Long? = null,
    val patient: PatientResponse? = null,
    val title: String? = null,
    val onset: TextFieldResponse? = null,
    val pattern: TextFieldResponse? = null,
    val site: SiteResponse? = null,
    val medications: MedicationsResponse? = null,
    val allergies: TextFieldResponse? = null,
    val questions: List<String> = emptyList(),
    val suggestedDepartment: String? = null,
    val rejectedFields: List<String> = emptyList(),
    val createdAt: String? = null,
    val confirmedAt: String? = null,
)

/** 전달 화면. `evidence`·`pipelineVersion` 같은 내부 추적값이 빠져 있다. */
@Serializable
internal data class HandoffResponse(
    val patient: PatientResponse? = null,
    val title: String? = null,
    val onset: TextFieldResponse? = null,
    val pattern: TextFieldResponse? = null,
    val site: SiteResponse? = null,
    val medications: MedicationsResponse? = null,
    val allergies: TextFieldResponse? = null,
    val questions: List<String> = emptyList(),
    val suggestedDepartment: String? = null,
    val confirmedAt: String? = null,
)

@Serializable
internal data class PatientResponse(val name: String? = null, val age: Int? = null, val sex: String? = null)

/** @param status `KNOWN` · `NONE` · `UNKNOWN`. 셋을 같게 그리면 안 된다. */
@Serializable
internal data class TextFieldResponse(val status: String? = null, val text: String? = null)

@Serializable
internal data class SiteResponse(
    val status: String? = null,
    val text: String? = null,
    val codes: List<String> = emptyList(),
)

@Serializable
internal data class MedicationsResponse(val status: String? = null, val items: List<MedicationResponse> = emptyList())

@Serializable
internal data class MedicationResponse(val name: String? = null, val note: String? = null)

/** 보낸 필드만 바뀐다. 건드리지 않은 것은 `null`로 두어 직렬화에서 빠지게 한다. */
@Serializable
internal data class UpdateCardRequest(
    val title: String? = null,
    val onset: TextFieldRequest? = null,
    val pattern: TextFieldRequest? = null,
    val allergies: TextFieldRequest? = null,
    val questions: List<String>? = null,
)

@Serializable
internal data class TextFieldRequest(val status: String, val text: String? = null)
