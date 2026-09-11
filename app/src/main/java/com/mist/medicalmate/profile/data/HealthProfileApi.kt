package com.mist.medicalmate.profile.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * 건강 프로필 API. `/v3/api-docs`의 `GET`·`PUT /api/me/health-profile` 기준이다.
 *
 * 프로필이 없어도 404가 아니라 빈 값이 온다.
 */
internal interface HealthProfileApi {
    @GET("api/me/health-profile")
    suspend fun healthProfile(): HealthProfileResponse

    /**
     * 통째로 덮어쓴다.
     *
     * 단계별 저장이 없다. 서버 문서가 "온보딩 5단계를 모아 한 번에 저장합니다"라고 적었고,
     * 요청도 부분 갱신이 아니라 여섯 필드를 모두 요구한다. 1b가 묻지 않는 이름·출생연도·성별은
     * 읽어 온 값을 그대로 되돌려 보낸다.
     */
    @PUT("api/me/health-profile")
    suspend fun save(@Body request: HealthProfileRequest): HealthProfileResponse
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
    val medications: ListFieldResponse? = null,
    val conditions: ListFieldResponse? = null,
    val allergies: TextFieldResponse? = null,
    val sources: SourcesResponse? = null,
    val onboardingCompleted: Boolean = false,
    val canStartIntake: Boolean = false,
)

/**
 * 저장 요청.
 *
 * 여섯 필드가 모두 필수다. null을 보내면 400이다.
 */
@Serializable
internal data class HealthProfileRequest(
    val name: String,
    val birthYear: Int,
    val birthMonthDay: String? = null,
    val sex: String,
    val medications: ListFieldRequest,
    val conditions: ListFieldRequest,
    val allergies: TextFieldRequest,
)

/**
 * 여러 개를 받는 칸. 복용약과 기저질환이다.
 *
 * [status]가 `NONE`이면 [items]는 비어 있어야 의미가 맞는다. 서버가 강제하지는 않는다.
 */
@Serializable
internal data class ListFieldRequest(val status: String, val items: List<String> = emptyList())

/** 한 줄로 받는 칸. 알러지다. */
@Serializable
internal data class TextFieldRequest(val status: String, val text: String = "")

@Serializable
internal data class ListFieldResponse(val status: String? = null, val items: List<String> = emptyList())

@Serializable
internal data class TextFieldResponse(val status: String? = null, val text: String? = null)

/** 값이 카카오에서 온 것인지 사용자가 넣은 것인지. `KAKAO` 또는 `SELF_INPUT`. */
@Serializable
internal data class SourcesResponse(val name: String? = null, val birthYear: String? = null, val sex: String? = null)
