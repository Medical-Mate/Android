package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** 와이어프레임 1r-1. 하단 탭의 캘린더다. */
@Serializable
internal data object CalendarDestination

/**
 * 와이어프레임 1r-2. 하루를 여는 목적지다.
 *
 * 날짜를 문자열로 담는다. `LocalDate`는 직렬화 규칙이 없어서 라우트에 그대로 넣을 수 없고,
 * ISO 문자열이 사람이 읽을 수 있어 딥링크로도 쓸 수 있다.
 */
@Serializable
internal data class CalendarDayDestination(val date: String)

internal fun NavGraphBuilder.calendarDestination(
    onDayOpen: (LocalDate) -> Unit,
    onCardOpen: (String) -> Unit,
    onAddClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
) {
    composable<CalendarDestination> {
        CalendarMonthRoute(
            onDayOpen = onDayOpen,
            onCardOpen = onCardOpen,
            onAddClick = onAddClick,
            onTabSelect = onTabSelect,
        )
    }
}

internal fun NavGraphBuilder.calendarDayDestination(
    onCardOpen: (String) -> Unit,
    onRecordAdd: () -> Unit,
    onRecordOpen: (String) -> Unit,
    onExit: () -> Unit,
) {
    composable<CalendarDayDestination> { entry ->
        val date = LocalDate.parse(entry.toRoute<CalendarDayDestination>().date)
        CalendarDayRoute(
            date = date,
            onCardOpen = onCardOpen,
            onRecordAdd = onRecordAdd,
            onRecordOpen = onRecordOpen,
            onExit = onExit,
        )
    }
}

/**
 * 월 화면의 진입점.
 *
 * 날을 고르는 것은 화면 안의 일이고, 일정을 누르는 것은 목적지 이동이다. 그래서 날 선택은
 * ViewModel이 받고 일정 클릭은 [onDayOpen]으로 넘긴다.
 *
 * 카드만 있는 날의 시트도 화면 안의 상태라 여닫는 것은 ViewModel이 받는다. 그 안에서 카드를
 * 누르면 브리핑 카드로 나가므로 [onCardOpen]만 목적지로 올린다.
 */
@Composable
private fun CalendarMonthRoute(
    onDayOpen: (LocalDate) -> Unit,
    onCardOpen: (String) -> Unit,
    onAddClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 일정을 넣거나 지우고 돌아오면 달라져 있다. 화면으로 올 때마다 다시 읽는다.
    LaunchedEffect(Unit) { viewModel.load() }

    CalendarMonthScreen(
        state = state,
        onPreviousMonthClick = viewModel::onPreviousMonth,
        onNextMonthClick = viewModel::onNextMonth,
        onDayClick = viewModel::onDaySelect,
        onScheduleClick = { onDayOpen(state.selected) },
        onCardOpenClick = onCardOpen,
        onCardSheetDismiss = viewModel::onCardSheetDismiss,
        onAddClick = onAddClick,
        onTabSelect = onTabSelect,
        modifier = modifier,
    )
}

/**
 * 일자 화면의 진입점.
 *
 * 할 일 체크와 편집은 아직 저장되지 않는다. 화면 안에서 고치는 것까지가 지금 범위이고,
 * 서버에 남기려면 그 API가 필요하다(#75).
 *
 * 일정을 지우면 이 날의 화면이 있을 이유가 없어져 캘린더로 돌아간다.
 */
@Composable
private fun CalendarDayRoute(
    date: LocalDate,
    onCardOpen: (String) -> Unit,
    onRecordAdd: () -> Unit,
    onRecordOpen: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarDayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(date) { viewModel.load(date) }

    CalendarDayScreen(
        state = state ?: return,
        callbacks =
        CalendarDayCallbacks(
            onBackClick = onExit,
            onCardOpenClick = onCardOpen,
            onTodoToggle = viewModel::onTodoToggle,
            onRecordAddClick = onRecordAdd,
            onRecordOpenClick = onRecordOpen,
            onEditClick = viewModel::onEditStart,
            onEditCancelClick = viewModel::onEditCancel,
            onEditDoneClick = viewModel::onEditDone,
            onTodoDeleteClick = viewModel::onTodoDelete,
            onScheduleDeleteClick = viewModel::onScheduleDeleteClick,
            onScheduleDeleteConfirm = { viewModel.onScheduleDeleteConfirm(onExit) },
            onScheduleDeleteDismiss = viewModel::onScheduleDeleteDismiss,
        ),
        modifier = modifier,
    )
}
