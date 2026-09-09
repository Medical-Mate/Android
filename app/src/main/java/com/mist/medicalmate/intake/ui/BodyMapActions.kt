package com.mist.medicalmate.intake.ui

/**
 * 인체도 단계의 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 한 클래스가 두 화면의 조작을 다 들고 있게 되기
 * 때문이다. 인체도만 여섯 가지고, 문답·통증 강도·질문의 조작이 이미 열 가지다.
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
     * 구역 단계로 들어가고, 구역이면 부위가 정해진다.
     */
    fun onDotClick(dotId: String) {
        val (id, side) = parseDotId(dotId)
        update { state ->
            val anchorId = state.selection?.anchorId
            val selection =
                if (anchorId == null || id.startsWith(ANCHOR_ID_PREFIX)) {
                    BodyMapSelection(anchorId = id, side = side)
                } else {
                    BodyMapSelection(anchorId = anchorId, zoneId = id, side = side)
                }
            state.copy(selection = selection)
        }
    }

    /** 전신·피부 칩. 구역이 없어서 누르면 바로 정해진다. */
    fun onSideAnchorClick(anchorId: String) {
        onPartSelect(BodyMapSelection(anchorId = anchorId))
    }

    /** 목록에서 골랐다. 인체도의 점과 같은 값이 들어온다. */
    fun onPartSelect(selection: BodyMapSelection) {
        update { it.copy(selection = selection) }
    }

    /** 앵커부터 다시. 구역 화면에서 큰 부위를 잘못 짚었을 때 쓴다. */
    fun onAnchorReset() {
        update { it.copy(selection = null) }
    }

    /** 인체도와 목록을 오간다. 고른 부위는 양쪽이 같은 값이라 그대로 둔다. */
    fun onListModeToggle() {
        update { it.copy(byList = !it.byList) }
    }

    private companion object {
        /** 앵커 id의 접두사. 구역은 `SUR:`이다. */
        const val ANCHOR_ID_PREFIX = "ANC:"
    }
}
