package com.mist.medicalmate.intake.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 문답 세션 API. `/v3/api-docs`의 `/api/sessions` 기준이다.
 *
 * 임시저장이 여기에 있다. [start]로 만든 세션을 서버가 들고 있고, [session]이 대화와
 * 진행도를 돌려준다. 문서가 "앱을 껐다 켜도 이어서 답할 수 있도록"이라고 적었다.
 *
 * **답을 보내는 경로가 없다.** 세션 경로는 만들기·읽기·카드 만들기 셋뿐이고
 * `MessageResponse`는 읽기로만 있다. 그래서 지금은 문답 진행이 서버에 남지 않는다.
 * 백엔드 트랙에 확인을 넘겼다(#137).
 */
internal interface SessionApi {
    /**
     * 짚은 부위와 함께 문답을 시작한다. 부위는 건너뛸 수 있다.
     *
     * 나이나 성별이 없으면 400이다. 둘은 의사용 카드 헤더에 반드시 찍혀서, 없으면 문답을
     * 다 해도 카드가 성립하지 않는다. `canStartIntake`로 미리 확인한다.
     */
    @POST("api/sessions")
    suspend fun start(@Body request: StartSessionRequest): SessionResponse

    @GET("api/sessions/{sessionId}")
    suspend fun session(@Path("sessionId") sessionId: Long): SessionResponse
}

/**
 * @param siteCodes 온톨로지 id다. 구역까지 골랐으면 `SUR:*`, 앵커까지면 `ANC:*`.
 * @param siteText 사람이 읽는 표현. 문답 첫 문장에 그대로 들어간다.
 */
@Serializable
internal data class StartSessionRequest(val siteCodes: List<String>, val siteText: String?)

@Serializable
internal data class SessionResponse(
    val sessionId: Long,
    val status: String? = null,
    val siteCodes: List<String> = emptyList(),
    val siteText: String? = null,
    val progress: ProgressResponse? = null,
    val messages: List<MessageResponse> = emptyList(),
)

@Serializable
internal data class ProgressResponse(val current: Int, val total: Int)

/** @param role `AI` 또는 `USER`. */
@Serializable
internal data class MessageResponse(val seq: Long, val role: String, val text: String)
