package com.mist.medicalmate.auth.data

import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.AuthFree
import com.mist.medicalmate.core.network.TokenRefresher
import com.mist.medicalmate.core.network.apiCall
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * 저장된 refresh 토큰으로 액세스 토큰을 다시 받는다.
 *
 * [AuthFree] API를 쓴다. 같은 Retrofit을 쓰면 이 호출이 `TokenAuthenticator`를 타고, 그
 * 401이 다시 재발급을 부른다.
 *
 * 서버가 거절하면 저장된 토큰을 지운다. 회전으로 이미 폐기된 토큰이라서 남겨두면 다음
 * 호출마다 같은 실패를 반복한다. 네트워크 문제라면 지우지 않는다. 아직 쓸 수 있는 토큰이고
 * 연결이 돌아오면 그대로 통한다. `DefaultAuthRepository.restoreSession`과 같은 판단이다.
 *
 * 토큰을 지우면 `TokenStore.hasSession`이 false를 흘리고 `SessionViewModel`이 그것을 보고
 * 로그인 화면으로 보낸다. 재발급이 실패한 자리에서 화면을 옮길 방법이 없어서 그렇게 뒀다.
 * 여기는 OkHttp 스레드이고 어느 화면이 떠 있는지 모른다.
 */
@Singleton
internal class DefaultTokenRefresher
@Inject
constructor(
    @AuthFree private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : TokenRefresher {
    override suspend fun refresh(): String? {
        val refreshToken = tokenStore.readRefreshToken() ?: return null

        return when (val result = apiCall(json) { api.refresh(RefreshRequest(refreshToken)) }) {
            is ApiResult.Success -> {
                tokenStore.save(
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                )
                result.value.accessToken
            }

            is ApiResult.Rejected -> {
                if (result.code == ApiErrorCode.UNAUTHORIZED) tokenStore.clear()
                null
            }

            is ApiResult.NetworkUnavailable -> null
        }
    }
}
