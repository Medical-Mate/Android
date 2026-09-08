package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Tooltip Bubble`(`575:1287`).
 *
 * [MedicalMateTooltipTrigger]를 누르면 띄우는 짧은 설명이다. 트리거 아래에 놓이고 꼬리가
 * 위를 가리킨다.
 *
 * 처음에 `Tooltip` 마스터 안에 `State=Open` 260x100으로 있었는데, 지금은 180x36 독립
 * 컴포넌트로 쪼개졌다. 그래서 트리거와 따로 만든다. DESIGN.md 8절에는 아직 항목이 없다.
 *
 * 규격은 Figma에서 읽고 렌더를 픽셀로 재서 맞췄다. 본문 180x36, 좌우 여백 12, 위아래 10,
 * 반경 8, 꼬리 16x10이고 꼬리 중심이 오른쪽 끝에서 24다.
 *
 * 면이 `bg/inverse-soft`다. Toast의 `bg/inverse`(`#131722`)보다 옅은 `#3A4053`이다. 화면에
 * 겹쳐 뜨는 짧은 설명이라 Toast만큼 무겁지 않게 둔 것으로 보인다.
 *
 * 꼬리를 별도 요소로 그리지 않고 [TooltipBubbleShape]에 넣었다. 위에 삼각형을 따로 얹으면
 * 본문과 만나는 자리에 이음선이 생긴다.
 *
 * [arrowOffsetFromEnd]는 꼬리 중심이 오른쪽 끝에서 떨어진 거리다. 트리거 위치에 맞춰
 * 호출자가 옮긴다. 기본값은 Figma 마스터와 같다.
 */
@Composable
fun MedicalMateTooltipBubble(
    text: String,
    modifier: Modifier = Modifier,
    arrowOffsetFromEnd: Dp = DefaultArrowOffsetFromEnd,
) {
    Surface(
        shape =
        TooltipBubbleShape(
            cornerRadius = BubbleCornerRadius,
            arrowWidth = ArrowWidth,
            arrowHeight = ArrowHeight,
            arrowOffsetFromEnd = arrowOffsetFromEnd,
        ),
        color = MedicalMateTheme.colors.bgInverseSoft,
        contentColor = MedicalMateTheme.colors.fgOnInverse,
        modifier = modifier.widthIn(max = BubbleMaxWidth),
    ) {
        Box(
            modifier =
            Modifier.padding(
                start = MedicalMateSpace.s12,
                end = MedicalMateSpace.s12,
                // 꼬리가 도형 안에 들어 있어 위 여백에 꼬리 높이를 더한다.
                top = ArrowHeight + MedicalMateSpace.s10,
                bottom = MedicalMateSpace.s10,
            ),
        ) {
            Text(text = text, style = MedicalMateTheme.typography.labelS)
        }
    }
}

/**
 * 위를 가리키는 꼬리가 달린 둥근 사각형.
 *
 * 본문과 꼬리를 한 도형으로 만든다. 같은 색 요소를 겹쳐 놓으면 안티에일리어싱 때문에
 * 경계에 얇은 선이 보인다.
 */
private class TooltipBubbleShape(
    private val cornerRadius: Dp,
    private val arrowWidth: Dp,
    private val arrowHeight: Dp,
    private val arrowOffsetFromEnd: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        with(density) {
            val arrowH = arrowHeight.toPx()
            val arrowW = arrowWidth.toPx()
            val radius = cornerRadius.toPx()
            val arrowCenter = size.width - arrowOffsetFromEnd.toPx()

            val path =
                Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 0f,
                            top = arrowH,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        ),
                    )
                    moveTo(arrowCenter - arrowW / 2f, arrowH)
                    lineTo(arrowCenter, 0f)
                    lineTo(arrowCenter + arrowW / 2f, arrowH)
                    close()
                }
            return Outline.Generic(path)
        }
    }
}

/** Figma 마스터의 본문 폭. 글이 길어지면 줄바꿈한다. */
private val BubbleMaxWidth = 180.dp

/** 렌더에서 재서 얻은 값. 위 모서리 곡선이 y 17~25에 걸쳐 있었다. */
private val BubbleCornerRadius = 8.dp

private val ArrowWidth = 16.dp

private val ArrowHeight = 10.dp

/** Figma 마스터의 꼬리 중심은 x 156, 본문 폭 180이라 오른쪽 끝에서 24다. */
private val DefaultArrowOffsetFromEnd = 24.dp
