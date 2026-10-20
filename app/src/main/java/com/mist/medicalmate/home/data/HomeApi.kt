package com.mist.medicalmate.home.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

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
     * 카드 하나. 홈 배너 첫 장이 "카드는 준비됐어요"인지 가르는 데만 쓴다(#312) — 최근 카드에 잡힌 일정이
     * 있는지가 홈 응답의 카드 요약에 없다. `card` 도메인의 같은 경로를 따로 적은 것은 도메인끼리 서로를
     * 참조하지 않기 위해서다. 필요한 값만 받는다.
     */
    @GET("api/cards/{cardId}")
    suspend fun card(@Path("cardId") cardId: Long): HomeCardResponse
}

/**
 * @param appointment 카드에 잡힌 일정. 안 지난 것 중 가장 가까운 것, 다 지났으면 가장 최근 것이다. 일정이
 *   없으면 `null`이다.
 */
@Serializable
internal data class HomeCardResponse(val cardId: Long, val appointment: HomeCardAppointment? = null)

@Serializable
internal data class HomeCardAppointment(val appointmentId: Long, val scheduledOn: String)

/**
 * @param lastVisitedOn 마지막 진료일(`yyyy-MM-dd`). 없으면 아직 진료 기록이 없는 사람이다.
 * @param pendingRecordOn 진료 후 기록이 아직 없는 지난 일정 가운데 가장 최근 날. 없으면
 *   `null`이다(Backend#123). 서버가 일정 날짜로 남긴 기록이 있는지를 보고 14일까지만 거슬러
 *   찾으며, 취소한 일정은 세지 않는다. 일정과 기록을 잇는 열쇠가 없어서 — 기록은 카드에
 *   붙는데 일정은 카드 없이도 만들 수 있다 — 날짜로 견주는 것이 서버가 할 수 있는 전부다.
 * @param inProgressSession 작성 중이던 문답. 홈의 "이어서 하기"가 이것 하나로 그려진다.
 */
@Serializable
internal data class HomeResponse(
    val lastVisitedOn: String? = null,
    val nextAppointment: AppointmentResponse? = null,
    val pendingRecordOn: String? = null,
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
    /**
     * 문답이 끝났는지. 끝났으면 카드를 만들기 전이고 "임시 저장"이다(#338) — 이어서 하기를 누르면 문답이 아니라
     * 증상 정리 완료(1c-5)로 간다.
     */
    val ended: Boolean = false,
)

@Serializable
internal data class CardSummaryResponse(
    val cardId: Long,
    val title: String? = null,
    /** 주 호소. 배너가 "복부 통증 카드가 기다리고 있어요"라고 부를 때 쓴다. */
    val chiefComplaint: String? = null,
    val status: String? = null,
    val visited: Boolean = false,
    val clinicName: String? = null,
    val createdAt: String,
)
