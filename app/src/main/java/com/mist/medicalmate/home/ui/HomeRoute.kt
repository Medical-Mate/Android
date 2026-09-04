package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 홈 화면의 상태 있는 진입점.
 *
 * 하단 캘린더·가족 공유함과 카드 열기는 목적지가 아직 없어 호출자가 빈 동작을 준다.
 * 네비게이션이 들어오면 이 자리에 navigate 람다가 연결된다.
 */
@Composable
fun HomeRoute(
    onStartIntakeClick: () -> Unit = {},
    onSavedCardClick: (String) -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onFamilyShareClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    HomeScreen(
        state = state,
        onStartIntakeClick = onStartIntakeClick,
        onSavedCardClick = onSavedCardClick,
        onCalendarClick = onCalendarClick,
        onFamilyShareClick = onFamilyShareClick,
        onRetryClick = viewModel::refresh,
        modifier = modifier,
    )
}
