package com.mist.medicalmate.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 백엔드의 공통 에러 봉투.
 *
 * OpenAPI 스키마 목록에는 없고 실제 응답으로 확인한 형태다.
 *
 * ```json
 * {"error":{"code":"UNAUTHORIZED","message":"...","retryable":false},
 *  "meta":{"requestId":"req_61c3205bdfee"}}
 * ```
 */
@Serializable
internal data class ApiErrorEnvelope(
    @SerialName("error") val error: Body? = null,
    @SerialName("meta") val meta: Meta? = null,
) {
    @Serializable
    internal data class Body(val code: String? = null, val message: String? = null, val retryable: Boolean = false)

    @Serializable
    internal data class Meta(val requestId: String? = null)
}

/**
 * 서버가 정한 에러 코드. 목록 밖의 값은 [UNKNOWN]으로 접는다.
 *
 * 인증 실패와 토큰 만료가 똑같이 [UNAUTHORIZED]로 내려오므로 코드만으로는
 * 구분할 수 없다. 어느 엔드포인트를 불렀는지로 판단해야 한다.
 */
enum class ApiErrorCode {
    INVALID_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,

    /** 서버가 카카오나 AI 서비스에 닿지 못했다. 앱 잘못이 아니고 재시도 여지가 있다. */
    UPSTREAM_ERROR,
    UPSTREAM_TIMEOUT,
    INTERNAL,
    UNKNOWN,
    ;

    internal companion object {
        fun from(raw: String?): ApiErrorCode = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}
