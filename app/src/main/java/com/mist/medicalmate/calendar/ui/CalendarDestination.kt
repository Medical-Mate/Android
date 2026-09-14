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
import java.time.YearMonth

/** 와이어프레임 1r-1. 하단 탭의 캘린더다. */
@Serializable
internal data object CalendarDestination

/**
 * 와이어프레임 1r-2. 하루를 여는 목적지다.
 *
 * 날짜를 문자열로 담는다. `LocalDate`는 직렬화 규칙이 없어서 라우트에 그대로 넣을 수 없고,
 * ISO 문자열이 사람이 읽을 수 있어 딥링크로도 쓸 수 있다.
 *
 * [appointmentId]는 월 화면에서 누른 일정이다. 하루에 일정이 둘 이상일 수 있어서 어느 것을
 * 눌렀는지를 들고 와야 한다 — 날짜만으로는 화면이 첫 건을 열고, 누른 것과 열린 것이 달랐다.
 * 없으면 그 날 첫 일정을 연다(#179).
 */
@Serializable
internal data class CalendarDayDestination(val date: String, val appointmentId: Long? = null)

internal fun NavGraphBuilder.calendarDestination(
    onDayOpen: (date: LocalDate, appointmentId: Long?) -> Unit,
    onCardOpen: (String) -> Unit,
    onAddClick: (date: LocalDate?, cardId: String?) -> Unit,
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
    onRecordAdd: (cardId: String, cardTitle: String, visitedOn: LocalDate) -> Unit,
    onRecordOpen: (String) -> Unit,
    onScheduleConfirm: (clinic: String?, date: LocalDate, appointmentId: Long?) -> Unit,
    onExit: () -> Unit,
) {
    composable<CalendarDayDestination> { entry ->
        val route = entry.toRoute<CalendarDayDestination>()
        val date = LocalDate.parse(route.date)
        CalendarDayRoute(
            date = date,
            appointmentId = route.appointmentId,
            onCardOpen = onCardOpen,
            onScheduleConfirm = onScheduleConfirm,
            onScheduleEdit = { id -> onScheduleConfirm(null, date, id) },
            onRecordAdd = { cardId, cardTitle -> onRecordAdd(cardId, cardTitle, date) },
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
    onDayOpen: (date: LocalDate, appointmentId: Long?) -> Unit,
    onCardOpen: (String) -> Unit,
    onAddClick: (date: LocalDate?, cardId: String?) -> Unit,
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
        onScheduleClick = { id -> onDayOpen(state.selected, id.toLongOrNull()) },
        onCardOpenClick = onCardOpen,
        onCardScheduleOpen = { date -> onDayOpen(date, null) },
        onCardScheduleAdd = { cardId -> onAddClick(state.selected, cardId) },
        onCardSheetDismiss = viewModel::onCardSheetDismiss,
        // 보고 있는 달의 날을 고른 상태에서만 그 날을 들고 간다. 다른 달로 넘기면 고른 날이
        // 격자 밖이라 화면에 표시가 없고, 그 날로 열리면 어디서 온 값인지 알 수 없다.
        onAddClick = { onAddClick(state.selected.takeIf { YearMonth.from(it) == state.month }, null) },
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
    appointmentId: Long?,
    onCardOpen: (String) -> Unit,
    onScheduleConfirm: (clinic: String?, date: LocalDate, appointmentId: Long?) -> Unit,
    onScheduleEdit: (Long) -> Unit,
    onRecordAdd: (cardId: String, cardTitle: String) -> Unit,
    onRecordOpen: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarDayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(date, appointmentId) { viewModel.load(date, appointmentId) }

    CalendarDayScreen(
        state = state ?: return,
        callbacks =
        CalendarDayCallbacks(
            onBackClick = onExit,
            onCardOpenClick = onCardOpen,
            onTodoToggle = viewModel.todo::onToggle,
            onRecordAddClick = onRecordAdd,
            onRecordOpenClick = onRecordOpen,
            onEditClick = viewModel.todo::onEditStart,
            onEditCancelClick = viewModel.todo::onEditCancel,
            onEditDoneClick = viewModel.todo::onEditDone,
            onTodoDeleteClick = viewModel.todo::onDelete,
            onScheduleDeleteClick = viewModel::onScheduleDeleteClick,
            onScheduleDeleteConfirm = { viewModel.onScheduleDeleteConfirm(onExit) },
            onScheduleDeleteDismiss = viewModel::onScheduleDeleteDismiss,
            // 1r-2-A의 "시간 정하고 확정하기". 병원이 채워진 일정 추가(1r-4-B)로 간다.
            // **그 일정의 날짜를 들고 간다.** 다음 진료는 이 화면의 날이 아니라 다른 날이다.
            // 이미 만들어진 일정이면 그것을 고치러 간다 — 새로 만들면 같은 재방문이 둘이 된다.
            onNextEventConfirmClick = {
                state?.nextEvent?.let { next ->
                    onScheduleConfirm(next.clinic, next.on, next.appointmentId)
                }
            },
            onScheduleEditClick = onScheduleEdit,
        ),
        modifier = modifier,
    )
}
