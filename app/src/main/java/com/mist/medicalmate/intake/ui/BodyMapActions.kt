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
     * 인체도의 점이나 목록의 줄을 눌렀다.
     *
     * 같은 캔버스가 앵커와 구역 둘 다에 쓰여서 눌린 id로 어느 쪽인지 가른다. 앵커면
     * 확대해 들어가고(고르는 것이 아니다), 구역이면 고르거나 뺀다.
     *
     * 전신·피부는 구역이 없는 앵커라 확대할 것이 없다. 그 둘만 앵커인데도 바로 골라진다.
     */
    fun onDotClick(dotId: String) {
        val (id, side) = parseDotId(dotId)
        if (!id.startsWith(ANCHOR_ID_PREFIX)) {
            update { state ->
                val anchorId = state.focus?.anchorId ?: return@update state
                state.toggling(BodyMapSelection(anchorId, id, side))
            }
            return
        }
        update { state ->
            if (bodyMapZonesOf(id).isEmpty()) {
                state.toggling(BodyMapSelection(id, side = side))
            } else {
                state.copy(focus = BodyMapSelection(id, side = side))
            }
        }
    }

    /**
     * 부위를 고르거나 뺀다.
     *
     * 이미 고른 것을 다시 누르면 빠진다. 고른 목록의 지우기도 이 길로 온다. 같은 조작에
     * 두 개의 창구를 두면 한쪽만 고쳐지는 일이 생긴다.
     */
    fun onPartToggle(selection: BodyMapSelection) {
        update { it.toggling(selection) }
    }

    /**
     * 확대를 닫고 앵커 화면으로 돌아간다.
     *
     * 고른 부위는 남는다. 여러 곳을 고를 수 있으므로 다른 앵커로 옮겨 계속 고르는 것이
     * 정상 흐름이다.
     */
    fun onFocusClear() {
        update { it.copy(focus = null) }
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

/**
 * 고른 목록에 넣거나 뺀 상태.
 *
 * 순서는 누른 순서다. 목록에 보이는 순서이자 문답 첫 마디에 들어가는 순서이기도 해서,
 * 집합으로 두면 순서가 흔들린다.
 */
private fun BodyMapUiState.toggling(selection: BodyMapSelection): BodyMapUiState = copy(
    selected = if (selection in selected) selected - selection else selected + selection,
)
