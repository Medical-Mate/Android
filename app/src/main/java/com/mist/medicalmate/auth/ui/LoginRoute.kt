package com.mist.medicalmate.auth.ui

import androidx.compose.runtime.Composable
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
 */
@Composable
fun LoginRoute(modifier: Modifier = Modifier, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kakaoLoginClient = remember { KakaoLoginClient() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
