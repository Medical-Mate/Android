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
 * 네 화면이 한 흐름이지만 목적지가 각각이라 ViewModel을 공유하지 않는다. 그래서 앞 화면에서
 * 정한 것이 라우트를 타고 따라간다. 마지막 화면(1q-1)의 저장 한 번이
 * `POST /api/cards/{cardId}/visit`이고, 그 요청에 들어갈 값이 세 화면에 흩어져 있다.
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

/**
 * 와이어프레임 1p.
 *
 * [clinic]은 1m에서 고른 병원의 **이름**이다. id를 나르지 않는 이유는 서버가 병원을 문자열
 * 하나(`clinicName`)로만 받기 때문이다. 심평원 키가 풀려 병원이 개체가 되면 id로 바꾼다.
 *
 * [cardId]는 기록을 붙일 카드다. 서버가 확정한 카드 하나에 기록 하나를 받는다. 캘린더 일자의
 * 일정에 걸린 카드에서 온다.
 */
@Serializable
internal data class VisitNoteDestination(val clinic: String? = null, val cardId: String? = null)

/**
 * 와이어프레임 1q-1과 1q-1-E.
 *
 * [note]는 1p에서 적은 원문이다. 라우트로 나르는 이유는 이 화면이 그 글을 그대로 아래에
 * 보여주고, 저장할 때 `rawNote`로 함께 보내기 때문이다. 길어질 수 있는 값이지만 목적지가
 * 달라 다른 방법이 없다. 흐름을 한 목적지로 합치면 1p에서 뒤로 갈 자리가 사라진다.
 */
@Serializable
internal data class VisitRecordDestination(
    val clinic: String? = null,
    val cardId: String? = null,
    val note: String = "",
)

/** 와이어프레임 1k. */
@Serializable
internal data class VisitSummaryDestination(val visitId: String)

/**
 * @param onPicked 진료 후(1m)에서 병원을 고르고 완료했을 때. 고른 병원과, 이 흐름이 어느
 *   카드에 붙는지가 함께 넘어간다.
 * @param onCardRequested 진료 전(1m-B)에서 카드로 넘어갈 때. 고른 병원이 없으면 null이
 *   넘어간다. 건너뛰기와 CTA가 같은 곳으로 가고, 다른 것은 병원을 들고 가는지뿐이다.
 */
internal fun NavGraphBuilder.hospitalPickDestination(
    onPicked: (cardId: String?, hospital: Hospital) -> Unit,
    onCardRequested: (cardId: String?, hospital: Hospital?) -> Unit,
    onScheduleRequested: (Hospital?) -> Unit,
    onExit: () -> Unit,
) {
    composable<HospitalPickDestination> { entry ->
        val route = entry.toRoute<HospitalPickDestination>()
        HospitalPickRoute(
            purpose = route.purpose,
            cardId = route.cardId,
            onPicked = onPicked,
            onCardRequested = onCardRequested,
            onScheduleRequested = onScheduleRequested,
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.visitNoteDestination(
    onSaved: (clinic: String?, cardId: String?, note: String) -> Unit,
    onExit: () -> Unit,
) {
    composable<VisitNoteDestination> { entry ->
        val route = entry.toRoute<VisitNoteDestination>()
        VisitNoteRoute(
            onSaved = { note -> onSaved(route.clinic, route.cardId, note) },
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.visitRecordDestination(
    onSaved: (visitId: String) -> Unit,
    onDeleted: () -> Unit,
    onExit: () -> Unit,
) {
    composable<VisitRecordDestination> { entry ->
        VisitRecordRoute(
            route = entry.toRoute<VisitRecordDestination>(),
            onSaved = onSaved,
            onDeleted = onDeleted,
            onExit = onExit,
        )
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
    onPicked: (cardId: String?, hospital: Hospital) -> Unit,
    onCardRequested: (cardId: String?, hospital: Hospital?) -> Unit,
    onScheduleRequested: (Hospital?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HospitalPickViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(purpose) { viewModel.load(purpose) }

    val beforeCard = purpose == HospitalPickPurpose.BEFORE_VISIT
    val selected = state.results.firstOrNull { it.id == state.selectedId }

    HospitalPickScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onHospitalClick = viewModel::onHospitalClick,
        // 목적마다 돌아가는 자리가 다르다. 진료 후에는 고른 병원의 id만 다음 화면으로 가고,
        // 진료 전에는 카드가 이름과 주소를 바로 그려야 해서 병원을 그대로 넘긴다. 일정
        // 추가는 필드에 이름만 채우고 돌아간다.
        onSubmitClick = {
            when (purpose) {
                HospitalPickPurpose.AFTER_VISIT -> selected?.let { onPicked(cardId, it) }
                HospitalPickPurpose.BEFORE_VISIT -> onCardRequested(cardId, selected)
                HospitalPickPurpose.SCHEDULE -> onScheduleRequested(selected)
            }
        },
        onBackClick = onExit,
        modifier = modifier,
        // 건너뛰기는 병원 없이 카드로 간다. 1m-B에만 있다. 일정 추가에서는 뒤로 가는 것이
        // 그대로 "정하지 않음"이라 따로 두지 않는다.
        onSkipClick = if (beforeCard) {
            { onCardRequested(cardId, null) }
        } else {
            null
        },
    )
}

@Composable
private fun VisitNoteRoute(
    onSaved: (note: String) -> Unit,
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
            // 적은 원문이 그대로 다음 화면으로 간다. 1q-1이 그 글을 보여주고 저장한다.
            onSaveClick = { onSaved(state.note) },
        ),
        modifier = modifier,
    )
}

/**
 * 상태 있는 진입점.
 *
 * 저장은 두 가지 일을 한다. 수정 중이면 초안을 옮기고 화면에 남고, 읽는 중이면 서버에
 * 보낸 뒤 이번 진료 정리(1k)로 넘어간다. 브리핑 카드 화면과 같은 구조다.
 */
@Composable
private fun VisitRecordRoute(
    route: VisitRecordDestination,
    onSaved: (visitId: String) -> Unit,
    onDeleted: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route) { viewModel.load(clinic = route.clinic, note = route.note) }

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
            onSaveClick = { viewModel.onSaveClick(cardId = route.cardId, onSaved = onSaved) },
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = {
                viewModel.onDeleteConfirm()
                onDeleted()
            },
            onRetryClick = { viewModel.load(clinic = route.clinic, note = route.note) },
        ),
        modifier = modifier,
    )
}
