package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * 와이어프레임 1j-3. 기록 목록에서 한 건을 눌러 들어온다.
 *
 * [recordId]를 라우트에 담는다. 어느 건을 여는지가 목적지의 일부다.
 */
@Serializable
internal data class RecordDetailDestination(val recordId: String)

/**
 * 나가는 길은 뒤로가기뿐이다.
 *
 * 전에는 카드 단계에서 브리핑 카드 화면으로 건너갔다. 시안 1j-3-X가 그 자리에서 펴 보는
 * 것으로 바꿔서 나갈 일이 없어졌다. 다른 두 단계의 여는 줄도 목적지가 없어 데이터에 넣지
 * 않았다. 눌러도 아무 일이 없는 줄을 두지 않는다(#79).
 */
internal fun NavGraphBuilder.recordDetailDestination(onBackClick: () -> Unit) {
    composable<RecordDetailDestination> { entry ->
        RecordDetailRoute(
            recordId = entry.toRoute<RecordDetailDestination>().recordId,
            onBackClick = onBackClick,
        )
    }
}

@Composable
private fun RecordDetailRoute(
    recordId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedSteps by viewModel.expandedSteps.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) { viewModel.load(recordId) }

    RecordDetailScreen(
        state = state,
        onBackClick = onBackClick,
        expandedSteps = expandedSteps,
        onExpandToggle = viewModel::onExpandToggle,
        onRetryClick = { viewModel.load(recordId) },
        modifier = modifier,
    )
}
