package com.mist.medicalmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mist.medicalmate.auth.ui.LoginDestination
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.calendar.ui.CalendarDestination
import com.mist.medicalmate.card.ui.BriefCardDestination
import com.mist.medicalmate.card.ui.BriefCardListDestination
import com.mist.medicalmate.card.ui.RecordDestination
import com.mist.medicalmate.card.ui.briefCardDestination
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.home.ui.homeDestination
import com.mist.medicalmate.intake.ui.IntakeDestination
import com.mist.medicalmate.profile.ui.AccountActionCallbacks
import com.mist.medicalmate.profile.ui.MyProfileDestination
import com.mist.medicalmate.profile.ui.OnboardingIntroDestination
import com.mist.medicalmate.visit.ui.HospitalPickDestination
import com.mist.medicalmate.visit.ui.HospitalPickPurpose
import kotlinx.coroutines.flow.drop

/**
 * 앱의 단일 네비게이션 그래프.
 *
 * `navigation`은 조합 루트라서 모든 기능 패키지를 참조해도 된다. 기능끼리 서로를
 * 참조하지 않게 하려고 목적지 연결을 여기로 모은 것이다.
 *
 * **그래프를 로그인·본문으로 쪼개지 않는다.** `NavHost`는 컴포지션을 떠날 때
 * 아무것도 정리하지 않고(`onDispose {}`), 목적지별 `ViewModelStore`는 Activity의
 * 스토어에 얹혀 있다. 그래서 세션 상태로 `NavHost` 자체를 갈아치우면 로그아웃마다
 * ViewModel이 정리되지 않고 쌓인다. 백스택에서 pop될 때만 확실히 정리되므로
 * 경계 이동을 [resetTo]의 `popUpTo(inclusive)`로 처리한다.
 *
 * 이 구조 덕에 화면 ViewModel은 목적지 스코프를 갖는다. 로그아웃하면 홈 엔트리가
 * pop되면서 `HomeViewModel`도 사라지고, 다음 계정으로 로그인했을 때 이전 계정의
 * 데이터가 남지 않는다.
 *
 * 시작 목적지와 경계 이동을 [SessionUiState] 하나로 정한다. 로그인 여부와 온보딩 필요
 * 여부를 각각 받으면 둘 다 참일 수 없는 조합까지 서명에 들어온다.
 */
@Composable
internal fun MedicalMateNavHost(
    session: SessionUiState,
    onboardingCompleted: Boolean,
    onAuthenticated: (onboardingRequired: Boolean) -> Unit,
    onOnboardingCompleted: () -> Unit,
    accountActions: AccountActionCallbacks,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = session.destination(onboardingCompleted),
        modifier = modifier,
        enterTransition = MedicalMateNavTransitions.enter,
        exitTransition = MedicalMateNavTransitions.exit,
        popEnterTransition = MedicalMateNavTransitions.popEnter,
        popExitTransition = MedicalMateNavTransitions.popExit,
        predictivePopEnterTransition = MedicalMateNavTransitions.predictivePopEnter,
        predictivePopExitTransition = MedicalMateNavTransitions.predictivePopExit,
    ) {
        entryDestinations(
            navController = navController,
            session = session,
            onAuthenticated = onAuthenticated,
            onOnboardingCompleted = onOnboardingCompleted,
        )
        intakeDestinations(navController)
        briefCardDestination(
            onSaved = { navController.resetTo(HomeDestination) },
            // 카드의 `변경`. 어느 카드로 돌아갈지 들고 간다.
            onHospitalChange = { cardId ->
                navController.navigate(
                    HospitalPickDestination(purpose = HospitalPickPurpose.BEFORE_VISIT, cardId = cardId),
                )
            },
            // 지운 카드의 화면에 남을 수 없다. 시안이 카드 목록으로 보낸다(1e-1-DC). 홈까지만
            // 걷어내고 목록을 얹어서, 뒤로 가면 홈이 나오고 목록이 두 장 쌓이지 않게 한다.
            onDeleted = {
                navController.navigate(BriefCardListDestination) {
                    popUpTo<HomeDestination> { inclusive = false }
                    launchSingleTop = true
                }
            },
            onExit = { navController.popBackStack() },
        )
        homeDestination(
            // 이어서 하기는 서버가 들고 있는 문답 id를 함께 넘긴다. 새로 시작하면 null이다.
            onIntakeClick = { sessionId ->
                navController.navigate(IntakeDestination(sessionId?.toLongOrNull()))
            },
            onCardClick = { cardId -> navController.navigate(BriefCardDestination(cardId = cardId)) },
            onAllCardsClick = { navController.navigate(BriefCardListDestination) },
            onProfileClick = { navController.navigate(MyProfileDestination) },
            onTabSelect = navController::selectTab,
        )
        recordDestinations(navController)
        profileDestinations(navController = navController, accountActions = accountActions)
        calendarDestinations(navController)
        visitDestinations(navController)
    }

    SessionBoundarySync(
        navController = navController,
        session = session,
        onboardingCompleted = onboardingCompleted,
    )
}

/**
 * 세션 상태가 가리키는 목적지.
 *
 * [SessionUiState.Checking]과 [SessionUiState.RestoreFailed]는 `MainActivity`가 스플래시로
 * 잡아서 여기까지 오지 않는다. 로그인과 같이 두는 것은 분기를 하나 더 만들지 않으려는
 * 것이고, 그래도 온다면 인증이 필요한 화면을 열지 않는 쪽이 안전하다.
 *
 * 온보딩은 가입하고 한 번만 나온다. 서버가 필요하다고 해도 기기에 마쳤다는 기록이 있으면
 * 홈으로 보낸다. 두 값을 함께 보는 이유는 `profile/data/OnboardingStore`에 적었다.
 */
private fun SessionUiState.destination(onboardingCompleted: Boolean): Any = when (this) {
    SessionUiState.Checking, SessionUiState.RestoreFailed, SessionUiState.SignedOut -> LoginDestination
    is SessionUiState.SignedIn ->
        if (onboardingRequired && !onboardingCompleted) {
            OnboardingIntroDestination
        } else {
            HomeDestination
        }
}

/**
 * 세션 경계가 바뀌면 그래프를 그 화면으로 되돌린다.
 *
 * 최초 값은 `startDestination`이 이미 반영했으므로 [drop] 1로 건너뛴다. 그러지 않으면
 * 시작 목적지로 한 번 더 navigate가 나간다.
 *
 * [rememberUpdatedState]를 거치는 이유는 [snapshotFlow]가 스냅샷 상태만 관찰하기
 * 때문이다. `session` 파라미터를 그대로 읽으면 [LaunchedEffect]가 처음 실행될 때의
 * 값에 고정돼 로그아웃을 놓친다.
 *
 * **로그인 여부만 관찰한다.** 목적지 전체를 관찰하면 온보딩을 마치는 순간에도 이동이 한 번
 * 더 나간다. 그 이동은 그래프가 등록 완료 토스트까지 담아 처리했는데, 뒤늦은 재설정이
 * 그것을 덮어써서 토스트가 사라진다. 넘어갈 곳을 정할 때만 온보딩 여부를 본다.
 */
@Composable
private fun SessionBoundarySync(
    navController: NavHostController,
    session: SessionUiState,
    onboardingCompleted: Boolean,
) {
    val currentSession by rememberUpdatedState(session)
    val currentOnboardingCompleted by rememberUpdatedState(onboardingCompleted)

    LaunchedEffect(navController) {
        snapshotFlow { currentSession is SessionUiState.SignedIn }
            .drop(1)
            .collect {
                navController.resetTo(currentSession.destination(currentOnboardingCompleted))
            }
    }
}

/**
 * 하단 탭 이동.
 *
 * 탭은 서로의 형제다. 탭을 옮길 때마다 백스택에 쌓으면 뒤로 가기가 탭 방문 이력을 되짚는다.
 * 시작 목적지까지 pop하고 상태를 저장·복원해서, 탭을 오갔다 돌아오면 스크롤과 고른 날이
 * 남아 있게 한다.
 *
 * `launchSingleTop`은 같은 탭을 다시 눌렀을 때 같은 화면이 두 장 쌓이는 것을 막는다.
 */
internal fun NavHostController.selectTab(tab: MedicalMateTab) {
    val destination =
        when (tab) {
            MedicalMateTab.RECORD -> RecordDestination
            MedicalMateTab.HOME -> HomeDestination
            MedicalMateTab.CALENDAR -> CalendarDestination
        }
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * 백스택을 비우고 [destination]만 남긴다.
 *
 * 로그인·로그아웃은 되돌아갈 수 없어야 한다. 뒤로 가기로 로그아웃 전 화면이 나오면
 * 인증이 끝난 화면을 인증 없이 보게 된다.
 */
internal fun NavHostController.resetTo(destination: Any) {
    navigate(destination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
