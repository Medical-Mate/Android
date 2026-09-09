package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 인체도 한 장과 그 위의 점들. Figma `Body Map`(`387:4164`).
 *
 * 이미지를 주어진 높이에 맞춰 넣고 남는 폭은 비운다. 점은 이미지의 실제 표시 크기에
 * 곱해서 얹는다. **점의 시각 크기(22)와 조작 크기(48)를 나눈** 이유는 좌표표가 48dp
 * 히트박스를 기준으로 서로 겹치지 않게 배치돼 있기 때문이다. 시각 크기를 48로 키우면
 * 인체도가 점으로 덮인다.
 *
 * [mirrored]는 이미지를 좌우로 뒤집어 그린다. 팔·다리가 본인 오른쪽 기준 한 장이고
 * 왼쪽은 이 뒤집기로 만든다. 뒤집는 것은 이미지뿐이고 점은 이미 뒤집힌 좌표로 들어온다.
 * 컨테이너째 뒤집으면 점의 접근성 라벨까지 반전된다.
 */
@Composable
internal fun BodyMapCanvas(
    image: BodyMapImage,
    dots: List<BodyMapDot>,
    onDotClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val fitted = fitInside(image.aspectRatio, maxWidth, maxHeight)
        Box(modifier = Modifier.size(fitted)) {
            Image(
                painter = painterResource(image.res),
                contentDescription = null,
                modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
                contentScale = ContentScale.Fit,
            )
            dots.forEach { dot ->
                BodyMapDotButton(
                    dot = dot,
                    onClick = { onDotClick(dot.id) },
                    modifier =
                    Modifier.offset(
                        x = fitted.width * dot.x - MedicalMateSize.touchMin / 2,
                        y = fitted.height * dot.y - MedicalMateSize.touchMin / 2,
                    ),
                )
            }
        }
    }
}

/**
 * 종횡비를 지키면서 [maxWidth] x [maxHeight] 안에 넣는 크기.
 *
 * 인체도는 세로로 길어서 대개 높이가 먼저 찬다. 폭이 먼저 차는 경우(가슴처럼 납작한
 * 이미지)도 있어서 두 쪽을 다 본다.
 */
private fun fitInside(aspectRatio: Float, maxWidth: Dp, maxHeight: Dp): DpSize {
    val widthAtFullHeight = maxHeight * aspectRatio
    return if (widthAtFullHeight <= maxWidth) {
        DpSize(widthAtFullHeight, maxHeight)
    } else {
        DpSize(maxWidth, maxWidth / aspectRatio)
    }
}

/**
 * 점 하나. 48dp 조작 영역 안에 22dp 동심원을 그린다.
 *
 * 고르지 않은 점은 흐린 회색이고 고른 점은 브랜드색에 흰 테를 두른다. 이미지 위라
 * 배경색이 부위마다 달라서, 고른 점만 흰 테로 경계를 만든다.
 *
 * 이미지 위의 좌표 조작은 스크린 리더로 쓸 수 없다. 그래서 점 하나하나가 부위 이름을
 * 라벨로 가진 선택 가능한 버튼이다. 목록으로 고르는 길은 [BodyMapPartList]에 따로 있다.
 */
@Composable
private fun BodyMapDotButton(dot: BodyMapDot, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .size(MedicalMateSize.touchMin)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = dot.label
                role = Role.Button
                selected = dot.selected
            },
        contentAlignment = Alignment.Center,
    ) {
        val colors = MedicalMateTheme.colors
        val halo = if (dot.selected) colors.bgSurface else colors.fgDefault.copy(alpha = HALO_ALPHA)
        val core = if (dot.selected) colors.bgPrimary else colors.fgDefault.copy(alpha = CORE_ALPHA)
        Box(
            modifier =
            Modifier
                .size(DotHaloSize)
                .background(halo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(DotCoreSize).background(core, CircleShape))
        }
    }
}

/** Figma `Point` 마스터가 22x22이고 안쪽 `Core`가 14x14다. */
private val DotHaloSize = 22.dp
private val DotCoreSize = 14.dp

/** 마스터의 `ink-10` · `ink-28`. 인체도가 밝아서 불투명 회색은 너무 무겁다. */
private const val HALO_ALPHA = 0.10f
private const val CORE_ALPHA = 0.28f
