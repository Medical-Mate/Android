package com.mist.medicalmate.auth.data

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오 로그인 SDK 래퍼.
 *
 * SDK가 Activity Context를 요구하므로 Context를 주입받지 않고 호출 시점에 받는다.
 * 덕분에 [com.mist.medicalmate.auth.ui.LoginViewModel]은 Context를 들지 않고
 * 상태 로직을 JVM에서 검증할 수 있다.
 */
class KakaoLoginClient {
    /**
     * 카카오톡이 설치돼 있으면 카카오톡으로, 아니면 카카오계정으로 로그인한다.
     * 카카오톡 로그인이 취소가 아닌 이유로 실패하면 카카오계정으로 한 번 더 시도한다.
     */
    suspend fun login(context: Context): KakaoLoginResult {
        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            return loginWithKakaoAccount(context)
        }
        return when (val talkResult = loginWithKakaoTalk(context)) {
            is KakaoLoginResult.Failure -> loginWithKakaoAccount(context)
            else -> talkResult
        }
    }

    private suspend fun loginWithKakaoTalk(context: Context): KakaoLoginResult =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (continuation.isActive) {
                    continuation.resume(toResult(token, error))
                }
            }
        }

    private suspend fun loginWithKakaoAccount(context: Context): KakaoLoginResult =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                if (continuation.isActive) {
                    continuation.resume(toResult(token, error))
                }
            }
        }

    private fun toResult(token: OAuthToken?, error: Throwable?): KakaoLoginResult = when {
        error is ClientError && error.reason == ClientErrorCause.Cancelled ->
            KakaoLoginResult.Cancelled

        error != null -> KakaoLoginResult.Failure(error)
        token != null -> KakaoLoginResult.Success(token.accessToken)
        else -> KakaoLoginResult.Failure(IllegalStateException("토큰과 오류가 모두 비어 있습니다"))
    }
}
