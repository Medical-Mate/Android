package com.mist.medicalmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mist.medicalmate.auth.ui.LoginDestination
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.auth.ui.loginDestination
import com.mist.medicalmate.card.ui.BriefCardDestination
import com.mist.medicalmate.card.ui.HandoffDestination
import com.mist.medicalmate.card.ui.briefCardDestination
import com.mist.medicalmate.card.ui.handoffDestination
import com.mist.medicalmate.home.ui.AccountActionCallbacks
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.home.ui.homeDestination
import com.mist.medicalmate.intake.ui.IntakeDestination
import com.mist.medicalmate.intake.ui.intakeDestination
import com.mist.medicalmate.profile.ui.OnboardingIntroDestination
import com.mist.medicalmate.profile.ui.ProfileCompleteDestination
import com.mist.medicalmate.profile.ui.ProfileSetupDestination
import com.mist.medicalmate.profile.ui.onboardingIntroDestination
import com.mist.medicalmate.profile.ui.profileCompleteDestination
import com.mist.medicalmate.profile.ui.profileSetupDestination
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
    ) {
        loginDestination(onAuthenticated = onAuthenticated)
        onboardingIntroDestination(
            onStartClick = { navController.navigate(ProfileSetupDestination) },
        )
        profileSetupDestination(
            onCompleted = { navController.navigate(ProfileCompleteDestination) },
            onExit = { navController.popBackStack() },
        )
        profileCompleteDestination(
            onFinished = {
                onOnboardingCompleted()
                navController.resetTo(HomeDestination(justRegistered = true))
            },
        )
        intakeDestination(
            onCompleted = { navController.navigate(BriefCardDestination(cardId = NEW_CARD_ID)) },
            onExit = { navController.popBackStack() },
        )
        briefCardDestination(
            onSaved = { navController.resetTo(HomeDestination()) },
            onHandoff = { cardId -> navController.navigate(HandoffDestination(cardId)) },
            onExit = { navController.popBackStack() },
        )
        handoffDestination(onDone = { navController.popBackStack() })
        homeDestination(
            accountActions = accountActions,
            onStartIntakeClick = { navController.navigate(IntakeDestination) },
            onCardClick = { cardId -> navController.navigate(BriefCardDestination(cardId)) },
        )
    }

    SessionBoundarySync(
        navController = navController,
        session = session,
        onboardingCompleted = onboardingCompleted,
    )
}

/**
 * 방금 만든 카드의 임시 id.
 *
 * 문답을 마치면 서버가 카드를 만들고 그 id를 준다. 그 호출이 아직 없어서 자리만 채운다.
 * 카드 화면은 지금 id를 보지 않고 픽스처를 그린다.
 */
private const val NEW_CARD_ID = "new"

/**
 * 세션 상태가 가리키는 목적지.
 *
 * [SessionUiState.Checking]은 `MainActivity`가 스플래시로 잡아서 여기까지 오지 않는다.
 * 로그인과 같이 두는 것은 분기를 하나 더 만들지 않으려는 것이고, 그래도 온다면 인증이
 * 필요한 화면을 열지 않는 쪽이 안전하다.
 *
 * 온보딩은 가입하고 한 번만 나온다. 서버가 필요하다고 해도 기기에 마쳤다는 기록이 있으면
 * 홈으로 보낸다. 두 값을 함께 보는 이유는 `profile/data/OnboardingStore`에 적었다.
 */
private fun SessionUiState.destination(onboardingCompleted: Boolean): Any = when (this) {
    SessionUiState.Checking, SessionUiState.SignedOut -> LoginDestination
    is SessionUiState.SignedIn ->
        if (onboardingRequired && !onboardingCompleted) {
            OnboardingIntroDestination
        } else {
            HomeDestination()
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
 * 백스택을 비우고 [destination]만 남긴다.
 *
 * 로그인·로그아웃은 되돌아갈 수 없어야 한다. 뒤로 가기로 로그아웃 전 화면이 나오면
 * 인증이 끝난 화면을 인증 없이 보게 된다.
 */
private fun NavHostController.resetTo(destination: Any) {
    navigate(destination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
