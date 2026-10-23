package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** 캘린더에서 버튼과 같게 맞춘 말(#336). */
class CalendarPartialVoiceTest {
    private val day = LocalDate.of(2026, 10, 20)

    private val dayState =
        CalendarDayUiState(
            date = day,
            schedule = null,
            card = null,
            todos = listOf(DayTodo("1", "약 챙기기", false), DayTodo("2", "신분증", false)),
        )

    @Test
    fun `일자 화면의 할 일을 체크하고 푼다`() {
        val toggled = mutableListOf<Pair<String, Boolean>>()

        dayState.todoVoice("약 챙기기 할 일 했어") { id, done -> toggled += id to done }
        dayState.todoVoice("두 번째 할 일 완료 취소") { id, done -> toggled += id to done }

        assertEquals(listOf("1" to true, "2" to false), toggled)
    }

    @Test
    fun `일자 화면도 되물은 할 일 안에서 번호를 센다`() {
        val state =
            dayState.copy(
                todos = listOf(
                    DayTodo("1", "약 챙기기", false),
                    DayTodo("2", "신분증", false),
                    DayTodo("3", "약 봉투 챙기기", false),
                ),
            )
        val candidates = VoiceCandidates()
        val toggled = mutableListOf<String>()

        state.todoVoice("챙기기 할 일 했어", candidates) { id, _ -> toggled += id }
        state.todoVoice("두 번째", candidates) { id, _ -> toggled += id }

        assertEquals(listOf("3"), toggled)
    }

    @Test
    fun `일자 화면의 할 일은 더하거나 지우지 않는다`() {
        // 편집이라 말로 하지 않는다(#296, 사용자 결정).
        assertEquals(VoiceReply.Done(R.string.voice_by_hand), dayState.todoVoice("할 일 추가, 보험증") { _, _ -> })
        assertEquals(VoiceReply.Done(R.string.voice_by_hand), dayState.todoVoice("신분증 할 일 지워 줘") { _, _ -> })
        assertNull(dayState.todoVoice("카드 보여 줘") { _, _ -> })
    }

    @Test
    fun `편집 중에는 말로 체크하지 않는다`() {
        val editing = dayState.copy(todoDraft = dayState.todos)

        assertEquals(VoiceReply.Done(R.string.voice_by_hand), editing.todoVoice("첫 번째 할 일 완료") { _, _ -> })
    }

    @Test
    fun `다음 일정은 시간이 없을 때만 확정하러 간다`() {
        var confirmed = 0
        val next = DayNextEvent(chip = "재방문", title = "서울OO병원", on = day.plusDays(14))

        dayState.copy(nextEvent = next).confirmNext { confirmed++ }
        val timed = dayState.copy(nextEvent = next.copy(at = "오전 10:30")).confirmNext { confirmed++ }
        val none = dayState.confirmNext { confirmed++ }

        assertEquals(1, confirmed)
        assertEquals(VoiceReply.Done(R.string.voice_day_next_timed), timed)
        assertEquals(VoiceReply.Done(R.string.voice_day_no_next), none)
    }

    @Test
    fun `일자 화면의 확정과 일정 추가를 가린다`() {
        assertEquals(DayVoice.CONFIRM_NEXT, parseDayVoice("시간 정할래"))
        assertEquals(DayVoice.CONFIRM_NEXT, parseDayVoice("다음 일정 확정해 줘"))
        assertEquals(DayVoice.SCHEDULE_ADD, parseDayVoice("일정 추가해 줘"))
    }

    private val month =
        CalendarUiState(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 13),
            selected = day,
            schedules =
            listOf(
                CalendarSchedule(id = "11", title = "서울OO병원", time = null, detail = "", dday = 7),
                CalendarSchedule(id = "12", title = "고려튼튼", time = null, detail = "", dday = 7),
            ),
        )

    private class Moves {
        val calls = mutableListOf<String>()
        val routes =
            CalendarMonthVoiceRoutes(
                onCardOpen = { calls += "card:$it" },
                onDayOpen = { date, id -> calls += "day:$date:$id" },
                onAdd = { date, card -> calls += "add:$date:$card" },
                dismissSheet = { calls += "dismiss" },
            )
    }

    @Test
    fun `캘린더의 일정 추가는 고른 날을 들고 간다`() {
        val moves = Moves()

        month.monthVoice("일정 추가해 줘", null, moves.routes)

        assertEquals(listOf("add:$day:null"), moves.calls)
    }

    @Test
    fun `날짜를 함께 말한 일정 추가는 전역 명령이 받는다`() {
        val spoken = VoiceDate.Day(LocalDate.of(2026, 10, 25))

        assertEquals(VoiceReply.NotMine, month.monthVoice("25일 일정 추가해 줘", spoken, Moves().routes))
    }

    @Test
    fun `고른 날의 두 번째 일정을 연다`() {
        val moves = Moves()

        month.monthVoice("두 번째 일정 열어 줘", null, moves.routes)

        assertEquals(listOf("day:$day:12"), moves.calls)
    }

    @Test
    fun `카드 시트가 떠 있으면 카드 열기와 일정 잡기를 받는다`() {
        val sheet = month.copy(cardSheet = DayCard(id = "7", title = "복부 통증"))
        val open = Moves()
        val schedule = Moves()

        sheet.monthVoice("카드 열어 줘", null, open.routes)
        sheet.monthVoice("일정 잡아 줘", null, schedule.routes)

        assertEquals(listOf("dismiss", "card:7"), open.calls)
        assertEquals(listOf("dismiss", "add:$day:7"), schedule.calls)
    }
}
