package com.mist.medicalmate.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * 와이어프레임 1n. 로그인 이후의 시작 목적지다.
 *
 * [justRegistered]는 신상정보 등록(1b)을 막 마치고 온 경우다. 홈이 등록 완료 토스트를
 * 띄운다. 화면에 값을 들려 보내는 대신 라우트에 담는 이유는, 이 사실이 "어디서 왔는지"에
 * 달려 있고 화면이 다시 만들어질 때 살아 있어야 하기 때문이다.
 */
@Serializable
internal data class HomeDestination(val justRegistered: Boolean = false)

/**
 * 그래프 등록. 홈에서 뻗어나가는 목적지(문답 1l, 카드 1e, 캘린더, 가족 공유함)가 아직 없다.
 * [HomeRoute]의 해당 콜백은 기본값인 빈 동작으로 남기고, 화면이 생길 때 여기서
 * `navController.navigate(...)`를 연결한다.
 */
internal fun NavGraphBuilder.homeDestination(accountActions: AccountActionCallbacks) {
    composable<HomeDestination> { entry ->
        val destination = entry.toRoute<HomeDestination>()
        HomeRoute(
            accountActions = accountActions,
            justRegistered = destination.justRegistered,
        )
    }
}
