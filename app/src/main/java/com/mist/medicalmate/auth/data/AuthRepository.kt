package com.mist.medicalmate.auth.data

import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * 로그인된 세션. 서버 JWT는 [TokenStore]에만 있고 바깥으로 내보내지 않는다.
 *
 * @param onboardingRequired 온보딩이 필요한지. `refresh` 경로에서는 서버가 항상
 *   `false`를 주므로 신뢰할 수 없다. 자세한 내용은 [TokenResponse] 주석 참고.
 */
data class Session(val onboardingRequired: Boolean)

sealed interface AuthResult {
    data class Success(val session: Session) : AuthResult

    /** 서버에 닿지 못했다. 토큰은 그대로 두고 다시 시도할 수 있다. */
    data object NetworkUnavailable : AuthResult

    /**
     * 서버가 거절했다.
     *
     * @param requestId 서버 로그와 이어붙이는 열쇠. 장애 문의 때 쓴다.
     */
    data class Rejected(val code: ApiErrorCode, val requestId: String?) : AuthResult
}

interface AuthRepository {
    /** 카카오 액세스 토큰을 서버 JWT로 교환하고 저장한다. */
    suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult

    /**
     * 저장된 refresh 토큰으로 세션을 복구한다.
     * 저장된 토큰이 없으면 `null`을 준다.
     */
    suspend fun restoreSession(): AuthResult?

    suspend fun clearSession()
}

@Singleton
internal class DefaultAuthRepository
@Inject
constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult =
        apiCall(json) { api.loginWithKakao(KakaoLoginRequest(kakaoAccessToken)) }
            .toAuthResult()

    override suspend fun restoreSession(): AuthResult? {
        val refreshToken = tokenStore.readRefreshToken() ?: return null
        val result = apiCall(json) { api.refresh(RefreshRequest(refreshToken)) }.toAuthResult()

        // 서버가 토큰을 거절했다면 회전으로 이미 폐기된 것이다. 남겨두면 다음
        // 실행에서 같은 실패를 반복한다. 반면 네트워크 문제라면 토큰은 아직
        // 쓸 수 있으므로 지우지 않는다. 지우면 연결이 돌아온 뒤에도 재로그인이다.
        if (result is AuthResult.Rejected && result.code == ApiErrorCode.UNAUTHORIZED) {
            tokenStore.clear()
        }
        return result
    }

    override suspend fun clearSession() {
        tokenStore.clear()
    }

    private suspend fun ApiResult<TokenResponse>.toAuthResult(): AuthResult = when (this) {
        is ApiResult.Success -> {
            tokenStore.save(
                accessToken = value.accessToken,
                refreshToken = value.refreshToken,
            )
            AuthResult.Success(Session(onboardingRequired = value.onboardingRequired))
        }

        is ApiResult.Rejected -> AuthResult.Rejected(code = code, requestId = requestId)
        is ApiResult.NetworkUnavailable -> AuthResult.NetworkUnavailable
    }
}
