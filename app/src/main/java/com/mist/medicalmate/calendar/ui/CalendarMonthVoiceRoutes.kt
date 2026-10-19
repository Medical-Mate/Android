package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceWords
import java.time.LocalDate
import java.time.YearMonth

/** 캘린더 월 화면이 말로 옮기는 길(#336). 손으로 누르는 버튼과 같은 것을 넘긴다. */
internal class CalendarMonthVoiceRoutes(
    val onCardOpen: (String) -> Unit,
    val onDayOpen: (LocalDate, Long?) -> Unit,
    val onAdd: (LocalDate?, String?) -> Unit,
    val dismissSheet: () -> Unit,
)

/**
 * 날짜 말보다 먼저 보는 말(#336). 이 화면의 일이 아니면 null이다.
 *
 * - 카드만 있는 날 시트가 떠 있으면 "카드 열어 줘"는 그 카드를, "일정 잡아 줘" · "일정 보여 줘"는 시트의 일정
 *   버튼과 같다(그 카드로 잡은 일정이 있으면 그 날, 없으면 그 카드로 일정 추가). 전에는 카드 목록이 열렸다.
 * - "일정 추가해 줘"는 + 버튼처럼 고른 날을 들고 간다. 날짜를 함께 말했으면 전역 명령이 그 말을 들고 연다.
 * - "두 번째 일정 열어 줘"는 고른 날의 그 일정을 연다. 전에는 날짜로만 열어 하루에 둘이면 첫 건만 열렸다.
 */
internal fun CalendarUiState.monthVoice(
    text: String,
    spoken: VoiceDate?,
    routes: CalendarMonthVoiceRoutes,
): VoiceReply? {
    val heard = VoiceWords.compact(text)
    val sheet = cardSheet
    val day = parseDayVoice(text)
    val ordinal = VoiceWords.ordinal(text)
    if (sheet != null) sheetVoice(sheet, day, heard, routes)?.let { return it }
    return when {
        day == DayVoice.SCHEDULE_ADD && spoken == null -> {
            routes.onAdd(selected.takeIf { YearMonth.from(it) == month }, null)
            VoiceReply.Done(R.string.voice_calendar_schedule_add)
        }

        day == DayVoice.SCHEDULE_ADD -> VoiceReply.NotMine
        spoken == null && ordinal != null && heard.contains(SCHEDULE_WORD) && schedules.isNotEmpty() -> {
            val schedule = schedules.getOrNull(if (ordinal == VoiceWords.LAST) schedules.lastIndex else ordinal)
            if (schedule == null) {
                VoiceReply.Done(R.string.voice_calendar_no_such_schedule)
            } else {
                routes.onDayOpen(selected, schedule.id.toLongOrNull())
                VoiceReply.Done(R.string.voice_calendar_day, listOf(selected.format(VOICE_DATE)))
            }
        }

        else -> null
    }
}

/** 카드만 있는 날 시트의 버튼 둘. 시트의 말이 아니면 null이다. */
private fun CalendarUiState.sheetVoice(
    sheet: DayCard,
    day: DayVoice?,
    heard: String,
    routes: CalendarMonthVoiceRoutes,
): VoiceReply? = when {
    day == DayVoice.CARD -> {
        routes.dismissSheet()
        routes.onCardOpen(sheet.id)
        VoiceReply.Done(R.string.voice_card_opened, listOf(sheet.title))
    }

    heard.contains(SCHEDULE_WORD) -> sheetSchedule(sheet, routes)
    else -> null
}

/** 시트의 일정 버튼. 그 카드로 잡은 일정이 있으면 보러 가고 없으면 잡는다. */
private fun CalendarUiState.sheetSchedule(sheet: DayCard, routes: CalendarMonthVoiceRoutes): VoiceReply {
    routes.dismissSheet()
    val on = sheet.scheduledOn
    return if (on != null) {
        routes.onDayOpen(on, null)
        VoiceReply.Done(R.string.voice_calendar_day, listOf(on.format(VOICE_DATE)))
    } else {
        routes.onAdd(selected, sheet.id)
        VoiceReply.Done(R.string.voice_calendar_schedule_add)
    }
}

private const val SCHEDULE_WORD = "일정"
