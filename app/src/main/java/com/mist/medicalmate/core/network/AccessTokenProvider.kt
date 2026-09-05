package com.mist.medicalmate.core.network

/**
 * 인터셉터가 붙일 액세스 토큰을 준다.
 *
 * `core`가 `auth` 도메인을 직접 참조하지 않게 하려고 둔 인터페이스다. 구현은
 * `auth/data`의 `TokenStore`이고 Hilt가 연결한다. 반대로 두면 공용 계층이 특정
 * 도메인에 묶여서 다른 도메인이 `core`를 쓸 때마다 `auth`가 따라온다.
 */
fun interface AccessTokenProvider {
    suspend fun accessToken(): String?
}
