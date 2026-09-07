package com.mist.medicalmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mist.medicalmate.auth.ui.AccountActionState
import com.mist.medicalmate.auth.ui.LoginRoute
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.auth.ui.SessionViewModel
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.home.ui.AccountActionCallbacks
import com.mist.medicalmate.home.ui.HomeRoute
import com.mist.medicalmate.home.ui.WithdrawFailedDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MedicalMateApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * 세션 상태로 첫 화면을 가른다.
 *
 * 목적지가 둘뿐이라 임시 분기로 둔다. 화면이 늘면 NavHost로 바꾼다.
 * 네비게이션 라이브러리 선택이 아직 미정이라 여기서 미리 정하지 않는다.
 *
 * `onboardingRequired`가 true여도 온보딩 화면(1a·1b)이 없어 홈으로 보낸다.
 * 값을 받아두는 것은 온보딩이 들어올 자리를 남기려는 것이다.
 *
 * 로그아웃·탈퇴는 `auth` 소관이라 `SessionViewModel`이 수행하고, 홈에는 콜백만
 * 내려보낸다. 홈이 `auth` 도메인을 직접 참조하지 않게 하려는 것이다.
 */
@Composable
private fun MedicalMateApp(modifier: Modifier = Modifier, sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val accountAction by sessionViewModel.accountAction.collectAsStateWithLifecycle()

    when (session) {
        SessionUiState.Checking -> CheckingContent(modifier)

        SessionUiState.SignedOut ->
            LoginRoute(
                onAuthenticated = sessionViewModel::onSignedIn,
                modifier = modifier,
            )

        is SessionUiState.SignedIn ->
            HomeRoute(
                accountActions =
                AccountActionCallbacks(
                    enabled = accountAction != AccountActionState.InProgress,
                    onLogoutClick = sessionViewModel::logout,
                    onWithdrawClick = sessionViewModel::withdraw,
                ),
                modifier = modifier,
            )
    }

    if (accountAction == AccountActionState.WithdrawFailed) {
        WithdrawFailedDialog(onDismiss = sessionViewModel::onAccountActionFailureAcknowledged)
    }
}

@Composable
private fun CheckingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}
