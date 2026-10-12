package com.mist.medicalmate.core.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException

/**
 * Vosk로 듣는 호출어 감지기.
 *
 * **모델은 들을 때 올리고 그만 들을 때 내린다.** 올리면 480MB를 쓴다. 앱이 뒤로 가도 들고
 * 있으면 시스템이 이 앱부터 죽인다. 다시 올리는 데 1초 남짓이라 화면으로 돌아올 때마다
 * 올려도 된다.
 *
 * 마이크는 `AudioRecord`로 직접 잡는다. Vosk의 `SpeechService`도 같은 일을 하지만 자기
 * 스레드와 콜백을 쓴다. 흐름을 거두는 것이 곧 마이크를 놓는 것이 되도록 이 안에서 돈다.
 */
internal class VoskWakeWordDetector
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val store: WakeWordModelStore,
    private val trace: VoiceWakeTrace,
) : WakeWordDetector {
    override fun detections(): Flow<Unit> = flow {
        val dir = store.directory()
        if (dir == null) {
            trace.log { "감지기: 모델이 없어 듣지 않는다 (${store.state.value})" }
            return@flow
        }
        LibVosk.setLogLevel(LogLevel.WARNINGS)

        val started = System.nanoTime()
        val model =
            try {
                Model(dir.path)
            } catch (e: IOException) {
                // 풀린 파일이 상했다. 토글을 다시 켜면 새로 받는다.
                trace.log { "감지기: 모델을 못 올렸다 (${e.message})" }
                return@flow
            }
        trace.log { "감지기: 모델을 올렸다 (${(System.nanoTime() - started) / NANOS_PER_MILLI}ms)" }
        try {
            Recognizer(model, SAMPLE_RATE.toFloat()).use { recognizer ->
                recognizer.setWords(true)
                listen(recognizer) { emit(Unit) }
            }
        } finally {
            model.close()
            trace.log { "감지기: 멈추고 모델을 내렸다" }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 발화가 끝날 때마다 판정한다.
     *
     * 말하는 중의 부분 결과에는 신뢰도가 없어서 끝난 발화만 본다. 그래서 "메이트야" 뒤에 잠깐
     * 쉬어야 불린다(Vosk가 0.5초 쉼을 발화의 끝으로 본다). 빅스비처럼 부르고 기다렸다 말하는
     * 흐름이다.
     */
    private suspend fun listen(recognizer: Recognizer, onWake: suspend () -> Unit) {
        val record = openMic() ?: return
        try {
            record.startRecording()
            trace.log { "감지기: 듣는 중 (마이크 상태 ${record.recordingState})" }
            val frame = ShortArray(FRAME_SAMPLES)
            var partial = ""
            var read = 0
            // 음수는 마이크를 빼앗겼거나 기기가 거절한 것이다. 다시 읽어도 같다.
            while (read >= 0) {
                currentCoroutineContext().ensureActive()
                read = record.read(frame, 0, frame.size)
                if (read >= 0) partial = consume(recognizer, frame, read, partial, onWake)
            }
            trace.log { "감지기: 마이크를 읽지 못했다 (오류 $read)" }
        } finally {
            record.stop()
            record.release()
        }
    }

    /**
     * 0.1초 조각 하나를 넘긴다. 발화가 끝났으면 판정하고, 아니면 말하는 중의 글을 남긴다.
     *
     * @return 지금까지 말하는 중의 글. 같은 글을 다시 찍지 않으려고 들고 다닌다.
     */
    private suspend fun consume(
        recognizer: Recognizer,
        frame: ShortArray,
        read: Int,
        partial: String,
        onWake: suspend () -> Unit,
    ): String {
        if (!recognizer.acceptWaveForm(frame, read)) return tracePartial(recognizer.partialResult, partial)
        if (judge(recognizer.result)) onWake()
        return ""
    }

    /** 끝난 발화를 판정하고 무엇을 들었는지 남긴다. 아무 말도 없던 발화는 남기지 않는다. */
    private fun judge(result: String): Boolean {
        val words = parseVoskWords(result)
        val wake = isWakeWord(words)
        if (words.isNotEmpty()) trace.log { "감지기: 들음 [${words.describe()}] → ${if (wake) "호출" else "아님"}" }
        return wake
    }

    /** 말하는 중의 글이 바뀌었을 때만 남긴다. 0.1초마다 같은 글을 찍으면 로그가 묻힌다. */
    private fun tracePartial(json: String, last: String): String {
        val text = parseVoskPartial(json)
        if (text.isNotEmpty() && text != last) trace.log { "감지기: 말하는 중 \"$text\"" }
        return text
    }

    /**
     * 마이크를 연다. 권한이 없거나 기기가 열어 주지 않으면 null이다.
     *
     * 켤 때 받아 둔 권한을 설정에서 거둘 수 있어서 잡기 직전에 다시 본다.
     */
    private fun openMic(): AudioRecord? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            trace.log { "감지기: 마이크 권한이 없다" }
            return null
        }
        val bufferSize =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                .coerceAtLeast(FRAME_SAMPLES * 2)
        val record =
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        return record.takeIf { it.state == AudioRecord.STATE_INITIALIZED } ?: run {
            trace.log { "감지기: 마이크를 열지 못했다 (상태 ${record.state})" }
            record.release()
            null
        }
    }

    private companion object {
        /** 모델이 16kHz로 학습됐다. */
        const val SAMPLE_RATE = 16_000

        /** 0.1초. 이보다 잘게 넘기면 JNA 왕복이 늘고, 크게 넘기면 판정이 늦다. */
        const val FRAME_SAMPLES = SAMPLE_RATE / 10

        const val NANOS_PER_MILLI = 1_000_000
    }
}
