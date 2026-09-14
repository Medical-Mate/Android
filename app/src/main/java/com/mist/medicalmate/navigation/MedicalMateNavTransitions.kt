package com.mist.medicalmate.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mist.medicalmate.calendar.ui.CalendarDestination
import com.mist.medicalmate.card.ui.RecordDestination
import com.mist.medicalmate.home.ui.HomeDestination

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
 * **가장자리를 쓸어 돌아가는 것도 같은 전환을 쓴다.** 돌아가는 길이 하나여야 한다.
 *
 * **탭끼리 오갈 때는 아무것도 하지 않는다.** 세 탭은 서로의 형제이고 백스택에도 쌓이지
 * 않는다. 미는 전환은 들어가고 나오는 깊이를 말하는 것이라, 같은 층을 옮기는 데 쓰면
 * 캘린더에서 기록으로 가는 것이 한 단계 들어가는 것처럼 보인다.
 *
 * 디자인 문서에 화면 전환 규격이 없다. 시간과 easing은 Material의 표준 전환 값이다.
 */
internal object MedicalMateNavTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs()) {
            EnterTransition.None
        } else {
            slideInHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) { it } +
                fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs()) {
            ExitTransition.None
        } else {
            slideOutHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) {
                -it / PARALLAX
            } + fadeOut(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs()) {
            EnterTransition.None
        } else {
            slideInHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) {
                -it / PARALLAX
            } + fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs()) {
            ExitTransition.None
        } else {
            slideOutHorizontally(animationSpec = tween(DURATION, easing = FastOutSlowInEasing)) { it } +
                fadeOut(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))
        }
    }

    /** 떠나는 곳과 닿는 곳이 모두 탭인지. */
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
        initialState.destination.isTab() && targetState.destination.isTab()

    private fun NavDestination.isTab(): Boolean = hasRoute(HomeDestination::class) ||
        hasRoute(CalendarDestination::class) ||
        hasRoute(RecordDestination::class)

    /**
     * 가장자리를 쓸어 돌아갈 때.
     *
     * 버튼으로 돌아갈 때와 같은 것을 쓴다. 쓸어내는 동안에는 이 전환이 손가락 위치만큼만
     * 진행되므로, 화면이 손을 따라 오른쪽으로 밀리고 이전 화면이 왼쪽에서 따라 들어온다.
     * 놓으면 남은 만큼이 이어서 돈다.
     *
     * 돌아가는 길이 하나여야 한다. 쓸어서 돌아간 것과 눌러서 돌아간 것이 다르게 움직이면
     * 같은 동작이 두 가지로 보인다. 기본값이 그 자리에 축소를 걸어 두는데, 화면이 작아져
     * 사라지는 것으로 읽혀서 걷어냈다.
     */
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        popEnter()
    }

    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        popExit()
    }

    /** Material의 표준 전환 시간. 기본값 700은 한 화면 넘기는 데 길다. */
    private const val DURATION = 320

    /** 물러나는 화면이 움직이는 몫. 앞의 화면이 네 배 더 움직인다. */
    private const val PARALLAX = 4
}
