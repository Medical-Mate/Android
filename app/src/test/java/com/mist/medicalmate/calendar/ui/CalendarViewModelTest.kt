package com.mist.medicalmate.calendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModelTest {
    @Test
    fun `처음에는 진료 예정일이 있는 달을 열고 그 날을 고른다`() {
        val state = CalendarViewModel().uiState.value

        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(LocalDate.of(2026, 9, 12), state.selected)
        assertTrue(state.schedules.isNotEmpty())
    }

    @Test
    fun `일정이 있는 날에 점이 찍힌다`() {
        val state = CalendarViewModel().uiState.value

        assertEquals(setOf(2, 4), state.recordDays)
        assertEquals(setOf(12, 26), state.plannedDays)
    }

    @Test
    fun `달을 넘기면 고른 날은 그대로 둔다`() {
        val viewModel = CalendarViewModel()
        val selected = viewModel.uiState.value.selected

        viewModel.onNextMonth()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.month)
        assertEquals(selected, viewModel.uiState.value.selected)

        viewModel.onPreviousMonth()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.month)
    }

    @Test
    fun `일정 없는 날을 고르면 목록이 빈다`() {
        val viewModel = CalendarViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertEquals(LocalDate.of(2026, 9, 13), viewModel.uiState.value.selected)
        assertEquals(emptyList<CalendarSchedule>(), viewModel.uiState.value.schedules)
    }

    @Test
    fun `카드만 쓴 날을 고르면 시트가 뜬다`() {
        val viewModel = CalendarViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        val state = viewModel.uiState.value
        assertEquals(emptyList<CalendarSchedule>(), state.schedules)
        assertEquals("card-1", state.cardSheet?.id)
    }

    @Test
    fun `일정이 있는 날은 시트를 띄우지 않는다`() {
        val viewModel = CalendarViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 12))

        assertTrue(viewModel.uiState.value.schedules.isNotEmpty())
        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `카드도 일정도 없는 날은 시트를 띄우지 않는다`() {
        val viewModel = CalendarViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `시트를 닫으면 고른 날은 그대로 둔다`() {
        val viewModel = CalendarViewModel()
        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        viewModel.onCardSheetDismiss()

        assertNull(viewModel.uiState.value.cardSheet)
        assertEquals(LocalDate.of(2026, 9, 4), viewModel.uiState.value.selected)
    }

    @Test
    fun `다른 날로 옮기면 앞서 뜬 시트가 닫힌다`() {
        val viewModel = CalendarViewModel()
        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `다녀온 날은 할 일이 없고 기록과 다음 일정이 붙는다`() {
        // 진료일이 지난 시점을 만들 수 없어(오늘은 기기가 준다) 픽스처 상태로 확인한다.
        val day = previewCalendarDayVisitedState

        assertTrue(day.visited)
        assertEquals(emptyList<DayTodo>(), day.todos)
        assertEquals("진료 후 기록", day.record?.title)
        assertEquals("9월 26일 (토)", day.nextEvent?.chip)
        assertNull(day.nextEvent?.at)
    }

    @Test
    fun `다녀온 날의 카드에는 배지가 없다`() {
        assertNull(previewCalendarDayVisitedState.card?.status)
        assertEquals("진료 전", previewCalendarDayState.card?.status)
    }

    @Test
    fun `다음 일정이 확정되면 칩과 시간이 바뀐다`() {
        val next = previewCalendarDayConfirmedState.nextEvent

        assertEquals("D-14", next?.chip)
        assertEquals("9월 26일 (토) 오전 10:30", next?.at)
    }

    @Test
    fun `진료 예정일의 일자 화면에는 일정과 카드와 할 일이 있다`() {
        val day = CalendarViewModel().dayState(LocalDate.of(2026, 9, 12))

        assertNotNull(day.schedule)
        assertNotNull(day.card)
        assertEquals(3, day.todos.size)
        assertNull(day.record)
    }

    @Test
    fun `다른 날의 일자 화면은 비어 있다`() {
        val day = CalendarViewModel().dayState(LocalDate.of(2026, 9, 13))

        assertNull(day.schedule)
        assertNull(day.card)
        assertEquals(emptyList<DayTodo>(), day.todos)
    }

    @Test
    fun `D-day는 오늘을 기준으로 센다`() {
        val viewModel = CalendarViewModel()
        val today = viewModel.uiState.value.today
        val schedule = viewModel.uiState.value.schedules.first()

        val expected = LocalDate.of(2026, 9, 12).toEpochDay() - today.toEpochDay()
        assertEquals(expected, schedule.dday)
    }
}
