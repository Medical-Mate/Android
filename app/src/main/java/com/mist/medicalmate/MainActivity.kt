package com.mist.medicalmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * 세션 상태에서 한다.
 *
 * 스플래시(1a-1)는 `Scaffold` 밖에서 그린다. 브랜드 면이 화면 끝까지 닿아야 하는데
 * `innerPadding`을 받으면 상태바 자리에 흰 띠가 남는다. 나머지 화면은 시스템 바를 피해야
 * 해서 `Scaffold`를 유지한다.
 */
@Composable
private fun MedicalMateApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val accountAction by sessionViewModel.accountAction.collectAsStateWithLifecycle()

    if (session == SessionUiState.Checking) {
        SplashScreen(modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MedicalMateNavHost(
            session = session,
            onAuthenticated = sessionViewModel::onSignedIn,
            onOnboardingCompleted = sessionViewModel::onOnboardingCompleted,
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
