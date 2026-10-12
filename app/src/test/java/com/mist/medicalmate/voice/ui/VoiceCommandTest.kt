package com.mist.medicalmate.voice.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {
    @Test
    fun `탭 셋으로 간다`() {
        assertEquals(VoiceCommand.HOME, parseVoiceCommand("홈으로 가줘"))
        assertEquals(VoiceCommand.CALENDAR, parseVoiceCommand("캘린더 열어줘"))
        assertEquals(VoiceCommand.CALENDAR, parseVoiceCommand("달력 보여줘"))
        assertEquals(VoiceCommand.RECORD, parseVoiceCommand("기록 보여줘"))
    }

    @Test
    fun `띄어쓰기와 문장부호는 가리지 않는다`() {
        // 받아쓰기가 같은 말을 그때그때 다르게 띄어 적는다.
        assertEquals(VoiceCommand.SCHEDULE_ADD, parseVoiceCommand("일정 추가"))
        assertEquals(VoiceCommand.SCHEDULE_ADD, parseVoiceCommand("일정추가."))
        assertEquals(VoiceCommand.PROFILE, parseVoiceCommand("내 정보!"))
    }

    @Test
    fun `겹치는 말은 좁은 쪽이 이긴다`() {
        // "일정"만 보면 캘린더지만 "일정 추가"는 일정 추가다. "기록"만 보면 기록 탭이지만
        // "증상 기록"은 증상 정리다.
        assertEquals(VoiceCommand.SCHEDULE_ADD, parseVoiceCommand("진료 일정 추가해줘"))
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("증상 기록할래"))
        assertEquals(VoiceCommand.BRIEF_CARDS, parseVoiceCommand("브리핑 카드 기록 보여줘"))
    }

    @Test
    fun `아프다고 하면 증상 정리를 연다`() {
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("배가 아파"))
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("아픈 데 정리하고 싶어"))
    }

    @Test
    fun `뒤로 간다`() {
        assertEquals(VoiceCommand.BACK, parseVoiceCommand("뒤로 가"))
        assertEquals(VoiceCommand.BACK, parseVoiceCommand("이전 화면"))
    }

    @Test
    fun `모르는 말은 명령이 아니다`() {
        assertNull(parseVoiceCommand("오늘 날씨 어때"))
        assertNull(parseVoiceCommand(""))
        assertNull(parseVoiceCommand("  . "))
    }
}
