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
 * 그래프 등록. 아직 없는 목적지(카드 1e, 기록 1j, 캘린더 1r, 내 정보 1s)의 콜백은 기본값인
 * 빈 동작으로 남기고, 화면이 생길 때 여기서 `navController.navigate(...)`를 연결한다.
 *
 * [onStartIntakeClick]은 증상 정리 시작하기와 이어서 하기가 함께 쓴다. 이어서 하기는
 * 저장된 진행 상태를 불러와야 하는데 그 저장이 아직 없어서 지금은 같은 곳으로 간다(#69).
 */
internal fun NavGraphBuilder.homeDestination(accountActions: AccountActionCallbacks, onStartIntakeClick: () -> Unit) {
    composable<HomeDestination> { entry ->
        val destination = entry.toRoute<HomeDestination>()
        HomeRoute(
            accountActions = accountActions,
            justRegistered = destination.justRegistered,
            callbacks =
            HomeCallbacks(
                onStartIntakeClick = onStartIntakeClick,
                onResumeClick = onStartIntakeClick,
            ),
        )
    }
}
