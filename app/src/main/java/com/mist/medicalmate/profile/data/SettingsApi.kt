package com.mist.medicalmate.profile.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * 계정 설정 API. `/v3/api-docs`의 `/api/me/settings`.
 *
 * **값이 하나뿐이다.** 진료 하루 전 알림을 받을지다. 나머지 토글 둘("브리핑 카드 자동 저장"·
 * "진료실 화면 밝기 최대")은 이 기기에서 어떻게 보일지의 문제라 서버가 읽을 일이 없고
 * `DataStore`에 둔다(Backend#85).
 *
 * 이 값만 계정에 붙는 이유는 받을지 말지가 기기 취향이 아니라 **그 사람의 선택**이기
 * 때문이다. 기기를 바꾸거나 앱을 다시 깔면 "안 받겠다"고 한 사람에게 알림이 다시 간다.
 *
 * **알림을 예약하는 것은 여전히 앱이다.** 이 값은 예약할지 말지를 정하는 값이다.
 */
internal interface SettingsApi {
    @GET("api/me/settings")
    suspend fun settings(): SettingsResponse

    @PATCH("api/me/settings")
    suspend fun update(@Body request: SettingsRequest): SettingsResponse
}

@Serializable
internal data class SettingsResponse(val visitReminderEnabled: Boolean = true)

@Serializable
internal data class SettingsRequest(val visitReminderEnabled: Boolean)
