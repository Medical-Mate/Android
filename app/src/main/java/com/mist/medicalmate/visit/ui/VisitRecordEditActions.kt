package com.mist.medicalmate.visit.ui

/**
 * 편집 모드에서 기록 사본을 고치는 조작.
 *
 * [VisitRecordViewModel]에 얹지 않은 이유는 브리핑 카드의 [BriefCardEditActions][com.mist.medicalmate.card.ui.BriefCardEditActions]와
 * 같다. 화면 조작(편집 진입·취소·확인·저장·일정 체크·삭제 확인 셋)이 이미 여덟 가지라
 * 사본 조작까지 얹으면 한 클래스가 열 가지를 넘는다.
 *
 * 상태는 [VisitRecordUiState.Content] 안에 그대로 둔다. 사본을 따로 흘리면 기록과 사본이 두
 * 흐름으로 갈리고 화면이 둘을 합쳐 그려야 한다.
 *
 * 원문 메모를 고치는 조작은 없다. 문서가 지울 수 없는 것 목록에 그 메모를 넣었다.
 */
class VisitRecordEditActions(private val update: ((VisitRecordDraft) -> VisitRecordDraft) -> Unit) {
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

    /**
     * 항목 줄의 ×. 확인을 붙이지 않는다.
     *
     * 개체가 아니라 안의 항목이고, 편집 모드를 벗어나기 전이면 `취소`가 실행 취소를 대신한다.
     * AI가 잘못 나눈 항목을 지우는 자리라 되돌릴 일이 잦다.
     */
    fun onItemDeleteClick(index: Int) {
        update { draft ->
            if (index !in draft.items.indices) {
                draft
            } else {
                draft.copy(items = draft.items.filterIndexed { at, _ -> at != index })
            }
        }
    }
}
