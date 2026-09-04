package com.mist.medicalmate.auth.data

/**
 * 카카오 로그인 SDK 호출 결과.
 *
 * 취소를 실패와 구분한다. 사용자가 스스로 닫은 것은 오류가 아니므로
 * 화면에 오류 문구를 띄우면 안 된다.
 */
sealed interface KakaoLoginResult {
    /** [accessToken]은 서버에 그대로 넘길 카카오 액세스 토큰이다. 앱은 저장하지 않는다. */
    data class Success(val accessToken: String) : KakaoLoginResult

    data object Cancelled : KakaoLoginResult

    data class Failure(val cause: Throwable) : KakaoLoginResult
}
