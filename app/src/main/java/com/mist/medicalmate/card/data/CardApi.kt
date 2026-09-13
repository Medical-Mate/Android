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
    /**
     * 문답으로 카드를 만든다.
     *
     * 본문 전체가 선택이다. 1m-B에서 병원을 골랐으면 함께 보내고, 건너뛰었으면 빈 본문을
     * 보낸다. 안 보내면 병원 없이 만들어진다.
     */
    @POST("api/sessions/{sessionId}/card")
    suspend fun createFromSession(@Path("sessionId") sessionId: Long, @Body request: GenerateCardRequest): CardResponse

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
}

@Serializable
internal data class CardSummaryResponse(
    val cardId: Long,
    /**
     * 진료받을 병원. 카드가 드는 값이다.
     *
     * [clinicName]과 다른 축이다. 그쪽은 진료를 **받은** 병원이라 진료 기록에서 오고,
     * 진료 전 카드는 비어 있다.
     */
    val clinic: ClinicResponse? = null,
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
    /**
     * 진료받을 병원. 이 카드를 어디로 가져갈 것인가다.
     *
     * **응답에 병원이 셋이고 이것만 쓴다.** `appointment`는 연결된 일정의 병원이고 `visit`은
     * 진료를 받은 병원이라 셋이 다 다를 수 있다. 시안 1e-1의 "진료받을 병원"은 이 값이다.
     * 안 골랐으면 없다 — 1m-B에 건너뛰기가 있어서 정상 상태다.
     */
    val clinic: ClinicResponse? = null,
)

/** 병원 이름과 주소. */
@Serializable
internal data class ClinicResponse(val name: String? = null, val address: String? = null)

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

/**
 * 카드 머리의 환자.
 *
 * 건강 정보가 여기 실린다. 전에는 없어서 화면이 `GET /api/me/health-profile`에서 읽어
 * 얹었고, 카드를 만든 시점이 아니라 **보는 시점의 프로필**이 찍히는 것이 한계였다. 서버가
 * 카드에 박아 주면서 그 우회를 걷어냈다(Backend#84).
 *
 * 알러지만 한 줄이고 나머지 둘은 목록이다. 프로필 응답과 같은 모양이라 나누는 규칙도 같다.
 */
@Serializable
internal data class PatientResponse(
    val name: String? = null,
    val age: Int? = null,
    val sex: String? = null,
    val allergies: CardTextFieldResponse? = null,
    val medications: CardListFieldResponse? = null,
    val conditions: CardListFieldResponse? = null,
)

/** @param status `KNOWN` 일 때만 값이 있다. `NONE`은 없다고 답한 것이고 `UNKNOWN`은 모른다는 것이다. */
@Serializable
internal data class CardTextFieldResponse(val status: String? = null, val text: String? = null)

@Serializable
internal data class CardListFieldResponse(val status: String? = null, val items: List<String> = emptyList())

/**
 * 보낸 필드만 바뀐다. 건드리지 않은 것은 `null`로 두어 직렬화에서 빠지게 한다.
 *
 * 제목과 진료과는 고칠 수 없다. 서버가 받지 않는다.
 */
@Serializable
internal data class GenerateCardRequest(val clinic: ClinicRequest? = null)

@Serializable
internal data class UpdateCardRequest(
    val chiefComplaint: String? = null,
    val axes: List<AxisEditRequest>? = null,
    val questions: List<String>? = null,
    val patientNotes: List<String>? = null,
    val clinic: ClinicRequest? = null,
)

/** 진료받을 병원. 병원 검색이 준 항목을 그대로 옮긴다. */
@Serializable
internal data class ClinicRequest(val name: String, val address: String? = null)

@Serializable
internal data class AxisEditRequest(val axis: String, val value: String)
