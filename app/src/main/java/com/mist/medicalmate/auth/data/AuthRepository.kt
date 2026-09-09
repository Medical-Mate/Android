package com.mist.medicalmate.auth.data

import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
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
    /**
     * 세션이 남아 있는지.
     *
     * 화면 밖에서 세션이 끝나는 경로가 있어서 흐름으로 준다. 토큰 재발급이 거절되면
     * 저장소가 비워지고 이 값이 false가 된다. 그때 로그인 화면으로 보내는 판단은
     * `SessionViewModel`이 한다.
     */
    val hasSession: Flow<Boolean>

    /** 카카오 액세스 토큰을 서버 JWT로 교환하고 저장한다. */
    suspend fun loginWithKakao(kakaoAccessToken: String): AuthResult

    /**
     * 저장된 refresh 토큰으로 세션을 복구한다.
     * 저장된 토큰이 없으면 `null`을 준다.
     */
    suspend fun restoreSession(): AuthResult?

    /**
     * 로그아웃. 서버 호출이 실패해도 로컬 토큰과 카카오 세션은 반드시 지운다.
     * 사용자의 의도는 "이 기기에서 나가겠다"이고, 서버가 거절했다고 기기에 토큰을
     * 남겨두면 의도와 반대가 된다. 그래서 결과를 돌려주지 않는다.
     */
    suspend fun logout()

    /**
     * 회원탈퇴. 서버가 계정과 카카오 연결을 지운다.
     *
     * 로그아웃과 달리 실패를 그대로 알린다. 서버 호출이 실패하면 계정이 남아 있는데
     * 로컬만 지우고 성공한 척하면 사용자는 탈퇴됐다고 믿게 된다.
     */
    suspend fun withdraw(): AuthResult

    suspend fun clearSession()
}

@Singleton
internal class DefaultAuthRepository
@Inject
constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val kakaoLoginClient: KakaoLoginClient,
    private val json: Json,
) : AuthRepository {
    override val hasSession: Flow<Boolean> = tokenStore.hasSession()

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

    override suspend fun logout() {
        // 결과를 보지 않는다. 만료된 토큰이면 401이 오지만 그래도 로컬은 정리한다.
        // 서버가 잠들어 있으면 첫 요청이 수십 초 걸리는데, 로그아웃의 본질은 로컬
        // 정리라서 응답을 무한정 기다리지 않는다. 시간을 넘기면 서버의 refresh
        // 토큰이 남지만 이 기기에는 사본이 없어 쓸 수 없다.
        withTimeoutOrNull(SERVER_LOGOUT_TIMEOUT_MS) { apiCall(json) { api.logout() } }
        // 서버는 자기 refresh 토큰만 폐기하고 카카오 세션은 건드리지 않는다.
        withTimeoutOrNull(KAKAO_LOGOUT_TIMEOUT_MS) { kakaoLoginClient.logout() }
        tokenStore.clear()
    }

    override suspend fun withdraw(): AuthResult {
        val result = apiCall(json) { api.withdraw() }
        return when (result) {
            is ApiResult.Success -> {
                // 카카오 연결 끊기는 서버가 어드민 키로 대신 부른다. 앱이 unlink()를
                // 또 부르면 중복 호출이다. 로컬 정리만 한다.
                tokenStore.clear()
                AuthResult.Success(Session(onboardingRequired = false))
            }

            is ApiResult.Rejected ->
                AuthResult.Rejected(code = result.code, requestId = result.requestId)

            is ApiResult.NetworkUnavailable -> AuthResult.NetworkUnavailable
        }
    }

    override suspend fun clearSession() {
        tokenStore.clear()
    }

    private companion object {
        const val SERVER_LOGOUT_TIMEOUT_MS = 10_000L
        const val KAKAO_LOGOUT_TIMEOUT_MS = 5_000L
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
