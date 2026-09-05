package com.mist.medicalmate.auth.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

/**
 * 인증 API. `https://jinryomate-backend.onrender.com/v3/api-docs` 기준이다.
 *
 * [loginWithKakao]와 [refresh]는 인증이 필요 없다(서버 `SecurityConfig`에서 열려 있음).
 * [logout]과 [withdraw]는 `Authorization: Bearer`가 필요하며, 없으면 401이 온다.
 * 헤더는 `AuthInterceptor`가 붙인다.
 */
internal interface AuthApi {
    @POST("api/auth/kakao")
    suspend fun loginWithKakao(@Body request: KakaoLoginRequest): TokenResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    /** 서버의 refresh 토큰만 폐기한다. 카카오 세션은 건드리지 않는다. */
    @POST("api/auth/logout")
    suspend fun logout()

    /** 탈퇴. 서버가 어드민 키로 카카오 연결 끊기까지 대신 호출한다. */
    @DELETE("api/me")
    suspend fun withdraw()
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
