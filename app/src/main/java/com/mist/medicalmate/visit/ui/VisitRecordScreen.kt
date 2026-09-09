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
import com.mist.medicalmate.core.designsystem.component.MedicalMateCheckbox
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
}

/** 분류 결과 화면의 조작. 수정은 화면 안에서 모드만 바뀌므로 목적지가 아니다. */
data class VisitRecordCallbacks(
    val onBackClick: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onDraftChange: (Int, String) -> Unit = { _, _ -> },
    val onScheduleChange: (Boolean) -> Unit = {},
    val onSaveClick: () -> Unit = {},
    val onCancelClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

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
 * 읽을 때는 일정 등록 체크와 저장하기다. 수정 중에는 저장하기와 취소 둘이 되고 체크는
 * 숨는다.
 */
@Composable
private fun Footer(state: VisitRecordUiState.Content, callbacks: VisitRecordCallbacks) {
    MedicalMateBottomCtaBar {
        if (state.editing) {
            MedicalMateButton(
                label = stringResource(R.string.visit_record_save),
                onClick = callbacks.onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            )
            MedicalMateButton(
                label = stringResource(R.string.visit_record_cancel),
                onClick = callbacks.onCancelClick,
                type = MedicalMateButtonType.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            MedicalMateCheckbox(
                checked = state.scheduleRevisit,
                onCheckedChange = callbacks.onScheduleChange,
                label = stringResource(R.string.visit_record_schedule),
            )
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
                editing = true,
                drafts = previewVisitRecord.items.map { it.value },
            ),
            callbacks = VisitRecordCallbacks(),
        )
    }
}
