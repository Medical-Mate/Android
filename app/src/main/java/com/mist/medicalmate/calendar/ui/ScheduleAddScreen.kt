package com.mist.medicalmate.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateAddRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardPick
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMatePickerField
import com.mist.medicalmate.core.designsystem.component.MedicalMateRowDelete
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateTodoRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 와이어프레임 1r-4. Figma `1062:3031`.
 *
 * 캘린더의 + 버튼과 카드만 있는 날 시트의 "이 카드로 일정 만들기"에서 들어온다. 진료 후
 * 기록에서 들어오면 병원이 이미 채워진 `1r-4-B` 상태다.
 *
 * 병원·날짜·시간이 필수다. 비운 채 저장을 누르면 그 칸 아래에 안내가 붙는다. 버튼을
 * 비활성으로 막지 않는다. 문서가 비활성만으로 필요한 행동을 숨기지 말라고 한다.
 *
 * 병원·날짜·시간은 [MedicalMatePickerField]다. 적는 것이 아니라 골라 채우는 값이라
 * 키보드를 띄우지 않는다. 병원은 검색 화면(1m-B)으로, 날짜와 시간은 시트로 간다.
 *
 * 상단 왼쪽은 닫기다. 시안 네 장 중 셋이 ×이고 `1r-4-C` 한 장만 ‹다. 흐름 중간이 아니라
 * 따로 열리는 폼이라 ×를 따랐다. 디자인 트랙에 확인을 넘겼다.
 */
@Composable
fun ScheduleAddScreen(state: ScheduleAddUiState, callbacks: ScheduleAddCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.schedule_add_title),
            leading = MedicalMateNavLeading.CLOSE,
            onLeadingClick = callbacks.onCloseClick,
        )
        FormContent(state = state, callbacks = callbacks)
        MedicalMateBottomCtaBar {
            // 늘 누를 수 있다. 눌러야 무엇이 비었는지 알 수 있다.
            MedicalMateButton(
                label = stringResource(R.string.schedule_add_save),
                onClick = callbacks.onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    ScheduleAddSheets(state = state, callbacks = callbacks)
}

/** 일정 추가 화면에서 나가는 길과 화면 안의 조작. */
data class ScheduleAddCallbacks(
    val todo: ScheduleAddTodoActions,
    val onCloseClick: () -> Unit = {},
    val onHospitalClick: () -> Unit = {},
    val onSheetOpen: (ScheduleAddSheet) -> Unit = {},
    val onSheetDismiss: () -> Unit = {},
    val onDateConfirm: (LocalDate) -> Unit = {},
    val onTimeConfirm: (LocalTime) -> Unit = {},
    val onCardPickChange: (String, Boolean) -> Unit = { _, _ -> },
    val onCardNewClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

@Composable
private fun ColumnScope.FormContent(state: ScheduleAddUiState, callbacks: ScheduleAddCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s8),
    ) {
        FieldLabel(stringResource(R.string.schedule_add_hospital))
        MedicalMatePickerField(
            value = state.hospital,
            placeholder = stringResource(R.string.schedule_add_hospital_placeholder),
            onClick = callbacks.onHospitalClick,
            errorText = stringResource(R.string.schedule_add_hospital_required).takeIf { state.hospitalMissing },
        )
        FieldLabel(stringResource(R.string.schedule_add_datetime))
        DateTimeFields(state = state, onSheetOpen = callbacks.onSheetOpen)
        CardSection(state = state, callbacks = callbacks)
        TodoSection(state = state, todo = callbacks.todo)
    }
}

/** 필드 위의 작은 이름. 시안이 필드마다 위에 한 줄 둔다. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MedicalMateTheme.typography.labelM,
        color = MedicalMateTheme.colors.fgSubtle,
        modifier = Modifier.padding(top = MedicalMateSpace.s16, bottom = MedicalMateSpace.s6),
    )
}

/**
 * 날짜와 시간을 좌우로 둔다.
 *
 * 아이콘이 다르다. 날짜는 달력, 시간은 시계다. 둘이 나란히 있어서 글자가 비었을 때
 * 어느 쪽이 무엇인지 아이콘으로 먼저 읽힌다.
 */
@Composable
private fun DateTimeFields(state: ScheduleAddUiState, onSheetOpen: (ScheduleAddSheet) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10)) {
        MedicalMatePickerField(
            value = state.date?.format(dateFormat),
            placeholder = stringResource(R.string.schedule_add_date_placeholder),
            onClick = { onSheetOpen(ScheduleAddSheet.DATE) },
            trailingIcon = MedicalMateIcons.Calendar,
            modifier = Modifier.weight(1f),
            errorText = stringResource(R.string.schedule_add_date_required).takeIf { state.dateMissing },
        )
        MedicalMatePickerField(
            value = state.time?.let { timeLabel(it) },
            placeholder = stringResource(R.string.schedule_add_time_placeholder),
            onClick = { onSheetOpen(ScheduleAddSheet.TIME) },
            trailingIcon = MedicalMateIcons.Clock,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 가져갈 브리핑 카드.
 *
 * 머리 오른쪽은 고른 수다. 누르는 링크가 아니라 읽기만 하는 표시라 `caption`으로 준다.
 */
@Composable
private fun ColumnScope.CardSection(state: ScheduleAddUiState, callbacks: ScheduleAddCallbacks) {
    val picked = state.pickedCardCount
    MedicalMateSectionHeader(
        title = stringResource(R.string.schedule_add_cards),
        caption =
        if (picked == 0) {
            stringResource(R.string.schedule_add_cards_none)
        } else {
            stringResource(R.string.schedule_add_cards_count, picked)
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10)) {
        state.cards.forEach { card ->
            MedicalMateCardPick(
                selected = card.picked,
                onSelectedChange = { callbacks.onCardPickChange(card.id, it) },
                title = card.title,
                meta = card.meta,
            )
        }
        MedicalMateAddRow(
            label = stringResource(R.string.schedule_add_card_new),
            onClick = callbacks.onCardNewClick,
        )
    }
}

/**
 * 진료 전 할 일.
 *
 * 추가를 누르면 목록 끝에 빈 줄이 하나 생기고 그 자리에서 받는다. 입력 필드를 따로 띄우지
 * 않는 것이 문서의 추가 방식이다.
 */
@Composable
private fun ColumnScope.TodoSection(state: ScheduleAddUiState, todo: ScheduleAddTodoActions) {
    MedicalMateSectionHeader(title = stringResource(R.string.schedule_add_todo))
    state.todos.forEach { item ->
        val label = item.label.ifBlank { stringResource(R.string.schedule_add_todo_placeholder) }
        MedicalMateTodoRow(
            checked = item.done,
            onCheckedChange = { todo.onToggle(item.id, it) },
            label = item.label,
            delete =
            MedicalMateRowDelete(
                contentDescription = stringResource(R.string.schedule_add_todo_delete, label),
                onClick = { todo.onDeleteClick(item.id) },
            ),
            onLabelChange = if (item.editing) ({ value: String -> todo.onLabelChange(item.id, value) }) else null,
            onEditDone = { todo.onEditDone(item.id) },
            labelPlaceholder = stringResource(R.string.schedule_add_todo_placeholder),
        )
    }
    MedicalMateAddRow(
        label = stringResource(R.string.schedule_add_todo_add),
        onClick = todo::onAddClick,
    )
}

/** 필드에 적는 "9월 26일 (토)". 컴포저블 밖에 둬서 기기 로케일을 직접 읽지 않는다. */
private val dateFormat = DateTimeFormatter.ofPattern("M월 d일 (E)")

/**
 * 필드에 적는 "오전 10:30".
 *
 * `DateTimeFormatter`의 `a`를 쓰지 않는다. 오전·오후가 기기 로케일에서 오고 리소스로
 * 번역할 수 없다.
 */
@Composable
private fun timeLabel(time: LocalTime): String {
    val meridiem =
        stringResource(
            if (time.hour < NOON_HOUR) R.string.schedule_add_time_am else R.string.schedule_add_time_pm,
        )
    val hour = time.hour % NOON_HOUR
    return stringResource(
        R.string.schedule_add_time_value,
        meridiem,
        if (hour == 0) NOON_HOUR else hour,
        time.minute,
    )
}

private const val NOON_HOUR = 12

@MedicalMateScreenPreviews
@Composable
private fun ScheduleAddScreenPreview() {
    MedicalMateTheme {
        ScheduleAddScreen(
            state = previewScheduleAddState,
            callbacks = ScheduleAddCallbacks(todo = ScheduleAddTodoActions(nextId = { "" }, update = {})),
        )
    }
}
