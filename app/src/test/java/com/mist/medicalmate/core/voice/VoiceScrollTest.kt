package com.mist.medicalmate.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 말로 화면을 굴리는 말(#284). */
class VoiceScrollTest {
    @Test
    fun `굴리라는 말을 방향으로 읽는다`() {
        assertEquals(VoiceScroll.DOWN, parseVoiceScroll("아래로"))
        assertEquals(VoiceScroll.DOWN, parseVoiceScroll("화면 좀 내려 줘"))
        assertEquals(VoiceScroll.DOWN, parseVoiceScroll("스크롤 내려"))
        assertEquals(VoiceScroll.UP, parseVoiceScroll("위로 올려 줘"))
        assertEquals(VoiceScroll.BOTTOM, parseVoiceScroll("맨 아래로"))
        assertEquals(VoiceScroll.BOTTOM, parseVoiceScroll("끝까지 내려 줘"))
        assertEquals(VoiceScroll.TOP, parseVoiceScroll("맨 위로 가 줘"))
    }

    @Test
    fun `목록의 마지막이나 문답의 답은 굴리라는 말이 아니다`() {
        // 병원 찾기에서 "맨 아래 거"는 마지막 줄을 고르는 말이다.
        assertNull(parseVoiceScroll("맨 아래 거"))
        assertNull(parseVoiceScroll("배 위로 아파요"))
        assertNull(parseVoiceScroll("위팔이 아파요"))
        assertNull(parseVoiceScroll("아래로 내려갈수록 더 아프고 저려요"))
    }
}
