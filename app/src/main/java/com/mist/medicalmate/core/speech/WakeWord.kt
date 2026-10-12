package com.mist.medicalmate.core.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * "메이트야"를 기다린다(#273).
 *
 * 앱이 화면에 있는 동안만 듣는다. 호출어가 들릴 때마다 한 번씩 흘리고, 거두면 마이크를 놓는다.
 * 모델이 없거나 마이크 권한이 없으면 아무것도 흘리지 않고 끝난다.
 */
interface WakeWordDetector {
    fun detections(): Flow<Unit>
}

/**
 * 받아쓴 낱말 하나와 그 신뢰도.
 *
 * Vosk가 `setWords(true)`일 때 결과에 싣는 모양이다. 시각(`start`·`end`)은 쓰지 않는다.
 */
@Serializable
internal data class HeardWord(val word: String, val conf: Double)

@Serializable
private data class VoskResult(@SerialName("result") val words: List<HeardWord> = emptyList())

@Serializable
private data class VoskPartial(val partial: String = "")

private val voskJson = Json { ignoreUnknownKeys = true }

/** 말하는 중의 부분 결과(`{"partial": "..."}`)에서 글만. 어긋나면 빈 글이다. */
internal fun parseVoskPartial(json: String): String = try {
    voskJson.decodeFromString<VoskPartial>(json).partial
} catch (_: SerializationException) {
    ""
} catch (_: IllegalArgumentException) {
    ""
}

/** 낱말과 신뢰도를 한 줄로. 로그에 쓴다. 예: `메이트 1.00 · 였다 0.93`. */
internal fun List<HeardWord>.describe(): String =
    joinToString(" · ") { "${it.word} ${"%.2f".format(Locale.ROOT, it.conf)}" }

/**
 * Vosk 결과를 낱말 목록으로.
 *
 * 아무것도 못 들었으면 `result` 없이 `{"text": ""}`만 온다. 모양이 어긋나면 못 들은 것으로
 * 친다 — 호출어 판정 하나 때문에 듣기를 멈출 일이 아니다.
 */
internal fun parseVoskWords(json: String): List<HeardWord> = try {
    voskJson.decodeFromString<VoskResult>(json).words
} catch (_: SerializationException) {
    emptyList()
} catch (_: IllegalArgumentException) {
    emptyList()
}

/**
 * 받아쓴 발화가 호출인지.
 *
 * **문법으로 좁히지 않고 전체를 받아쓴 뒤 찾는다.** 한국어 모델이 `[unk]`(그 밖의 말)을
 * 모르기 때문이다. 문법을 `메이트 야`로 좁히면 "배가 아파요"까지 모든 말을 그것으로 끼워
 * 맞춘다. PC에서 TTS 14문장으로 확인했다.
 *
 * "메이트야"는 한 낱말로는 어휘에 없다. 받아쓰면 `메이트 였다`나 `메이트 의`처럼 `메이트`가
 * 홀로 서고 뒤는 그때그때 다르다. 그래서 `메이트`만 본다.
 *
 * **신뢰도 0.9 아래는 버린다.** 같은 시험에서 호출어 셋은 모두 1.0이었고 "룸메이트야"는
 * `용 메이트 의`의 0.85였다. 사람 목소리와 소음에서 이 선이 맞는지는 기기에서 다시 본다.
 */
internal fun isWakeWord(words: List<HeardWord>): Boolean =
    words.any { it.word == WAKE_WORD && it.conf >= WAKE_WORD_MIN_CONFIDENCE }

private const val WAKE_WORD = "메이트"

private const val WAKE_WORD_MIN_CONFIDENCE = 0.9
