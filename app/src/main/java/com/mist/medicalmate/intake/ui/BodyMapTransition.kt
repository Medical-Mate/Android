package com.mist.medicalmate.intake.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext

/**
 * 앵커에서 구역으로 넘어갈 때의 확대.
 *
 * 전신 이미지와 확대 이미지는 서로 다른 파일이라 하나에서 다른 하나로 이어 확대할 수
 * 없다. 대신 **짚은 점을 축으로 전신을 밀어내고** 확대 이미지를 조금 큰 상태에서 제자리로
 * 들여보낸다. 카메라가 그 점으로 들어가는 것처럼 읽힌다.
 *
 * 축을 짚은 점에 두는 것이 이 효과의 핵심이다. 가운데를 축으로 하면 어느 부위를 짚어도
 * 같은 움직임이 나와서 확대라기보다 화면 교체로 보인다.
 *
 * 판 높이가 부위마다 달라서 크기는 애니메이션하지 않는다([SizeTransform]의 `snap`).
 * 높이까지 움직이면 아래 여백이 함께 늘었다 줄어든다.
 *
 * 기기에서 애니메이션을 끈 사람에게는 즉시 바뀐다. 멀미나 주의력 문제로 끄는 설정이고,
 * 그 뜻을 화면이 무시하면 안 된다.
 */
@Composable
internal fun BodyMapCardTransition(
    screen: BodyMapScreen,
    zoomOrigin: TransformOrigin,
    modifier: Modifier = Modifier,
    content: @Composable (BodyMapScreen) -> Unit,
) {
    val animated = bodyMapAnimationsEnabled()
    AnimatedContent(
        targetState = screen,
        modifier = modifier,
        transitionSpec = { cardTransform(animated, zoomOrigin) },
        label = "body-map-card",
    ) { target ->
        content(target)
    }
}

private fun AnimatedContentTransitionScope<BodyMapScreen>.cardTransform(
    animated: Boolean,
    zoomOrigin: TransformOrigin,
): ContentTransform {
    val keepSize = SizeTransform(clip = false) { _, _ -> snap() }
    if (!animated) {
        return (fadeIn(snap()) togetherWith fadeOut(snap())).using(keepSize)
    }
    val zoomingIn = initialState == BodyMapScreen.ANCHOR && targetState == BodyMapScreen.ZONE
    val zoomingOut = initialState == BodyMapScreen.ZONE && targetState == BodyMapScreen.ANCHOR
    val spec = tween<Float>(durationMillis = DURATION_MILLIS)
    val transform =
        when {
            zoomingIn ->
                (fadeIn(spec) + scaleIn(spec, initialScale = DETAIL_ENTER_SCALE)) togetherWith
                    (fadeOut(spec) + scaleOut(spec, targetScale = BODY_EXIT_SCALE, transformOrigin = zoomOrigin))

            zoomingOut ->
                (
                    fadeIn(
                        spec,
                    ) + scaleIn(spec, initialScale = BODY_EXIT_SCALE, transformOrigin = zoomOrigin)
                    ) togetherWith
                    (fadeOut(spec) + scaleOut(spec, targetScale = DETAIL_ENTER_SCALE))

            else -> fadeIn(spec) togetherWith fadeOut(spec)
        }
    return transform.using(keepSize)
}

/**
 * 짚은 점을 판 안의 비율로 바꾼다. [TransformOrigin]이 0..1을 받는다.
 *
 * 이미지는 판 안에 종횡비를 지켜 가운데 놓이므로 남는 폭이 있다. 점의 위치는 그 여백을
 * 더한 값이라 이미지 기준 좌표를 그대로 쓸 수 없다.
 */
internal fun bodyMapZoomOrigin(point: BodyMapPoint?, imageWidth: Float, cardWidth: Float): TransformOrigin {
    if (point == null || cardWidth <= 0f) return TransformOrigin.Center
    val inset = (cardWidth - imageWidth) / 2f
    return TransformOrigin(pivotFractionX = (inset + imageWidth * point.x) / cardWidth, pivotFractionY = point.y)
}

/**
 * 기기에서 애니메이션이 켜져 있는지.
 *
 * 안드로이드에 `prefers-reduced-motion`에 해당하는 API가 없어서 개발자 옵션의 애니메이션
 * 배율을 본다. 0이면 사용자가 전환 효과를 끈 것이다.
 */
@Composable
private fun bodyMapAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

private const val DURATION_MILLIS = 240

/** 밀려나는 전신 이미지가 커지는 배율. 짚은 점이 화면을 채우며 사라진다. */
private const val BODY_EXIT_SCALE = 1.6f

/** 들어오는 확대 이미지가 시작하는 배율. 조금 큰 상태에서 제자리로 좁혀진다. */
private const val DETAIL_ENTER_SCALE = 1.12f
