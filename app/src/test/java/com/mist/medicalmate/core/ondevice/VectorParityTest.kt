package com.mist.medicalmate.core.ondevice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * AI 트랙이 넘긴 묶음 v2(Qwen3.5-2B)의 테스트 벡터로 우리 쪽 계약을 확인한다.
 *
 * 모델 출력은 기기에서만 나온다. 여기서 보는 것은 **모델에 들어가는 글이 AI 쪽과 같은지**와
 * **AI 쪽 기대 출력을 우리가 읽어내는지**다. 이 둘이 맞으면 기기에서 다른 출력이 나왔을 때
 * 원인이 앱의 조립이 아니라 엔진이나 모델 쪽이라고 가를 수 있다.
 *
 * 묶음이 v1(1.7B)에서 v2로 바뀌면서 벡터·개수·라벨 수가 함께 바뀌었다(#289, AI#35).
 */
class VectorParityTest {
    @Test
    fun `진료 전 기대 출력 79개를 전부 읽는다`() {
        val failures =
            extractVectors().mapNotNull { vector ->
                val result = parseTurnExtraction(vector.expectedOutput)
                (result as? OnDeviceResult.Malformed)?.let { "${vector.id}: ${it.cause.message}" }
            }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `진료 전 벡터가 79개다`() {
        // 묶음이 바뀌면 개수도 바뀐다. 파일만 갈아 끼우고 지나가지 않게 세어 둔다.
        assertEquals(79, extractVectors().size)
    }

    @Test
    fun `설정이 묶음과 같다`() {
        // 온도 0이 일치 확인의 전제다. 벡터가 그 값으로 만들어졌는지 확인한다.
        val settings = OnDeviceSettings()

        extractVectors().forEach { vector ->
            assertEquals(vector.id, settings.temperature, vector.temperature, 0.0)
            assertEquals(vector.id, settings.seed, vector.seed)
            assertEquals(vector.id, settings.maxTokens, vector.maxTokens)
        }
    }

    @Test
    fun `자산의 프롬프트가 벡터의 시스템 프롬프트와 같다`() {
        // 프롬프트 파일을 잘못 갈아 끼우면 모델은 돌지만 다른 묶음이 된다. 아무 에러도 나지 않는다.
        assertEquals(extractVectors().first().system, asset(OnDeviceBundle.EXTRACT_PROMPT_ASSET))
        assertEquals(memoVectors().first().system, asset(OnDeviceBundle.MEMO_PROMPT_ASSET))
    }

    @Test
    fun `프롬프트 전문이 AI 쪽 렌더와 글자까지 같다`() {
        // 생각 끄기 블록이 빠지거나 줄바꿈 하나가 달라도 같은 모델이 다른 출력을 낸다.
        val vector = extractVectors().single { it.id == "A01#0" }

        val rendered = ChatPrompt.of(vector.system, vector.user).full

        assertEquals(resource("/ondevice/rendered-2b-A01-0.enable_thinking_false.txt"), rendered)
    }

    @Test
    fun `메모 사용자 메시지를 벡터와 같게 만든다`() {
        memoVectors().forEach { vector ->
            assertEquals(vector.id, vector.user, memoUserMessage(vector.sentences))
        }
    }

    @Test
    fun `네 문장 메모 문법이 AI 쪽 예시 파일과 같다`() {
        assertEquals(resource("/ondevice/memo_labels-memo-small-v5.n4.gbnf"), memoLabelsGrammar(sentenceCount = 4))
    }

    @Test
    fun `문법이 문장 수만큼 번호를 차례로 요구한다`() {
        // 4개짜리 예시를 그대로 쓰면 다섯 문장 메모의 다섯 번째 라벨이 나오지 않는다.
        val grammar = memoLabelsGrammar(sentenceCount = 11)

        val root = grammar.lines().single { it.startsWith("root ::=") }
        assertEquals(
            "root ::= \"{\" space " + (0..10).joinToString(" \",\" space ") { "$it-kv" } + " space \"}\"",
            root,
        )
    }

    @Test
    fun `문법의 라벨은 여섯 가지다`() {
        // v5에서 생활 지침이 들어왔다. 빠지면 "커피 줄이래요"가 약 칸으로 간다.
        val labels = Regex("\\\\\"([a-z_]+)\\\\\"").findAll(memoLabelsGrammar(1).lines().first()).map {
            it.groupValues[1]
        }

        assertEquals(
            listOf("findings", "tests", "medication_instructions", "lifestyle_instructions", "follow_up", "none"),
            labels.toList(),
        )
    }

    @Test
    fun `메모 기대 출력 34개를 전부 읽는다`() {
        val failures =
            memoVectors().mapNotNull { vector ->
                val result = parseMemoLabels(vector.expectedOutput, vector.sentences.size)
                (result as? OnDeviceResult.Malformed)?.let { "${vector.id}: ${it.cause.message}" }
            }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `번호 키를 자리 그대로 읽는다`() {
        // 번호를 잘못 읽으면 라벨이 밀린다. 서버가 그 번호로 문장을 찾으므로 치명적이다.
        memoVectors().forEach { vector ->
            val raw = json.decodeFromString<JsonObject>(vector.expectedOutput)
            val parsed = parseMemoLabels(vector.expectedOutput, vector.sentences.size) as OnDeviceResult.Success

            vector.sentences.indices.forEach { index ->
                assertEquals(
                    "${vector.id} #$index",
                    raw.getValue(index.toString()).jsonPrimitive.content,
                    parsed.value.getValue(index).wireName,
                )
            }
        }
    }

    @Test
    fun `모델 라벨이 사람 정답과 다른 줄이 있다`() {
        // 문서가 "라벨 124/132"라고 적었다. 기기 결과가 이 수치와 크게 달라지면 엔진이나 묶음이 다른 것이다.
        val labels =
            memoVectors().flatMap { vector ->
                val parsed = parseMemoLabels(vector.expectedOutput, vector.sentences.size) as OnDeviceResult.Success
                vector.expectedLabels.mapIndexed { index, human -> human to parsed.value.getValue(index).wireName }
            }

        assertEquals(132, labels.size)
        assertEquals(124, labels.count { (human, model) -> human == model })
    }

    @Test
    fun `모르는 키가 섞이면 계약 위반으로 본다`() {
        // 모델이 diagnosis를 덧붙이면 잡아야 한다.
        val raw = """{"updates":[],"notes":[],"wants_to_stop":false,"diagnosis":"위염"}"""

        assertTrue(parseTurnExtraction(raw) is OnDeviceResult.Malformed)
    }

    @Test
    fun `라벨이 하나 빠지면 계약 위반으로 본다`() {
        // 서버가 번호로 문장을 찾는다. 하나라도 없으면 조립이 어긋난다.
        val raw = """{"0":"findings","1":"tests"}"""

        assertTrue(parseMemoLabels(raw, sentenceCount = 3) is OnDeviceResult.Malformed)
    }

    private fun extractVectors(): List<ExtractVector> =
        readLines("/ondevice/vectors-qwen3.5-2b-q4_0-ssmq8-extract-small-2b-v1.jsonl").map { line ->
            val o = json.decodeFromString<JsonObject>(line)
            val settings = o.getValue("settings").jsonObject
            ExtractVector(
                id = o.getValue("id").jsonPrimitive.content,
                system = o.getValue("system").jsonPrimitive.content,
                user = o.getValue("user").jsonPrimitive.content,
                expectedOutput = o.getValue("expected_output").jsonPrimitive.content,
                temperature = settings.getValue("temperature").jsonPrimitive.content.toDouble(),
                seed = settings.getValue("seed").jsonPrimitive.content.toInt(),
                maxTokens = settings.getValue("max_tokens").jsonPrimitive.content.toInt(),
            )
        }

    private fun memoVectors(): List<MemoVector> =
        readLines("/ondevice/vectors-memo-qwen3.5-2b-q4_0-ssmq8-memo-small-v5.jsonl").map { line ->
            val o = json.decodeFromString<JsonObject>(line)
            MemoVector(
                id = o.getValue("id").jsonPrimitive.content,
                system = o.getValue("system").jsonPrimitive.content,
                user = o.getValue("user").jsonPrimitive.content,
                sentences = o.getValue("sentences").jsonArray.map { it.jsonPrimitive.content },
                expectedOutput = o.getValue("expected_output").jsonPrimitive.content,
                expectedLabels = o.getValue("expected_labels").jsonArray.map { it.jsonPrimitive.content },
            )
        }

    private fun readLines(path: String): List<String> = resource(path).lines().filter { it.isNotBlank() }

    private fun resource(path: String): String =
        requireNotNull(javaClass.getResourceAsStream(path)) { "$path 없음" }.bufferedReader().use { it.readText() }

    /** 자산은 유닛 시험의 클래스패스에 없다. 모듈 폴더가 작업 폴더라 경로로 읽는다. */
    private fun asset(path: String): String = File("src/main/assets/$path").readText()

    private data class ExtractVector(
        val id: String,
        val system: String,
        val user: String,
        val expectedOutput: String,
        val temperature: Double,
        val seed: Int,
        val maxTokens: Int,
    )

    private data class MemoVector(
        val id: String,
        val system: String,
        val user: String,
        val sentences: List<String>,
        val expectedOutput: String,
        val expectedLabels: List<String>,
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
