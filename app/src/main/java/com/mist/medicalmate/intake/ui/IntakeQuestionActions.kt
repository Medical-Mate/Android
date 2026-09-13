package com.mist.medicalmate.intake.ui

/**
 * 4단계(1i) 질문 목록 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 [BodyMapActions]와 같다. 그 클래스가 인체도 · 문답 ·
 * 강도 · 질문 네 단계를 한 번에 들고 있어서, 단계마다 조작을 더하면 한 클래스가 너무 많은
 * 일을 한다.
 *
 * 상태를 갖지 않는다. [update]로 받은 창구를 통해 상태만 고친다.
 */
class IntakeQuestionActions(private val update: (IntakeUiState.() -> IntakeUiState) -> Unit) {
    fun onDraftChange(draft: String) {
        update { copy(questionDraft = draft) }
    }

    /** 적어 둔 것을 목록에 올린다. 올릴 수 없는 상태면 아무 일도 하지 않는다. */
    fun onAdd() {
        update {
            if (!canAddQuestion) this else copy(questions = questions + questionDraft.trim(), questionDraft = "")
        }
    }

    fun onRemove(index: Int) {
        update {
            if (index !in questions.indices) this else copy(questions = questions.filterIndexed { i, _ -> i != index })
        }
    }
}
