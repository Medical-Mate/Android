package com.mist.medicalmate.visit.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
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
}

@Serializable
internal data class VisitSummaryResponse(
    val visitId: Long,
    val cardId: Long? = null,
    val cardTitle: String? = null,
    val clinicName: String? = null,
    val visitedOn: String,
)

/**
 * @param whatWasDone 진료에서 한 것.
 * @param result 들은 결과.
 * @param prescription 처방.
 * @param rawNote 환자가 적은 원문. 상세에만 온다.
 */
@Serializable
internal data class VisitResponse(
    val visitId: Long,
    val cardId: Long? = null,
    val clinicName: String? = null,
    val visitedOn: String? = null,
    val whatWasDone: String? = null,
    val result: String? = null,
    val prescription: String? = null,
    val rawNote: String? = null,
)

@Serializable
internal data class CreateVisitRequest(
    val clinicName: String? = null,
    val visitedOn: String? = null,
    val whatWasDone: String? = null,
    val result: String? = null,
    val prescription: String? = null,
    val rawNote: String? = null,
)
