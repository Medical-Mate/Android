package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import kotlinx.serialization.Serializable

/** 와이어프레임 1j. 하단 탭의 기록이다. */
@Serializable
internal data object RecordDestination

internal fun NavGraphBuilder.recordDestination(
    onItemClick: (String) -> Unit,
    onStartIntakeClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
) {
    composable<RecordDestination> {
        RecordRoute(
            onItemClick = onItemClick,
            onStartIntakeClick = onStartIntakeClick,
            onTabSelect = onTabSelect,
        )
    }
}

@Composable
private fun RecordRoute(
    onItemClick: (String) -> Unit,
    onStartIntakeClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    RecordScreen(
        state = state,
        callbacks =
        RecordCallbacks(
            onItemClick = onItemClick,
            onStartIntakeClick = onStartIntakeClick,
            onTabSelect = onTabSelect,
            onEditStart = viewModel::onEditStart,
            onEditCancel = viewModel::onEditCancel,
            onSelectChange = viewModel::onSelectChange,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss,
        ),
        modifier = modifier,
    )
}
