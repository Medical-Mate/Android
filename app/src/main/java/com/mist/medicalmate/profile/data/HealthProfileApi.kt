package com.mist.medicalmate.profile.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * 건강 프로필 API. `/v3/api-docs`의 `GET /api/me/health-profile` 기준이다.
 *
 * 지금은 읽기만 쓴다. 저장(`PUT`)은 신상정보 화면(1b)을 옮길 때 함께 붙인다. 문서가
 * "온보딩 5단계를 모아 한 번에 저장합니다. 단계별 저장은 없습니다"라고 적고 있어, 그
 * 화면의 임시저장을 어떻게 할지 정한 뒤에 손대야 한다(#137).
 *
 * 프로필이 없어도 404가 아니라 빈 값이 온다.
 */
internal interface HealthProfileApi {
    @GET("api/me/health-profile")
    suspend fun healthProfile(): HealthProfileResponse
}

/**
 * @param name 카카오에서 받은 값이 채워져 온다. 사용자가 넣은 값이면 [sources]가 알려준다.
 * @param onboardingCompleted 온보딩을 실제로 마쳤는지. 카카오 값이 채워진 것만으로는 false다.
 * @param canStartIntake 문답을 시작할 수 있는지. 나이와 성별이 있어야 true다. 없이
 *   `POST /api/sessions`를 부르면 400이 온다.
 */
@Serializable
internal data class HealthProfileResponse(
    val name: String? = null,
    val birthYear: Int? = null,
    val birthMonthDay: String? = null,
    val age: Int? = null,
    val sex: String? = null,
    val onboardingCompleted: Boolean = false,
    val canStartIntake: Boolean = false,
)
