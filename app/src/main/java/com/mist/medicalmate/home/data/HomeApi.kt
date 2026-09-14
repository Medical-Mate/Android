package com.mist.medicalmate.home.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 홈 API. `/v3/api-docs`의 `GET /api/me/home` 기준이다.
 *
 * 홈에 필요한 네 덩어리를 한 번에 준다. 문서가 "문구와 D-day는 서버가 만들지 않습니다"라고
 * 적었고 실제로 날짜와 숫자만 온다. 문구를 서버가 내려주면 말투를 바꿀 때마다 배포해야 하고
 * 시간대가 어긋나면 날짜 수가 하루 틀린다.
 *
 * 신규 사용자는 404가 아니라 전부 `null`과 빈 배열이다. 그것이 1n-2 화면이다.
 */
internal interface HomeApi {
    @GET("api/me/home")
    suspend fun home(): HomeResponse

    /**
     * 그 달의 일정.
     *
     * 홈 응답에 지난 일정이 없어서 따로 부른다(#235). 기록이 빠진 지난 진료를 알리려면 이미
     * 지나간 일정을 알아야 하는데 `nextAppointment`는 앞으로의 것 하나뿐이다.
     *
     * 캘린더의 같은 엔드포인트를 쓰지만 그쪽 Repository를 가져다 쓰지 않는다. 홈이 캘린더를
     * 참조하면 두 도메인이 붙는다.
     */
    @GET("api/me/appointments")
    suspend fun appointments(@Query("year") year: Int, @Query("month") month: Int): List<AppointmentResponse>
}

/**
 * @param lastVisitedOn 마지막 진료일(`yyyy-MM-dd`). 없으면 아직 진료 기록이 없는 사람이다.
 * @param inProgressSession 작성 중이던 문답. 홈의 "이어서 하기"가 이것 하나로 그려진다.
 */
@Serializable
internal data class HomeResponse(
    val lastVisitedOn: String? = null,
    val nextAppointment: AppointmentResponse? = null,
    val inProgressSession: InProgressSessionResponse? = null,
    val recentCards: List<CardSummaryResponse> = emptyList(),
)

/**
 * 다음 일정.
 *
 * **날짜와 시각이 따로 온다**(#202). [scheduledTime]이 없으면 시간 미정이다. 카드도 한 장이
 * 아니라 목록이다. 전에는 `scheduledAt` 하나와 `cardId`·`cardTitle`이었고, 서버가 모양을
 * 바꾸면서 홈에서 앱이 죽었다.
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
)

/** 일정에 붙은 카드. */
@Serializable
internal data class LinkedCardResponse(val cardId: Long, val title: String? = null)

/**
 * 임시저장된 문답.
 *
 * 진행도가 숫자 둘로 온다. "3단계 중 2단계까지"를 서버가 문자열로 만들지 않는다.
 */
@Serializable
internal data class InProgressSessionResponse(
    val sessionId: Long,
    val siteText: String? = null,
    val progressCurrent: Int,
    val progressTotal: Int,
)

@Serializable
internal data class CardSummaryResponse(
    val cardId: Long,
    val title: String? = null,
    val status: String? = null,
    val visited: Boolean = false,
    val clinicName: String? = null,
    val createdAt: String,
)
