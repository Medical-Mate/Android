package com.mist.medicalmate.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateCheckbox
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import java.time.format.DateTimeFormatter

/**
 * 와이어프레임 1r-2. Figma `406:2514`.
 *
 * 하루를 진료 하나를 중심으로 본다. 그 날 일정, 가져갈 카드, 진료 전 할 일, 그리고 진료가
 * 끝난 뒤의 기록이다.
 *
 * 2Depth라서 하단 탭을 그리지 않는다. 상단 왼쪽이 뒤로다.
 *
 * 각 덩어리의 머리에 동작이 붙는다. 일정은 수정, 카드는 열기, 할 일과 기록은 추가다.
 * 목적지가 아직 없는 것은 호출자가 빈 동작을 준다.
 */
@Composable
fun CalendarDayScreen(state: CalendarDayUiState, callbacks: CalendarDayCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = state.date.format(dayFormat),
            onLeadingClick = callbacks.onBackClick,
        )
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        ) {
            ScheduleSection(state = state, callbacks = callbacks)
            CardSection(state = state, callbacks = callbacks)
            TodoSection(state = state, callbacks = callbacks)
            RecordSection(state = state, callbacks = callbacks)
        }
    }
}

/**
 * 일자 화면에서 나가는 길들.
 *
 * 파라미터로 하나씩 받으면 일곱 개가 된다. 덩어리마다 하나씩 있어서 한 덩어리로 묶었다.
 */
data class CalendarDayCallbacks(
    val onBackClick: () -> Unit = {},
    val onScheduleEditClick: () -> Unit = {},
    val onCardOpenClick: (String) -> Unit = {},
    val onTodoToggle: (String, Boolean) -> Unit = { _, _ -> },
    val onTodoAddClick: () -> Unit = {},
    val onRecordAddClick: () -> Unit = {},
)

/** 이 날 일정. 옅은 브랜드 면에 D-day와 제목, 시간·의사·가져갈 것을 담는다. */
@Composable
private fun ColumnScope.ScheduleSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    val schedule = state.schedule ?: return

    MedicalMateSectionHeader(
        title = stringResource(R.string.calendar_day_schedule),
        actionLabel = stringResource(R.string.calendar_day_schedule_edit),
        onActionClick = callbacks.onScheduleEditClick,
    )
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.lg)
            .padding(MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        if (schedule.dday >= 0) {
            Text(
                text = stringResource(R.string.calendar_day_dday, schedule.dday),
                style = MedicalMateTheme.typography.labelS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        Text(
            text = schedule.title,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = "${schedule.time} · ${schedule.detail}",
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 가져갈 브리핑 카드. 눌러서 카드로 들어간다. */
@Composable
private fun ColumnScope.CardSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    val card = state.card ?: return

    MedicalMateSectionHeader(
        title = stringResource(R.string.calendar_day_card),
        actionLabel = stringResource(R.string.calendar_day_card_open),
        onActionClick = { callbacks.onCardOpenClick(card.id) },
    )
    MedicalMateListRow(
        title = card.title,
        meta = card.meta,
        badge = card.status,
        badgeTone = MedicalMateBadgeTone.NEUTRAL,
        type = MedicalMateListRowType.BADGE,
        onClick = { callbacks.onCardOpenClick(card.id) },
    )
}

/** 진료 전 할 일. 체크는 그 자리에서 켜고 끈다. */
@Composable
private fun ColumnScope.TodoSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    if (state.todos.isEmpty()) return

    MedicalMateSectionHeader(
        title = stringResource(R.string.calendar_day_todo),
        actionLabel = stringResource(R.string.calendar_day_todo_add),
        onActionClick = callbacks.onTodoAddClick,
    )
    state.todos.forEach { todo ->
        MedicalMateCheckbox(
            checked = todo.done,
            onCheckedChange = { callbacks.onTodoToggle(todo.id, it) },
            label = todo.label,
        )
    }
}

/**
 * 이 날 기록.
 *
 * 진료가 끝나기 전에는 빈 상태다. 무엇을 적을 자리인지 알려주고 진료 후 기록으로 보낸다.
 */
@Composable
private fun ColumnScope.RecordSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    MedicalMateSectionHeader(
        title = stringResource(R.string.calendar_day_record),
        actionLabel = stringResource(R.string.calendar_day_record_add),
        onActionClick = callbacks.onRecordAddClick,
    )
    val record = state.record
    if (record == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MedicalMateEmptyState(
                type = MedicalMateEmptyStateType.NO_RECORD,
                title = stringResource(R.string.calendar_day_record_empty_title),
                description = stringResource(R.string.calendar_day_record_empty_description),
                actionLabel = stringResource(R.string.calendar_day_record_empty_action),
                onActionClick = callbacks.onRecordAddClick,
            )
        }
        return
    }
    Text(
        text = record,
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgDefault,
    )
}

/** "9월 12일 (금)" 형식. 컴포저블 밖에 둬서 기기 로케일을 직접 읽지 않는다. */
private val dayFormat = DateTimeFormatter.ofPattern("M월 d일 (E)")

@MedicalMateScreenPreviews
@Composable
private fun CalendarDayScreenPreview() {
    MedicalMateTheme {
        CalendarDayScreen(state = previewCalendarDayState, callbacks = CalendarDayCallbacks())
    }
}
