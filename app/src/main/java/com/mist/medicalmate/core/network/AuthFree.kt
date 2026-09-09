package com.mist.medicalmate.core.network

import jakarta.inject.Qualifier

/**
 * 인증을 붙이지 않는 네트워크 경로.
 *
 * 토큰 재발급이 이것을 쓴다. 재발급 호출이 [AuthInterceptor]와 [TokenAuthenticator]를 타면
 * 두 가지가 어긋난다. 만료된 토큰을 헤더로 달고 나가고, 그 호출이 401을 받으면 다시 재발급을
 * 부르려 든다.
 *
 * 조립 순서도 이 경로가 있어야 풀린다. `OkHttpClient`가 [TokenAuthenticator]를 받고 그것이
 * 재발급 API를 받는데, 그 API를 같은 `Retrofit`에서 만들면 순환이 된다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthFree
