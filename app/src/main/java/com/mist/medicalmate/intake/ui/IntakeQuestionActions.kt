package com.mist.medicalmate.intake.ui

/**
 * 추천 질문 고르기(1c-D~D4) 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 [BodyMapActions]와 같다. 그 클래스가 인체도 · 강도 · 문답
 * 세 단계를 한 번에 들고 있어서, 단계마다 조작을 더하면 한 클래스가 너무 많은 일을 한다.
 *
 * 상태를 갖지 않는다. [update]로 받은 창구를 통해 상태만 고친다. 창구 모양은 같은 패키지의
 * [IntakeSessionActions]·[BodyMapActions]와 맞췄다.
 */
class IntakeQuestionActions(private val update: ((IntakeUiState) -> IntakeUiState) -> Unit) {
    /**
     * 줄을 눌러 체크를 바꾼다. 카드에 담을 수 있는 수(#264)가 찼으면 새로 체크하지 않는다 — 푸는 것은
     * 된다.
     */
    fun onToggle(index: Int) {
        update { state ->
            val row = state.questionRows.getOrNull(index)
            if (row == null || (!row.selected && state.questionsFull)) {
                state
            } else {
                val rows = state.questionRows.toMutableList()
                rows[index] = row.copy(selected = !row.selected)
                state.copy(questionRows = rows)
            }
        }
    }

    /** 빈 칸("질문 직접 추가")을 눌렀다. 하단이 입력칸으로 바뀐다(1c-D2). 다 찼으면 빈 칸이 없다. */
    fun onAddClick() {
        update { state -> if (state.questionsFull) state else state.copy(writingQuestion = true, draft = "") }
    }

    /** 적던 것을 버리고 다음 버튼으로 돌아간다. */
    fun onCancel() {
        update { it.copy(writingQuestion = false, draft = "") }
    }

    /**
     * 적은 질문을 줄로 붙인다(1c-D4). 직접 적은 것은 담으려고 적은 것이라 체크된 채 붙는다. 붙일 수 없는
     * 상태면 아무 일도 하지 않는다.
     */
    fun onAdd() {
        update { state ->
            if (!state.canAddQuestion) {
                state
            } else {
                state.copy(
                    questionRows =
                    state.questionRows + IntakeQuestion(state.draft.trim(), selected = true, custom = true),
                    writingQuestion = false,
                    draft = "",
                )
            }
        }
    }
}
