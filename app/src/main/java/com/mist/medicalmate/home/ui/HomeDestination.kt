package com.mist.medicalmate.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
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
 * [onIntakeClick]에 세션 id가 함께 간다. 증상 정리 시작하기는 `null`, 이어서 하기는 서버에
 * 남은 문답의 id다. 같은 화면으로 가고 그 화면이 id를 보고 불러온다.
 */
internal fun NavGraphBuilder.homeDestination(
    onIntakeClick: (String?) -> Unit,
    onCardClick: (String) -> Unit,
    onAllCardsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
) {
    composable<HomeDestination> { entry ->
        val destination = entry.toRoute<HomeDestination>()
        HomeRoute(
            justRegistered = destination.justRegistered,
            callbacks =
            HomeCallbacks(
                onStartIntakeClick = { onIntakeClick(null) },
                onResumeClick = onIntakeClick,
                onSavedCardClick = onCardClick,
                onAllCardsClick = onAllCardsClick,
                onProfileClick = onProfileClick,
            ),
            onTabSelect = onTabSelect,
        )
    }
}
