package com.mist.medicalmate.visit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 진료 후 기록 플로우의 목적지들. Figma 흐름은 `1m → 1p → 1q-1 → 1k`다.
 *
 * 병원 id가 라우트로 다음 화면에 건너간다. 어느 병원의 진료를 적는지가 목적지의 일부이고,
 * 화면이 다시 만들어질 때 살아 있어야 한다. 서버 연동 전에는 쓰이지 않고 픽스처가 나온다.
 */
@Serializable
internal data object HospitalPickDestination

/** 와이어프레임 1p. */
@Serializable
internal data class VisitNoteDestination(val hospitalId: String)

/** 와이어프레임 1q-1과 1q-1-E. */
@Serializable
internal data class VisitRecordDestination(val hospitalId: String)

/** 와이어프레임 1k. */
@Serializable
internal data class VisitSummaryDestination(val visitId: String)

internal fun NavGraphBuilder.hospitalPickDestination(onPicked: (String) -> Unit, onExit: () -> Unit) {
    composable<HospitalPickDestination> {
        HospitalPickRoute(onPicked = onPicked, onExit = onExit)
    }
}

internal fun NavGraphBuilder.visitNoteDestination(onSaved: () -> Unit, onExit: () -> Unit) {
    composable<VisitNoteDestination> {
        VisitNoteRoute(onSaved = onSaved, onExit = onExit)
    }
}

internal fun NavGraphBuilder.visitRecordDestination(onSaved: () -> Unit, onDeleted: () -> Unit, onExit: () -> Unit) {
    composable<VisitRecordDestination> {
        VisitRecordRoute(onSaved = onSaved, onDeleted = onDeleted, onExit = onExit)
    }
}

internal fun NavGraphBuilder.visitSummaryDestination(onHome: () -> Unit, onExit: () -> Unit) {
    composable<VisitSummaryDestination> {
        VisitSummaryScreen(state = previewVisitSummary, onHomeClick = onHome, onBackClick = onExit)
    }
}

@Composable
private fun HospitalPickRoute(
    onPicked: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HospitalPickViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    HospitalPickScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onHospitalClick = viewModel::onHospitalClick,
        onSubmitClick = { state.selectedId?.let(onPicked) },
        onBackClick = onExit,
        modifier = modifier,
    )
}

@Composable
private fun VisitNoteRoute(
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    VisitNoteScreen(
        state = state,
        callbacks =
        VisitNoteCallbacks(
            onBackClick = onExit,
            onNoteChange = viewModel::onNoteChange,
            onOrganizeClick = viewModel::onOrganizeClick,
            onSaveClick = onSaved,
        ),
        modifier = modifier,
    )
}

/**
 * 상태 있는 진입점.
 *
 * 저장은 두 가지 일을 한다. 수정 중이면 초안을 옮기고 화면에 남고, 읽는 중이면 이번 진료
 * 정리(1k)로 넘어간다. 브리핑 카드 화면과 같은 구조다.
 */
@Composable
private fun VisitRecordRoute(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    VisitRecordScreen(
        state = state,
        callbacks =
        VisitRecordCallbacks(
            onBackClick = onExit,
            onEditClick = viewModel::onEditClick,
            onEditDoneClick = viewModel::onEditDoneClick,
            onCancelClick = viewModel::onCancelClick,
            edit = viewModel.editActions,
            onScheduleChange = viewModel::onScheduleChange,
            // 편집 중에는 하단에 저장하기가 없다. 그 자리가 삭제이고 사본을 옮기는 것은
            // Nav 우측 `확인`이 한다. 그래서 저장하기는 항상 화면을 나간다.
            onSaveClick = onSaved,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = {
                viewModel.onDeleteConfirm()
                onDeleted()
            },
            onRetryClick = viewModel::load,
        ),
        modifier = modifier,
    )
}
