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
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialog
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateRowDelete
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateTodoRow
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
            actionLabel = stringResource(navActionLabel(state)),
            onActionClick = { onNavAction(state, callbacks) },
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
            ScheduleSection(state = state)
            CardSection(state = state, callbacks = callbacks)
            TodoSection(state = state, callbacks = callbacks)
            RecordSection(state = state, callbacks = callbacks)
            NextEventSection(state = state, callbacks = callbacks)
        }
        if (state.editing) {
            MedicalMateBottomCtaBar {
                MedicalMateButton(
                    label = stringResource(R.string.calendar_day_schedule_delete),
                    onClick = callbacks.onScheduleDeleteClick,
                    type = MedicalMateButtonType.DANGER,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.deleteRequested) {
        MedicalMateDialog(
            title = stringResource(R.string.calendar_day_schedule_delete_title),
            message = stringResource(R.string.calendar_day_schedule_delete_body),
            confirmLabel = stringResource(R.string.calendar_day_schedule_delete_confirm),
            onConfirm = callbacks.onScheduleDeleteConfirm,
            dismissLabel = stringResource(R.string.calendar_day_edit_cancel),
            onDismissRequest = callbacks.onScheduleDeleteDismiss,
        )
    }
}

/**
 * Nav 우측 버튼의 이름. 한 자리에서 셋으로 갈린다.
 *
 * 편집 중이 아니면 `편집`, 편집 중이고 바뀐 것이 없으면 `취소`, 바뀐 것이 있으면 `확인`이다.
 * 브리핑 카드(1e-1)와 진료 후 기록(1q-1)이 쓰는 것과 같은 규칙이다.
 */
private fun navActionLabel(state: CalendarDayUiState): Int = when {
    !state.editing -> R.string.calendar_day_edit
    state.changed -> R.string.calendar_day_edit_done
    else -> R.string.calendar_day_edit_cancel
}

private fun onNavAction(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    when {
        !state.editing -> callbacks.onEditClick()
        state.changed -> callbacks.onEditDoneClick()
        else -> callbacks.onEditCancelClick()
    }
}

/**
 * 일자 화면에서 나가는 길들.
 *
 * 파라미터로 하나씩 받으면 여섯이 된다. 덩어리마다 하나씩 있어서 한 덩어리로 묶었다.
 */
data class CalendarDayCallbacks(
    val onBackClick: () -> Unit = {},
    val onCardOpenClick: (String) -> Unit = {},
    val onTodoToggle: (String, Boolean) -> Unit = { _, _ -> },
    /** 기록을 붙일 카드의 id와 제목. 제목은 1p가 "무엇으로 진료받았는지"를 적는 데 쓴다. */
    val onRecordAddClick: (cardId: String, cardTitle: String) -> Unit = { _, _ -> },
    val onRecordOpenClick: (String) -> Unit = {},
    val onNextEventConfirmClick: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onEditCancelClick: () -> Unit = {},
    val onEditDoneClick: () -> Unit = {},
    val onTodoDeleteClick: (String) -> Unit = {},
    val onScheduleDeleteClick: () -> Unit = {},
    val onScheduleDeleteConfirm: () -> Unit = {},
    val onScheduleDeleteDismiss: () -> Unit = {},
)

/**
 * 이 날 일정. 옅은 브랜드 면에 머리말과 제목, 시간과 가져갈 것을 담는다.
 *
 * 머리말이 진료 전후로 갈린다. 전에는 남은 날수, 다녀온 뒤에는 "진료 완료"다. 지난 날의
 * 카드에 D-day가 남아 있으면 아직 남은 일처럼 읽힌다.
 *
 * 시간과 가져갈 것을 한 줄로 잇지 않는다. 시안이 두 줄로 나눠 뒀고, 이어 붙이면 긴 기기
 * 글꼴에서 어디까지가 시간인지 흐려진다.
 */
@Composable
private fun ColumnScope.ScheduleSection(state: CalendarDayUiState) {
    val schedule = state.schedule ?: return

    MedicalMateSectionHeader(title = stringResource(R.string.calendar_day_schedule))
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.lg)
            .padding(MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        when {
            state.visited ->
                Text(
                    text = stringResource(R.string.calendar_day_visited),
                    style = MedicalMateTheme.typography.labelS,
                    color = MedicalMateTheme.colors.fgSuccess,
                )

            schedule.dday >= 0 ->
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
            text = schedule.time ?: stringResource(R.string.calendar_time_unset),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Text(
            text = schedule.detail,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 브리핑 카드. 줄 자체가 카드로 들어가는 이동이라 헤더에 액션을 두지 않는다.
 *
 * 제목이 진료 전후로 갈린다. 전에는 "가져갈 브리핑 카드"로 챙길 것을 가리키고, 다녀온
 * 뒤에는 가져갈 일이 끝나서 "브리핑 카드"다.
 */
@Composable
private fun ColumnScope.CardSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    val card = state.card ?: return

    MedicalMateSectionHeader(
        title =
        stringResource(if (state.visited) R.string.calendar_day_card_done else R.string.calendar_day_card),
    )
    MedicalMateListRow(
        title = card.title,
        meta = card.meta,
        badge = card.status,
        badgeTone = MedicalMateBadgeTone.NEUTRAL,
        type = if (card.status == null) MedicalMateListRowType.DEFAULT else MedicalMateListRowType.BADGE,
        onClick = { callbacks.onCardOpenClick(card.id) },
    )
}

/**
 * 진료 전 할 일. 체크는 그 자리에서 켜고 끈다.
 *
 * 삭제 ×는 편집 상태(1r-2-E)에만 붙는다. 추가는 시안의 어느 상태에도 보이지 않는다.
 */
@Composable
private fun ColumnScope.TodoSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    val todos = state.shownTodos
    if (todos.isEmpty()) return

    MedicalMateSectionHeader(title = stringResource(R.string.calendar_day_todo))
    todos.forEach { todo ->
        MedicalMateTodoRow(
            checked = todo.done,
            onCheckedChange = { callbacks.onTodoToggle(todo.id, it) },
            label = todo.label,
            delete =
            if (state.editing) {
                MedicalMateRowDelete(
                    contentDescription = stringResource(R.string.calendar_day_todo_delete, todo.label),
                    onClick = { callbacks.onTodoDeleteClick(todo.id) },
                )
            } else {
                null
            },
        )
    }
}

/**
 * 이 날 기록.
 *
 * 진료가 끝나기 전에는 빈 상태다. 무엇을 적을 자리인지 알려주고 진료 후 기록으로 보낸다.
 *
 * **카드가 걸린 일정에서만 기록으로 보낸다.** 서버가 확정한 카드에 매달린 기록만 받아서,
 * 카드 없이 그 흐름에 들어가면 끝에서 저장이 아무 일도 하지 않는다. 눌러도 되지 않는 버튼을
 * 두는 대신 빈 상태의 안내만 남긴다.
 */
@Composable
private fun ColumnScope.RecordSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    MedicalMateSectionHeader(title = stringResource(R.string.calendar_day_record))
    val record = state.record
    if (record == null) {
        val card = state.card
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MedicalMateEmptyState(
                type = MedicalMateEmptyStateType.NO_RECORD,
                title = stringResource(R.string.calendar_day_record_empty_title),
                description = stringResource(R.string.calendar_day_record_empty_description),
                actionLabel = card?.let { stringResource(R.string.calendar_day_record_empty_action) },
                onActionClick = card?.let { { callbacks.onRecordAddClick(it.id, it.title) } },
            )
        }
        return
    }
    MedicalMateListRow(
        title = record.title,
        meta = record.meta,
        onClick = { callbacks.onRecordOpenClick(record.id) },
    )
}

/**
 * 다음 일정. Figma 1r-2-A `1060:2879`, 1r-2-A2 `1060:2998`.
 *
 * 진료 후 기록의 재방문에서 자동으로 만들어진다. 그래서 사용자가 적은 것이 아니라는 말이
 * 함께 있고, 시간을 정하는 조작이 그 안에 붙는다.
 *
 * 면이 브랜드 채움이다. 이 날의 다른 덩어리는 모두 지난 일인데 이것만 앞으로 올 일이라
 * 무게가 다르다. 한 화면에서 채운 면은 여기 하나뿐이다.
 */
@Composable
private fun ColumnScope.NextEventSection(state: CalendarDayUiState, callbacks: CalendarDayCallbacks) {
    val next = state.nextEvent ?: return

    MedicalMateSectionHeader(title = stringResource(R.string.calendar_day_next))
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimary, shape = MedicalMateRadius.lg)
            .padding(MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        Text(
            text = next.chip,
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgPrimary,
            modifier =
            Modifier
                .background(color = MedicalMateTheme.colors.bgSurface, shape = MedicalMateRadius.full)
                .padding(horizontal = MedicalMateSpace.s10, vertical = MedicalMateSpace.s4),
        )
        Text(
            text = next.title,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgOnPrimary,
        )
        if (next.at == null) {
            Text(
                text = stringResource(R.string.calendar_day_next_auto),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
            Text(
                text = stringResource(R.string.calendar_day_next_hint),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
            MedicalMateButton(
                label = stringResource(R.string.calendar_day_next_confirm),
                onClick = callbacks.onNextEventConfirmClick,
                type = MedicalMateButtonType.TONAL,
                size = MedicalMateButtonSize.M,
                leadingIcon = MedicalMateIcons.Clock,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MedicalMateSpace.s8),
            )
        } else {
            Text(
                text = next.at,
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
        }
    }
}

/** "9월 12일 (금)" 형식. 컴포저블 밖에 둬서 기기 로케일을 직접 읽지 않는다. */
private val dayFormat = DateTimeFormatter.ofPattern("M월 d일 (E)")
