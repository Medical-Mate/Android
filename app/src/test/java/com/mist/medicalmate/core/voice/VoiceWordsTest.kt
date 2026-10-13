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
    fun `네를 여러 말로 한다`() {
        listOf("넵", "응 그렇게 해", "해 줘", "좋습니다", "오케이", "콜", "웅").forEach { assertTrue(it, VoiceWords.isYes(it)) }
    }

    @Test
    fun `아니요를 여러 말로 한다`() {
        listOf("노노", "하지 마", "음 그건 안 할래", "다시 생각해 볼게", "별로야").forEach { text ->
            assertTrue(text, VoiceWords.isNo(text))
            assertFalse(text, VoiceWords.isYes(text))
        }
    }

    @Test
    fun `몇 번째를 여러 말로 한다`() {
        assertEquals(0, VoiceWords.ordinal("첫째 거"))
        assertEquals(1, VoiceWords.ordinal("둘째"))
        assertEquals(0, VoiceWords.ordinal("맨 위에 거"))
        assertEquals(0, VoiceWords.ordinal("제일 처음"))
        assertEquals(VoiceWords.LAST, VoiceWords.ordinal("맨 아래 거"))
        assertEquals(VoiceWords.LAST, VoiceWords.ordinal("끝에 거"))
        assertEquals(1, VoiceWords.ordinal("2번째"))
    }

    @Test
    fun `몇 번째를 가리키는 말을 걷어낸다`() {
        assertEquals("할 일", VoiceWords.stripOrdinal("두 번째 할 일").trim())
        assertEquals("지워", VoiceWords.stripOrdinal("맨 위에 거 지워").trim())
    }

    @Test
    fun `저장을 여러 말로 한다`() {
        listOf("일정 잡아 줘", "이걸로 할게", "다 됐어 저장", "마무리해 줘", "확정할게").forEach { assertTrue(it, VoiceWords.isSave(it)) }
    }

    @Test
    fun `저장하라는 말을 알아듣는다`() {
        listOf("저장해 줘", "등록해", "이대로 완료").forEach { assertTrue(it, VoiceWords.isSave(it)) }
        assertFalse(VoiceWords.isSave("10월 20일"))
    }
}
