package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.speech.rememberMicPermission
import com.mist.medicalmate.core.voice.LocalVoiceTargets
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
    onTabSelect: (MedicalMateTab) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // 배너 5의 "메이트에게 말해보기"(#312). 마이크 권한을 받고 메이트 카드를 띄운다. 거절하면 아무 일도 없다.
    val targets = LocalVoiceTargets.current
    val summonMate = rememberMicPermission(onGranted = { targets?.summon?.invoke() }, onDenied = {})

    val homeCallbacks = callbacks.copy(onRetryClick = viewModel::refresh, onMateClick = summonMate)
    // 이어서 하기 · 다음 진료 · 배너 링크를 말로 부른다(#320).
    HomeVoiceTarget(uiState = viewModel.uiState, callbacks = homeCallbacks)

    HomeScreen(
        state = state,
        today = today,
        callbacks = homeCallbacks,
        modifier = modifier,
        onTabSelect = onTabSelect,
    )
}
