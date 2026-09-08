package com.mist.medicalmate.profile.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 1a-2. 로그인 뒤 온보딩이 필요할 때의 목적지다. */
@Serializable
internal data object OnboardingIntroDestination

/**
 * 그래프 등록.
 *
 * [onStartClick]은 1b-1 신상정보로 가야 하는데 그 화면이 없다. 지금은 호출자가 온보딩을
 * 마친 것으로 처리해서 홈으로 보낸다. 1b가 생기면 여기서 그쪽으로 연결한다.
 */
internal fun NavGraphBuilder.onboardingIntroDestination(onStartClick: () -> Unit) {
    composable<OnboardingIntroDestination> {
        OnboardingIntroScreen(onStartClick = onStartClick)
    }
}
