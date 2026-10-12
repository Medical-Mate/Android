package com.mist.medicalmate.core.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * "헤이 메이트"를 기다린다(#273·#278).
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
 * 인식기에 줄 문법. 호출어와 "그 밖의 말"(`[unk]`) 둘 중에서만 고르게 한다.
 *
 * 영어 모델은 `[unk]`을 안다. 그래서 문법을 이렇게 좁혀도 "배가 아파요"가 호출어로 끼워
 * 맞춰지지 않고 `[unk]`으로 빠진다. 한국어 모델은 `[unk]`을 몰라 이것이 안 됐다.
 */
internal const val WAKE_GRAMMAR = """["hey mate", "[unk]"]"""

/**
 * 받아쓴 발화가 호출인지.
 *
 * **호출어는 영어 "hey mate"다(#278).** 처음에는 한국어 모델로 "메이트야"를 들었다. 그
 * 모델은 `[unk]`을 몰라 전체를 받아쓴 뒤 `메이트`를 찾아야 했고, 작은 언어 모델이 외래어
 * "메이트"를 거의 몰라 첫소리를 ㄴ·ㄹ로 들으면 `네이트`·`레이트`로 끌려갔다. 갤럭시에서 세 번
 * 재서 35%·33%·38%만 켜졌다. 영어 소형 모델은 문법을 [WAKE_GRAMMAR]로 좁힐 수 있고, PC에서
 * 영어·한국어 TTS로 "Hey mate"·"헤이 메이트"·"헤이메이트"를 빠르기 셋으로 읽힌 15번이 모두
 * 1.0으로 잡혔다. 모델도 압축 41MB, 메모리 150~180MB로 한국어 모델의 3분의 1이다.
 *
 * `hey` 바로 뒤에 `mate`가 와야 한다. "메이트"만 말하거나 "Hey man"이면 아니다.
 *
 * **`mate` 뒤에 확실한 다른 말이 붙으면 호출이 아니다.** 같은 시험에서 "헤어 메이크업 했어"가
 * `hey mate [unk] [unk]`로 세 번 모두 켜졌다. 일상 말이라 거른다. 확실하다는 선은 신뢰도
 * 0.8이다. 정말 부른 것도 끝에 0.4·0.55의 `[unk]`이 묻어 나온 일이 있었다. 대신 "헤이 메이트"
 * 뒤에 잠깐 쉬어야 한다. 명령은 패널이 뜬 뒤에 듣으므로 쓰는 흐름은 같다.
 *
 * "Hey Kate"는 `hey mate`로 들려 켜진다. 영어 문장이라 우리 사용자가 말할 일이 드물어 두었다.
 */
internal fun isWakeWord(words: List<HeardWord>): Boolean {
    val at = words.indices.firstOrNull { i ->
        words[i].word == "hey" &&
            words.getOrNull(i + 1)?.let { it.word == "mate" && it.conf >= WAKE_WORD_MIN_CONFIDENCE } == true
    } ?: return false
    return words.drop(at + 2).none { it.word == UNKNOWN && it.conf >= TRAILING_SPEECH_CONFIDENCE }
}

private const val UNKNOWN = "[unk]"

private const val WAKE_WORD_MIN_CONFIDENCE = 0.6

private const val TRAILING_SPEECH_CONFIDENCE = 0.8
