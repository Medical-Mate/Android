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
) : WakeWordDetector {
    override fun detections(): Flow<Unit> = flow {
        val dir = store.directory() ?: return@flow
        LibVosk.setLogLevel(LogLevel.WARNINGS)

        val model =
            try {
                Model(dir.path)
            } catch (_: IOException) {
                // 풀린 파일이 상했다. 토글을 다시 켜면 새로 받는다.
                return@flow
            }
        try {
            Recognizer(model, SAMPLE_RATE.toFloat()).use { recognizer ->
                recognizer.setWords(true)
                listen(recognizer) { emit(Unit) }
            }
        } finally {
            model.close()
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
            val frame = ShortArray(FRAME_SAMPLES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = record.read(frame, 0, frame.size)
                // 음수는 마이크를 빼앗겼거나 기기가 거절한 것이다. 다시 읽어도 같다.
                if (read < 0) break
                if (recognizer.acceptWaveForm(frame, read) && isWakeWord(parseVoskWords(recognizer.result))) {
                    onWake()
                }
            }
        } finally {
            record.stop()
            record.release()
        }
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
            record.release()
            null
        }
    }

    private companion object {
        /** 모델이 16kHz로 학습됐다. */
        const val SAMPLE_RATE = 16_000

        /** 0.1초. 이보다 잘게 넘기면 JNA 왕복이 늘고, 크게 넘기면 판정이 늦다. */
        const val FRAME_SAMPLES = SAMPLE_RATE / 10
    }
}
