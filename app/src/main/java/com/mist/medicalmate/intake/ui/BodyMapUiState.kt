package com.mist.medicalmate.intake.ui

/**
 * 인체도 단계의 상태.
 *
 * 화면은 셋(1l-1 앞면 · 1l-3 뒷면 · 1l-2 세부 구역)이지만 목적지는 하나다. 앞면과
 * 뒷면은 같은 화면의 토글이고, 구역은 [focus]가 채워지면 그 위에 이어진다. 시안이
 * 셋으로 나뉜 것은 상태별 그림이기 때문이다.
 *
 * **[focus]와 [selection]을 나눈 이유가 있다.** 하나로 두면 구역을 고르는 순간 그 값이
 * 구역을 가리키게 되고, 화면은 앵커를 짚은 상태가 끝난 것으로 읽어 앵커 단계로
 * 돌아간다. 고른 점도 고른 줄도 한 번 보이지 않고 사라진다. 확대한 앵커가 무엇인지는
 * 고른 부위와 별개로 남아 있어야 한다.
 *
 * 나눠 두면 확대 화면의 제목과 좌우 반전도 맞는다. 둘 다 **앵커의** 좌우를 봐야 하는데,
 * 값이 하나면 구역을 고른 뒤 그 자리에 구역의 좌우가 들어온다. 머리에서 왼쪽 눈을
 * 고르면 제목이 "왼쪽 머리 어디가 아프세요?"가 된다.
 *
 * 고르는 것은 한 곳이다. 다른 구역을 누르면 앞서 고른 것을 대신한다.
 */
data class BodyMapUiState(
    val view: BodyMapView = BodyMapView.FRONT,
    val focus: BodyMapSelection? = null,
    val selection: BodyMapSelection? = null,
    val byList: Boolean = false,
) {
    /** 구역을 고르는 중인지. 구역이 있는 앵커를 확대한 상태면 그렇다. */
    val pickingZone: Boolean get() = focus != null && zones.isNotEmpty()

    /**
     * 지금 보여줄 화면.
     *
     * 세 화면이 한 목적지 안에 있어서 스크롤 상태와 전환 애니메이션이 이 값을 기준으로
     * 갈린다. 조건을 화면마다 다시 쓰면 셋이 어긋난다.
     */
    val screen: BodyMapScreen
        get() = when {
            byList -> BodyMapScreen.LIST
            pickingZone -> BodyMapScreen.ZONE
            else -> BodyMapScreen.ANCHOR
        }

    /** 확대해서 보고 있는 앵커. */
    val anchor: BodyMapAnchorGeometry? get() = focus?.let { bodyMapAnchorOf(it.anchorId) }

    val zones: List<BodyMapZoneGeometry> get() = anchor?.zones.orEmpty()

    /** 지금 보여줄 전신 이미지. */
    val bodyImage: BodyMapImage
        get() = if (view == BodyMapView.BACK) bodyMapBack else bodyMapFront

    /**
     * 확대해 들어간 앵커의 전신 이미지 위 좌표. 확대 애니메이션의 축이 된다.
     *
     * 팔·다리는 좌우 두 점이라 짚은 쪽을 골라야 한다. 반대쪽을 축으로 삼으면 화면이
     * 엉뚱한 방향으로 밀려난다.
     */
    val focusPoint: BodyMapPoint?
        get() = focus?.let { f -> bodyMapAnchorOf(f.anchorId).points.firstOrNull { it.side == f.side } }
}

/**
 * 인체도 단계의 화면 셋.
 *
 * [ANCHOR]는 전신에서 앵커를 짚는 1l-1·1l-3, [ZONE]은 확대해 구역을 고르는 1l-2,
 * [LIST]는 인체도를 쓸 수 없을 때의 목록이다.
 */
enum class BodyMapScreen { ANCHOR, ZONE, LIST }

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
 *
 * 고른 표시는 그 앵커 안에서 부위를 골랐을 때 켠다. 앵커 자체는 고르는 대상이 아니라
 * 확대해 들어가는 입구다.
 */
internal fun bodyMapAnchorDots(view: BodyMapView, selection: BodyMapSelection?): List<BodyMapDot> =
    bodyMapAnchorsOn(view).flatMap { anchor ->
        anchor.points.map { point ->
            val side = point.side
            BodyMapDot(
                id = bodyMapDotId(anchor.id, side),
                label = BodyMapSelection(anchor.id, side = side).title(),
                x = point.x,
                y = point.y,
                selected = selection?.belongsTo(anchor.id, side) == true,
            )
        }
    }

/**
 * 구역 화면에 얹을 점들.
 *
 * 좌우가 갈리는 구역은 점이 둘이고 각자 좌우를 물고 있다. 팔·다리처럼 이미지 한 장을
 * 좌우가 함께 쓰는 경우는 점이 하나([BodyMapSide.BASE])이고 좌우는 [side]로 들어온다.
 * 그때 왼쪽이면 이미지를 뒤집으므로 좌표도 함께 뒤집는다.
 *
 * [side]는 **확대한 앵커의** 좌우다. 고른 구역의 좌우가 아니다. 팔을 왼쪽으로 확대한 채
 * 구역을 고르면 그 뒤에도 이미지가 계속 뒤집혀 있어야 한다.
 */
internal fun bodyMapZoneDots(
    anchor: BodyMapAnchorGeometry,
    side: BodyMapSide,
    selection: BodyMapSelection?,
): List<BodyMapDot> {
    val flip = anchor.detail?.mirrored == true && side == BodyMapSide.LEFT
    return anchor.zones.flatMap { zone ->
        zone.points.map { point ->
            val candidate = BodyMapSelection(anchor.id, zone.id, zoneSideOf(point, side))
            BodyMapDot(
                id = bodyMapDotId(zone.id, candidate.side),
                label = candidate.title(),
                x = if (flip) 1f - point.x else point.x,
                y = point.y,
                selected = selection == candidate,
            )
        }
    }
}

/**
 * 확대한 앵커에서 고를 수 있는 부위들.
 *
 * 인체도의 점과 목록의 줄이 같은 값을 써야 한다. 두 길이 각자 목록을 만들면 한쪽에만
 * 있는 부위가 생기고, 고른 표시도 서로 어긋난다.
 */
internal fun bodyMapZoneChoices(anchor: BodyMapAnchorGeometry, side: BodyMapSide): List<BodyMapSelection> =
    anchor.zones.flatMap { zone ->
        zone.points.map { point -> BodyMapSelection(anchor.id, zone.id, zoneSideOf(point, side)) }
    }

/**
 * 구역 점의 좌우.
 *
 * 좌우 공용 이미지의 기준점은 확대한 쪽을 따르고, 좌우가 갈린 점은 자기 값을 쓴다.
 */
private fun zoneSideOf(point: BodyMapPoint, anchorSide: BodyMapSide): BodyMapSide =
    if (point.side == BodyMapSide.BASE) anchorSide else point.side

/**
 * 점의 id. 온톨로지 id 하나로는 좌우 두 점을 구분할 수 없어서 좌우를 덧붙인다.
 *
 * 눈·귀처럼 좌우가 갈리는 구역이 한 이미지에 두 점으로 있고, 캔버스는 눌린 점을 id로만
 * 돌려준다.
 */
internal fun bodyMapDotId(ontologyId: String, side: BodyMapSide): String = "$ontologyId@${side.name}"

/** [bodyMapDotId]를 되돌린다. */
internal fun parseDotId(dotId: String): Pair<String, BodyMapSide> {
    val (id, side) = dotId.split('@', limit = 2)
    return id to BodyMapSide.valueOf(side)
}
