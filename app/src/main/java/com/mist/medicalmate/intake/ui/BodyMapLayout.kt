package com.mist.medicalmate.intake.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 인체도를 얼마나 크게 보여줘야 하는지.
 *
 * 점의 조작 영역이 48dp 정사각형이고 좌표표는 그 크기로 서로 겹치지 않게 배치돼 있다.
 * 그런데 그 보장은 **표시 크기에 달려 있다.** 이미지를 줄이면 점 사이 거리도 함께
 * 줄고 어느 순간 두 조작 영역이 겹친다. 겹치면 짚은 곳과 골라진 부위가 달라진다.
 *
 * 그래서 화면이 인체도에 주는 높이를 시안에서 받아 적지 않고 좌표에서 계산한다. 좌표가
 * 바뀌면 필요한 높이도 함께 바뀌고, 시안과 어긋나면 [BodyMapLayoutTest]가 잡는다.
 *
 * 정사각형 두 개는 x·y 어느 한쪽만 48dp 이상 떨어져도 겹치지 않는다. 그래서 거리는
 * 유클리드가 아니라 두 축의 큰 쪽으로 센다.
 */

/** 점 조작 영역의 한 변. `size/touch-min`과 같은 48이다. */
private const val TOUCH_MIN_DP = 48f

/**
 * 48dp 조작 영역이 겹치지 않는 최소 표시 폭.
 *
 * [aspectRatio]가 필요한 이유는 y 좌표가 폭이 아니라 높이에 곱해지기 때문이다. 종횡비를
 * 지키며 넣으므로 높이는 `폭 / 종횡비`다.
 *
 * 점이 하나뿐이면 겹칠 상대가 없어서 0이다.
 */
internal fun bodyMapMinWidth(dots: List<BodyMapDot>, aspectRatio: Float): Dp {
    val tightest = tightestSeparation(dots, aspectRatio) ?: return 0.dp
    return (TOUCH_MIN_DP / tightest).dp
}

/** [bodyMapMinWidth]에 대응하는 높이. 인체도가 세로로 길어서 화면에서 정하는 값은 이쪽이다. */
internal fun bodyMapMinHeight(dots: List<BodyMapDot>, aspectRatio: Float): Dp =
    bodyMapMinWidth(dots, aspectRatio) / aspectRatio

/**
 * 가장 가까운 두 점의 간격. 표시 폭 1일 때의 값이고, 폭을 곱하면 dp가 된다.
 *
 * 겹칠 상대가 없으면 null이다.
 */
private fun tightestSeparation(dots: List<BodyMapDot>, aspectRatio: Float): Float? {
    if (dots.size < 2) return null
    var tightest = Float.MAX_VALUE
    for (i in dots.indices) {
        for (j in i + 1 until dots.size) {
            tightest = min(tightest, separation(dots[i], dots[j], aspectRatio))
        }
    }
    return tightest
}

/** 두 점 사이의 축별 거리 중 큰 쪽. y는 높이에 곱해지므로 종횡비로 나눠 폭 기준으로 맞춘다. */
private fun separation(a: BodyMapDot, b: BodyMapDot, aspectRatio: Float): Float =
    max(abs(a.x - b.x), abs(a.y - b.y) / aspectRatio)

/**
 * 판의 높이.
 *
 * 인체도를 콘텐츠 폭까지 채우는 높이로 잡되, 겹침을 막는 최소치보다 작아지지 않고
 * 전신 판보다 커지지 않는다.
 *
 * 위쪽을 전신 판으로 막는 이유는 팔 확대 이미지가 세로로 아주 길어서다(1 : 2.64).
 * 폭을 320까지 채우면 높이가 844가 되고 화면 하나를 넘긴다.
 *
 * 아래쪽을 최소치로 막는 이유는 겹침 때문이다. 가슴처럼 납작한 이미지는 폭을 다 채워도
 * 최소치에 못 미칠 수 있다.
 */
internal fun bodyMapCardHeight(dots: List<BodyMapDot>, image: BodyMapImage): Dp {
    val minimum = bodyMapMinHeight(dots, image.aspectRatio)
    val atContentWidth = MedicalMateSize.contentWidth / image.aspectRatio
    return atContentWidth.coerceIn(minimum, maxOf(bodyMapBodyHeight(), minimum))
}

/**
 * 전신 판의 높이. 앞면과 뒷면이 같은 값이라 앞뒤를 바꿔도 판이 튀지 않는다.
 *
 * 뒷면은 앵커가 허리·엉덩이 하나뿐이어서 겹칠 상대가 없고 최소치가 0이다. 그 값을 그대로
 * 쓰면 뒷면 판이 사라진다. 두 면 중 큰 쪽을 쓰면 그 문제와 전환 시 튀는 문제가 함께
 * 사라진다.
 */
internal fun bodyMapBodyHeight(): Dp = maxOf(
    bodyMapMinHeight(bodyMapAnchorDots(BodyMapView.FRONT, null), bodyMapFront.aspectRatio),
    bodyMapMinHeight(bodyMapAnchorDots(BodyMapView.BACK, null), bodyMapBack.aspectRatio),
)
