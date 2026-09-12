package com.mist.medicalmate.intake.ui

/**
 * 인체도 단계의 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 한 클래스가 두 화면의 조작을 다 들고 있게 되기
 * 때문이다. 인체도만 다섯 가지고, 문답·통증 강도·질문의 조작이 이미 열 가지다.
 *
 * 상태는 [IntakeUiState] 안에 그대로 둔다. 부위는 문답의 첫 마디와 통증 강도의 물음이
 * 함께 읽는 값이라 따로 흘리면 두 흐름을 합쳐야 한다. 그래서 이 클래스는 상태를 갖지
 * 않고 [update]로 받은 창구를 통해 [BodyMapUiState]만 고친다.
 */
class BodyMapActions(private val update: ((BodyMapUiState) -> BodyMapUiState) -> Unit) {
    /** 앞면과 뒷면을 바꾼다. 고른 부위는 지우지 않는다. 반대 면을 보다가 돌아올 수 있다. */
    fun onViewChange(view: BodyMapView) {
        update { it.copy(view = view) }
    }

    /**
     * 인체도의 점을 눌렀다.
     *
     * 같은 캔버스가 앵커와 구역 둘 다에 쓰여서 눌린 id로 어느 쪽인지 가른다. 앵커면
     * 확대해 들어가고(고르는 것이 아니다), 구역이면 고른다.
     *
     * 전신·피부는 구역이 없는 앵커라 확대할 것이 없다. 그 둘만 앵커인데도 바로 골라진다.
     */
    fun onDotClick(dotId: String) {
        val (id, side) = parseDotId(dotId)
        if (!id.startsWith(ANCHOR_ID_PREFIX)) {
            update { state ->
                val anchorId = state.focus?.anchorId ?: return@update state
                state.copy(selection = BodyMapSelection(anchorId, id, side))
            }
            return
        }
        update { state ->
            if (bodyMapZonesOf(id).isEmpty()) {
                state.copy(focus = null, selection = BodyMapSelection(id, side = side))
            } else {
                state.copy(focus = BodyMapSelection(id, side = side))
            }
        }
    }

    /** 확대한 앵커에서 부위를 골랐다. 목록에서 고르는 길이 쓴다. */
    fun onPartSelect(selection: BodyMapSelection) {
        update { it.copy(selection = selection) }
    }

    /** 구역이 없는 앵커를 골랐다. 전신·피부 둘이다. */
    fun onSideAnchorSelect(anchorId: String) {
        update { it.copy(focus = null, selection = BodyMapSelection(anchorId)) }
    }

    /**
     * 확대할 앵커를 짚었다. 목록에서 앵커 줄을 누르는 길이 쓴다.
     *
     * 검색을 함께 닫는다. 검색 결과에서 앵커를 누르면 그 앵커의 구역 목록으로 가야 하는데,
     * 검색어가 남아 있으면 화면이 계속 결과를 그려서 누른 것이 아무 일도 안 한 것처럼 보인다.
     * 목록에서 누른 경우에는 검색어가 이미 비어 있어 달라지는 것이 없다.
     */
    fun onAnchorFocus(selection: BodyMapSelection) {
        update { it.copy(focus = selection, search = "", searchResults = emptyList()) }
    }

    /**
     * 확대를 닫고 앵커 화면으로 돌아간다.
     *
     * 고른 부위는 남는다. 확대를 닫는 것과 고른 것을 버리는 것은 다른 일이고, 닫을
     * 때마다 버리면 다른 부위를 둘러보려다 고른 것을 잃는다.
     */
    fun onFocusClear() {
        update { it.copy(focus = null) }
    }

    /** 인체도와 목록을 오간다. 고른 부위는 양쪽이 같은 값이라 그대로 둔다. */
    fun onListModeToggle() {
        update { it.copy(byList = !it.byList, search = "", searchResults = emptyList()) }
    }

    /**
     * 목록에서 부위를 찾는다.
     *
     * **0건이면 직전 결과를 남긴다.** 한글은 마지막 글자가 조합되는 동안 중간 상태가 되고
     * ("옆 → 옆ㄱ → 옆구") 그대로 그리면 한 글자마다 목록이 깜빡인다. 검색어를 비웠을 때만
     * 결과도 비운다. 그때는 못 찾은 것이 아니라 지운 것이다.
     */
    fun onSearchChange(query: String) {
        val found = searchBodyParts(query)
        update { state ->
            state.copy(
                search = query,
                searchResults = when {
                    query.isBlank() -> emptyList()
                    found.isEmpty() -> state.searchResults
                    else -> found
                },
            )
        }
    }

    private companion object {
        /** 앵커 id의 접두사. 구역은 `SUR:`이다. */
        const val ANCHOR_ID_PREFIX = "ANC:"
    }
}
