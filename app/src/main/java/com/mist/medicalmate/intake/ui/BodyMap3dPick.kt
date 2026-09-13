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
 */
internal fun bodyMap3dNearest(hit: BodyMap3dVector): BodyMap3dPick {
    var nearest = bodyMap3dRegions.first()
    var best = Float.MAX_VALUE
    for (region in bodyMap3dRegions) {
        val distance = hit.distanceTo(BodyMap3dVector(region.x, region.y, region.z))
        if (distance < best) {
            best = distance
            nearest = region
        }
    }
    return BodyMap3dPick(nearest, best)
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
