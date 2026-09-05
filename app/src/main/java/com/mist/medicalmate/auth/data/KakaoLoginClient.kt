package com.mist.medicalmate.auth.data

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오 로그인 SDK 래퍼.
 *
 * 로그인은 SDK가 Activity Context를 요구하므로 Context를 주입받지 않고 호출 시점에 받는다.
 * 로그아웃은 Context가 필요 없어 Repository에서 바로 부른다.
 * 덕분에 [com.mist.medicalmate.auth.ui.LoginViewModel]은 Context를 들지 않고
 * 상태 로직을 JVM에서 검증할 수 있다.
 */
@Singleton
class KakaoLoginClient
@Inject
constructor() {
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

    /**
     * 카카오 세션을 끊는다.
     *
     * 이걸 부르지 않으면 로그아웃 뒤 로그인 버튼을 눌렀을 때 계정 선택 없이
     * 이전 계정으로 바로 들어간다. 기기를 나눠 쓰는 상황에서 실제 문제가 된다.
     *
     * 실패해도 알리지 않는다. 앱이 서버 토큰과 로컬 저장소를 이미 지운 뒤이고,
     * 사용자가 할 수 있는 일이 없다.
     */
    suspend fun logout() {
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.logout {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
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
