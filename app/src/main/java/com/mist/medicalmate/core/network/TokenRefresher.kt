package com.mist.medicalmate.core.network

/**
 * 만료된 액세스 토큰을 다시 받아 온다.
 *
 * `core`가 `auth` 도메인을 직접 참조하지 않게 하려고 둔 인터페이스다. [AccessTokenProvider]와
 * 같은 방식이고 구현은 `auth/data`에 있다.
 *
 * 실패를 예외가 아니라 `null`로 알린다. 호출자가 [okhttp3.Authenticator]이고 그쪽은 "다시
 * 보낼 요청"이나 "포기"만 돌려줄 수 있어서, 실패 사유를 나눠 받아도 할 수 있는 일이 없다.
 */
fun interface TokenRefresher {
    /** 재발급된 액세스 토큰. 저장된 refresh 토큰이 없거나 서버가 거절하면 `null`. */
    suspend fun refresh(): String?
}
