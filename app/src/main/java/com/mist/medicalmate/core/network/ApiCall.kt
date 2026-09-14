package com.mist.medicalmate.core.network

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Retrofit suspend 호출을 감싸 결과를 [ApiResult]로 바꾼다.
 *
 * 잡는 예외는 [HttpException]과 [IOException] 둘뿐이다. 그 밖의 예외는 그대로
 * 올린다. 직렬화 실패 같은 것은 계약이 어긋났다는 뜻이고, 사용자에게 "다시
 * 시도해주세요"를 띄워 감출 문제가 아니다.
 *
 * 응답 본문을 결과에 담지 않는다. 증상·복용약 같은 민감정보가 로그나 크래시
 * 리포트로 새지 않게 하려는 백엔드 규칙을 앱에서도 지킨다.
 */
internal suspend fun <T> apiCall(json: Json, block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: HttpException) {
    e.toRejected(json)
} catch (e: IOException) {
    ApiResult.NetworkUnavailable(e)
}

private fun HttpException.toRejected(json: Json): ApiResult.Rejected {
    val envelope =
        runCatching {
            response()?.errorBody()?.string()?.let { json.decodeFromString<ApiErrorEnvelope>(it) }
        }.getOrNull()

    return ApiResult.Rejected(
        code = ApiErrorCode.from(envelope?.error?.code),
        message = envelope?.error?.message,
        requestId = envelope?.meta?.requestId,
        retryable = envelope?.error?.retryable ?: false,
        details = envelope?.error?.details,
    )
}
