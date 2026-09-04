package com.mist.medicalmate.auth.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증 API. `https://jinryomate-backend.onrender.com/v3/api-docs` 기준이다.
 *
 * 두 엔드포인트 모두 인증이 필요 없다(서버 `SecurityConfig`에서 열려 있음).
 */
internal interface AuthApi {
    @POST("api/auth/kakao")
    suspend fun loginWithKakao(@Body request: KakaoLoginRequest): TokenResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse
}

@Serializable
internal data class KakaoLoginRequest(val kakaoAccessToken: String)

@Serializable
internal data class RefreshRequest(val refreshToken: String)

/**
 * @param onboardingRequired 로그인 응답에서는 서버가 실제 프로필 완료 여부로 계산한다.
 *   다만 `refresh` 응답에서는 서버가 `false`로 하드코딩하므로 자동 로그인 경로에서
 *   이 값을 신뢰할 수 없다. 온보딩 판단은 프로필 조회로 옮겨야 한다.
 */
@Serializable
internal data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresInSeconds: Long,
    val onboardingRequired: Boolean,
)
