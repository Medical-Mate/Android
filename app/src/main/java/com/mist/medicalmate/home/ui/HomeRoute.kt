package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/**
 * 홈 화면의 상태 있는 진입점.
 *
 * 목적지가 아직 없는 이동은 호출자가 빈 동작을 준다. 문답(1l), 카드(1e), 기록(1j),
 * 캘린더(1r), 프로필이 생기면 `navigation`에서 navigate 람다를 연결한다.
 *
 * `today`를 `remember`로 한 번만 읽는다. 조합마다 다시 읽으면 D-day가 화면이 살아 있는
 * 동안 바뀔 수 있고, 자정을 넘겨 값이 달라지는 것을 지금 다루지 않는다.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    callbacks: HomeCallbacks = HomeCallbacks(),
    accountActions: AccountActionCallbacks = AccountActionCallbacks(),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    LaunchedEffect(Unit) { viewModel.refresh() }

    HomeScreen(
        state = state,
        today = today,
        callbacks = callbacks.copy(onRetryClick = viewModel::refresh),
        modifier = modifier,
        accountActions = accountActions,
    )
}
