package com.mist.medicalmate.card.ui

/**
 * 기록 탭의 상태. Figma 1j-1 `406:2569`, 1j-2 `406:2646`.
 *
 * 카드와 진료 기록을 한 목록으로 본다. Figma 수정사항이 "브리핑 카드 = 브리핑 카드 +
 * 진료 기록 통합"으로 정리했고, 그래서 이 화면이 `card` 도메인에 있다.
 *
 * 월별로 묶어서 보여준다. 목록이 길어지면 언제 것인지가 먼저 필요하다.
 */
sealed interface RecordUiState {
    data object Loading : RecordUiState

    data object Failed : RecordUiState

    /**
     * [groups]가 비어 있으면 화면이 빈 상태(1j-2)가 된다.
     *
     * [selectedIds]가 null이 아니면 편집 중이다(1j-1-D). 빈 집합과 null을 나눠 쓴다.
     * 편집에 막 들어와 아무것도 고르지 않은 상태와 편집이 아닌 상태는 화면이 다르다.
     * 앞은 탭바 대신 삭제 버튼이 서고, 뒤는 탭바가 선다.
     */
    data class Content(
        val groups: List<RecordGroup>,
        val selectedIds: Set<String>? = null,
        val deleteRequested: Boolean = false,
    ) : RecordUiState {
        val editing: Boolean get() = selectedIds != null

        val selectedCount: Int get() = selectedIds?.size ?: 0
    }
}

/** 한 달 묶음. [count]를 따로 두지 않고 [items]의 크기를 쓴다. */
data class RecordGroup(val monthLabel: String, val items: List<RecordItem>)

/**
 * 목록의 한 줄.
 *
 * [status]에 따라 아래 줄이 달라진다. 작성 중이면 이어서 하라는 안내가 붙고, 진료 전이면
 * 준비한 것이, 진료 완료면 들은 것이 온다. 그 문구를 [detail]로 받는 이유는 상태마다
 * 다른 값을 화면이 조립하지 않게 하려는 것이다. 어떤 말을 담을지는 데이터가 정한다.
 *
 * [resumeLabel]은 작성 중인 카드에만 있다. 눌러서 문답으로 돌아가는 줄이고, 다른 상태에는
 * 이어서 할 것이 없다.
 */
data class RecordItem(
    val id: String,
    /**
     * 이 기록이 매달린 카드.
     *
     * 목록에서 지우면 서버가 지우는 것이 카드다. 기록만 지우는 API가 없다. 카드 없이 만든
     * 기록은 없지만 응답이 null을 줄 수 있어서 선택으로 둔다.
     */
    val cardId: String? = null,
    val title: String,
    val status: Status,
    val meta: String,
    val detail: String? = null,
    val resumeLabel: String? = null,
) {
    enum class Status { DRAFT, BEFORE_VISIT, CONFIRMED }
}
