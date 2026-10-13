package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.voice.VoiceWords
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** 캘린더에서 말로 가리킨 날이나 달(#286). */
internal sealed interface CalendarVoice {
    /** 그 날의 일자 화면(1r-2)을 연다. */
    data class Day(val date: LocalDate) : CalendarVoice

    /** 그 달로 넘긴다. */
    data class Month(val month: YearMonth) : CalendarVoice
}

/**
 * 말에서 날이나 달을 읽는다. 못 읽으면 null이다.
 *
 * - "오늘", "내일", "모레", "어제", "그저께"
 * - "화요일", "다음 주 화요일", "이번 주 금요일", "지난주 월요일". 주만 말하지 않았으면 오늘부터 처음 오는 그
 *   요일이다
 * - "10월 20일", "20일"(보고 있는 달), "다음 달 5일"
 * - "다음 달", "지난달", "이번 달", "12월"
 *
 * 받아쓰기가 이미 숫자로 적는 날짜를 읽는다. 예약 문자처럼 긴 글에서 고르는 것이 아니라 짧은 말이라 기기의
 * 추출기를 부르지 않는다. 해는 보고 있는 달의 해다.
 */
internal fun parseCalendarVoice(text: String, today: LocalDate, shown: YearMonth): CalendarVoice? {
    val heard = VoiceWords.compact(text)
    val relative = RELATIVE_DAYS.entries.firstOrNull { (word, _) -> heard.contains(word) }?.value
    val weekday = WEEKDAY.find(heard)
    val month = monthOf(heard, today, shown)
    val day = DAY.find(heard)?.groupValues?.get(1)?.toInt()
    return when {
        relative != null -> CalendarVoice.Day(today.plusDays(relative))
        weekday != null -> CalendarVoice.Day(weekdayOf(weekday, today))
        day != null -> (month ?: shown).dayOrNull(day)?.let(CalendarVoice::Day)
        month != null -> CalendarVoice.Month(month)
        else -> null
    }
}

/** 말한 달. 숫자로 말한 달은 보고 있는 해다. */
private fun monthOf(heard: String, today: LocalDate, shown: YearMonth): YearMonth? {
    val number = MONTH.find(heard)?.groupValues?.get(1)?.toInt()?.takeIf { it in 1..MONTHS }
    return when {
        number != null -> YearMonth.of(shown.year, number)
        heard.contains("다음달") -> YearMonth.from(today).plusMonths(1)
        LAST_MONTH.any { heard.contains(it) } -> YearMonth.from(today).minusMonths(1)
        heard.contains("이번달") -> YearMonth.from(today)
        else -> null
    }
}

/** "다음 주 화요일". 주를 말하지 않았으면 오늘부터 처음 오는 그 요일이다. */
private fun weekdayOf(match: MatchResult, today: LocalDate): LocalDate {
    val target = DayOfWeek.of(WEEKDAYS.indexOf(match.groupValues[2]) + 1)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return when (match.groupValues[1]) {
        "이번주" -> monday.with(TemporalAdjusters.nextOrSame(target))
        "다음주" -> monday.plusWeeks(1).with(TemporalAdjusters.nextOrSame(target))
        "지난주", "저번주" -> monday.minusWeeks(1).with(TemporalAdjusters.nextOrSame(target))
        else -> today.with(TemporalAdjusters.nextOrSame(target))
    }
}

private fun YearMonth.dayOrNull(day: Int): LocalDate? = if (day in 1..lengthOfMonth()) atDay(day) else null

private val RELATIVE_DAYS = linkedMapOf("그저께" to -2L, "그제" to -2L, "어제" to -1L, "오늘" to 0L, "내일" to 1L, "모레" to 2L)

private const val WEEKDAYS = "월화수목금토일"

private val WEEKDAY = Regex("""(이번주|다음주|지난주|저번주)?([월화수목금토일])요일""")

private val MONTH = Regex("""(\d{1,2})월(?!요일)""")

private val DAY = Regex("""(?<!\d)(\d{1,2})일(?!요일)""")

private val LAST_MONTH = listOf("지난달", "저번달")

private const val MONTHS = 12
