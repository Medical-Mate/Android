package com.mist.medicalmate.calendar.ui

/**
 * 일정 추가 화면의 할 일 목록 조작.
 *
 * [ScheduleAddViewModel]에 얹지 않은 이유는 [BriefCardEditActions][com.mist.medicalmate.card.ui.BriefCardEditActions]와
 * 같다. 할 일 조작만 다섯 가지고 병원·날짜·시간·카드·시트가 이미 여섯이다.
 *
 * 상태를 갖지 않는다. [update]로 받은 창구를 통해 목록만 고친다.
 *
 * 새 항목의 id는 [nextId]가 만든다. 시간으로 만들면 같은 밀리초에 두 번 눌렀을 때 겹치고,
 * 목록 크기로 만들면 지운 뒤 추가할 때 살아 있는 항목과 부딪친다.
 */
class ScheduleAddTodoActions(
    private val nextId: () -> String,
    private val update: ((List<ScheduleAddTodo>) -> List<ScheduleAddTodo>) -> Unit,
) {
    /** 목록 끝에 빈 할 일을 하나 만들고 그 자리에서 받는다. */
    fun onAddClick() {
        update { todos -> todos + ScheduleAddTodo(id = nextId(), label = "", editing = true) }
    }

    fun onLabelChange(id: String, label: String) {
        update { todos -> todos.map { if (it.id == id) it.copy(label = label) else it } }
    }

    /** 적기를 마쳤다. 한 글자도 없으면 빈 줄이 남지 않게 지운다. */
    fun onEditDone(id: String) {
        update { todos ->
            todos.mapNotNull { todo ->
                when {
                    todo.id != id -> todo
                    todo.label.isBlank() -> null
                    else -> todo.copy(editing = false)
                }
            }
        }
    }

    fun onToggle(id: String, done: Boolean) {
        update { todos -> todos.map { if (it.id == id) it.copy(done = done) else it } }
    }

    /** 행 오른쪽 ×. 확인을 붙이지 않는다. 개체가 아니라 안의 항목이다. */
    fun onDeleteClick(id: String) {
        update { todos -> todos.filterNot { it.id == id } }
    }
}
