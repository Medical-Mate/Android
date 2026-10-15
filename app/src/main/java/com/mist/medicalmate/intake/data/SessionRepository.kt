package com.mist.medicalmate.intake.data

import com.mist.medicalmate.core.model.PatientHealthProvider
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import com.mist.medicalmate.core.ondevice.ExtractedTurn
import com.mist.medicalmate.core.ondevice.OnDeviceBundle
import com.mist.medicalmate.core.ondevice.OnDeviceExtractor
import com.mist.medicalmate.core.ondevice.OnDeviceResult
import com.mist.medicalmate.core.ondevice.QuestionCard
import com.mist.medicalmate.core.ondevice.extractUserMessage
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/**
 * 문답 세션 읽고 쓰기.
 *
 * 임시저장이 여기 있다. [start]가 만든 세션을 서버가 들고 있고 [load]가 대화와 진행도를
 * 돌려준다. 홈의 "이어서 하기"가 [load]로 들어온다.
 *
 * **폰 모델이 있으면 뽑는 일을 폰이 한다**(#300). 부르는 쪽은 그 사실을 몰라도 된다. 아래
 * [DefaultSessionRepository]에 갈래가 있다.
 */
interface SessionRepository {
    /**
     * 짚은 부위와 고른 강도로 문답을 시작한다(Backend#169).
     *
     * @param siteCode 구역까지 골랐으면 `SUR:*`, 앵커까지면 `ANC:*`. 한 곳만 고른다.
     * @param side 고른 점의 좌우. 좌우가 없는 부위면 보내지 않는다 — 저장소가 가린다.
     * @param siteText 사람이 읽는 이름. 문답 첫 문장에 그대로 들어간다.
     * @param severity 2단계에서 고른 강도. 표시 문구도 함께 보낸다. 서버가 카피를 들고 있지 않다.
     */
    suspend fun start(
        siteCode: String,
        side: IntakeSide?,
        siteText: String?,
        severity: IntakeSeverity,
    ): ApiResult<IntakeSession>

    suspend fun load(sessionId: Long): ApiResult<IntakeSession>

    /**
     * 환자 발화를 보내고 다음 질문을 받는다.
     *
     * @param byVoice 음성으로 말했는지. 오디오는 보내지 않고 그 사실만 기록된다.
     */
    suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn>

    /** 문답을 연 뒤 강도 단계로 돌아가 다시 고른 값. 표시 문구도 함께 보낸다. */
    suspend fun setSeverity(sessionId: Long, severity: IntakeSeverity): ApiResult<IntakeSession>

    /** 브리핑 카드에 담을 질문. 체크한 줄을 화면 순서대로 통째로 보낸다. */
    suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession>
}

/** 고른 점의 좌우. 서버는 대문자로 받는다. */
enum class IntakeSide { LEFT, RIGHT }

/** 고른 강도. [level]은 1~5 서열척도이고 [label]은 "꽤 아파요" 같은 표시 문구다. */
data class IntakeSeverity(val level: Int, val label: String)

/**
 * 세션 저장소.
 *
 * **폰 모델이 있으면 세 군데가 달라진다**(#300, Backend#157). 하나라도 막히면 서버가 하던 대로 간다.
 * 1. 세션을 `ONDEVICE`로 연다. 첫 자유 발화 없이 첫 축 질문부터 시작한다.
 * 2. 답할 때마다 응답의 [AskingAxisResponse]로 폰 프롬프트를 만들어 뽑은 것을 함께 보낸다. 폰이
 *    못 뽑은 턴은 그것 없이 보내고, 그 턴은 서버가 뽑는다.
 * 3. 문답이 끝난 턴에 후보가 비어 있으면 폰이 후보를 만들어 보낸다. 온디바이스 세션은 서버가
 *    후보를 만들지 않는다. 폰이 못 만들면 빈 배열을 보내고 AI가 템플릿으로 채운다.
 *
 * 묻는 축은 세션마다 마지막 응답 것을 들고 있는다. 화면에 내보낼 값이 아니고, 서버가 저장해
 * 두어서 [load]로 다시 받을 수 있다.
 */
internal class DefaultSessionRepository
@Inject
constructor(
    private val api: SessionApi,
    private val json: Json,
    private val onDevice: OnDeviceExtractor,
    private val health: PatientHealthProvider,
) : SessionRepository {
    private val askingAxes = mutableMapOf<Long, AskingAxisResponse?>()

    override suspend fun start(
        siteCode: String,
        side: IntakeSide?,
        siteText: String?,
        severity: IntakeSeverity,
    ): ApiResult<IntakeSession> {
        val request =
            StartSessionRequest(
                siteNodeId = siteCode,
                side = side?.name?.takeIf { siteCode in LATERAL_SITES },
                siteText = siteText,
                profile = PROFILE_ON_DEVICE.takeIf { onDevice.available() },
                severity = SeverityRequest(level = severity.level, label = severity.label),
            )
        return apiCall(json) { api.start(request) }.map { it.remembered().toSession() }
    }

    override suspend fun load(sessionId: Long): ApiResult<IntakeSession> =
        apiCall(json) { api.session(sessionId) }.map { it.remembered().toSession() }

    override suspend fun send(sessionId: Long, text: String, byVoice: Boolean): ApiResult<IntakeTurn> {
        val extracted = askingAxes[sessionId]?.let { extractOnDevice(it, text) }
        val request =
            SendMessageRequest(
                text = text,
                inputMethod = if (byVoice) "STT" else "TEXT",
                extraction = extracted?.json,
                extractionMeta = extracted?.let { OnDeviceBundle.meta(OnDeviceBundle.EXTRACT_PROMPT_VERSION) },
            )
        val sent = apiCall(json) { api.sendMessage(sessionId, request) }
        val turn = (sent as? ApiResult.Success)?.value ?: return sent.map { it.toTurn() }
        askingAxes[sessionId] = turn.askingAxis
        val candidates = turn.takeIf { it.ended && it.questionCandidates.isEmpty() }?.let { candidatesOf(sessionId) }
        return ApiResult.Success(turn.toTurn(candidates))
    }

    /**
     * 문답이 끝났는데 후보가 비어 있을 때 채운다. 온디바이스 세션이면 늘 이렇다 — 서버가 후보를 만들지
     * 않는다. 폰 질문 후보의 재료(카드 축 · 강도)가 턴 응답에 없어서 세션을 한 번 읽는다. 못 채우면
     * null이고 화면은 직접 적는 칸만 둔다.
     */
    private suspend fun candidatesOf(sessionId: Long): List<String>? {
        val session = (apiCall(json) { api.session(sessionId) } as? ApiResult.Success)?.value ?: return null
        val filled = if (session.needsCandidates()) suggestOnDevice(session) else ApiResult.Success(session.toSession())
        return (filled as? ApiResult.Success)?.value?.questionCandidates
    }

    override suspend fun setSeverity(sessionId: Long, severity: IntakeSeverity): ApiResult<IntakeSession> =
        apiCall(json) { api.putSeverity(sessionId, SeverityRequest(level = severity.level, label = severity.label)) }
            .map { it.toSession() }

    /** 폰이 이 답을 뽑는다. 폰 모델이 없거나 못 뽑으면 null이고 그 턴은 서버가 뽑는다. */
    private suspend fun extractOnDevice(axis: AskingAxisResponse, text: String): ExtractedTurn? {
        if (!onDevice.available()) return null
        val result = onDevice.extractTurn(extractUserMessage(axis.key, axis.label, text))
        return (result as? OnDeviceResult.Success)?.value
    }

    /**
     * 폰이 만든 후보를 보내 검증·보충된 후보를 받는다. 보내지 못하면 null이다.
     *
     * 폰 모델이 없어도 보낸다(빈 배열). 그 세션이 온디바이스로 시작했다면 서버가 후보를 만들지 않아,
     * 보내지 않으면 추천 질문이 빈 채로 열린다.
     */
    private suspend fun suggestOnDevice(session: SessionResponse): ApiResult<IntakeSession>? {
        val suggested =
            if (onDevice.available()) {
                (onDevice.suggestQuestions(session.toQuestionCard()) as? OnDeviceResult.Success)?.value.orEmpty()
            } else {
                emptyList()
            }
        val request =
            PhoneQuestionCandidatesRequest(
                candidates = suggested.map { PhoneQuestionCandidate(it.text, it.source) },
                candidatesMeta = OnDeviceBundle.meta(OnDeviceBundle.QUESTIONS_PROMPT_VERSION),
            )
        val posted = apiCall(json) { api.postQuestionCandidates(session.sessionId, request) }
        return (posted as? ApiResult.Success)?.map { it.toSession() }
    }

    private suspend fun SessionResponse.toQuestionCard(): QuestionCard {
        val profile = health.patientHealth()
        return QuestionCard(
            axes = cardAxes.mapNotNull { axis -> axis.value?.let { axis.key to it } }.toMap(),
            severity = severity?.let {
                listOfNotNull(it.level.toString(), it.label?.let { label -> "($label)" }).joinToString(" ")
            },
            medications = profile?.medications.orEmpty(),
            conditions = profile?.conditions.orEmpty(),
            allergies = profile?.allergies.orEmpty(),
        )
    }

    private fun SessionResponse.remembered(): SessionResponse = also { askingAxes[sessionId] = askingAxis }

    override suspend fun setQuestions(sessionId: Long, questions: List<String>): ApiResult<IntakeSession> =
        apiCall(json) { api.putQuestions(sessionId, QuestionsRequest(questions)) }.map { it.toSession() }
}

/**
 * 서버가 들고 있는 문답 하나.
 *
 * [answered]와 [total]을 그대로 둔다. 화면이 "4단계 중 2단계"를 만들고, 이어서 할 때
 * 어느 단계로 돌아갈지도 이 숫자가 정한다.
 */
data class IntakeSession(
    val id: Long,
    val status: IntakeSessionStatus,
    val siteCodes: List<String>,
    val siteText: String?,
    val answered: Int,
    val total: Int,
    val messages: List<IntakeSessionMessage>,
    /** 2단계에서 고른 강도. 1~5 서열척도다. */
    val severityLevel: Int? = null,
    /** 환자가 체크해 저장한 질문. 화면 순서대로다. */
    val questions: List<String> = emptyList(),
    /** AI가 고른 추천 질문. 문답이 끝나야 채워진다. `rank` 순이다. */
    val questionCandidates: List<String> = emptyList(),
)

enum class IntakeSessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }

/**
 * 한 턴의 결과.
 *
 * [messages]에 대화 전체가 들어 있다. 화면이 그것으로 다시 그리면 세션을 또 조회하지
 * 않아도 된다.
 *
 * [ended]가 서면 문답이 끝난 것이다. 그 뒤에 또 보내도 오류가 아니라 마지막 문장만
 * 돌아온다. 네트워크가 끊긴 사이에 끝났을 수 있어서 서버가 그렇게 열어 뒀다.
 */
data class IntakeTurn(
    val ended: Boolean,
    val messages: List<IntakeSessionMessage>,
    val answered: Int,
    val total: Int,
    /** 문답이 끝난 턴에 실린 추천 질문. `rank` 순이다. 그 밖에는 비어 있다. */
    val questionCandidates: List<String> = emptyList(),
)

data class IntakeSessionMessage(val seq: Long, val fromPatient: Boolean, val text: String)

/** [filled]는 턴에 후보가 없어 따로 채운 것이다. 없으면 턴에 실린 것을 쓴다. */
private fun TurnResponse.toTurn(filled: List<String>? = null) = IntakeTurn(
    ended = ended,
    messages = messages.map { it.toMessage() },
    answered = progress?.current ?: 0,
    total = progress?.total ?: 0,
    questionCandidates = filled ?: questionCandidates.ranked(),
)

private fun List<QuestionCandidateResponse>.ranked(): List<String> = sortedBy { it.rank }.map { it.text }

private fun MessageResponse.toMessage() = IntakeSessionMessage(seq = seq, fromPatient = role == ROLE_USER, text = text)

private fun SessionResponse.toSession() = IntakeSession(
    id = sessionId,
    status = statusOf(status),
    siteCodes = listOfNotNull(siteNodeId),
    siteText = siteText,
    answered = progress?.current ?: 0,
    total = progress?.total ?: 0,
    messages = messages.map { it.toMessage() },
    severityLevel = severity?.level,
    // 둘을 섞지 않는다. 체크한 것과 제안받은 것을 화면이 따로 알아야 이어서 할 때 체크를 되살린다.
    questions = questions,
    questionCandidates = questionCandidates.ranked(),
)

/**
 * 모르는 값은 진행 중으로 본다.
 *
 * 서버가 상태를 늘렸을 때 세션을 못 여는 것보다, 열어 두고 진행도로 판단하는 쪽이 낫다.
 * 임시저장을 잃는 것이 더 큰 손해다.
 */
private fun statusOf(value: String?): IntakeSessionStatus = when (value) {
    "COMPLETED" -> IntakeSessionStatus.COMPLETED
    "ABANDONED" -> IntakeSessionStatus.ABANDONED
    else -> IntakeSessionStatus.IN_PROGRESS
}

private const val ROLE_USER = "USER"

private const val PROFILE_ON_DEVICE = "ONDEVICE"

/**
 * 문답이 끝났는데 후보도 체크해 둔 질문도 없다. 온디바이스 세션이면 서버가 후보를 만들지 않아
 * 늘 이렇다. 서버 세션에서 서버 후보 생성이 실패한 경우도 여기 들어오고, 그때도 폰 후보를
 * 보내면 빈 추천 질문을 면한다.
 */
private fun SessionResponse.needsCandidates(): Boolean =
    status == STATUS_COMPLETED && questionCandidates.isEmpty() && questions.isEmpty()

private const val STATUS_COMPLETED = "COMPLETED"

/**
 * 좌우를 붙여 보낼 수 있는 부위. 나머지(머리·배 등 13곳)에 좌우를 보내면 400이다.
 *
 * AI 트랙 `docs/examples/body-map.json`(스냅샷 `f848848baea4`)에서 `laterality`가 `left_right`인
 * 21곳이다. 명치·아랫배도 여기 든다. 온톨로지가 바뀌면 `BodyMapOntology.kt`와 함께 다시 뽑는다.
 */
private val LATERAL_SITES =
    setOf(
        "SUR:002", "SUR:003", "SUR:012", "SUR:022", "SUR:031", "SUR:032", "SUR:042",
        "ANC:013", "SUR:051", "SUR:055", "SUR:061", "SUR:065", "SUR:071", "SUR:072",
        "ANC:014", "SUR:081", "SUR:090", "SUR:091", "SUR:097", "SUR:101", "SUR:102",
    )
