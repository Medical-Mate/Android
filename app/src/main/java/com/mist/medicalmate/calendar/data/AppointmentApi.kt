package com.mist.medicalmate.calendar.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 캘린더 일정 API. `/v3/api-docs`의 `/api/me/appointments` 기준이다.
 *
 * 날짜 경계는 서버가 한국 시각으로 자른다. UTC로 자르면 오전 9시 이전 일정이 전날로 밀린다.
 *
 * **D-day는 앱이 센다.** 문서가 "서버가 계산하면 사용자 시간대와 어긋날 때 하루 틀립니다"
 * 라고 적었고, 실제로 `scheduledAt`만 오고 남은 날수는 오지 않는다.
 */
internal interface AppointmentApi {
    /** [date]를 주면 그 하루, [year]·[month]를 주면 그 달이다. */
    @GET("api/me/appointments")
    suspend fun appointments(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("date") date: String? = null,
    ): List<AppointmentResponse>

    /** 아직 안 지났고 취소되지 않은 것만 가까운 순으로 온다. */
    @GET("api/me/appointments/upcoming")
    suspend fun upcoming(): List<AppointmentResponse>

    /** 카드 연결은 선택이다. 카드 없이 "다음 주 치과"만 적을 수 있다. */
    @POST("api/me/appointments")
    suspend fun create(@Body request: CreateAppointmentRequest): AppointmentResponse

    @PATCH("api/me/appointments/{appointmentId}")
    suspend fun update(
        @Path("appointmentId") appointmentId: Long,
        @Body request: UpdateAppointmentRequest,
    ): AppointmentResponse

    @DELETE("api/me/appointments/{appointmentId}")
    suspend fun delete(@Path("appointmentId") appointmentId: Long)
}

/**
 * @param purpose "재진"처럼 무엇 하러 가는지. 화면의 "서울OO병원 내과 재진"은 앱이
 *   [clinicName]·[department]·[purpose] 셋을 합쳐 만든다.
 */
@Serializable
internal data class AppointmentResponse(
    val appointmentId: Long,
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledAt: String,
    val status: String? = null,
    val cardId: Long? = null,
    val cardTitle: String? = null,
)

@Serializable
internal data class CreateAppointmentRequest(
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledAt: String,
    val cardId: Long? = null,
)

/**
 * 보낸 필드만 바뀐다.
 *
 * **카드 연결을 끊으려면 [clearCard]를 세운다.** `cardId = null`은 "안 바꿈"이다. null을
 * 두 뜻으로 쓰면 연결을 끊을 방법이 없어서 서버가 플래그를 따로 뒀다.
 */
@Serializable
internal data class UpdateAppointmentRequest(
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledAt: String? = null,
    val status: String? = null,
    val cardId: Long? = null,
    val clearCard: Boolean? = null,
)
