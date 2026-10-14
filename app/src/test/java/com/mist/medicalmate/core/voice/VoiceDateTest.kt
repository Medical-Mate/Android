package com.mist.medicalmate.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** 말로 날이나 달을 가리킨다(#286 · #288). 2026-10-13은 화요일이다. */
class VoiceDateTest {
    @Test
    fun `날짜와 가까운 날을 읽는다`() {
        assertEquals(day(2026, 10, 20), parse("10월 20일 일정 보여줘"))
        assertEquals(day(2026, 10, 25), parse("25일"))
        assertEquals(day(2026, 10, 14), parse("내일 일정 뭐야"))
        assertEquals(day(2026, 10, 12), parse("어제"))
        assertEquals(day(2026, 11, 5), parse("다음 달 5일"))
    }

    @Test
    fun `요일은 주를 말하면 그 주이고 아니면 처음 오는 그 요일이다`() {
        assertEquals(day(2026, 10, 20), parse("다음 주 화요일 일정"))
        assertEquals(day(2026, 10, 16), parse("금요일"))
        assertEquals(day(2026, 10, 12), parse("이번 주 월요일"))
    }

    @Test
    fun `달만 말하면 그 달로 넘긴다`() {
        assertEquals(VoiceDate.Month(YearMonth.of(2026, 11)), parse("다음 달"))
        assertEquals(VoiceDate.Month(YearMonth.of(2026, 12)), parse("12월 보여 줘"))
        assertEquals(VoiceDate.Month(YearMonth.of(2026, 9)), parse("지난달"))
    }

    @Test
    fun `날짜가 없거나 없는 날이면 읽지 않는다`() {
        assertNull(parse("캘린더 열어"))
        assertNull(parse("2월 30일"))
    }

    private fun parse(text: String) = parseVoiceDate(text, LocalDate.of(2026, 10, 13), YearMonth.of(2026, 10))

    private fun day(year: Int, month: Int, day: Int) = VoiceDate.Day(LocalDate.of(year, month, day))
}
