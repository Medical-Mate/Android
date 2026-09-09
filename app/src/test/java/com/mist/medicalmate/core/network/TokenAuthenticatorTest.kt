package com.mist.medicalmate.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {
    @Test
    fun `401을 받으면 새 토큰으로 다시 보낸다`() {
        val authenticator = authenticator(stored = "expired", refreshed = "fresh")

        val request = authenticator.authenticate(null, unauthorized(token = "expired"))

        assertEquals("Bearer fresh", request?.header("Authorization"))
    }

    @Test
    fun `재발급하지 못하면 포기한다`() {
        val authenticator = authenticator(stored = "expired", refreshed = null)

        assertNull(authenticator.authenticate(null, unauthorized(token = "expired")))
    }

    @Test
    fun `빈 토큰을 받으면 포기한다`() {
        val authenticator = authenticator(stored = "expired", refreshed = "  ")

        assertNull(authenticator.authenticate(null, unauthorized(token = "expired")))
    }

    @Test
    fun `토큰 없이 보낸 요청은 재발급하지 않는다`() {
        val refresher = CountingRefresher(refreshed = "fresh")
        val authenticator = TokenAuthenticator({ "stored" }, refresher)

        val request = authenticator.authenticate(null, unauthorized(token = null))

        assertNull(request)
        assertEquals(0, refresher.calls)
    }

    @Test
    fun `다시 보낸 요청이 또 401이면 멈춘다`() {
        val refresher = CountingRefresher(refreshed = "fresh")
        val authenticator = TokenAuthenticator({ "expired" }, refresher)
        val retried = unauthorized(token = "fresh", prior = unauthorized(token = "expired"))

        assertNull(authenticator.authenticate(null, retried))
        assertEquals(0, refresher.calls)
    }

    @Test
    fun `이미 갈린 토큰이 있으면 재발급하지 않고 그것으로 보낸다`() {
        val refresher = CountingRefresher(refreshed = "another")
        val authenticator = TokenAuthenticator({ "rotated" }, refresher)

        val request = authenticator.authenticate(null, unauthorized(token = "expired"))

        assertEquals("Bearer rotated", request?.header("Authorization"))
        assertEquals(0, refresher.calls)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `동시에 401을 받아도 재발급은 한 번만 돈다`() = runTest {
        // 저장소가 재발급 결과를 반영하도록 묶어 둔다. 실제 TokenStore가 그렇게 동작하고,
        // 두 번째 요청이 잠금을 얻었을 때 이미 갈린 토큰을 보게 되는 것이 핵심이다.
        val store = MutableTokenStore(initial = "expired")
        val refresher = CountingRefresher(refreshed = "fresh", store = store)
        val authenticator = TokenAuthenticator(store, refresher)

        val first = async { authenticator.authenticate(null, unauthorized(token = "expired")) }
        val second = async { authenticator.authenticate(null, unauthorized(token = "expired")) }

        assertEquals("Bearer fresh", first.await()?.header("Authorization"))
        assertEquals("Bearer fresh", second.await()?.header("Authorization"))
        assertEquals(1, refresher.calls)
    }

    private fun authenticator(stored: String?, refreshed: String?) =
        TokenAuthenticator({ stored }, CountingRefresher(refreshed))

    private fun unauthorized(token: String?, prior: Response? = null): Response {
        val builder = Request.Builder().url("https://example.test/api/me")
        if (token != null) builder.header("Authorization", "Bearer $token")
        return Response
            .Builder()
            .request(builder.build())
            .protocol(Protocol.HTTP_1_1)
            .code(UNAUTHORIZED)
            .message("Unauthorized")
            .apply { if (prior != null) priorResponse(prior) }
            .build()
    }

    private class CountingRefresher(private val refreshed: String?, private val store: MutableTokenStore? = null) :
        TokenRefresher {
        var calls: Int = 0
            private set

        override suspend fun refresh(): String? {
            calls++
            store?.set(refreshed)
            return refreshed
        }
    }

    /** 재발급이 저장소에 반영되는 것까지 흉내 낸다. */
    private class MutableTokenStore(initial: String?) : AccessTokenProvider {
        private val mutex = Mutex()
        private var token: String? = initial

        override suspend fun accessToken(): String? = mutex.withLock { token }

        suspend fun set(value: String?) {
            mutex.withLock { token = value }
        }
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
