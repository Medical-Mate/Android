package com.mist.medicalmate.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 백엔드의 공통 에러 봉투.
 *
 * OpenAPI 스키마 목록에는 없고 실제 응답으로 확인한 형태다.
 *
 * ```json
 * {"error":{"code":"UNAUTHORIZED","message":"...","retryable":false},
 *  "meta":{"requestId":"req_61c3205bdfee"}}
 * ```
 *
 * [Body.details]는 오류마다 다른 값이 오는 자리다(Backend#117). 도메인 필드를 봉투에 직접
 * 박으면 오류 종류가 늘 때마다 봉투가 늘고 파싱이 갈린다. 없으면 null이다.
 */
@Serializable
internal data class ApiErrorEnvelope(
    @SerialName("error") val error: Body? = null,
    @SerialName("meta") val meta: Meta? = null,
) {
    @Serializable
    internal data class Body(
        val code: String? = null,
        val message: String? = null,
        val retryable: Boolean = false,
        val details: JsonObject? = null,
    )

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

    /**
     * 이미 고친 카드를 또 고치려 했다(Backend#117).
     *
     * 그대로 두면 버전이 가지를 치고 그 가지에 넣은 편집은 어느 화면에도 나오지 않는다.
     * `details.latestCardId`에 갈아탈 카드가 온다.
     */
    CARD_ALREADY_EDITED,

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
