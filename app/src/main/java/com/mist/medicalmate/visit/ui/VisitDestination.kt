package com.mist.medicalmate.visit.ui

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
 * 진료 후 기록 플로우의 목적지들. Figma 흐름은 `1m → 1p → 1q-1 → 1k`다.
 *
 * 병원 id가 라우트로 다음 화면에 건너간다. 어느 병원의 진료를 적는지가 목적지의 일부이고,
 * 화면이 다시 만들어질 때 살아 있어야 한다. 서버 연동 전에는 쓰이지 않고 픽스처가 나온다.
 */
/**
 * 와이어프레임 1m과 1m-B.
 *
 * [purpose]가 두 자리를 가른다. 라우트에 담는 이유는 어느 목적으로 열린 화면인지가 목적지의
 * 일부이고, 화면이 다시 만들어질 때 살아 있어야 하기 때문이다.
 *
 * [cardId]는 브리핑 카드의 `변경`에서 들어온 경우에만 있다. 고른 뒤 그 카드로 돌아가야 해서
 * 어느 카드였는지를 들고 간다.
 */
@Serializable
internal data class HospitalPickDestination(
    val purpose: HospitalPickPurpose = HospitalPickPurpose.AFTER_VISIT,
    val cardId: String? = null,
)

/** 와이어프레임 1p. */
@Serializable
internal data class VisitNoteDestination(val hospitalId: String)

/** 와이어프레임 1q-1과 1q-1-E. */
@Serializable
internal data class VisitRecordDestination(val hospitalId: String)

/** 와이어프레임 1k. */
@Serializable
internal data class VisitSummaryDestination(val visitId: String)

/**
 * @param onPicked 진료 후(1m)에서 병원을 고르고 완료했을 때. 병원 id가 넘어간다.
 * @param onCardRequested 진료 전(1m-B)에서 카드로 넘어갈 때. 고른 병원이 없으면 null이
 *   넘어간다. 건너뛰기와 CTA가 같은 곳으로 가고, 다른 것은 병원을 들고 가는지뿐이다.
 */
internal fun NavGraphBuilder.hospitalPickDestination(
    onPicked: (String) -> Unit,
    onCardRequested: (cardId: String?, hospital: Hospital?) -> Unit,
    onExit: () -> Unit,
) {
    composable<HospitalPickDestination> { entry ->
        val route = entry.toRoute<HospitalPickDestination>()
        HospitalPickRoute(
            purpose = route.purpose,
            cardId = route.cardId,
            onPicked = onPicked,
            onCardRequested = onCardRequested,
            onExit = onExit,
        )
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
    purpose: HospitalPickPurpose,
    cardId: String?,
    onPicked: (String) -> Unit,
    onCardRequested: (cardId: String?, hospital: Hospital?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HospitalPickViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(purpose) { viewModel.load(purpose) }

    val before = purpose == HospitalPickPurpose.BEFORE_VISIT
    val selected = state.results.firstOrNull { it.id == state.selectedId }

    HospitalPickScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onHospitalClick = viewModel::onHospitalClick,
        // 진료 후에는 고른 병원의 id만 다음 화면으로 간다. 진료 전에는 카드가 이름과 주소를
        // 바로 그려야 해서 병원을 그대로 넘긴다.
        onSubmitClick = {
            if (before) onCardRequested(cardId, selected) else state.selectedId?.let(onPicked)
        },
        onBackClick = onExit,
        modifier = modifier,
        // 건너뛰기는 병원 없이 카드로 간다. 진료 전에만 있다.
        onSkipClick = if (before) {
            { onCardRequested(cardId, null) }
        } else {
            null
        },
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
