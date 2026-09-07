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
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.auth.ui.SessionViewModel
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MedicalMateApp(modifier = Modifier.padding(innerPadding))
                }
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
 * `onboardingRequired`가 true여도 온보딩 화면(1a·1b)이 없어 홈으로 보낸다. 화면이
 * 생기면 `MedicalMateNavHost`의 시작 목적지 계산에 이 값이 들어간다.
 */
@Composable
private fun MedicalMateApp(modifier: Modifier = Modifier, sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val accountAction by sessionViewModel.accountAction.collectAsStateWithLifecycle()

    when (session) {
        SessionUiState.Checking -> CheckingContent(modifier)

        SessionUiState.SignedOut, is SessionUiState.SignedIn ->
            MedicalMateNavHost(
                signedIn = session is SessionUiState.SignedIn,
                onAuthenticated = sessionViewModel::onSignedIn,
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
