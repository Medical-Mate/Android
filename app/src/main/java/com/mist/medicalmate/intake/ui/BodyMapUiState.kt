package com.mist.medicalmate.intake.ui

/**
 * 인체도 단계의 상태.
 *
 * 화면은 셋(1l-1 앞면 · 1l-3 뒷면 · 1l-2 세부 구역)이지만 목적지는 하나다. 앞면과
 * 뒷면은 같은 화면의 토글이고, 구역은 [selection]이 채워지면 그 위에 이어진다. 시안이
 * 셋으로 나뉜 것은 상태별 그림이기 때문이다.
 */
data class BodyMapUiState(
    val view: BodyMapView = BodyMapView.FRONT,
    val selection: BodyMapSelection? = null,
    val byList: Boolean = false,
) {
    /** 구역을 고르는 중인지. 앵커를 짚었고 아직 구역이 남아 있으면 그렇다. */
    val pickingZone: Boolean get() = selection != null && selection.zoneId == null && zones.isNotEmpty()

    /** 짚은 앵커. 아직 안 짚었으면 null이다. */
    val anchor: BodyMapAnchorGeometry? get() = selection?.let { bodyMapAnchorOf(it.anchorId) }

    val zones: List<BodyMapZoneGeometry> get() = anchor?.zones.orEmpty()

    /** 지금 보여줄 전신 이미지. */
    val bodyImage: BodyMapImage
        get() = if (view == BodyMapView.BACK) bodyMapBack else bodyMapFront
}

/**
 * 인체도에 자리가 없는 앵커. 전신과 피부다.
 *
 * 온톨로지가 `view: none`으로 보내고 좌표표도 이미지를 비워 둔다. 몸 전체에 걸리는
 * 증상(열·피로)과 어디든 생기는 증상(발진)이라 한 점으로 찍을 수 없다. 시안의 사이드
 * 칩이 이 둘이다.
 */
internal val bodyMapSideAnchors: List<BodyMapAnchorGeometry>
    get() = bodyMapAnchors.filter { it.view == null }

/** 지금 면에 있는 앵커들. 앞면 6종(팔·다리는 좌우 2점), 뒷면 1종이다. */
internal fun bodyMapAnchorsOn(view: BodyMapView): List<BodyMapAnchorGeometry> =
    bodyMapAnchors.filter { it.view == view }

/**
 * 앵커 화면에 얹을 점들.
 *
 * 팔·다리는 좌우 두 점이 한 앵커에 묶여 있어서 점마다 좌우를 라벨에 붙인다. 뒤에
 * 확대할 이미지가 좌우 공용 한 장이라, 어느 쪽을 짚었는지가 여기서 정해져야 한다.
 */
internal fun bodyMapAnchorDots(view: BodyMapView, selection: BodyMapSelection?): List<BodyMapDot> =
    bodyMapAnchorsOn(view).flatMap { anchor ->
        anchor.points.map { point ->
            val side = point.side
            BodyMapDot(
                id = dotId(anchor.id, side),
                label = BodyMapSelection(anchor.id, side = side).title(),
                x = point.x,
                y = point.y,
                selected = selection?.anchorId == anchor.id && selection.side == side,
            )
        }
    }

/**
 * 구역 화면에 얹을 점들.
 *
 * 좌우가 갈리는 구역은 점이 둘이고 각자 좌우를 물고 있다. 팔·다리처럼 이미지 한 장을
 * 좌우가 함께 쓰는 경우는 점이 하나([BodyMapSide.BASE])이고 좌우는 [side]로 들어온다.
 * 그때 왼쪽이면 이미지를 뒤집으므로 좌표도 함께 뒤집는다.
 */
internal fun bodyMapZoneDots(
    anchor: BodyMapAnchorGeometry,
    side: BodyMapSide,
    selection: BodyMapSelection?,
): List<BodyMapDot> {
    val flip = anchor.detail?.mirrored == true && side == BodyMapSide.LEFT
    return anchor.zones.flatMap { zone ->
        zone.points.map { point ->
            val zoneSide = if (point.side == BodyMapSide.BASE) side else point.side
            BodyMapDot(
                id = dotId(zone.id, zoneSide),
                label = BodyMapSelection(anchor.id, zone.id, zoneSide).title(),
                x = if (flip) 1f - point.x else point.x,
                y = point.y,
                selected = selection?.zoneId == zone.id && selection.side == zoneSide,
            )
        }
    }
}

/**
 * 점의 id. 온톨로지 id 하나로는 좌우 두 점을 구분할 수 없어서 좌우를 덧붙인다.
 *
 * 눈·귀처럼 좌우가 갈리는 구역이 한 이미지에 두 점으로 있고, 캔버스는 눌린 점을 id로만
 * 돌려준다.
 */
private fun dotId(ontologyId: String, side: BodyMapSide): String = "$ontologyId@${side.name}"

/** [dotId]를 되돌린다. */
internal fun parseDotId(dotId: String): Pair<String, BodyMapSide> {
    val (id, side) = dotId.split('@', limit = 2)
    return id to BodyMapSide.valueOf(side)
}
