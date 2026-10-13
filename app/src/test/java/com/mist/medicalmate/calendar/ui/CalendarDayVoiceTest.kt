package com.mist.medicalmate.calendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 일자 화면에서 말로 하는 일(#288). */
class CalendarDayVoiceTest {
    @Test
    fun `기록을 남기거나 보거나 카드를 연다`() {
        assertEquals(DayVoice.RECORD_ADD, parseDayVoice("진료 후 기록할래"))
        assertEquals(DayVoice.RECORD_ADD, parseDayVoice("기록 남길게"))
        assertEquals(DayVoice.RECORD_OPEN, parseDayVoice("기록 보여 줘"))
        assertEquals(DayVoice.CARD, parseDayVoice("카드 보여 줘"))
        assertNull(parseDayVoice("오늘 날씨 어때"))
    }
}
