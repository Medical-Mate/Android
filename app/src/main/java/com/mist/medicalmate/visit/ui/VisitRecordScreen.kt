package com.mist.medicalmate.visit.ui

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
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1q-1과 1q-1-E. Figma `405:2193`, `636:3675`.
 *
 * 메모를 AI가 소견·검사·약·재방문으로 나눈 결과다. 읽기와 전체 수정이 같은 화면의 두
 * 모드고, 아래에 원문 메모를 그대로 남긴다. 나눈 것이 틀렸을 때 대조할 것이 원문뿐이다.
 *
 * 수정 중에는 일정 등록 체크박스가 숨는다. 시안이 그 자리를 `hidden`으로 표시해 뒀다.
 * 고치는 동안 일정까지 결정하게 두면 무엇을 저장하는지가 흐려진다.
 */
@Composable
fun VisitRecordScreen(state: VisitRecordUiState, callbacks: VisitRecordCallbacks, modifier: Modifier = Modifier) {
    val content = state as? VisitRecordUiState.Content
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_record_title),
            onLeadingClick = callbacks.onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
            actionLabel = content?.let { stringResource(navActionLabel(it)) },
            onActionClick = content?.let { { onNavAction(it, callbacks) } },
        )
        when (state) {
            VisitRecordUiState.Loading ->
                MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))

            VisitRecordUiState.Failed ->
                FailedContent(onRetryClick = callbacks.onRetryClick, modifier = Modifier.weight(1f))

            is VisitRecordUiState.Content -> {
                RecordContent(state = state, callbacks = callbacks)
                Footer(state = state, callbacks = callbacks)
            }
        }
    }

    if (content?.deleteRequested == true) {
        MedicalMateDialog(
            title = stringResource(R.string.visit_record_delete_title),
            message = stringResource(R.string.visit_record_delete_body),
            confirmLabel = stringResource(R.string.visit_record_delete_confirm),
            onConfirm = callbacks.onDeleteConfirm,
            dismissLabel = stringResource(R.string.visit_record_cancel),
            onDismissRequest = callbacks.onDeleteDismiss,
        )
    }
}

/** 분류 결과 화면의 조작. 편집은 화면 안에서 모드만 바뀌므로 목적지가 아니다. */
data class VisitRecordCallbacks(
    val onBackClick: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onEditDoneClick: () -> Unit = {},
    val onCancelClick: () -> Unit = {},
    val edit: VisitRecordEditActions = VisitRecordEditActions {},
    val onSaveClick: () -> Unit = {},
    val onDeleteClick: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

/**
 * Nav 우측 버튼의 이름. 브리핑 카드와 같은 규칙이다.
 *
 * 편집 중이 아니면 `편집`, 편집 중이고 바뀐 것이 없으면 `취소`, 바뀐 것이 있으면 `확인`이다.
 */
private fun navActionLabel(content: VisitRecordUiState.Content): Int = when {
    !content.editing -> R.string.visit_record_edit
    content.changed -> R.string.visit_record_edit_done
    else -> R.string.visit_record_cancel
}

private fun onNavAction(content: VisitRecordUiState.Content, callbacks: VisitRecordCallbacks) {
    when {
        !content.editing -> callbacks.onEditClick()
        content.changed -> callbacks.onEditDoneClick()
        else -> callbacks.onCancelClick()
    }
}

@Composable
private fun ColumnScope.RecordContent(state: VisitRecordUiState.Content, callbacks: VisitRecordCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
    ) {
        VisitRecordCard(state = state, callbacks = callbacks)
    }
}

/**
 * 하단.
 *
 * 읽을 때는 일정 등록 체크와 저장하기다. 편집 중에는 `진료 후 기록 삭제` 하나가 되고 체크는
 * 숨는다. 사본을 옮기는 것은 Nav 우측 `확인`이 하므로 저장하기를 함께 두면 같은 일이 두
 * 번이 된다.
 *
 * 저장에 실패하면 버튼 위에 그 사실을 적는다. 아무 말도 하지 않으면 버튼이 안 먹는 것으로
 * 읽고 계속 누르게 된다 — 기기에서 실제로 세 번 눌러 세 번 거절당했다(#198).
 */
@Composable
private fun Footer(state: VisitRecordUiState.Content, callbacks: VisitRecordCallbacks) {
    MedicalMateBottomCtaBar {
        if (state.editing) {
            MedicalMateButton(
                label = stringResource(R.string.visit_record_delete),
                onClick = callbacks.onDeleteClick,
                type = MedicalMateButtonType.DANGER,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // **재방문 일정 등록 체크가 없다.** 시안에서 빠졌다(#170). 저장 요청에 재방문
            // 날짜를 실을 자리가 없어서 눌러도 아무 일이 없던 자리이기도 하다. 재방문이
            // 캘린더로 이어지는 길은 일자 화면의 "다음 일정"(1r-2-A)이 맡는다.
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.visit_record_save_failed),
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgDanger,
                )
            }
            MedicalMateButton(
                label = stringResource(R.string.visit_record_save),
                onClick = callbacks.onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FailedContent(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.visit_record_failed_title),
            description = stringResource(R.string.visit_record_failed_description),
            actionLabel = stringResource(R.string.visit_record_retry),
            onActionClick = onRetryClick,
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun VisitRecordScreenPreview() {
    MedicalMateTheme {
        VisitRecordScreen(
            state = VisitRecordUiState.Content(record = previewVisitRecord),
            callbacks = VisitRecordCallbacks(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun VisitRecordScreenEditingPreview() {
    MedicalMateTheme {
        VisitRecordScreen(
            state =
            VisitRecordUiState.Content(
                record = previewVisitRecord,
                draft = VisitRecordDraft.of(previewVisitRecord),
            ),
            callbacks = VisitRecordCallbacks(),
        )
    }
}
