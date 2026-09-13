package com.mist.medicalmate.calendar.ui

/**
 * 일자 화면의 진료 전 할 일 조작.
 *
 * [CalendarDayViewModel]에 얹지 않은 이유는 [ScheduleAddTodoActions]와 같다. 할 일 조작만
 * 다섯 가지이고 일정 삭제와 불러오기가 이미 그만큼 있다.
 *
 * 상태를 갖지 않는다. [update]로 받은 창구로 상태를 고치고, 서버에 남겨야 할 때 [save]를
 * 부른다.
 *
 * **편집 중에는 저장하지 않는다.** 취소가 실행 취소를 대신하는데 이미 보냈으면 되돌릴 것이
 * 없다. 편집을 마칠 때 한 번 보낸다.
 */
class CalendarDayTodoActions(
    private val editing: () -> Boolean,
    private val update: (CalendarDayUiState.() -> CalendarDayUiState) -> Unit,
    private val save: () -> Unit,
) {
    /** 할 일 체크. 편집 중이면 사본을 고친다. */
    fun onToggle(id: String, done: Boolean) {
        val inEdit = editing()
        update {
            if (inEdit) {
                copy(todoDraft = todoDraft?.map { if (it.id == id) it.copy(done = done) else it })
            } else {
                copy(todos = todos.map { if (it.id == id) it.copy(done = done) else it })
            }
        }
        if (!inEdit) save()
    }

    fun onEditStart() {
        update { copy(todoDraft = todos) }
    }

    /** 편집을 버린다. 사본을 지우면 본값이 그대로 남는다. */
    fun onEditCancel() {
        update { copy(todoDraft = null, deleteRequested = false) }
    }

    /** 편집을 마친다. 사본이 본값이 되고 서버로 나간다. */
    fun onEditDone() {
        update { copy(todos = todoDraft ?: todos, todoDraft = null) }
        save()
    }

    /**
     * 할 일 줄의 ×.
     *
     * 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고, 편집을 벗어나기 전이면 취소가
     * 실행 취소를 대신한다.
     */
    fun onDelete(id: String) {
        update { copy(todoDraft = todoDraft?.filterNot { it.id == id }) }
    }
}
