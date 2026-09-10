package com.mist.medicalmate.card.ui

/**
 * 편집 모드에서 카드 사본을 고치는 조작.
 *
 * [BriefCardViewModel]에 얹지 않은 이유는 한 클래스가 화면 조작과 사본 조작을 다 들고 있게
 * 되기 때문이다. 사본 조작만 여섯 가지고, 편집 진입·취소·확인·삭제 확인이 이미 다섯이다.
 *
 * 상태는 [BriefCardUiState.Content] 안에 그대로 둔다. 사본을 따로 흘리면 카드와 사본이 두
 * 흐름으로 갈리고, 화면은 둘을 합쳐 그려야 한다. 그래서 이 클래스는 상태를 갖지 않고
 * [update]로 받은 창구를 통해 [BriefCardDraft]만 고친다.
 *
 * 값이 빈 항목을 지우지 않는다. 편집 중에 글자를 다 지우는 것과 항목을 없애는 것은 다른
 * 일이고, 없애는 조작은 ×로 따로 있다.
 */
class BriefCardEditActions(private val update: ((BriefCardDraft) -> BriefCardDraft) -> Unit) {
    fun onItemValueChange(index: Int, value: String) {
        update { draft ->
            if (index !in draft.items.indices) {
                draft
            } else {
                val items = draft.items.mapIndexed { at, item ->
                    if (at == index) item.copy(value = value) else item
                }
                draft.copy(items = items)
            }
        }
    }

    /** 항목 줄의 ×. 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고 취소가 되돌린다. */
    fun onItemDeleteClick(index: Int) {
        update { draft ->
            if (index !in draft.items.indices) draft else draft.copy(items = draft.items.withoutAt(index))
        }
    }

    fun onQuestionChange(index: Int, value: String) {
        update { draft ->
            if (index !in draft.questions.indices) {
                draft
            } else {
                draft.copy(questions = draft.questions.mapIndexed { at, q -> if (at == index) value else q })
            }
        }
    }

    fun onQuestionDeleteClick(index: Int) {
        update { draft ->
            if (index !in draft.questions.indices) draft else draft.copy(questions = draft.questions.withoutAt(index))
        }
    }

    /**
     * `+ 질문 추가`. 입력 필드를 따로 띄우지 않고 목록 끝에 빈 질문을 하나 더한다.
     *
     * 문서의 항목 추가 규칙이다. 빈 항목이 생기고 적으면 그대로 항목이 된다. 번호는 목록
     * 순서가 정하므로 3개였으면 새 질문이 4번이 된다.
     */
    fun onQuestionAddClick() {
        update { draft -> draft.copy(questions = draft.questions + "") }
    }
}

/** [index]만 빼고 새 목록을 만든다. */
private fun <T> List<T>.withoutAt(index: Int): List<T> = filterIndexed { at, _ -> at != index }
