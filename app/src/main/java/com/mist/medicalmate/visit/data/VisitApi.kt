package com.mist.medicalmate.visit.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 진료 후 기록 API. `/v3/api-docs`의 `/api/me/visits`·`/api/visits`·`/api/cards/{id}/visit`.
 *
 * 녹음은 저장하지 않는다. 오디오 컬럼 자체가 없다. 음성으로 적어도 변환한 글만 간다.
 */
internal interface VisitApi {
    /**
     * 기록 목록. 최근 진료일 순이다.
     *
     * 원문(`rawNote`)이 담기지 않는다. 증상·복용약이 섞인 긴 글이라 목록마다 실어 나를 이유가
     * 없다. 월별 묶음은 앱이 만든다.
     */
    @GET("api/me/visits")
    suspend fun visits(): List<VisitSummaryResponse>

    @GET("api/visits/{visitId}")
    suspend fun visit(@Path("visitId") visitId: Long): VisitResponse

    /**
     * 진료 후 기록을 남긴다.
     *
     * **확정한 카드에만** 남길 수 있고 카드 하나에 기록 하나다. 모든 항목이 선택이라
     * `rawNote`만 적어도 저장된다. 병원을 막 나온 사람에게 필수 입력을 요구하면 아무것도
     * 안 남는다.
     */
    @POST("api/cards/{cardId}/visit")
    suspend fun create(@Path("cardId") cardId: Long, @Body request: CreateVisitRequest): VisitResponse

    /**
     * 기록 하나를 지운다. 카드는 남는다.
     *
     * 전에는 이 자리가 없어서 기록 삭제가 카드 삭제로 나갔고, 기록 한 건을 지우려던 사람이
     * 카드와 문답까지 잃었다(#157).
     */
    @DELETE("api/visits/{visitId}")
    suspend fun delete(@Path("visitId") visitId: Long)

    /**
     * 적어 둔 메모를 항목으로 나눈다.
     *
     * **저장하지 않는다.** 카드에도 매이지 않아서 저장 전에 부를 수 있다. 나눈 결과를 그대로
     * [create]의 요청으로 옮기면 된다.
     */
    @POST("api/visits/classify")
    suspend fun classify(@Body request: ClassifyMemoRequest): ClassifyMemoResponse
}

@Serializable
internal data class VisitSummaryResponse(
    val visitId: Long,
    val cardId: Long? = null,
    val cardTitle: String? = null,
    val clinicName: String? = null,
    val visitedOn: String,
    /**
     * 재방문. 상세와 같은 모양이다(Backend#101).
     *
     * 전에는 `followUpDate` 한 칸이라 "2주 뒤"에서 나온 날짜인지 알 수 없었다. 이제
     * [FollowUpResponse.approximate]가 함께 온다.
     */
    val followUp: FollowUpResponse? = null,
)

/**
 * 기록 하나.
 *
 * **항목이 가변이다**(#178). 전에는 `whatWasDone`·`result`·`prescription` 셋으로 고정이었다.
 * 지금은 브리핑 카드와 같은 모양의 축 맵이고 AI가 축을 늘려도 실린다. 못 찾은 항목은 빈 값이
 * 아니라 **키가 없다.**
 *
 * @param rawNote 환자가 적은 원문. 상세에만 온다.
 */
@Serializable
internal data class VisitResponse(
    val visitId: Long,
    val cardId: Long? = null,
    val clinicName: String? = null,
    val visitedOn: String? = null,
    val axes: Map<String, VisitAxisResponse> = emptyMap(),
    val followUp: FollowUpResponse? = null,
    val patientNotes: List<String> = emptyList(),
    val rawNote: String? = null,
)

/**
 * 축 하나.
 *
 * 카드의 축과 모양이 같지만 타입을 함께 쓰지 않는다. 한 도메인이 다른 도메인을 참조하지 않고,
 * 두 계약이 지금 같아 보여도 같이 움직인다는 보장이 없다.
 */
@Serializable
internal data class VisitAxisResponse(
    val axis: String? = null,
    val status: String? = null,
    val value: String? = null,
    val evidence: List<String> = emptyList(),
    val source: String? = null,
)

/** @param approximate "2주 뒤"처럼 범위로 말한 것. 화면이 "전후"를 붙인다. */
@Serializable
internal data class FollowUpResponse(
    val date: String? = null,
    val text: String? = null,
    val approximate: Boolean = false,
)

/**
 * 남길 기록.
 *
 * **`status`와 `source`를 보내지 않는다.** 값이 있으면 `FILLED`, 비었으면 `UNKNOWN`이고 출처는
 * 서버가 `PATIENT_EDIT`로 박는다. 앱이 "AI가 뽑았다"고 주장할 수 있으면 의사 화면의 출처
 * 표시가 의미를 잃는다.
 *
 * @param patientNotes 어느 항목에도 들어가지 않은 문장.
 */
@Serializable
internal data class CreateVisitRequest(
    val clinicName: String? = null,
    val visitedOn: String? = null,
    val axes: List<VisitAxisRequest> = emptyList(),
    val followUp: FollowUpRequest? = null,
    val patientNotes: List<String> = emptyList(),
    val rawNote: String? = null,
)

@Serializable
internal data class VisitAxisRequest(val axis: String, val value: String)

/**
 * @param labels 직전 응답의 분류. **있으면 반드시 함께 보낸다.** 안 보내면 줄 하나를 옮길
 *   때마다 AI 모델 호출이 나가고, 그 비용이 서버 크레딧과 같은 주머니에서 빠진다. 보내면
 *   모델을 부르지 않고 재조립만 한다.
 */
@Serializable
internal data class ClassifyMemoRequest(
    val memo: String,
    val visitedOn: String? = null,
    val clinicName: String? = null,
    val labels: Map<String, String>? = null,
)

/**
 * @param sentences 메모를 문장으로 나눈 것. 인덱스가 [labels]의 키다.
 * @param patientNotes 어느 항목에도 들어가지 않은 문장.
 */
@Serializable
internal data class ClassifyMemoResponse(
    val axes: Map<String, VisitAxisResponse> = emptyMap(),
    val sentences: List<String> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val patientNotes: List<String> = emptyList(),
    val followUp: FollowUpResponse? = null,
)

@Serializable
internal data class FollowUpRequest(
    val date: String? = null,
    val text: String? = null,
    val approximate: Boolean = false,
)
