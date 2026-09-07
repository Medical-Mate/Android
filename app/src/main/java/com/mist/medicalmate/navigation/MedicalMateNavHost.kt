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
import com.mist.medicalmate.auth.ui.loginDestination
import com.mist.medicalmate.home.ui.AccountActionCallbacks
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.home.ui.homeDestination
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
 */
@Composable
internal fun MedicalMateNavHost(
    signedIn: Boolean,
    onAuthenticated: (onboardingRequired: Boolean) -> Unit,
    accountActions: AccountActionCallbacks,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (signedIn) HomeDestination else LoginDestination,
        modifier = modifier,
    ) {
        loginDestination(onAuthenticated = onAuthenticated)
        homeDestination(accountActions = accountActions)
    }

    SessionBoundarySync(navController = navController, signedIn = signedIn)
}

/**
 * 세션 경계가 바뀌면 그래프를 그 화면으로 되돌린다.
 *
 * 최초 값은 `startDestination`이 이미 반영했으므로 [drop] 1로 건너뛴다. 그러지 않으면
 * 시작 목적지로 한 번 더 navigate가 나간다.
 *
 * [rememberUpdatedState]를 거치는 이유는 [snapshotFlow]가 스냅샷 상태만 관찰하기
 * 때문이다. `signedIn` 파라미터를 그대로 읽으면 [LaunchedEffect]가 처음 실행될 때의
 * 값에 고정돼 로그아웃을 놓친다.
 */
@Composable
private fun SessionBoundarySync(navController: NavHostController, signedIn: Boolean) {
    val currentSignedIn by rememberUpdatedState(signedIn)

    LaunchedEffect(navController) {
        snapshotFlow { currentSignedIn }
            .drop(1)
            .collect { nowSignedIn ->
                navController.resetTo(if (nowSignedIn) HomeDestination else LoginDestination)
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
