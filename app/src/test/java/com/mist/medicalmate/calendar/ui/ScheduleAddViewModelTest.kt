package com.mist.medicalmate.calendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleAddViewModelTest {
    @Test
    fun `처음에는 카드 후보만 있고 아무것도 고르지 않았다`() {
        val state = ScheduleAddViewModel().uiState.value

        assertEquals(2, state.cards.size)
        assertEquals(0, state.pickedCardCount)
        assertNull(state.hospital)
        assertEquals(emptyList<ScheduleAddTodo>(), state.todos)
        assertEquals(ScheduleAddSheet.NONE, state.sheet)
    }

    @Test
    fun `날짜와 시간이 둘 다 있어야 저장할 수 있다`() {
        val viewModel = ScheduleAddViewModel()

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTimeConfirm(LocalTime.of(10, 30))
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `병원이 없어도 저장할 수 있다`() {
        val viewModel = ScheduleAddViewModel()

        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))
        viewModel.onTimeConfirm(LocalTime.of(10, 30))

        assertNull(viewModel.uiState.value.hospital)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `확인하면 시트가 닫히고 값이 남는다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.onSheetOpen(ScheduleAddSheet.DATE)

        assertEquals(ScheduleAddSheet.DATE, viewModel.uiState.value.sheet)

        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))

        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
        assertEquals(LocalDate.of(2026, 9, 26), viewModel.uiState.value.date)
    }

    @Test
    fun `끌어내려 닫으면 값이 그대로다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.onTimeConfirm(LocalTime.of(9, 0))
        viewModel.onSheetOpen(ScheduleAddSheet.TIME)

        viewModel.onSheetDismiss()

        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
        assertEquals(LocalTime.of(9, 0), viewModel.uiState.value.time)
    }

    @Test
    fun `빈 이름으로 돌아오면 병원을 지우지 않는다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.onHospitalPicked("서울OO병원 내과")

        viewModel.onHospitalPicked(null)
        viewModel.onHospitalPicked("")

        assertEquals("서울OO병원 내과", viewModel.uiState.value.hospital)
    }

    @Test
    fun `카드를 고르면 개수가 오른다`() {
        val viewModel = ScheduleAddViewModel()

        viewModel.onCardPickChange("card-1", true)
        assertEquals(1, viewModel.uiState.value.pickedCardCount)

        viewModel.onCardPickChange("card-2", true)
        assertEquals(2, viewModel.uiState.value.pickedCardCount)

        viewModel.onCardPickChange("card-1", false)
        assertEquals(1, viewModel.uiState.value.pickedCardCount)
    }

    @Test
    fun `할 일을 추가하면 적는 중인 빈 줄이 생긴다`() {
        val viewModel = ScheduleAddViewModel()

        viewModel.todo.onAddClick()

        val todo = viewModel.uiState.value.todos.single()
        assertEquals("", todo.label)
        assertTrue(todo.editing)
    }

    @Test
    fun `적고 마치면 줄이 남는다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onLabelChange(id, "복용 중인 약 챙기기")
        viewModel.todo.onEditDone(id)

        val todo = viewModel.uiState.value.todos.single()
        assertEquals("복용 중인 약 챙기기", todo.label)
        assertFalse(todo.editing)
    }

    @Test
    fun `한 글자도 없이 마치면 빈 줄이 사라진다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onEditDone(id)

        assertEquals(emptyList<ScheduleAddTodo>(), viewModel.uiState.value.todos)
    }

    @Test
    fun `공백만 적고 마쳐도 사라진다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onLabelChange(id, "   ")
        viewModel.todo.onEditDone(id)

        assertEquals(emptyList<ScheduleAddTodo>(), viewModel.uiState.value.todos)
    }

    @Test
    fun `지운 뒤 추가해도 id가 겹치지 않는다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.todo.onAddClick()
        val first = viewModel.uiState.value.todos.single().id
        viewModel.todo.onDeleteClick(first)

        viewModel.todo.onAddClick()

        assertEquals(1, viewModel.uiState.value.todos.size)
        assertTrue(first != viewModel.uiState.value.todos.single().id)
    }

    @Test
    fun `체크는 그 줄만 바꾼다`() {
        val viewModel = ScheduleAddViewModel()
        viewModel.todo.onAddClick()
        viewModel.todo.onAddClick()
        val (first, second) = viewModel.uiState.value.todos.map { it.id }

        viewModel.todo.onToggle(second, true)

        val todos = viewModel.uiState.value.todos.associateBy { it.id }
        assertFalse(todos.getValue(first).done)
        assertTrue(todos.getValue(second).done)
    }
}
