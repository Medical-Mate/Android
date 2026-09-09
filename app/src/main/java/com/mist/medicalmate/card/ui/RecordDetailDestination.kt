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
 * [onBriefCardClick]은 단계 안의 "카드 열기"다.
 *
 * 시안의 다른 두 단계에도 여는 줄이 있지만 목적지가 없어 데이터에 넣지 않았다. 문답 전체
 * 보기는 1c-1이 지난 대화를 다시 보여주는 화면이 아니고(지금은 새 문답을 시작한다), 진료
 * 후 기록 화면(1q-1)은 F 플로우라 아직 없다. 눌러도 아무 일이 없는 줄을 두지 않았다(#79).
 */
internal fun NavGraphBuilder.recordDetailDestination(onBackClick: () -> Unit, onBriefCardClick: (String) -> Unit) {
    composable<RecordDetailDestination> { entry ->
        val recordId = entry.toRoute<RecordDetailDestination>().recordId
        RecordDetailRoute(
            recordId = recordId,
            onBackClick = onBackClick,
            onActionClick = { target ->
                when (target) {
                    RecordStepAction.Target.BRIEF_CARD -> onBriefCardClick(recordId)
                }
            },
        )
    }
}

@Composable
private fun RecordDetailRoute(
    recordId: String,
    onBackClick: () -> Unit,
    onActionClick: (RecordStepAction.Target) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) { viewModel.load(recordId) }

    RecordDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onActionClick = onActionClick,
        onRetryClick = { viewModel.load(recordId) },
        modifier = modifier,
    )
}
