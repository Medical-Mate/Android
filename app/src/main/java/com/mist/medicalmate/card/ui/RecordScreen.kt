package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialog
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.designsystem.component.MedicalMateTabBar

/**
 * 와이어프레임 1j-1과 1j-2. Figma `406:2569`, `406:2646`.
 *
 * 하단 탭의 기록이다. 카드와 진료 기록을 월별로 묶어 보여준다.
 *
 * 1Depth 화면이라 하단 탭을 함께 그린다. 문답이나 카드처럼 흐름 안에 들어간 화면에서는
 * 감춘다(문서의 컴포넌트 규격).
 *
 * 편집(1j-1-D)에서는 탭바 자리에 삭제 버튼이 선다. 시안이 그 상태의 탭바를 감췄다. 고르는
 * 중에 다른 탭으로 나갈 수 있으면 고른 것이 어떻게 되는지 설명할 수 없다.
 *
 * 상단 왼쪽은 편집 중에도 비운다. 시안에는 뒤로가기가 있지만 탭으로 들어오는 화면이라
 * 돌아갈 곳이 없다. 편집을 빠져나가는 길은 오른쪽 취소다.
 */
@Composable
fun RecordScreen(state: RecordUiState, callbacks: RecordCallbacks, modifier: Modifier = Modifier) {
    val content = state as? RecordUiState.Content
    val editing = content?.editing == true

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.record_title),
            leading = MedicalMateNavLeading.NONE,
            actionLabel = navActionLabel(content),
            onActionClick = if (editing) callbacks.onEditCancel else callbacks.onEditStart,
        )
        when (state) {
            RecordUiState.Loading -> MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))
            is RecordUiState.Content ->
                if (state.groups.isEmpty()) {
                    EmptyContent(onStartIntakeClick = callbacks.onStartIntakeClick)
                } else {
                    GroupList(
                        groups = state.groups,
                        countRes = R.string.record_count,
                        onItemClick = callbacks.onItemClick,
                        selectedIds = state.selectedIds,
                        onSelectChange = callbacks.onSelectChange,
                    )
                }
        }
        if (editing) {
            DeleteBar(count = content?.selectedCount ?: 0, onDeleteClick = callbacks.onDeleteClick)
        } else {
            MedicalMateTabBar(selected = MedicalMateTab.RECORD, onSelect = callbacks.onTabSelect)
        }
    }

    if (content?.deleteRequested == true) {
        MedicalMateDialog(
            title = stringResource(R.string.record_delete_title, content.selectedCount),
            message = stringResource(R.string.record_delete_body),
            confirmLabel = stringResource(R.string.record_delete_confirm),
            onConfirm = callbacks.onDeleteConfirm,
            dismissLabel = stringResource(R.string.record_edit_cancel),
            onDismissRequest = callbacks.onDeleteDismiss,
        )
    }
}

/** 기록 탭에서 나가는 길과 화면 안의 조작. */
data class RecordCallbacks(
    val onItemClick: (String) -> Unit = {},
    val onStartIntakeClick: () -> Unit = {},
    val onTabSelect: (MedicalMateTab) -> Unit = {},
    val onEditStart: () -> Unit = {},
    val onEditCancel: () -> Unit = {},
    val onSelectChange: (String, Boolean) -> Unit = { _, _ -> },
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
)

/**
 * Nav 우측 문구.
 *
 * 목록이 비어 있으면 두지 않는다. 지울 것이 없는데 편집으로 들어갈 수 있으면 빈 화면에
 * 삭제 버튼만 서게 된다. 문서의 CRUD 규칙대로 한 자리에서 이름만 바뀐다.
 */
@Composable
private fun navActionLabel(content: RecordUiState.Content?): String? = when {
    content == null || content.groups.isEmpty() -> null
    content.editing -> stringResource(R.string.record_edit_cancel)
    else -> stringResource(R.string.record_edit)
}

/**
 * 편집 중 하단.
 *
 * 고른 건수가 버튼 글자에 들어간다. 시안이 `Select Bar`를 지우고 건수를 이 자리로 옮겼다.
 * 줄 하나를 더 쓰지 않고도 몇 건인지가 누르기 직전에 보인다.
 */
@Composable
private fun DeleteBar(count: Int, onDeleteClick: () -> Unit) {
    MedicalMateBottomCtaBar {
        MedicalMateButton(
            label =
            if (count == 0) {
                stringResource(R.string.record_delete)
            } else {
                stringResource(R.string.record_delete_count, count)
            },
            onClick = onDeleteClick,
            type = MedicalMateButtonType.DANGER,
            enabled = count > 0,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 1j-2. 빈 상태를 화면 위쪽에 둔다. Figma가 가운데가 아니라 1/4 지점에 놓았다. */
@Composable
private fun ColumnScope.EmptyContent(onStartIntakeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s40),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RECORD,
            title = stringResource(R.string.record_empty_title),
            description = stringResource(R.string.record_empty_description),
            actionLabel = stringResource(R.string.record_empty_action),
            onActionClick = onStartIntakeClick,
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun RecordScreenPreview() {
    MedicalMateTheme {
        RecordScreen(
            state = RecordUiState.Content(groups = previewRecordGroups),
            callbacks = RecordCallbacks(),
        )
    }
}

/** 1j-1-D2. 두 건을 고른 편집 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun RecordScreenEditingPreview() {
    MedicalMateTheme {
        RecordScreen(
            state =
            RecordUiState.Content(
                groups = previewRecordGroups,
                selectedIds = setOf("card-3", "card-1"),
            ),
            callbacks = RecordCallbacks(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun RecordScreenEmptyPreview() {
    MedicalMateTheme {
        RecordScreen(
            state = RecordUiState.Content(groups = emptyList()),
            callbacks = RecordCallbacks(),
        )
    }
}
