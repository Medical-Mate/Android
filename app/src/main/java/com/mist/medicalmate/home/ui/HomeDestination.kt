package com.mist.medicalmate.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 1n. 로그인 이후의 시작 목적지다. */
@Serializable
internal data object HomeDestination

/**
 * 그래프 등록. 홈에서 뻗어나가는 목적지(문답 1l, 카드 1e, 캘린더, 가족 공유함)가 아직 없다.
 * [HomeRoute]의 해당 콜백은 기본값인 빈 동작으로 남기고, 화면이 생길 때 여기서
 * `navController.navigate(...)`를 연결한다.
 */
internal fun NavGraphBuilder.homeDestination(accountActions: AccountActionCallbacks) {
    composable<HomeDestination> {
        HomeRoute(accountActions = accountActions)
    }
}
