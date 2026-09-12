package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 1j-4. 홈의 "전체 보기"에서 밀려 들어온다. */
@Serializable
internal data object BriefCardListDestination

internal fun NavGraphBuilder.briefCardListDestination(
    onCardClick: (cardId: String, clinic: String?) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardListDestination> {
        BriefCardListRoute(
            onCardClick = { item -> onCardClick(item.id, item.clinic) },
            onStartIntakeClick = onStartIntakeClick,
            onExit = onExit,
        )
    }
}

@Composable
private fun BriefCardListRoute(
    onCardClick: (RecordItem) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    BriefCardListScreen(
        state = state,
        callbacks =
        BriefCardListCallbacks(
            onBackClick = onExit,
            onCardClick = onCardClick,
            onStartIntakeClick = onStartIntakeClick,
            onEditStart = viewModel::onEditStart,
            onEditCancel = viewModel::onEditCancel,
            onSelectChange = viewModel::onSelectChange,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onRetryClick = viewModel::load,
        ),
        modifier = modifier,
    )
}
