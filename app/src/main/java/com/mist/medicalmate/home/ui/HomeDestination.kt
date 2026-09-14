package com.mist.medicalmate.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import kotlinx.serialization.Serializable

/** 와이어프레임 1n. 로그인 이후의 시작 목적지다. */
@Serializable
internal data object HomeDestination

/**
 * 그래프 등록.
 *
 * 나가는 길을 하나씩 받지 않고 [HomeCallbacks]를 통째로 받는다. 목적지가 늘 때마다 이
 * 함수의 서명이 함께 길어지는데, 여기가 하는 일은 그 묶음을 `HomeRoute`에 넘기는 것뿐이라
 * 중간에서 한 번 더 풀어 쓸 이유가 없다.
 *
 * 아직 없는 목적지의 콜백은 호출자가 기본값(빈 동작)으로 둔다.
 */
internal fun NavGraphBuilder.homeDestination(callbacks: HomeCallbacks, onTabSelect: (MedicalMateTab) -> Unit) {
    composable<HomeDestination> {
        HomeRoute(callbacks = callbacks, onTabSelect = onTabSelect)
    }
}
