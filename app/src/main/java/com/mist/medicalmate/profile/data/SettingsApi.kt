package com.mist.medicalmate.profile.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * 계정 설정 API. `/v3/api-docs`의 `/api/me/settings`.
 *
 * **진료 알림 설정만 있다**(Backend#176). 진료 전 알림 · 진료 후 기록 알림을 받을지와, 시간 미정 일정에서
 * 각각 언제 받을지다. 받을지와 언제 받을지는 기기 취향이 아니라 **그 사람의 선택**이라 계정에 붙는다 — 기기를
 * 바꾸거나 앱을 다시 깔면 "안 받겠다"고 한 사람에게 알림이 다시 간다. "헤이 메이트로 부르기"는 기기 사양에 묶여
 * 서버에 두지 않는다.
 *
 * **PATCH는 보낸 필드만 바꾼다.** 빠지거나 `null`인 필드는 그대로다. 시각은 `HH:mm`만 받고 응답도 늘 `HH:mm`이다.
 *
 * **알림을 예약하는 것은 여전히 앱이다.** 서버가 보내는 것은 없다.
 */
internal interface SettingsApi {
    @GET("api/me/settings")
    suspend fun settings(): SettingsResponse

    @PATCH("api/me/settings")
    suspend fun update(@Body request: SettingsRequest): SettingsResponse
}

@Serializable
internal data class SettingsResponse(
    val visitReminderEnabled: Boolean = true,
    val recordReminderEnabled: Boolean = true,
    val visitReminderTime: String? = null,
    val recordReminderTime: String? = null,
)

@Serializable
internal data class SettingsRequest(
    val visitReminderEnabled: Boolean? = null,
    val recordReminderEnabled: Boolean? = null,
    val visitReminderTime: String? = null,
    val recordReminderTime: String? = null,
)
