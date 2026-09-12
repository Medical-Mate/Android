package com.mist.medicalmate.intake.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * 문답 세션 API. `/v3/api-docs`의 `/api/sessions` 기준이다.
 *
 * 임시저장이 여기에 있다. [start]로 만든 세션을 서버가 들고 있고, [session]이 대화와
 * 진행도를 돌려준다. 문서가 "앱을 껐다 켜도 이어서 답할 수 있도록"이라고 적었다.
 *
 * 답을 보내는 경로가 2026-09-11에 열렸다. 그 전에는 세션을 만들고 읽을 수만 있어서 문답
 * 진행이 서버에 남지 않았다(#137).
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

    /**
     * 환자 발화를 보내고 다음 질문을 받는다.
     *
     * 한 번에 대화가 두 줄 쌓인다. 보낸 말과 AI의 다음 질문이다. 응답의 `messages`에 대화
     * 전체가 들어 있어 화면을 다시 그릴 때 세션을 또 조회하지 않아도 된다.
     *
     * 끝난 뒤에 또 보내도 오류가 아니다. 마지막 문장만 돌아오고 상태는 그대로다. 네트워크가
     * 끊긴 사이에 끝났을 수 있어서 앱이 종료 시점을 정확히 몰라도 되게 서버가 열어 뒀다.
     */
    @POST("api/sessions/{sessionId}/messages")
    suspend fun sendMessage(@Path("sessionId") sessionId: Long, @Body request: SendMessageRequest): TurnResponse

    /** 3단계(1d)의 통증 강도. 끝난 문답에도 보낼 수 있고 다시 고르면 덮어쓴다. */
    @PUT("api/sessions/{sessionId}/severity")
    suspend fun putSeverity(@Path("sessionId") sessionId: Long, @Body request: SeverityRequest): SessionResponse

    /** 4단계(1i)의 질문. 목록을 통째로 보낸다. 추가·편집·삭제·순서가 한 번에 처리된다. */
    @PUT("api/sessions/{sessionId}/questions")
    suspend fun putQuestions(@Path("sessionId") sessionId: Long, @Body request: QuestionsRequest): SessionResponse
}

/**
 * @param inputMethod 음성으로 말했어도 오디오를 보내지 않는다. 앱이 변환한 글만 간다.
 *   `STT`는 음성이었다는 사실만 남기고 녹음은 저장되지 않는다.
 */
@Serializable
internal data class SendMessageRequest(val text: String, val inputMethod: String)

/**
 * @param level 1~5 서열척도다. NRS 0~10이 아니다.
 * @param label "꽤 아파요" 같은 표시 문구. 앱이 보낸다. 서버가 들고 있으면 문구를 바꿀 때마다
 *   배포해야 하고, 이것은 디자인 카피라 서버 것이 아니다.
 */
@Serializable
internal data class SeverityRequest(val level: Int, val label: String)

@Serializable
internal data class QuestionsRequest(val questions: List<String>)

/** @param rank 낮을수록 먼저 보여줄 것. 서버가 정렬해 주지만 순서를 믿지 않는다. */
@Serializable
internal data class QuestionCandidateResponse(val text: String, val source: String? = null, val rank: Int = 0)

/**
 * 한 턴의 결과.
 *
 * @param reply AI의 다음 질문.
 * @param ended 문답이 끝났는지.
 */
@Serializable
internal data class TurnResponse(
    val sessionId: Long,
    val status: String? = null,
    val reply: String? = null,
    val ended: Boolean = false,
    val endReason: String? = null,
    val progress: ProgressResponse? = null,
    val messages: List<MessageResponse> = emptyList(),
)

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
    val severity: SeverityResponse? = null,
    val questions: List<String> = emptyList(),
    /**
     * AI가 고른 질문 후보.
     *
     * 환자가 적어 둔 [questions]와 다르다. 이쪽은 제안이고 저쪽은 확정이다. 4단계(1i)가
     * 후보를 목록에 채워 두고 환자가 지우거나 더한다.
     */
    val questionCandidates: List<QuestionCandidateResponse> = emptyList(),
)

@Serializable
internal data class SeverityResponse(val level: Int, val label: String? = null)

@Serializable
internal data class ProgressResponse(val current: Int, val total: Int)

/** @param role `AI` 또는 `USER`. */
@Serializable
internal data class MessageResponse(val seq: Long, val role: String, val text: String)
