package com.mist.medicalmate.core.ondevice

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 기기에서 테스트 벡터를 돌려 PC 결과와 견준다(AI 문서 "일치 확인").
 *
 * 앱이 모델에 넣는 글은 JVM 시험이 확인한다(`VectorParityTest`). 여기서 보는 것은 **이 기기의
 * 엔진이 앱이 부르는 길(C API)로 같은 출력을 내는지**다. AI 쪽 숫자는 llama-server로 잰 것이라
 * 그 길과 같은지가 따로 확인돼야 한다.
 *
 * 하드웨어 수치 차이로 몇 건은 갈릴 수 있고, 그것이 맞는지는 AI 쪽 채점기가 판정한다. 그래서 갈린
 * 수를 단정하지 않는다. 단정하는 것은 **모든 출력이 문법대로 읽힌다는 것** 하나다. 출력은 AI 쪽
 * 실측 스크립트(`ondevice_device_run.py`)와 같은 모양의 jsonl로 남겨 그대로 넘길 수 있게 한다.
 *
 * 진료 전 추출은 화면에 아직 붙지 않았지만 같은 엔진·같은 문법이라 함께 돌린다.
 *
 * 모델이 기기에 있어야 돈다. 내 정보에서 휴대폰 정리를 켜 받아 두거나, 받은 파일을 앱의
 * `no_backup/ondevice/`에 넣는다. 없으면 건너뛴다. CI는 androidTest를 돌리지 않는다.
 *
 * 결과는 `adb pull /sdcard/Android/data/com.mist.medicalmate/files/device-app-*.jsonl`로
 * 꺼내고, `adb logcat -s OnDeviceParity`에 요약이 남는다.
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceParityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun memoVectorsOnDevice() = runVectors(MEMO_VECTORS, "device-app-memo.jsonl") { vector ->
        val sentences = vector.getValue("sentences").jsonArray.map { it.jsonPrimitive.content }
        Run(
            prompt = ChatPrompt.of(asset(OnDeviceBundle.MEMO_PROMPT_ASSET), memoUserMessage(sentences)),
            grammar = memoLabelsGrammar(sentences.size),
            parse = { parseMemoLabels(it, sentences.size) },
        )
    }

    @Test
    fun extractVectorsOnDevice() = runVectors(EXTRACT_VECTORS, "device-app-extract.jsonl") { vector ->
        Run(
            prompt =
            ChatPrompt.of(
                vector.getValue("system").jsonPrimitive.content,
                vector.getValue("user").jsonPrimitive.content,
            ),
            grammar = asset(OnDeviceBundle.EXTRACT_GRAMMAR_ASSET),
            parse = ::parseTurnExtraction,
        )
    }

    /** 벡터마다 한 번 생성하고 출력과 시간을 jsonl로 남긴다. 읽지 못한 출력이 있으면 실패다. */
    private fun runVectors(vectorFile: String, outFile: String, build: (JsonObject) -> Run) = runBlocking {
        val model = File(context.noBackupFilesDir, "ondevice/${OnDeviceBundle.MODEL_FILE}")
        assumeTrue("모델이 없다: ${model.path}", model.isFile)
        val engine = OnDeviceEngine(context)
        val state = engine.state()
        assumeTrue("엔진이 열리지 않았다", state is OnDeviceEngineState.Ready)
        Log.i(TAG, "엔진 ${(state as OnDeviceEngineState.Ready).version} · NPU ${state.npuReady}")

        val out = File(context.getExternalFilesDir(null), outFile)
        val malformed = mutableListOf<String>()
        var sameAsPc = 0
        val vectors = vectors(vectorFile)
        out.bufferedWriter().use { writer ->
            vectors.forEach { vector ->
                val id = vector.getValue("id").jsonPrimitive.content
                val expected = vector.getValue("expected_output").jsonPrimitive.content
                val run = build(vector)
                val started = SystemClock.elapsedRealtime()
                val raw = engine.generate(model, run.prompt, run.grammar, OnDeviceSettings().maxTokens)
                val seconds = (SystemClock.elapsedRealtime() - started) / MILLIS
                val readable = raw?.let(run.parse) is OnDeviceResult.Success
                val same = raw != null && sameJson(raw, expected)
                if (!readable) malformed += id
                if (same) sameAsPc++
                writer.appendLine(
                    buildJsonObject {
                        put("id", id)
                        put("output", raw.orEmpty())
                        put("error", if (raw == null) "생성 실패" else null)
                        put("latency_s", seconds)
                        put("same_as_pc", same)
                    }.toString(),
                )
                Log.i(TAG, "$id ${seconds}s ${if (same) "same" else "diff"}${if (readable) "" else " unreadable"}")
            }
        }
        engine.release()

        Log.i(TAG, "요약 $outFile: 벡터 ${vectors.size} · PC와 같은 출력 $sameAsPc · 못 읽음 ${malformed.size}")
        assertEquals(emptyList<String>(), malformed)
    }

    /** 공백·키 순서를 무시하고 견준다(AI 문서 "JSON 파싱 후 비교"). */
    private fun sameJson(a: String, b: String): Boolean =
        runCatching { json.parseToJsonElement(a) == json.parseToJsonElement(b) }.getOrDefault(false)

    private fun vectors(name: String): List<JsonObject> = instrumentation.context.assets
        .open(name)
        .bufferedReader()
        .useLines { lines -> lines.filter { it.isNotBlank() }.toList() }
        .map { json.decodeFromString<JsonObject>(it) }

    private fun asset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    private class Run(val prompt: ChatPrompt, val grammar: String, val parse: (String) -> OnDeviceResult<*>)

    private companion object {
        const val TAG = "OnDeviceParity"
        const val MEMO_VECTORS = "vectors-memo-qwen3.5-2b-q4_0-ssmq8-memo-small-v5.jsonl"
        const val EXTRACT_VECTORS = "vectors-qwen3.5-2b-q4_0-ssmq8-extract-small-2b-v1.jsonl"
        const val MILLIS = 1000.0
        val json = Json { ignoreUnknownKeys = true }
    }
}
