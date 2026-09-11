package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * 캘린더 일자 화면 상태 보유자.
 *
 * 월 화면과 나눈다. 일자 화면이 편집 상태를 들면서 한 클래스가 두 화면의 조작을 다 갖게
 * 됐고, 픽스처는 [CalendarFixtures][VisitDate]에서 함께 본다.
 *
 * 편집은 문서의 CRUD 규칙을 따른다. 편집 중에는 사본을 고치고 취소하면 버린다. 확인은
 * 사본을 본값으로 옮긴다. 서버가 붙으면 그 자리에서 저장 호출이 나간다.
 */
@HiltViewModel
class CalendarDayViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<CalendarDayUiState?>(null)
    val uiState: StateFlow<CalendarDayUiState?> = mutableUiState.asStateFlow()

    fun load(date: LocalDate) {
        mutableUiState.value = dayState(date)
    }

    /** 할 일 체크. 편집 중이면 사본을 고친다. */
    fun onTodoToggle(id: String, done: Boolean) {
        mutableUiState.update { state ->
            state?.copy(todos = state.todos.map { if (it.id == id) it.copy(done = done) else it })
        }
    }

    fun onEditStart() {
        mutableUiState.update { it?.copy(todoDraft = it.todos) }
    }

    /** 편집을 버린다. 사본을 지우면 본값이 그대로 남는다. */
    fun onEditCancel() {
        mutableUiState.update { it?.copy(todoDraft = null, deleteRequested = false) }
    }

    /** 편집을 마친다. 사본이 본값이 된다. */
    fun onEditDone() {
        mutableUiState.update { state ->
            state?.copy(todos = state.todoDraft ?: state.todos, todoDraft = null)
        }
    }

    /**
     * 할 일 줄의 ×.
     *
     * 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고, 편집을 벗어나기 전이면 취소가
     * 실행 취소를 대신한다.
     */
    fun onTodoDelete(id: String) {
        mutableUiState.update { state ->
            state?.copy(todoDraft = state.todoDraft?.filterNot { it.id == id })
        }
    }

    fun onScheduleDeleteClick() {
        mutableUiState.update { it?.copy(deleteRequested = true) }
    }

    fun onScheduleDeleteDismiss() {
        mutableUiState.update { it?.copy(deleteRequested = false) }
    }
}
