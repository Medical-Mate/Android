package com.mist.medicalmate.core.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream

/**
 * 받아쓰기에 넘길 마이크 소리를 앱이 직접 잡아 다듬는다(#328).
 *
 * ML Kit가 받는 형식(헤더 없는 16비트 PCM · 모노 · 16kHz)으로 `AudioRecord`를 열고, 기기가 지원하면 잡음
 * 제거와 자동 음량 조절을 붙인다. 읽은 소리는 [NoiseGate]를 거쳐 파이프에 쓰고, 받아쓰기는 파이프의 읽는 쪽
 * [input]을 `AudioSource.fromPfd`로 받는다. `AudioRecord`가 실시간 속도로 읽히므로 문서가 요구하는 속도가
 * 저절로 맞는다.
 *
 * 마이크는 한 번에 하나만 잡힌다. 호출어 감지기가 다 거둔 뒤에 열리는 순서는 지금과 같다
 * (`VoiceCommandViewModel`). 어떤 길로 끝나든 [close]가 마이크·효과·파이프를 놓아야 감지기가 다시 잡는다.
 */
internal class CleanMic private constructor(
    private val record: AudioRecord,
    private val effects: List<AudioEffect>,
    /** 받아쓰기가 읽는 쪽. */
    val input: ParcelFileDescriptor,
    private val output: OutputStream,
) {
    private val gate = NoiseGate()

    @Volatile
    private var running = true

    /** 붙인 효과의 이름. debug 로그에 남긴다. */
    val effectNames: List<String> get() = effects.map { it.javaClass.simpleName }

    /** 0.1초씩 읽어 다듬어 파이프에 쓴다. [close]가 불릴 때까지 돌아서 IO 스레드에서 부른다. */
    fun pump() {
        val frame = ShortArray(FRAME_SAMPLES)
        record.startRecording()
        while (running) {
            val read = record.read(frame, 0, frame.size)
            val out = if (read > 0) gate.push(frame.copyOf(read)) else null
            // 마이크가 닫혔거나 받아쓰기가 먼저 끝나 쓸 곳이 없으면 멈춘다.
            if (read <= 0 || (out != null && !write(out))) running = false
        }
    }

    /** 다 들은 뒤의 소음 바닥과 연 비율. debug 로그에 남긴다. */
    fun summary(): String = "소음 바닥 ${gate.noiseFloor.toInt()} · 연 조각 ${gate.opened}/${gate.emitted} · 효과 $effectNames"

    fun close() {
        running = false
        runCatching { record.stop() }
        record.release()
        effects.forEach { it.release() }
        runCatching { output.close() }
        runCatching { input.close() }
    }

    /** 받아쓰기가 먼저 끝나 읽는 쪽이 닫히면 쓰기가 실패한다. 그때 멈춘다. */
    private fun write(samples: ShortArray): Boolean = try {
        output.write(samples.toLittleEndian())
        true
    } catch (_: IOException) {
        false
    }

    companion object {
        /** 받아쓰기와 같은 형식이다. 호출어 감지기도 같은 값으로 연다. */
        private const val SAMPLE_RATE = 16_000
        private const val FRAME_SAMPLES = SAMPLE_RATE / 10

        /**
         * 마이크를 연다. 권한이 없거나 기기가 열어 주지 않으면 null이다.
         *
         * 음성 인식용 입력(`VOICE_RECOGNITION`)을 쓴다. 통화용 처리가 덜 걸려 말소리가 덜 뭉개진다.
         */
        fun open(context: Context): CleanMic? {
            val record =
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    newRecord()
                } else {
                    null
                }
            if (record?.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                return null
            }
            val (read, write) = ParcelFileDescriptor.createPipe()
            return CleanMic(
                record = record,
                effects = effectsFor(record.audioSessionId),
                input = read,
                output = ParcelFileDescriptor.AutoCloseOutputStream(write),
            )
        }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        private fun newRecord(): AudioRecord {
            val bufferSize =
                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    .coerceAtLeast(FRAME_SAMPLES * 2)
            return AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        }

        /** 기기가 지원하는 것만 붙인다. 못 붙이면 그 효과 없이 간다. */
        private fun effectsFor(session: Int): List<AudioEffect> = listOfNotNull(
            if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(session) else null,
            if (AutomaticGainControl.isAvailable()) AutomaticGainControl.create(session) else null,
        ).onEach { it.enabled = true }
    }
}

/** PCM 16비트는 리틀 엔디언이다. */
private fun ShortArray.toLittleEndian(): ByteArray {
    val bytes = ByteArray(size * 2)
    forEachIndexed { i, sample ->
        bytes[i * 2] = (sample.toInt() and BYTE_MASK).toByte()
        bytes[i * 2 + 1] = (sample.toInt() shr BYTE_BITS and BYTE_MASK).toByte()
    }
    return bytes
}

private const val BYTE_MASK = 0xFF
private const val BYTE_BITS = 8
