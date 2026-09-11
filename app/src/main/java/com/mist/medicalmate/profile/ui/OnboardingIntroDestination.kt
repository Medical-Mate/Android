package com.mist.medicalmate.profile.ui

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
 * 네 장이 한 목적지 안에서 넘어간다. 장마다 목적지를 두면 뒤로 가기로 장을 되짚을 수 있는데,
 * 시안에 뒤로 가는 길이 없고 마지막 장을 지나면 돌아올 자리도 없다.
 *
 * [onDoneClick]은 신상정보(1b-1)로 간다.
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
    val pages = remember { OnboardingPage.entries }
    val page = pages[index]

    OnboardingScreen(
        page = page,
        onNextClick = { if (page.isLast) onDoneClick() else index += 1 },
    )
}
