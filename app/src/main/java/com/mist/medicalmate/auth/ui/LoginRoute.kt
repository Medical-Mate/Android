package com.mist.medicalmate.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mist.medicalmate.auth.data.KakaoLoginClient
import kotlinx.coroutines.launch

/**
 * 로그인 화면의 상태 있는 진입점.
 *
 * 카카오 SDK가 Activity Context를 요구하므로 SDK 호출을 이 계층이 담당한다.
 * [KakaoLoginClient]를 Hilt로 주입하지 않는 이유도 같다. ViewModel에 Context가
 * 들어가면 상태 로직을 JVM에서 검증할 수 없다.
 *
 * [onAuthenticated]는 토큰을 넘기지 않는다. 서버 JWT는 ViewModel과 저장소가 다루고,
 * 자격증명을 UI 콜백으로 올려보낼 이유가 없다. 넘기는 것은 온보딩이 필요한지 여부뿐이다.
 */
@Composable
fun LoginRoute(
    onAuthenticated: (onboardingRequired: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kakaoLoginClient = remember { KakaoLoginClient() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val current = state
        if (current is LoginUiState.Authenticated) {
            // 소비 표시를 먼저 남긴다. onAuthenticated가 화면을 홈으로 바꾸면 이
            // 효과가 취소되므로, 뒤에 두면 상태가 Authenticated로 남아 다음 로그아웃
            // 때 로그인 화면이 열리자마자 다시 홈으로 튕긴다.
            viewModel.onAuthenticationHandled()
            onAuthenticated(current.onboardingRequired)
        }
    }

    LoginScreen(
        state = state,
        onKakaoLoginClick = {
            viewModel.onLoginStarted()
            scope.launch {
                viewModel.onLoginResult(kakaoLoginClient.login(context))
            }
        },
        modifier = modifier,
    )
}
