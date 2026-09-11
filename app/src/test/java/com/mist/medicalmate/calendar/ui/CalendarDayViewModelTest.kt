package com.mist.medicalmate.calendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDayViewModelTest {
    private fun loaded(): CalendarDayViewModel = CalendarDayViewModel().apply { load(VisitDate) }

    private fun CalendarDayViewModel.state(): CalendarDayUiState = uiState.value!!

    @Test
    fun `불러오기 전에는 상태가 없다`() {
        assertNull(CalendarDayViewModel().uiState.value)
    }

    @Test
    fun `처음에는 편집이 아니다`() {
        val state = loaded().state()

        assertFalse(state.editing)
        assertNull(state.todoDraft)
        assertEquals(3, state.shownTodos.size)
    }

    @Test
    fun `편집으로 들어가면 사본이 본값과 같다`() {
        val viewModel = loaded()

        viewModel.onEditStart()

        assertTrue(viewModel.state().editing)
        assertEquals(viewModel.state().todos, viewModel.state().todoDraft)
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `할 일을 지우면 바뀐 것이 있다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onTodoDelete("todo-2")

        assertTrue(viewModel.state().changed)
        assertEquals(2, viewModel.state().shownTodos.size)
        // 본값은 확인을 누르기 전까지 그대로다.
        assertEquals(3, viewModel.state().todos.size)
    }

    @Test
    fun `취소하면 지운 것이 돌아온다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onTodoDelete("todo-2")

        viewModel.onEditCancel()

        assertFalse(viewModel.state().editing)
        assertEquals(3, viewModel.state().shownTodos.size)
    }

    @Test
    fun `확인하면 지운 것이 본값이 된다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onTodoDelete("todo-2")

        viewModel.onEditDone()

        assertFalse(viewModel.state().editing)
        assertEquals(listOf("todo-1", "todo-3"), viewModel.state().todos.map { it.id })
    }

    @Test
    fun `체크는 바뀐 것으로 세지 않는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onTodoToggle("todo-2", true)

        // 체크는 편집 밖에서도 누를 수 있는 조작이라 편집으로 바꾼 것이 아니다.
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `편집이 아닐 때도 체크는 켜고 끈다`() {
        val viewModel = loaded()

        viewModel.onTodoToggle("todo-2", true)

        assertTrue(viewModel.state().todos.first { it.id == "todo-2" }.done)
    }

    @Test
    fun `일정 삭제는 확인을 묻는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onScheduleDeleteClick()
        assertTrue(viewModel.state().deleteRequested)

        viewModel.onScheduleDeleteDismiss()
        assertFalse(viewModel.state().deleteRequested)
    }

    @Test
    fun `취소하면 삭제 확인도 닫힌다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onScheduleDeleteClick()

        viewModel.onEditCancel()

        assertFalse(viewModel.state().deleteRequested)
        assertFalse(viewModel.state().editing)
    }

    @Test
    fun `다녀온 날은 할 일이 없어 편집해도 지울 줄이 없다`() {
        val viewModel = CalendarDayViewModel().apply { load(PastVisitDate) }

        viewModel.onEditStart()

        assertTrue(viewModel.state().editing)
        assertEquals(emptyList<DayTodo>(), viewModel.state().shownTodos)
    }
}
