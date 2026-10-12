package com.mist.medicalmate.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceWordsTest {
    @Test
    fun `네로 답한 말을 알아듣는다`() {
        listOf("네", "네.", "응 저장해 줘", "그래", "좋아요", "저장", "예").forEach { assertTrue(it, VoiceWords.isYes(it)) }
    }

    @Test
    fun `아니요는 네가 아니다`() {
        // "아니, 저장하지 마"에 "저장"이 들어 있어도 아니다.
        listOf("아니요", "아니 저장하지 마", "취소", "잠깐만").forEach { text ->
            assertTrue(text, VoiceWords.isNo(text))
            assertFalse(text, VoiceWords.isYes(text))
        }
    }

    @Test
    fun `어로 시작하는 말을 네로 읽지 않는다`() {
        // "어제", "어디"가 확인으로 읽히면 묻던 일이 그대로 저장된다.
        listOf("어제 진료", "어디 병원").forEach { assertFalse(it, VoiceWords.isYes(it)) }
    }

    @Test
    fun `그만하라는 짧은 말만 닫는다`() {
        listOf("그만", "닫아 줘", "끝", "됐어").forEach { assertTrue(it, VoiceWords.isStop(it)) }
        // 긴 문장 속의 "끝"은 닫으라는 말이 아니다.
        assertFalse(VoiceWords.isStop("끝나고 약 먹을 때 물어볼 질문 추가"))
    }

    @Test
    fun `몇 번째인지 읽는다`() {
        assertEquals(0, VoiceWords.ordinal("첫 번째"))
        assertEquals(1, VoiceWords.ordinal("두번째 거"))
        assertEquals(2, VoiceWords.ordinal("세 번째 병원"))
        assertEquals(2, VoiceWords.ordinal("3번"))
        assertEquals(9, VoiceWords.ordinal("10번째"))
        assertEquals(VoiceWords.LAST, VoiceWords.ordinal("마지막 거"))
        assertNull(VoiceWords.ordinal("고려튼튼통증의학과"))
    }

    @Test
    fun `저장하라는 말을 알아듣는다`() {
        listOf("저장해 줘", "등록해", "이대로 완료").forEach { assertTrue(it, VoiceWords.isSave(it)) }
        assertFalse(VoiceWords.isSave("10월 20일"))
    }
}
