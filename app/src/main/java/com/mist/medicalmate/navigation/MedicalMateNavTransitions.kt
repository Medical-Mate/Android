package com.mist.medicalmate.navigation

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * 화면 전환.
 *
 * navigation-compose의 기본값은 0.7초 크로스페이드다. 화면이 어느 쪽으로 갔는지가 남지
 * 않아서 되짚는 느낌이 없고, 0.7초는 한 화면 넘기는 데 길다.
 *
 * 가로로 민다. 새 화면이 오른쪽에서 들어오고 물러나는 화면은 반대쪽으로 [PARALLAX]분의 1만
 * 움직인다. 둘이 같은 거리를 움직이면 두 장이 나란히 흐르는 것으로 보이고, 뒤에 있는 것이
 * 덜 움직여야 앞뒤가 읽힌다. 되돌아올 때는 그대로 뒤집는다.
 *
 * 디자인 문서에 화면 전환 규격이 없다. 시간과 easing은 Material의 표준 전환 값이다.
 */
internal object MedicalMateNavTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) { it } +
            fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) {
            -it / PARALLAX
        } + fadeOut(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) {
            -it / PARALLAX
        } + fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) { it } +
            fadeOut(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
    }

    /**
     * 가장자리를 쓸어 돌아가는 동안 아래에서 드러나는 화면.
     *
     * 아무 것도 걸지 않는다. 기본값은 여기에 `fadeIn`을 걸어서, 위 화면이 줄어드는 동안
     * 아래가 비어 보이다가 뒤늦게 떠오른다. 손가락을 따라 위가 벗겨지고 아래가 그대로
     * 있어야 덮여 있던 것으로 읽힌다.
     */
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        EnterTransition.None
    }

    /**
     * 가장자리를 쓸어 돌아가는 동안 벗겨지는 화면.
     *
     * 줄이면서 쓸어낸 쪽으로 민다. platform의 예측 뒤로가기와 같은 방향이다. 기본값은
     * 0.7까지 줄이는데 그러면 화면이 작아져 사라지는 것으로 읽힌다. [PREDICTIVE_SCALE]까지만
     * 줄여서 뒤로 물러나는 것에 가깝게 둔다.
     */
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { edge ->
        scaleOut(targetScale = PREDICTIVE_SCALE) +
            slideOutHorizontally { width ->
                if (edge == BackEventCompat.EDGE_LEFT) width / PREDICTIVE_SHIFT else -width / PREDICTIVE_SHIFT
            }
    }

    /** Material의 표준 전환 시간. 기본값 700은 한 화면 넘기는 데 길다. */
    private const val DURATION = 320

    /** 물러나는 화면이 움직이는 몫. 앞의 화면이 네 배 더 움직인다. */
    private const val PARALLAX = 4

    private const val PREDICTIVE_SCALE = 0.9f

    /** 쓸어낸 쪽으로 미는 몫. 폭의 1/16이다. */
    private const val PREDICTIVE_SHIFT = 16
}
