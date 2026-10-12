package com.mist.medicalmate.core.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 호출어 판정.
 *
 * 결과 JSON은 PC에서 `vosk-model-small-en-us-0.15`에 문법 `["hey mate", "[unk]"]`을 주고,
 * Windows TTS(영어 Zira, 한국어 Heami)를 빠르기 셋으로 받아쓴 실제 값이다(#278). 모양을 손으로
 * 지어내면 Vosk가 실제로 내놓는 것과 어긋나도 시험이 통과한다.
 */
class WakeWordTest {
    @Test
    fun `hey mate는 호출이다`() {
        assertTrue(isWakeWord(parseVoskWords(result("hey" to 1.0, "mate" to 1.0))))
    }

    @Test
    fun `한국어 억양의 헤이 메이트도 호출이다`() {
        // 한국어 음성이 "헤이 메이트"·"헤이메이트"를 읽은 것. 끝에 약한 [unk]이 묻어 나왔다.
        listOf(
            result("hey" to 1.0, "mate" to 1.0, "[unk]" to 0.4),
            result("hey" to 1.0, "mate" to 1.0, "[unk]" to 0.55),
        ).forEach { json -> assertTrue(json, isWakeWord(parseVoskWords(json))) }
    }

    @Test
    fun `앞에 다른 말이 있어도 호출이다`() {
        // "음, 헤이 메이트"처럼 부를 수 있다.
        assertTrue(isWakeWord(parseVoskWords(result("[unk]" to 1.0, "hey" to 1.0, "mate" to 1.0))))
    }

    @Test
    fun `mate 뒤에 확실한 다른 말이 붙으면 호출이 아니다`() {
        // "헤어 메이크업 했어"가 이렇게 받아써져 세 번 모두 켜졌다. 일상 말이라 거른다.
        listOf(
            result("hey" to 1.0, "mate" to 1.0, "[unk]" to 1.0, "[unk]" to 1.0),
            result("hey" to 1.0, "mate" to 0.7, "[unk]" to 1.0),
        ).forEach { json -> assertFalse(json, isWakeWord(parseVoskWords(json))) }
    }

    @Test
    fun `hey 바로 뒤에 mate가 와야 한다`() {
        // "Hey man", "메이트"만, "Make it eight", "네 이따 봐".
        listOf(
            result("hey" to 1.0, "[unk]" to 1.0),
            result("mate" to 1.0),
            result("mate" to 0.54),
            result("mate" to 0.82, "[unk]" to 1.0),
            result("hey" to 1.0, "[unk]" to 1.0, "mate" to 1.0),
        ).forEach { json -> assertFalse(json, isWakeWord(parseVoskWords(json))) }
    }

    @Test
    fun `mate의 신뢰도가 모자라면 호출이 아니다`() {
        assertFalse(isWakeWord(parseVoskWords(result("hey" to 1.0, "mate" to 0.59))))
        assertTrue(isWakeWord(parseVoskWords(result("hey" to 1.0, "mate" to 0.6))))
    }

    @Test
    fun `그 밖의 말은 호출이 아니다`() {
        // "배가 아파요", "오늘 진료 예약했어요" 같은 말은 [unk] 하나로 빠진다.
        assertFalse(isWakeWord(parseVoskWords(result("[unk]" to 1.0))))
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
        assertEquals(emptyList<HeardWord>(), parseVoskWords("""{"result": "mate"}"""))
    }

    @Test
    fun `말하는 중의 글을 읽는다`() {
        assertEquals("hey mate", parseVoskPartial("""{"partial" : "hey mate"}"""))
        assertEquals("", parseVoskPartial("""{"partial" : ""}"""))
        assertEquals("", parseVoskPartial("not json"))
    }

    @Test
    fun `로그에는 낱말과 신뢰도를 소수 둘째 자리까지 적는다`() {
        // 선 근처에서 갈리는지 보려고 남기는 값이다.
        val words = parseVoskWords(result("hey" to 1.0, "mate" to 0.674))

        assertEquals("hey 1.00 · mate 0.67", words.describe())
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
