package com.mist.medicalmate.visit.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.ondevice.ExtractedTurn
import com.mist.medicalmate.core.ondevice.MemoLabel
import com.mist.medicalmate.core.ondevice.OnDeviceExtractor
import com.mist.medicalmate.core.ondevice.OnDeviceResult
import com.mist.medicalmate.core.ondevice.QuestionCard
import com.mist.medicalmate.core.ondevice.SuggestedQuestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * 메모 분류의 두 길(#289).
 *
 * 폰이 라벨을 붙이는 길은 서버를 두 번 부른다. 문장만 나눠 받고, 라벨을 붙여 조립을 맡긴다.
 * 어느 단계에서 막혀도 지금까지의 서버 분류로 넘어가야 한다. 폰에서 못 했다고 메모를 못
 * 나누면 안 된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VisitClassifyOnDeviceTest {
    private val sentences = listOf("위염 초기래요.", "커피 줄이래요.")

    @Test
    fun `모델이 없으면 지금처럼 서버가 나눈다`() = runTest {
        val api = FakeVisitApi()

        classify(api, FakeOnDevice(available = false))

        assertEquals(
            listOf(ClassifyMemoRequest(memo = MEMO, visitedOn = "2026-10-14", clinicName = CLINIC)),
            api.requests,
        )
    }

    @Test
    fun `폰이 붙인 라벨로 서버가 조립한다`() = runTest {
        val api = FakeVisitApi()

        val result = classify(
            api,
            FakeOnDevice(
                labels = mapOf(
                    0 to MemoLabel.FINDINGS,
                    1 to MemoLabel.LIFESTYLE_INSTRUCTIONS,
                ),
            ),
        )

        assertEquals(2, api.requests.size)
        assertEquals(false, api.requests[0].classify)
        assertNull(api.requests[0].labels)
        val assemble = api.requests[1]
        assertEquals(mapOf("0" to "findings", "1" to "lifestyle_instructions"), assemble.labels)
        assertEquals(LabelsMetaRequest("Qwen3.5-2B-Q4_0-ssmout-q8", "memo-small-v5"), assemble.labelsMeta)
        // 문장을 나눈 규칙을 함께 보내야 그 사이 규칙이 바뀌었을 때 서버가 막는다.
        assertEquals("split-v3", assemble.splitVersion)
        assertEquals(ASSEMBLED, (result as ApiResult.Success).value.labels)
    }

    @Test
    fun `폰 출력이 어긋나면 서버가 나눈다`() = runTest {
        val api = FakeVisitApi()

        val result = classify(api, FakeOnDevice(malformed = true))

        assertEquals(2, api.requests.size)
        assertEquals(false, api.requests[0].classify)
        assertEquals(ClassifyMemoRequest(memo = MEMO, visitedOn = "2026-10-14", clinicName = CLINIC), api.requests[1])
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `조립을 거절하면 서버가 처음부터 나눈다`() = runTest {
        // 1과 3 사이에 서버의 문장 분리 규칙이 바뀌면 409가 온다. 라벨이 다른 문장을 가리킨다.
        val api = FakeVisitApi(rejectLabels = true)

        val result = classify(api, FakeOnDevice(labels = mapOf(0 to MemoLabel.FINDINGS, 1 to MemoLabel.NONE)))

        assertEquals(3, api.requests.size)
        assertEquals(ClassifyMemoRequest(memo = MEMO, visitedOn = "2026-10-14", clinicName = CLINIC), api.requests[2])
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `문장이 없으면 폰을 부르지 않는다`() = runTest {
        val api = FakeVisitApi(sentences = emptyList())
        val onDevice = FakeOnDevice(labels = emptyMap())

        val result = classify(api, onDevice)

        assertEquals(1, api.requests.size)
        assertEquals(0, onDevice.calls)
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `직전 라벨로 다시 나눌 때는 폰을 부르지 않는다`() = runTest {
        // 환자가 줄을 옮긴 뒤의 재조립이다. 라벨이 이미 있어 나눌 것이 없다.
        val api = FakeVisitApi()
        val onDevice = FakeOnDevice(labels = mapOf(0 to MemoLabel.FINDINGS))

        DefaultVisitRepository(api, Json, onDevice).classify(MEMO, null, CLINIC, labels = mapOf("0" to "tests"))

        assertEquals(
            listOf(ClassifyMemoRequest(memo = MEMO, clinicName = CLINIC, labels = mapOf("0" to "tests"))),
            api.requests,
        )
        assertEquals(0, onDevice.calls)
    }

    private suspend fun classify(api: FakeVisitApi, onDevice: OnDeviceExtractor): ApiResult<VisitClassification> =
        DefaultVisitRepository(api, Json, onDevice).classify(
            memo = MEMO,
            visitedOn = java.time.LocalDate.of(2026, 10, 14),
            clinic = CLINIC,
            labels = null,
        )

    private inner class FakeVisitApi(
        private val sentences: List<String> = this@VisitClassifyOnDeviceTest.sentences,
        private val rejectLabels: Boolean = false,
    ) : VisitApi {
        val requests = mutableListOf<ClassifyMemoRequest>()

        override suspend fun classify(request: ClassifyMemoRequest): ClassifyMemoResponse {
            requests += request
            return when {
                request.classify == false ->
                    ClassifyMemoResponse(
                        sentences = sentences,
                        labels = sentences.indices.associate { it.toString() to "none" },
                        splitVersion = "split-v3",
                    )
                request.labelsMeta != null && rejectLabels ->
                    throw HttpException(Response.error<Unit>(409, "".toResponseBody()))
                request.labelsMeta != null -> ClassifyMemoResponse(sentences = sentences, labels = ASSEMBLED)
                else -> ClassifyMemoResponse(sentences = sentences, labels = SERVER)
            }
        }

        override suspend fun visits() = error("이 시험이 부르지 않는다")

        override suspend fun visit(visitId: Long) = error("이 시험이 부르지 않는다")

        override suspend fun cardVisits(cardId: Long) = error("이 시험이 부르지 않는다")

        override suspend fun create(cardId: Long, request: CreateVisitRequest) = error("이 시험이 부르지 않는다")

        override suspend fun delete(visitId: Long) = error("이 시험이 부르지 않는다")
    }

    private class FakeOnDevice(
        private val available: Boolean = true,
        private val labels: Map<Int, MemoLabel> = emptyMap(),
        private val malformed: Boolean = false,
    ) : OnDeviceExtractor {
        var calls = 0

        override suspend fun available() = available

        override suspend fun extractTurn(user: String): OnDeviceResult<ExtractedTurn> = OnDeviceResult.Unavailable

        override suspend fun suggestQuestions(card: QuestionCard): OnDeviceResult<List<SuggestedQuestion>> =
            OnDeviceResult.Unavailable

        override suspend fun labelMemo(sentences: List<String>): OnDeviceResult<Map<Int, MemoLabel>> {
            calls++
            return if (malformed) {
                OnDeviceResult.Malformed(
                    "{",
                    IllegalStateException(),
                )
            } else {
                OnDeviceResult.Success(labels)
            }
        }
    }

    private companion object {
        const val MEMO = "위염 초기래요. 커피 줄이래요."
        const val CLINIC = "튼튼내과"
        val ASSEMBLED = mapOf("0" to "findings", "1" to "lifestyle_instructions")
        val SERVER = mapOf("0" to "findings", "1" to "medication_instructions")
    }
}
