package com.mist.medicalmate.card.ui

/**
 * 브리핑 카드 전체의 상태. Figma 1j-4 `1122:4830`.
 *
 * 홈의 "전체 보기"에서 들어온다. 기록 탭(1j-1)이 카드와 진료 기록을 함께 보여주는 것과
 * 달리 여기는 카드만 모은다. 줄에 적히는 보조 문구도 진료가 아니라 카드가 담은 것이다.
 *
 * 줄과 묶음은 [RecordGroup]·[RecordItem]을 그대로 쓴다. 두 화면의 줄이 제목·배지·메타·보조
 * 한 줄로 같고 상태 배지도 같은 셋이다. 타입을 따로 만들면 같은 모양이 두 벌이 된다.
 *
 * [selectedIds]가 null이 아니면 편집 중이다(1j-4-D). 빈 집합과 null을 나눠 쓰는 이유는
 * 1j-1과 같다. 편집에 막 들어온 상태와 편집이 아닌 상태는 화면이 다르다.
 */
sealed interface BriefCardListUiState {
    data object Loading : BriefCardListUiState

    data object Failed : BriefCardListUiState

    data class Content(
        val groups: List<RecordGroup>,
        val selectedIds: Set<String>? = null,
        val deleteRequested: Boolean = false,
    ) : BriefCardListUiState {
        val editing: Boolean get() = selectedIds != null

        val selectedCount: Int get() = selectedIds?.size ?: 0
    }
}
