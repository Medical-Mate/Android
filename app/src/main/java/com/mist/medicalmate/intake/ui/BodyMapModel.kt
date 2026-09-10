package com.mist.medicalmate.intake.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * 인체도(1l-1·1l-2·1l-3)의 타입.
 *
 * 부위는 두 단계다. **앵커** 9개를 전신에서 짚고, 그 앵커를 확대해 **구역** 25개 중
 * 하나를 고른다. 이름과 번호는 AI 트랙의 부위 온톨로지(`ANC:*` · `SUR:*`)를 그대로
 * 쓴다. 앱이 따로 부위를 정의하지 않는 이유는 브리핑 카드와 서버 문답이 같은 id로
 * 부위를 가리켜야 하기 때문이다.
 *
 * 좌표(기하)와 이름(온톨로지)을 나눠 둔 것은 출처가 다르기 때문이다. 좌표와 이미지는
 * 앱이 들고 있는 에셋이고([bodyMapAnchors]), 이름은 서버 응답에서 온다
 * (`docs/examples/body-map.json`). 연동 전까지 후자만 픽스처다.
 */

/** 전신 이미지의 면. 앵커 8개가 앞면, 허리·엉덩이만 뒷면에 있다. */
enum class BodyMapView { FRONT, BACK }

/**
 * 점이 가리키는 좌우.
 *
 * [CENTER]는 좌우가 없는 부위다. [LEFT]·[RIGHT]는 **본인 기준**이고 뒷면에서는 화면
 * 방향과 같다. [BASE]는 팔·다리처럼 한 장을 좌우가 함께 쓰는 이미지의 기준점이다.
 */
enum class BodyMapSide { CENTER, LEFT, RIGHT, BASE }

/**
 * 인체도 이미지 한 장.
 *
 * [widthPx]·[heightPx]는 원본 픽셀이다. 표시할 때 쓰는 것은 종횡비([aspectRatio])뿐이고
 * 원본 값을 남긴 이유는 좌표표와 대조할 때 필요해서다.
 *
 * [mirrored]가 true면 본인 오른쪽만 그린 한 장이다. 왼쪽은 좌우로 뒤집어 쓴다.
 */
@Immutable
data class BodyMapImage(
    @DrawableRes val res: Int,
    val widthPx: Int,
    val heightPx: Int,
    val view: BodyMapView,
    val mirrored: Boolean,
) {
    val aspectRatio: Float get() = widthPx.toFloat() / heightPx.toFloat()
}

/**
 * 이미지 위의 한 점. [x]·[y]는 0..1로 정규화한 값이다.
 *
 * 픽셀로 두지 않은 이유는 이미지를 화면 높이에 맞춰 줄이기 때문이다. 정규화해 두면
 * 표시 크기를 곱하기만 하면 되고 원본 해상도가 바뀌어도 코드가 안 바뀐다.
 */
@Immutable
data class BodyMapPoint(val side: BodyMapSide, val x: Float, val y: Float)

/** 구역의 좌표. 이름은 [bodyMapOntology]에 있다. */
@Immutable
data class BodyMapZoneGeometry(val id: String, val points: List<BodyMapPoint>)

/**
 * 앵커의 좌표.
 *
 * [view]가 null이면 전신·피부처럼 인체도에 자리가 없는 앵커다. 시안이 그것을 이미지
 * 밖 칩으로 그린다. [detail]과 [zones]도 비어 있어서 구역 단계 없이 바로 정해진다.
 */
@Immutable
data class BodyMapAnchorGeometry(
    val id: String,
    val view: BodyMapView?,
    val points: List<BodyMapPoint>,
    val detail: BodyMapImage?,
    val zones: List<BodyMapZoneGeometry>,
)

/**
 * 인체도 위에 그릴 점 하나. 좌표는 이미지 기준 0..1이고 좌우 반전은 이미 반영된 값이다.
 *
 * [BodyMapPoint]와 나눠 둔 이유는 쓰임이 다르기 때문이다. 저쪽은 좌표표에서 온 원본이고
 * 이쪽은 화면에 그릴 상태다. [id]에 좌우가 붙고 [label]과 [selected]가 더해진다.
 */
@Immutable
internal data class BodyMapDot(val id: String, val label: String, val x: Float, val y: Float, val selected: Boolean)

/**
 * 부위 하나를 가리키는 값.
 *
 * 고른 부위와 확대해서 보고 있는 앵커가 같은 타입이다. [zoneId]가 null이면 앵커까지만
 * 가리킨 것이고, 그것이 확대 대상이거나 구역이 없는 앵커(전신·피부)다.
 *
 * [side]는 좌우 구분이 있는 부위에서만 의미가 있다. 없는 부위는 [BodyMapSide.CENTER]다.
 */
@Immutable
data class BodyMapSelection(
    val anchorId: String,
    val zoneId: String? = null,
    val side: BodyMapSide = BodyMapSide.CENTER,
)

/**
 * 이 선택이 [anchorId] · [side]로 열리는 확대 화면에 속하는지.
 *
 * 좌우 공용 이미지를 쓰는 앵커(팔·다리)는 왼쪽과 오른쪽이 서로 다른 화면이라 좌우까지
 * 봐야 한다. 나머지 앵커는 한 이미지에 좌우 구역이 함께 있어서 앵커만 맞으면 된다.
 *
 * 이 구분이 없으면 머리를 확대해 왼쪽 눈을 골랐을 때 머리 점의 고른 표시가 켜지지
 * 않는다. 머리 앵커의 좌우는 `CENTER`이고 고른 구역의 좌우는 `LEFT`이기 때문이다.
 */
internal fun BodyMapSelection.belongsTo(anchorId: String, side: BodyMapSide): Boolean {
    if (this.anchorId != anchorId) return false
    val sharedImage = bodyMapAnchorOf(anchorId).detail?.mirrored == true
    return !sharedImage || this.side == side
}

/** 앵커를 id로 찾는다. 좌표표와 온톨로지가 같은 9개를 담고 있어서 없을 수 없다. */
internal fun bodyMapAnchorOf(anchorId: String): BodyMapAnchorGeometry = bodyMapAnchors.first { it.id == anchorId }

internal fun bodyMapZonesOf(anchorId: String): List<BodyMapZoneGeometry> = bodyMapAnchorOf(anchorId).zones

/** 부위 이름. 온톨로지에 없는 id는 없지만, 서버 응답이 바뀌면 id를 그대로 보여준다. */
internal fun bodyMapLabelOf(id: String): String = bodyMapOntology[id] ?: id

/**
 * 판 안의 알약에 넣는 이름. "무릎(왼쪽)"처럼 읽힌다. 시안의 `Selected Label` 형식이다.
 *
 * 좌우가 없는 부위에는 아무것도 붙이지 않는다. "가슴 가운데(가운데)"는 군더더기다.
 */
internal fun BodyMapSelection.label(): String {
    val base = bodyMapLabelOf(zoneId ?: anchorId)
    return when (side) {
        BodyMapSide.LEFT -> "$base(왼쪽)"
        BodyMapSide.RIGHT -> "$base(오른쪽)"
        else -> base
    }
}

/**
 * 문장에 넣는 이름. "왼쪽 무릎"처럼 좌우가 앞에 온다.
 *
 * 제목과 스크린 리더 라벨이 이쪽을 쓴다. "무릎(왼쪽) 어디가 아프세요?"는 읽히지 않고,
 * 괄호를 읽는 방식도 리더마다 다르다.
 */
internal fun BodyMapSelection.title(): String {
    val base = bodyMapLabelOf(zoneId ?: anchorId)
    return when (side) {
        BodyMapSide.LEFT -> "왼쪽 $base"
        BodyMapSide.RIGHT -> "오른쪽 $base"
        else -> base
    }
}
