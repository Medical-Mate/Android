package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Immutable

/**
 * 3D 인체도의 구역 점 하나. 좌표는 [bodyMap3dRegions]가 생성 파일로 들고 있다.
 *
 * [nx]·[ny]·[nz]는 그 자리의 표면 노멀이다. 판정에는 쓰지 않고 카메라 반대쪽을 보는 점을
 * 가리는 데만 쓴다. 판정에 노멀을 걸면 뒷면 점이 허리·엉덩이뿐이라 뒤에서 짚은 것이 전부
 * 그쪽으로 빨려 들어간다.
 */
@Immutable
internal data class BodyMap3dPoint(
    val zoneId: String,
    val anchorId: String,
    val side: BodyMapSide,
    val x: Float,
    val y: Float,
    val z: Float,
    val nx: Float,
    val ny: Float,
    val nz: Float,
)

/**
 * 짚은 자리의 판정 결과.
 *
 * [point]는 늘 가장 가까운 구역이고, 너무 멀면 [accepted]가 false다. 멀어도 무엇이
 * 가까웠는지를 버리지 않는 이유는 화면에서 "다시 짚어 주세요"를 그 자리에 띄우기
 * 때문이다.
 */
@Immutable
internal data class BodyMap3dPick(val point: BodyMap3dPoint, val distance: Float) {
    val accepted: Boolean get() = distance <= BODY_3D_PICK_MAX_DISTANCE
}

/**
 * 표면에서 짚은 한 점에 가장 가까운 구역.
 *
 * 거리만 본다. 노멀은 보지 않는다 — 뒷면에 점이 있는 것은 허리·엉덩이뿐이라 노멀로
 * 후보를 거르면 뒤를 향한 탭이 전부 그쪽으로 몰린다.
 *
 * [candidates]를 좁히면 그 안에서만 고른다. 부위를 확대한 화면에서 쓴다 — 화면에 그 부위만
 * 차 있는데 가장자리를 짚었다고 옆 부위가 골라지면 확대한 것이 무의미해진다.
 */
internal fun bodyMap3dNearest(
    hit: BodyMap3dVector,
    candidates: List<BodyMap3dPoint> = bodyMap3dRegions,
): BodyMap3dPick {
    var nearest = candidates.first()
    var best = Float.MAX_VALUE
    for (region in candidates) {
        val distance = hit.distanceTo(BodyMap3dVector(region.x, region.y, region.z))
        if (distance < best) {
            best = distance
            nearest = region
        }
    }
    return BodyMap3dPick(nearest, best)
}

/**
 * 지금 고를 수 있는 구역들.
 *
 * 전신을 보는 중이면 전부다. 그때 고르는 것은 구역이 아니라 그 구역이 딸린 앵커인데,
 * 앵커 점으로 직접 판정하지 않는 이유는 점이 아홉뿐이라 발끝처럼 앵커에서 먼 자리가
 * 채택 문턱을 넘기 때문이다. 구역으로 찾고 그 앵커를 쓰면 발을 짚어도 다리로 들어간다.
 *
 * 부위를 확대했으면 그 앵커의 구역만 남긴다. 좌우 중 한쪽만 담는 앵커(팔·다리)에서는
 * 보고 있는 쪽만 남긴다 — 반대쪽은 화면 밖이라 짚을 수 없는데 후보로 남으면 그쪽이 골라진다.
 */
internal fun bodyMap3dCandidates(focus: BodyMapSelection?): List<BodyMap3dPoint> {
    if (focus == null) return bodyMap3dRegions
    val oneSide = bodyMap3dFrameOf(focus.anchorId)?.oneSide == true
    return bodyMap3dRegions.filter { point ->
        point.anchorId == focus.anchorId && (!oneSide || point.side == focus.side)
    }
}

/**
 * 확대해 들어갈 앵커.
 *
 * 좌우는 한쪽만 담는 앵커에서만 물고 간다. 나머지는 가운데로 둔다 — 눈을 짚었다고 제목이
 * "왼쪽 머리 어디가 아프세요?"가 되면 안 된다. 머리는 한 화면에 좌우가 함께 있다.
 */
internal fun BodyMap3dPoint.toFocus(): BodyMapSelection {
    val oneSide = bodyMap3dFrameOf(anchorId)?.oneSide == true
    return BodyMapSelection(anchorId, side = if (oneSide) side else BodyMapSide.CENTER)
}

/**
 * 3D 구역 점을 2D 인체도와 같은 선택 값으로 옮긴다.
 *
 * 저장되는 값이 두 길에서 같아야 한다. 그래서 좌우를 3D 좌표에서 그대로 가져오지 않고
 * 2D가 그 구역에 실제로 두고 있는 갈래에서 고른다. 지금은 두 자료의 좌우가 모두 맞아
 * 떨어지고 그것을 시험이 붙잡고 있는데, 한쪽 좌표만 다시 생성해 어긋나면 3D로 고른
 * 부위만 2D에 없는 값이 되어 인체도로 돌아왔을 때 고른 표시가 켜지지 않는다.
 */
internal fun BodyMap3dPoint.toSelection(): BodyMapSelection {
    val choices = bodyMapChoicesForZone(anchorId, zoneId)
    return choices.firstOrNull { it.side == side }
        ?: choices.firstOrNull()
        ?: BodyMapSelection(anchorId, zoneId, side)
}
