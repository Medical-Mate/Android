package com.mist.medicalmate.intake.data

import com.mist.medicalmate.core.model.PatientHealth
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.ondevice.ExtractedTurn
import com.mist.medicalmate.core.ondevice.MemoLabel
import com.mist.medicalmate.core.ondevice.OnDeviceExtractor
import com.mist.medicalmate.core.ondevice.OnDeviceResult
import com.mist.medicalmate.core.ondevice.QuestionCard
import com.mist.medicalmate.core.ondevice.SuggestedQuestion
import com.mist.medicalmate.core.ondevice.TurnExtraction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * 진료 전 문답의 폰 경로(#300, Backend#157).
 *
 * 폰 모델이 있으면 세션을 온디바이스로 열고, 답마다 폰이 뽑은 것을 함께 보내고, 문답이 끝나면 폰이
 * 만든 질문 후보를 보낸다. 어디서 막혀도 서버가 하던 대로 가야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionOnDeviceTest {
    @Test
    fun `폰 모델이 있으면 온디바이스로 연다`() = runTest {
        val api = FakeSessionApi()

        repository(api, FakeSessionOnDevice()).start("SUR:051", IntakeSide.LEFT, "왼쪽 어깨")

        assertEquals(StartSessionRequest("SUR:051", "LEFT", "왼쪽 어깨", "ONDEVICE"), api.started)
    }

    @Test
    fun `폰 모델이 없으면 서버 프로필로 연다`() = runTest {
        val api = FakeSessionApi()

        repository(api, FakeSessionOnDevice(available = false)).start("SUR:051", IntakeSide.RIGHT, "오른쪽 어깨")

        assertNull(api.started?.profile)
    }

    @Test
    fun `부위는 siteNodeId로 하나만 보낸다`() = runTest {
        // 전에는 siteCodes 배열로 보냈는데 백엔드에 그 필드가 없어 조용히 버려졌다.
        val api = FakeSessionApi()

        repository(api, FakeSessionOnDevice()).start("SUR:032", IntakeSide.LEFT, "아랫배")

        assertEquals("SUR:032", api.started?.siteNodeId)
        assertEquals("LEFT", api.started?.side)
    }

    @Test
    fun `좌우가 없는 부위에는 좌우를 보내지 않는다`() = runTest {
        // 머리·배 등 13곳에 좌우를 보내면 400이다. 그러면 세션이 열리지 않는다.
        val api = FakeSessionApi()

        repository(api, FakeSessionOnDevice()).start("SUR:021", IntakeSide.LEFT, "가슴 가운데")

        assertNull(api.started?.side)
    }

    @Test
    fun `묻는 축으로 폰 프롬프트를 만들고 뽑은 것을 함께 보낸다`() = runTest {
        val api = FakeSessionApi(nextAxis = AskingAxisResponse("character", "어떤 느낌"))
        val onDevice = FakeSessionOnDevice()
        val repository = repository(api, onDevice)
        repository.start("SUR:051", IntakeSide.LEFT, "왼쪽 어깨")

        repository.send(SESSION, "한 달 전부터요", byVoice = false)
        repository.send(SESSION, "찌릿찌릿해요", byVoice = true)

        assertEquals(
            listOf(
                "질문한 항목: 언제부터·어떻게 시작 (axis=onset). 이 항목만 답한다.\n환자: 한 달 전부터요\nJSON:",
                "질문한 항목: 어떤 느낌 (axis=character). 이 항목만 답한다.\n환자: 찌릿찌릿해요\nJSON:",
            ),
            onDevice.prompts,
        )
        val sent = api.sent.first()
        assertEquals(EXTRACTED, sent.extraction)
        assertEquals("extract-small-2b-v1", sent.extractionMeta?.get("prompt_version")?.jsonPrimitive?.content)
        assertEquals("Qwen3.5-2B-Q4_0-ssmout-q8", sent.extractionMeta?.get("model_id")?.jsonPrimitive?.content)
    }

    @Test
    fun `이어서 할 때도 다시 받은 축으로 뽑는다`() = runTest {
        // 서버가 묻는 축을 세션에 저장해 두어 다시 조회해도 같은 값이 온다.
        val api = FakeSessionApi()
        val onDevice = FakeSessionOnDevice()
        val repository = repository(api, onDevice)
        repository.load(SESSION)

        repository.send(SESSION, "어제부터요", byVoice = false)

        assertEquals(EXTRACTED, api.sent.single().extraction)
    }

    @Test
    fun `폰이 못 뽑은 턴은 그것 없이 보낸다`() = runTest {
        // 그 턴은 서버가 뽑는다.
        val api = FakeSessionApi()
        val repository = repository(api, FakeSessionOnDevice(extracted = false))
        repository.start("SUR:051", IntakeSide.LEFT, "왼쪽 어깨")

        repository.send(SESSION, "한 달 전부터요", byVoice = false)

        assertNull(api.sent.single().extraction)
        assertNull(api.sent.single().extractionMeta)
    }

    @Test
    fun `묻는 축이 없으면 폰을 부르지 않는다`() = runTest {
        // 서버 프로필의 첫 자유 발화다. 폰 프롬프트는 물은 항목이 있어야 성립한다.
        val api = FakeSessionApi(startAxis = null)
        val onDevice = FakeSessionOnDevice()
        val repository = repository(api, onDevice)
        repository.start("SUR:051", IntakeSide.LEFT, "왼쪽 어깨")

        repository.send(SESSION, "어깨가 아파요", byVoice = false)

        assertEquals(emptyList<String>(), onDevice.prompts)
        assertNull(api.sent.single().extraction)
    }

    @Test
    fun `문답이 끝나 후보가 없으면 폰이 만든 후보를 보내고 그 결과를 쓴다`() = runTest {
        val api = FakeSessionApi()

        val session = repository(api, FakeSessionOnDevice()).setSeverity(SESSION, 3, "꽤 아파요")

        assertEquals(listOf(PhoneQuestionCandidate("어깨를 써도 되나요?", "exacerbating")), api.candidates?.candidates)
        assertEquals("questions-small4", api.candidates?.candidatesMeta?.get("prompt_version")?.jsonPrimitive?.content)
        assertEquals(listOf("검증을 지난 질문"), (session as ApiResult.Success).value.questions)
    }

    @Test
    fun `카드 값과 강도와 건강 정보로 후보를 만든다`() = runTest {
        val onDevice = FakeSessionOnDevice()

        repository(
            FakeSessionApi(),
            onDevice,
            PatientHealth(medications = listOf("진통제")),
        ).setSeverity(SESSION, 3, "꽤 아파요")

        assertEquals(
            QuestionCard(
                axes = mapOf("site" to "왼쪽 어깨", "onset" to "한 달 전부터"),
                severity = "3 (꽤 아파요)",
                medications = listOf("진통제"),
                conditions = emptyList(),
                allergies = emptyList(),
            ),
            onDevice.cards.single(),
        )
    }

    @Test
    fun `폰 모델이 없어도 빈 후보를 보낸다`() = runTest {
        // 온디바이스로 시작한 세션은 서버가 후보를 만들지 않는다. 안 보내면 4단계가 빈 채로 열린다.
        val api = FakeSessionApi()

        repository(api, FakeSessionOnDevice(available = false)).setSeverity(SESSION, 3, "꽤 아파요")

        assertEquals(emptyList<PhoneQuestionCandidate>(), api.candidates?.candidates)
    }

    @Test
    fun `서버가 이미 후보를 줬으면 보내지 않는다`() = runTest {
        // 서버 프로필 세션이다. 지금까지와 같다.
        val api = FakeSessionApi(serverCandidates = listOf("서버가 고른 질문"))

        val session = repository(api, FakeSessionOnDevice()).setSeverity(SESSION, 3, "꽤 아파요")

        assertNull(api.candidates)
        assertEquals(listOf("서버가 고른 질문"), (session as ApiResult.Success).value.questions)
    }

    @Test
    fun `후보를 보내지 못하면 강도 저장 결과를 그대로 쓴다`() = runTest {
        val api = FakeSessionApi(rejectCandidates = true)

        val session = repository(api, FakeSessionOnDevice()).setSeverity(SESSION, 3, "꽤 아파요")

        assertEquals(emptyList<String>(), (session as ApiResult.Success).value.questions)
    }

    private fun repository(api: SessionApi, onDevice: OnDeviceExtractor, health: PatientHealth? = null) =
        DefaultSessionRepository(api, Json, onDevice, health = { health })

    private class FakeSessionApi(
        private val startAxis: AskingAxisResponse? = AskingAxisResponse("onset", "언제부터·어떻게 시작"),
        private val nextAxis: AskingAxisResponse? = null,
        private val serverCandidates: List<String> = emptyList(),
        private val rejectCandidates: Boolean = false,
    ) : SessionApi {
        var started: StartSessionRequest? = null
        val sent = mutableListOf<SendMessageRequest>()
        var candidates: PhoneQuestionCandidatesRequest? = null

        override suspend fun start(request: StartSessionRequest): SessionResponse {
            started = request
            return SessionResponse(sessionId = SESSION, askingAxis = startAxis)
        }

        override suspend fun session(sessionId: Long) = SessionResponse(sessionId = SESSION, askingAxis = startAxis)

        override suspend fun sendMessage(sessionId: Long, request: SendMessageRequest): TurnResponse {
            sent += request
            return TurnResponse(sessionId = SESSION, askingAxis = nextAxis)
        }

        override suspend fun putSeverity(sessionId: Long, request: SeverityRequest) = SessionResponse(
            sessionId = SESSION,
            status = "COMPLETED",
            severity = SeverityResponse(request.level, request.label),
            questionCandidates = serverCandidates.map { QuestionCandidateResponse(it, rank = 1) },
            cardAxes =
            listOf(
                CardAxisResponse("site", "filled", "왼쪽 어깨"),
                CardAxisResponse("onset", "filled", "한 달 전부터"),
                CardAxisResponse("radiation", "not_asked"),
            ),
        )

        override suspend fun putQuestions(sessionId: Long, request: QuestionsRequest) = SessionResponse(SESSION)

        override suspend fun postQuestionCandidates(
            sessionId: Long,
            request: PhoneQuestionCandidatesRequest,
        ): SessionResponse {
            candidates = request
            if (rejectCandidates) throw HttpException(Response.error<Unit>(500, "".toResponseBody()))
            return SessionResponse(
                sessionId = SESSION,
                status = "COMPLETED",
                questionCandidates = listOf(QuestionCandidateResponse("검증을 지난 질문", rank = 1, origin = "model")),
            )
        }
    }

    private companion object {
        const val SESSION = 7L
        val EXTRACTED = JsonObject(mapOf("notes" to JsonPrimitive("x")))
    }
}

/**
 * 폰 추출 대역.
 *
 * [extractTurn]은 받은 프롬프트를 남기고 [EXTRACTED]를 돌려준다. [suggestQuestions]는 받은 카드를
 * 남기고 후보 하나를 돌려준다.
 */
internal class FakeSessionOnDevice(private val available: Boolean = true, private val extracted: Boolean = true) :
    OnDeviceExtractor {
    val prompts = mutableListOf<String>()
    val cards = mutableListOf<QuestionCard>()

    override suspend fun available() = available

    override suspend fun extractTurn(user: String): OnDeviceResult<ExtractedTurn> {
        prompts += user
        return if (extracted) {
            OnDeviceResult.Success(ExtractedTurn(TurnExtraction(), JsonObject(mapOf("notes" to JsonPrimitive("x")))))
        } else {
            OnDeviceResult.Malformed("{", IllegalStateException())
        }
    }

    override suspend fun suggestQuestions(card: QuestionCard): OnDeviceResult<List<SuggestedQuestion>> {
        cards += card
        return OnDeviceResult.Success(listOf(SuggestedQuestion("어깨를 써도 되나요?", "exacerbating")))
    }

    override suspend fun labelMemo(sentences: List<String>): OnDeviceResult<Map<Int, MemoLabel>> =
        OnDeviceResult.Unavailable
}
