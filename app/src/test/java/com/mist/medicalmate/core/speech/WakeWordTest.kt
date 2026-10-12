package com.mist.medicalmate.core.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 호출어 판정.
 *
 * 결과 JSON은 PC에서 `vosk-model-small-ko-0.22`로 Windows TTS(Heami) 음성을 받아쓴 실제 값이다.
 * 모양을 손으로 지어내면 Vosk가 실제로 내놓는 것과 어긋나도 시험이 통과한다.
 */
class WakeWordTest {
    @Test
    fun `메이트야는 호출이다`() {
        assertTrue(isWakeWord(parseVoskWords(result("메이트" to 1.0, "였다" to 1.0))))
    }

    @Test
    fun `진료메이트야도 호출이다`() {
        // 앱 이름을 붙여 불러도 된다. `진료`와 `메이트`가 따로 선다.
        assertTrue(isWakeWord(parseVoskWords(result("진료" to 1.0, "메이트" to 1.0, "였다" to 0.9))))
    }

    @Test
    fun `룸메이트야는 신뢰도가 모자라 호출이 아니다`() {
        // 받아쓰면 `용 메이트 의`가 되는데 `메이트`가 0.85다. 선을 0.9에 둔 이유다.
        assertFalse(isWakeWord(parseVoskWords(result("용" to 0.83, "메이트" to 0.85, "의" to 0.76, "안녕" to 1.0))))
    }

    @Test
    fun `비슷하게 들리는 말은 호출이 아니다`() {
        // 데이트·마트·메이크업. 모두 같은 시험에서 받아쓴 그대로다.
        listOf(
            result("아재" to 1.0, "테이트" to 1.0, "했어" to 1.0),
            result("마트" to 1.0, "타야" to 1.0, "지" to 1.0),
            result("메이크업" to 0.54, "에" to 1.0, "있어" to 1.0),
        ).forEach { json -> assertFalse(json, isWakeWord(parseVoskWords(json))) }
    }

    @Test
    fun `아무것도 못 들었으면 호출이 아니다`() {
        // 쉼만 들어오면 `result` 없이 빈 글만 온다.
        assertEquals(emptyList<HeardWord>(), parseVoskWords("""{"text" : ""}"""))
        assertFalse(isWakeWord(parseVoskWords("""{"text" : ""}""")))
    }

    @Test
    fun `모양이 어긋난 결과는 못 들은 것으로 친다`() {
        // 판정 하나 때문에 듣기를 멈출 일이 아니다.
        assertEquals(emptyList<HeardWord>(), parseVoskWords("not json"))
        assertEquals(emptyList<HeardWord>(), parseVoskWords("""{"result": "메이트"}"""))
    }

    @Test
    fun `말하는 중의 글을 읽는다`() {
        assertEquals("메이트 였", parseVoskPartial("""{"partial" : "메이트 였"}"""))
        assertEquals("", parseVoskPartial("""{"partial" : ""}"""))
        assertEquals("", parseVoskPartial("not json"))
    }

    @Test
    fun `로그에는 낱말과 신뢰도를 소수 둘째 자리까지 적는다`() {
        // 0.9 선 근처에서 갈리는지 보려고 남기는 값이다.
        val words = parseVoskWords(result("메이트" to 0.854, "였다" to 1.0))

        assertEquals("메이트 0.85 · 였다 1.00", words.describe())
    }

    /** Vosk가 `setWords(true)`일 때 내놓는 모양. 시각은 판정에 쓰지 않아 아무 값이나 둔다. */
    private fun result(vararg words: Pair<String, Double>): String {
        val items =
            words.joinToString(",") { (word, conf) ->
                """{"conf" : $conf, "end" : 1.2, "start" : 0.9, "word" : "$word"}"""
            }
        val text = words.joinToString(" ") { it.first }
        return """{"result" : [$items], "text" : "$text"}"""
    }
}
