package com.mist.medicalmate.card.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    /**
     * 카드를 지운다.
     *
     * **딸린 것이 갈린다.** 문답과 진료 기록은 함께 지워지고, 일정은 남고 연결만 끊긴다.
     * 증상 대화가 서버에 남아 있는데 카드만 지우면 환자는 지웠다고 생각하면서 증상·복용약이
     * 계속 보관되고, 반대로 카드를 지웠다고 병원 예약까지 사라지면 진료를 놓친다.
     *
     * 같은 문답에서 나온 카드는 버전을 가리지 않고 전부 지워진다. 환자에게는 한 장이고
     * 버전은 서버 사정이다. 확정·전달한 카드도 지울 수 있고 **되돌릴 수 없다.**
     */
    @DELETE("api/cards/{cardId}")
    suspend fun delete(@Path("cardId") cardId: Long)

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
    /** 아직 `null`이다. 목록 제목은 [chiefComplaint]를 쓴다. 환자가 말한 원문이라 길 수 있다. */
    val title: String? = null,
    val chiefComplaint: String? = null,
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
/**
 * @param title 아직 서버가 내려주지 않는다(`null`). 카드 제목은 [chiefComplaint]를 쓴다.
 * @param axes 8축이 늘 자리를 차지한다. 없는 축을 만들지 않는다.
 * @param version 확정 뒤 수정할 때마다 오른다. [parentCardId]가 이어받은 앞 카드다.
 * @param rejectedFields 검증에 걸려 저장되지 않은 필드 이름. 카드 만들기 자체는 성공한다.
 *   통째로 실패시키면 환자가 답한 문답이 날아간다.
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
    val chiefComplaint: String? = null,
    val axes: Map<String, AxisResponse> = emptyMap(),
    val redFlags: List<String> = emptyList(),
    val patientNotes: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val departmentGuidance: DepartmentGuidanceResponse? = null,
    val completeness: Double? = null,
    val minimallyComplete: Boolean = false,
    val rejectedFields: List<String> = emptyList(),
    val createdAt: String? = null,
    val confirmedAt: String? = null,
)

/**
 * 축 하나.
 *
 * @param status `NOT_ASKED` · `FILLED` · `UNKNOWN` · `SKIPPED` · `AMBIGUOUS`. 1턴째는 대부분
 *   `NOT_ASKED`다.
 * @param evidence 환자가 실제로 한 말. 지어낸 값과 들은 값을 가르는 자리다.
 */
@Serializable
internal data class AxisResponse(
    val axis: String? = null,
    val status: String? = null,
    val value: String? = null,
    val evidence: List<String> = emptyList(),
    val source: String? = null,
)

/**
 * 진료과 안내.
 *
 * 배열이다. 하나로 좁히지 않는다. 비어 있으면 줄을 숨긴다. [source]("의료인 자문 확인 전")를
 * 화면에 함께 보여야 하고 "추천"이라는 말은 쓰지 않는다.
 */
@Serializable
internal data class DepartmentGuidanceResponse(val departments: List<String> = emptyList(), val source: String? = null)

/** 전달 화면. 카드 조회와 본문이 같고 내부 추적값만 빠져 있다. */
@Serializable
internal data class HandoffResponse(
    val patient: PatientResponse? = null,
    val title: String? = null,
    val chiefComplaint: String? = null,
    val axes: Map<String, AxisResponse> = emptyMap(),
    val redFlags: List<String> = emptyList(),
    val patientNotes: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val departmentGuidance: DepartmentGuidanceResponse? = null,
    val confirmedAt: String? = null,
)

@Serializable
internal data class PatientResponse(val name: String? = null, val age: Int? = null, val sex: String? = null)

/**
 * 보낸 필드만 바뀐다. 건드리지 않은 것은 `null`로 두어 직렬화에서 빠지게 한다.
 *
 * 제목과 진료과는 고칠 수 없다. 서버가 받지 않는다.
 */
@Serializable
internal data class UpdateCardRequest(
    val chiefComplaint: String? = null,
    val axes: List<AxisEditRequest>? = null,
    val questions: List<String>? = null,
    val patientNotes: List<String>? = null,
)

@Serializable
internal data class AxisEditRequest(val axis: String, val value: String)
