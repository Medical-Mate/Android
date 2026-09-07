package com.mist.medicalmate.core.network

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 저장된 액세스 토큰을 `Authorization` 헤더로 붙인다.
 *
 * 토큰이 없으면 헤더 없이 그대로 보낸다. 로그인과 토큰 재발급은 인증이 필요 없는
 * 경로라서 헤더가 없어도 정상 동작한다.
 *
 * OkHttp 인터셉터는 블로킹 API라 [runBlocking]으로 DataStore를 읽는다. 인터셉터는
 * 이미 OkHttp의 백그라운드 스레드에서 도므로 메인 스레드를 막지 않는다.
 *
 * 401을 받았을 때 토큰을 재발급하는 `Authenticator`는 아직 없다. 만료된 토큰으로
 * 호출하면 그대로 401이 온다.
 */
@Singleton
class AuthInterceptor
@Inject
constructor(private val accessTokenProvider: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { accessTokenProvider.accessToken() }
        val request =
            if (accessToken.isNullOrBlank()) {
                chain.request()
            } else {
                chain
                    .request()
                    .newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            }
        return chain.proceed(request)
    }
}
