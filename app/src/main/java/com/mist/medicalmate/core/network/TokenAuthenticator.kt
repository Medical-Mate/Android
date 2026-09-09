package com.mist.medicalmate.core.network

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401을 받으면 토큰을 다시 받아 원래 요청을 한 번 더 보낸다.
 *
 * OkHttp가 401 응답을 받으면 이것을 부르고, 여기서 돌려준 요청으로 다시 보낸다. `null`을
 * 주면 포기하고 401이 호출자에게 그대로 간다.
 *
 * **재발급은 한 번만 돈다.** 화면 하나가 여러 API를 동시에 부르면 401도 동시에 온다. 그때
 * 각자 재발급하면 서버가 refresh 토큰을 회전시키므로 뒤에 도착한 요청이 이미 폐기된 토큰을
 * 들고 가서 실패한다. [mutex]로 하나만 통과시키고, 잠금을 얻은 뒤 저장된 토큰을 다시 읽어
 * 이미 갈렸으면 그것으로 보낸다.
 *
 * [runBlocking]을 쓰는 이유는 [Authenticator]가 블로킹 API이기 때문이다. OkHttp의 자체
 * 스레드에서 돌아 메인 스레드를 막지 않는다. [AuthInterceptor]와 같은 사정이다.
 *
 * 재발급 호출 자체는 [AuthFree] 경로로 나간다. 같은 클라이언트를 쓰면 그 호출의 401이 다시
 * 여기로 들어온다.
 */
@Singleton
class TokenAuthenticator
@Inject
constructor(
    private val accessTokenProvider: AccessTokenProvider,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedHeader = response.refreshableHeader() ?: return null

        return runBlocking {
            mutex.withLock {
                val token = validTokenOrRefresh(failedHeader) ?: return@withLock null
                response.request
                    .newBuilder()
                    .header(AUTHORIZATION, bearer(token))
                    .build()
            }
        }
    }

    /**
     * 재발급으로 풀 수 있는 401인지 보고, 그렇다면 실패한 요청이 달고 갔던 헤더를 준다.
     *
     * 토큰 없이 보낸 요청은 대상이 아니다. 로그인처럼 인증이 필요 없는 경로가 401을 주면
     * 자격증명 문제가 아니라 서버의 거절이다.
     *
     * 이미 다시 보낸 요청이 또 401이면 재발급으로 풀릴 문제가 아니다. 멈추지 않으면 OkHttp가
     * 자체 상한까지 같은 왕복을 반복한다.
     */
    private fun Response.refreshableHeader(): String? =
        request.header(AUTHORIZATION)?.takeIf { retryCount() < MAX_RETRY }

    /**
     * 쓸 수 있는 토큰.
     *
     * 잠금을 기다리는 동안 다른 요청이 이미 재발급했을 수 있다. 저장된 토큰이 실패한 것과
     * 다르면 그것을 쓴다. 같으면 아직 아무도 바꾸지 않은 것이므로 재발급한다.
     */
    private suspend fun validTokenOrRefresh(failedHeader: String): String? {
        val stored = accessTokenProvider.accessToken()
        if (!stored.isNullOrBlank() && bearer(stored) != failedHeader) return stored
        return tokenRefresher.refresh()?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"

        /** 재발급 후 한 번만 다시 보낸다. */
        const val MAX_RETRY = 1

        fun bearer(token: String) = "Bearer $token"

        /** 이 응답에 이르기까지 재발급을 거쳐 다시 보낸 횟수. */
        fun Response.retryCount(): Int {
            var count = 0
            var prior = priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }
}
