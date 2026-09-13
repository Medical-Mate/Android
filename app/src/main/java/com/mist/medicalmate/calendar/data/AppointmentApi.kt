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
 * 라고 적었고, 실제로 날짜만 오고 남은 날수는 오지 않는다.
 *
 * **날짜와 시각이 따로 온다**(#202). 전에는 `scheduledAt` 하나였는데 서버가
 * `scheduledOn`·`scheduledTime`으로 갈랐다. 날짜로 견주므로 시간대 때문에 하루가 밀리지
 * 않고, `scheduledTime`이 없으면 시간 미정이다.
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
 * @param scheduledTime 없으면 시간 미정이다.
 * @param cards 가져갈 카드. 여러 장 붙을 수 있다.
 * @param origin 어디서 만든 일정인지. 진료 후 기록의 재방문이면 `VISIT_FOLLOW_UP`이다.
 */
@Serializable
internal data class AppointmentResponse(
    val appointmentId: Long,
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledOn: String,
    val scheduledTime: String? = null,
    val status: String? = null,
    val origin: String? = null,
    val cards: List<LinkedCardResponse> = emptyList(),
    val todos: List<TodoResponse> = emptyList(),
)

/** 일정에 붙은 카드. */
@Serializable
internal data class LinkedCardResponse(val cardId: Long, val title: String? = null)

/** 진료 전 할 일 한 줄. 일정에 매달린다. */
@Serializable
internal data class TodoResponse(val text: String, val done: Boolean = false)

/** [scheduledOn]만 필수다. 시각을 빼면 "시간 미정"으로 만들어진다. */
@Serializable
internal data class CreateAppointmentRequest(
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledOn: String,
    val scheduledTime: String? = null,
    val cardIds: List<Long> = emptyList(),
    val origin: String? = null,
    val todos: List<TodoResponse> = emptyList(),
)

/**
 * 보낸 필드만 바뀐다.
 *
 * **시각을 다시 미정으로 되돌리려면 [clearTime]을 세운다.** `scheduledTime = null`은
 * "안 바꿈"이다. null을 두 뜻으로 쓰면 시각을 지울 방법이 없어서 서버가 플래그를 따로 뒀다.
 *
 * **날짜는 지울 수 없다.** 날짜 없는 일정은 캘린더에 그릴 자리가 없다.
 */
@Serializable
internal data class UpdateAppointmentRequest(
    val clinicName: String? = null,
    val department: String? = null,
    val purpose: String? = null,
    val scheduledOn: String? = null,
    val scheduledTime: String? = null,
    val clearTime: Boolean? = null,
    val status: String? = null,
    /** 통째로 갈아끼운다. `null`이 "안 바꿈", `[]`가 "전부 뗌"이다. */
    val cardIds: List<Long>? = null,
    /** 통째로 갈아끼운다. 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다. */
    val todos: List<TodoResponse>? = null,
)
