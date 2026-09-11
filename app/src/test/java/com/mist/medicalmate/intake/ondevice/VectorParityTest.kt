package com.mist.medicalmate.intake.ondevice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 트랙이 넘긴 테스트 벡터로 우리 쪽 계약을 확인한다.
 *
 * **엔진이 아직 없다.** 그래서 여기서 보는 것은 모델의 출력이 아니라 *기대 출력*이다.
 * PC에서 같은 설정으로 얻은 원문 118개를 우리 파서가 전부 읽어내는지, 그리고 일치 확인에
 * 쓸 비교기가 맞게 도는지를 지금 확정한다.
 *
 * 엔진이 붙으면 기기에서 88개를 돌려 나온 출력을 같은 비교기에 넣는다. 그때 파서가
 * 문제였는지 모델이 문제였는지 가를 수 있어야 한다.
 */
class VectorParityTest {
    @Test
    fun `진료 전 기대 출력 88개를 전부 읽는다`() {
        val failures =
            extractVectors().mapNotNull { vector ->
                val result = parseTurnExtraction(vector.expectedOutput)
                (result as? OnDeviceResult.Malformed)?.let { "${vector.id}: ${it.cause.message}" }
            }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `진료 전 벡터가 88개다`() {
        // 묶음이 바뀌면 개수도 바뀐다. 파일만 갈아 끼우고 지나가지 않게 세어 둔다.
        assertEquals(88, extractVectors().size)
    }

    @Test
    fun `설정이 묶음과 같다`() {
        // 온도 0과 고정 시드가 일치 확인의 전제다. 벡터가 그 값으로 만들어졌는지 확인한다.
        val settings = OnDeviceSettings()

        extractVectors().forEach { vector ->
            assertEquals(vector.id, settings.temperature, vector.temperature, 0.0)
            assertEquals(vector.id, settings.seed, vector.seed)
            assertEquals(vector.id, settings.maxTokens, vector.maxTokens)
        }
    }

    @Test
    fun `모든 축과 상태가 우리 열거형에 있다`() {
        // 스키마에 있는 값이 실제 출력에 다 나오지는 않는다. 나온 것만이라도 빠짐없이 읽혀야 한다.
        val axes = mutableSetOf<ExtractionAxis>()
        val statuses = mutableSetOf<FieldStatus>()

        extractVectors().forEach { vector ->
            val parsed = parseTurnExtraction(vector.expectedOutput) as OnDeviceResult.Success
            parsed.value.updates.forEach {
                axes += it.axis
                statuses += it.status
            }
        }

        assertTrue(axes.isNotEmpty())
        assertTrue(statuses.isNotEmpty())
    }

    @Test
    fun `메모 기대 출력 30개를 전부 읽는다`() {
        val failures =
            memoVectors().mapNotNull { vector ->
                val result = parseMemoLabels(vector.expectedOutput, vector.sentenceCount)
                (result as? OnDeviceResult.Malformed)?.let { "${vector.id}: ${it.cause.message}" }
            }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `번호 키를 자리 그대로 읽는다`() {
        // 번호를 잘못 읽으면 라벨이 밀린다. 서버가 그 번호로 문장을 찾으므로 치명적이다.
        // 사람 정답과 비교하지 않는다. 모델이 틀린 라벨을 붙인 줄이 있고 그것은 이 시험의
        // 관심사가 아니다.
        memoVectors().forEach { vector ->
            val raw = json.decodeFromString<JsonObject>(vector.expectedOutput)
            val parsed = parseMemoLabels(vector.expectedOutput, vector.sentenceCount) as OnDeviceResult.Success

            (0 until vector.sentenceCount).forEach { index ->
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
        // 문서가 "PC 라벨 정확도 107/115 = 93%"라고 적었다. 그 차이를 여기에 박아 둔다.
        // 기기에서 돌린 결과가 이 수치와 크게 달라지면 엔진이나 묶음이 다른 것이다.
        val labels =
            memoVectors().flatMap { vector ->
                val parsed = parseMemoLabels(vector.expectedOutput, vector.sentenceCount) as OnDeviceResult.Success
                vector.expectedLabels.mapIndexed { index, human -> human to parsed.value.getValue(index).wireName }
            }

        assertEquals(115, labels.size)
        assertEquals(107, labels.count { (human, model) -> human == model })
    }

    @Test
    fun `문장 수에 맞춰 번호 키 스키마를 만든다`() {
        // 파일은 N=4 예시뿐이다. 다섯 문장 메모에서 다섯 번째가 강제되지 않으면 라벨이 빈다.
        val schema = memoLabelsSchema(sentenceCount = 5)

        val properties = schema["properties"]!!.jsonObject
        val required = schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("0", "1", "2", "3", "4"), properties.keys.toList())
        assertEquals(listOf("0", "1", "2", "3", "4"), required)
        assertEquals(false, schema["additionalProperties"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `스키마의 라벨은 다섯 가지다`() {
        val enums =
            memoLabelsSchema(sentenceCount = 1)["properties"]!!
                .jsonObject["0"]!!
                .jsonObject["enum"]!!
                .jsonArray
                .map { it.jsonPrimitive.content }

        assertEquals(
            listOf("findings", "tests", "medication_instructions", "follow_up", "none"),
            enums,
        )
    }

    @Test
    fun `모르는 키가 섞이면 계약 위반으로 본다`() {
        // 스키마가 additionalProperties=false다. 모델이 diagnosis를 덧붙이면 잡아야 한다.
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
        readLines("/ondevice/vectors-qwen3-1.7b-q4_0-small-v4.jsonl").map { line ->
            val o = json.decodeFromString<JsonObject>(line)
            val settings = o.getValue("settings").jsonObject
            ExtractVector(
                id = o.getValue("id").jsonPrimitive.content,
                expectedOutput = o.getValue("expected_output").jsonPrimitive.content,
                temperature = settings.getValue("temperature").jsonPrimitive.content.toDouble(),
                seed = settings.getValue("seed").jsonPrimitive.content.toInt(),
                maxTokens = settings.getValue("max_tokens").jsonPrimitive.content.toInt(),
            )
        }

    private fun memoVectors(): List<MemoVector> =
        readLines("/ondevice/vectors-memo-qwen3-1.7b-q4_0-memo-small-v4.jsonl").map { line ->
            val o = json.decodeFromString<JsonObject>(line)
            MemoVector(
                id = o.getValue("id").jsonPrimitive.content,
                expectedOutput = o.getValue("expected_output").jsonPrimitive.content,
                sentenceCount = o.getValue("sentences").jsonArray.size,
                expectedLabels = o.getValue("expected_labels").jsonArray.map { it.jsonPrimitive.content },
            )
        }

    private fun readLines(path: String): List<String> =
        requireNotNull(javaClass.getResourceAsStream(path)) { "$path 없음" }
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }

    private data class ExtractVector(
        val id: String,
        val expectedOutput: String,
        val temperature: Double,
        val seed: Int,
        val maxTokens: Int,
    )

    private data class MemoVector(
        val id: String,
        val expectedOutput: String,
        val sentenceCount: Int,
        val expectedLabels: List<String>,
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
