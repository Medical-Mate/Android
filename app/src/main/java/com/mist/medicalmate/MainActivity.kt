package com.mist.medicalmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mist.medicalmate.auth.ui.AccountActionState
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.auth.ui.SessionViewModel
import com.mist.medicalmate.auth.ui.SplashScreen
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.home.ui.AccountActionCallbacks
import com.mist.medicalmate.home.ui.WithdrawFailedDialog
import com.mist.medicalmate.navigation.MedicalMateNavHost
import com.mist.medicalmate.profile.ui.OnboardingGateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalMateTheme {
                MedicalMateApp()
            }
        }
    }
}

/**
 * 세션 확인이 끝나기 전에는 그래프를 세우지 않는다.
 *
 * `NavHost`의 `startDestination`은 첫 조합에서만 읽힌다. 복구 중에 그래프를 세우면
 * 시작 목적지를 로그인으로 잡아놓고 복구 성공 후 홈으로 옮기게 되고, 자동 로그인에서
 * 로그인 화면이 한 번 스쳐 보인다.
 *
 * [SessionViewModel]만 Activity 스코프로 둔다. 세션은 화면이 아니라 앱 전체의
 * 상태다. 화면 ViewModel은 `MedicalMateNavHost` 안에서 목적지 스코프를 갖는다.
 *
 * 로그아웃·탈퇴는 `auth` 소관이라 [SessionViewModel]이 수행하고 홈에는 콜백만
 * 내려보낸다. 홈이 `auth`를 직접 참조하지 않게 하려는 것이다.
 *
 * 온보딩이 필요하면 온보딩 인트로(1a-2)로 보낸다. 그 판단은 `MedicalMateNavHost`가
 * 세션 상태와 온보딩 기록에서 한다. 기록을 읽는 동안에도 스플래시를 유지한다. 기본값을
 * 정해 두고 시작하면 이미 마친 사람에게 온보딩이 한 프레임 스친다.
 *
 * 스플래시(1a-1)는 `Scaffold` 밖에서 그린다. 브랜드 면이 화면 끝까지 닿아야 하는데
 * `innerPadding`을 받으면 상태바 자리에 흰 띠가 남는다. 나머지 화면은 시스템 바를 피해야
 * 해서 `Scaffold`를 유지한다.
 */
@Composable
private fun MedicalMateApp(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    onboardingGateViewModel: OnboardingGateViewModel = hiltViewModel(),
) {
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val accountAction by sessionViewModel.accountAction.collectAsStateWithLifecycle()
    val onboardingCompleted by onboardingGateViewModel.completed.collectAsStateWithLifecycle()

    if (session == SessionUiState.Checking || onboardingCompleted == null) {
        SplashScreen(modifier = Modifier.fillMaxSize())
        return
    }

    // 인셋을 한 곳에서 합친다. safeDrawing이 시스템 바와 키보드, 컷아웃을 합집합으로
    // 계산해 준다. imePadding과 화면별 safe-area 여백을 각각 두면 키보드 위에 여백이
    // 두 겹으로 남는다.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        MedicalMateNavHost(
            session = session,
            onboardingCompleted = onboardingCompleted == true,
            onAuthenticated = sessionViewModel::onSignedIn,
            onOnboardingCompleted = {
                sessionViewModel.onOnboardingCompleted()
                onboardingGateViewModel.markCompleted()
            },
            accountActions =
            AccountActionCallbacks(
                enabled = accountAction != AccountActionState.InProgress,
                onLogoutClick = sessionViewModel::logout,
                onWithdrawClick = sessionViewModel::withdraw,
            ),
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (accountAction == AccountActionState.WithdrawFailed) {
        WithdrawFailedDialog(onDismiss = sessionViewModel::onAccountActionFailureAcknowledged)
    }
}
