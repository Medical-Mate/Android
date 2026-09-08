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

        assertEquals(setOf(4, 12, 26), state.markedDays)
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
