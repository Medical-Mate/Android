package com.mist.medicalmate.core.network

import java.io.IOException

/**
 * API 호출 결과. 실패를 예외가 아니라 값으로 다룬다.
 *
 * 예외로 계층을 넘기면 호출자가 어떤 예외를 잡아야 하는지 타입에 드러나지 않고,
 * 결국 `catch (e: Exception)`으로 뭉개게 된다. 갈래를 타입으로 열어두면 `when`이
 * 빠진 분기를 컴파일 시점에 알려준다.
 */
sealed interface ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>

    /**
     * 서버가 에러 봉투로 응답한 경우.
     *
     * @param requestId 서버 로그와 이어붙이는 유일한 열쇠다. 장애를 추적할 때 쓴다.
     */
    data class Rejected(val code: ApiErrorCode, val message: String?, val requestId: String?, val retryable: Boolean) :
        ApiResult<Nothing>

    /** 서버에 닿지 못한 경우. 응답이 없으므로 에러 코드가 없다. */
    data class NetworkUnavailable(val cause: IOException) : ApiResult<Nothing>
}
