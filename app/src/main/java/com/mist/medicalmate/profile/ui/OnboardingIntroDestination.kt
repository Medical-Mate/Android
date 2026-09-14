package com.mist.medicalmate.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 온보딩 v2. 로그인 뒤 온보딩이 필요할 때의 목적지다. */
@Serializable
internal data object OnboardingIntroDestination

/**
 * 그래프 등록.
 *
 * 네 장이 한 목적지 안에서 넘어간다. 장마다 목적지를 두면 백스택이 네 겹 쌓이고, 신상정보로
 * 나간 뒤에도 그 네 장이 뒤에 남는다.
 *
 * [onDoneClick]은 신상정보(1b-1)로 간다. 건너뛰기도 같은 자리로 나간다 — 건너뛰는 것은
 * 소개 네 장이지 그다음 신상정보가 아니다.
 */
internal fun NavGraphBuilder.onboardingIntroDestination(onDoneClick: () -> Unit) {
    composable<OnboardingIntroDestination> {
        OnboardingRoute(onDoneClick = onDoneClick)
    }
}

/**
 * 온보딩의 진입점.
 *
 * 장 번호를 화면 상태로 든다. ViewModel을 두지 않는 이유는 남길 것도 불러올 것도 없어서다.
 * 화면이 다시 만들어져도 보던 장에 머물도록 [rememberSaveable]을 쓴다.
 */
@Composable
private fun OnboardingRoute(onDoneClick: () -> Unit) {
    var index by rememberSaveable { mutableStateOf(0) }
    var forward by rememberSaveable { mutableStateOf(true) }
    val pages = remember { OnboardingPage.entries }
    val page = pages[index]

    // 뒤로가기로 앞 장을 되짚는다(#227). 상단 바에 뒤로가기를 두지 않는 것은 시안대로이지만,
    // 기기 뒤로가기까지 막으면 잘못 넘긴 장을 다시 볼 길이 없다.
    //
    // **첫 장에서는 아무 일도 하지 않는다.** 그냥 두면 로그인 화면으로 나가는데, 이미 로그인한
    // 사람을 로그인 화면에 세우는 것이라 로그아웃된 것으로 읽힌다. 소개를 끝내거나 건너뛰어야
    // 앞으로 간다.
    BackHandler {
        if (index > 0) {
            forward = false
            index -= 1
        }
    }

    OnboardingScreen(
        page = page,
        onNextClick = {
            if (page.isLast) {
                onDoneClick()
            } else {
                forward = true
                index += 1
            }
        },
        onSkipClick = onDoneClick,
        forward = forward,
    )
}
