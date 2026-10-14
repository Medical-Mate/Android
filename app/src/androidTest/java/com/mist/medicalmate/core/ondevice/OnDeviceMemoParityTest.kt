package com.mist.medicalmate.core.ondevice

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 기기에서 메모 벡터 34개를 돌려 PC 결과와 견준다(AI 문서 "일치 확인").
 *
 * 앱이 모델에 넣는 글은 JVM 시험이 확인한다(`VectorParityTest`). 여기서 보는 것은 **이 기기의
 * 엔진이 같은 글로 같은 라벨을 내는지**다. 하드웨어 수치 차이로 몇 건은 갈릴 수 있고(AI 실측에서
 * 45회 중 5건), 그것이 안전 위반인지는 AI 쪽 채점기가 판정한다. 그래서 갈린 수를 단정하지 않고
 * 로그에 남기며, 단정하는 것은 **모든 출력이 문법대로 읽힌다는 것** 하나다.
 *
 * 모델이 기기에 있어야 돈다. 내 정보에서 휴대폰 정리를 켜 받아 두거나, 받은 파일을 앱의
 * `no_backup/ondevice/`에 넣는다. 없으면 건너뛴다. CI는 androidTest를 돌리지 않는다.
 *
 * `adb logcat -s OnDeviceParity`로 줄마다의 결과와 시간을 본다.
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceMemoParityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun memoVectorsMatchPc() = runBlocking {
        val model = File(context.noBackupFilesDir, "ondevice/${OnDeviceBundle.MODEL_FILE}")
        assumeTrue("모델이 없다: ${model.path}", model.isFile)
        val engine = OnDeviceEngine(context)
        val state = engine.state()
        assumeTrue("엔진이 열리지 않았다", state is OnDeviceEngineState.Ready)
        Log.i(TAG, "엔진 ${(state as OnDeviceEngineState.Ready).version} · NPU ${state.npuReady}")

        val system = context.assets.open(OnDeviceBundle.MEMO_PROMPT_ASSET).bufferedReader().use { it.readText() }
        val vectors = vectors()
        val malformed = mutableListOf<String>()
        var sameAsPc = 0
        var labels = 0
        var sameLabels = 0

        vectors.forEach { vector ->
            val started = SystemClock.elapsedRealtime()
            val raw =
                engine.generate(
                    model,
                    ChatPrompt.of(system, memoUserMessage(vector.sentences)),
                    memoLabelsGrammar(vector.sentences.size),
                    OnDeviceSettings().maxTokens,
                )
            val elapsed = SystemClock.elapsedRealtime() - started
            val parsed = raw?.let { parseMemoLabels(it, vector.sentences.size) }
            if (parsed !is OnDeviceResult.Success) {
                malformed += "${vector.id}: $raw"
                Log.w(TAG, "${vector.id} 읽지 못함 ${elapsed}ms: $raw")
                return@forEach
            }
            val pc = (parseMemoLabels(vector.expectedOutput, vector.sentences.size) as OnDeviceResult.Success).value
            if (parsed.value == pc) sameAsPc++
            labels += pc.size
            sameLabels += pc.count { (index, label) -> parsed.value[index] == label }
            Log.i(TAG, "${vector.id} ${elapsed}ms ${if (parsed.value == pc) "=" else "≠"} $raw")
        }
        engine.release()

        Log.i(TAG, "요약: 벡터 ${vectors.size} · PC와 같은 출력 $sameAsPc · 라벨 $sameLabels/$labels · 못 읽음 ${malformed.size}")
        assertEquals(emptyList<String>(), malformed)
    }

    private fun vectors(): List<MemoVector> = instrumentation.context.assets
        .open(VECTORS)
        .bufferedReader()
        .useLines { lines -> lines.filter { it.isNotBlank() }.toList() }
        .map { line ->
            val o = json.decodeFromString<JsonObject>(line)
            MemoVector(
                id = o.getValue("id").jsonPrimitive.content,
                sentences = o.getValue("sentences").jsonArray.map { it.jsonPrimitive.content },
                expectedOutput = o.getValue("expected_output").jsonPrimitive.content,
            )
        }

    private data class MemoVector(val id: String, val sentences: List<String>, val expectedOutput: String)

    private companion object {
        const val TAG = "OnDeviceParity"
        const val VECTORS = "vectors-memo-qwen3.5-2b-q4_0-ssmq8-memo-small-v5.jsonl"
        val json = Json { ignoreUnknownKeys = true }
    }
}
