package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

/**
 * 트리거와 말풍선을 묶은 툴팁. Figma `542:1295` + `575:1287`.
 *
 * 화면마다 트리거와 말풍선을 따로 조립하지 않는다. Figma 수정사항이 "모든 페이지에 들어가는
 * 툴팁도 위치 및 디자인, 아이콘 동일해야 함"이라고 못 박았고, 화면마다 조립하면 띄우는
 * 위치와 여닫는 방식이 화면별로 어긋난다.
 *
 * **말풍선을 [Popup]으로 띄운다.** 같은 레이아웃 안에서 겹치면 감싸는 상자가 말풍선 높이만큼
 * 커져서 아래 내용이 밀린다. 신상정보 등록의 알러지 단계에서 칩이 내려가던 것이 그
 * 경우다(#71). `Popup`은 별도 창에 그려져 아래 내용의 자리를 건드리지 않는다.
 *
 * 말풍선 밖을 누르면 닫힌다. 열어 둔 채로 화면을 계속 쓰는 안내가 아니라, 읽고 지우는
 * 짧은 설명이다.
 *
 * 트리거 아래에 오른쪽을 맞춰 붙인다. 말풍선의 꼬리가 끝에서 24 지점에 있어서(문서 8.4)
 * 오른쪽을 맞추면 꼬리가 트리거를 가리킨다.
 */
@Composable
fun MedicalMateTooltip(text: String, contentDescription: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val positionProvider = rememberTooltipPositionProvider()

    Box(modifier = modifier) {
        MedicalMateTooltipTrigger(
            active = open,
            onClick = { open = !open },
            contentDescription = contentDescription,
        )
        if (open) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { open = false },
            ) {
                MedicalMateTooltipBubble(text = text)
            }
        }
    }
}

/**
 * 트리거 아래, 오른쪽을 맞춘 자리.
 *
 * 트리거의 히트 영역이 48이고 그 안의 채움은 32다. 채움 아래에서 조금 띄우려면 히트 영역
 * 끝에서 [TooltipGap]만 내리면 된다.
 *
 * 화면 오른쪽 끝에서 넘치지 않도록 [TooltipMargin]만큼은 남긴다. 트리거가 화면 끝에 붙어
 * 있으면 오른쪽 맞춤만으로는 말풍선이 밖으로 나간다.
 */
@Composable
private fun rememberTooltipPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    val gap = with(density) { TooltipGap.roundToPx() }
    val margin = with(density) { TooltipMargin.roundToPx() }

    return remember(gap, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val right = anchorBounds.right - popupContentSize.width
                val x = right.coerceIn(
                    margin,
                    (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin),
                )
                val y = anchorBounds.bottom + gap
                return IntOffset(x, y)
            }
        }
    }
}

/** 트리거와 말풍선 사이. */
private val TooltipGap: Dp = 4.dp

/** 화면 끝에 남기는 여백. 본문 여백과 같다. */
private val TooltipMargin: Dp = 20.dp
