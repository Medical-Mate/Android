package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextArea
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltip

/**
 * 와이어프레임 1p. Figma `1185:12667`.
 *
 * 진료실에서 들은 말을 정리하지 않고 그대로 받는다. 형식을 물으면 기억이 먼저 흐려진다.
 * 나누는 일은 다음 화면에서 AI가 한다.
 *
 * "AI로 정리하기"는 지금 넘어가는 조작이 아니라 적은 것을 다듬는 조작이다. 옆의 툴팁이
 * 무엇을 하는지 알린다. 저장하기를 누르면 분류 결과(1q-1)로 간다.
 *
 * 마이크는 본문 흐름이 아니라 우하단에 뜬다. 적는 도중 어디까지 썼든 손이 닿는 자리에
 * 있어야 하는 조작인데, 본문 끝에 두면 스크롤을 내려야 나온다. 캘린더의 일정 추가와 같은
 * 배치다.
 */
@Composable
fun VisitNoteScreen(state: VisitNoteUiState, callbacks: VisitNoteCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_note_title),
            onLeadingClick = callbacks.onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        Box(modifier = Modifier.weight(1f)) {
            NoteContent(state = state, callbacks = callbacks)
            MedicalMateIconButton(
                onClick = callbacks.onVoiceClick,
                icon = MedicalMateIcons.Mic,
                contentDescription = stringResource(R.string.visit_note_voice),
                style = MedicalMateIconButtonStyle.SOLID,
                modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MedicalMateSize.gutter),
            )
        }
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.visit_note_save),
                onClick = callbacks.onSaveClick,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 메모 화면에서 나가는 길과 화면 안의 조작. */
data class VisitNoteCallbacks(
    val onBackClick: () -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onOrganizeClick: () -> Unit = {},
    val onVoiceClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

@Composable
private fun NoteContent(state: VisitNoteUiState, callbacks: VisitNoteCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        Heading()
        VisitCard(state.visit)
        MedicalMateTextArea(
            value = state.note,
            onValueChange = callbacks.onNoteChange,
            placeholder = stringResource(R.string.visit_note_placeholder),
            enabled = !state.organizing,
            maxLength = NOTE_MAX_LENGTH,
        )
        OrganizeRow(state = state, onOrganizeClick = callbacks.onOrganizeClick)
    }
}

@Composable
private fun Heading() {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6)) {
        Text(
            text = stringResource(R.string.visit_note_heading),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = stringResource(R.string.visit_note_description),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 어떤 진료를 적는 것인지. 카드로 진료를 받았으면 그 카드의 제목이 아래 줄에 온다. */
@Composable
private fun VisitCard(visit: VisitHeadline) {
    MedicalMateCard(emphasis = MedicalMateCardEmphasis.QUIET) {
        Text(
            text = visit.label,
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Text(
            text = visit.title,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = visit.detail,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * AI로 정리하기.
 *
 * 칩으로 둔다. 화면의 주 행동은 저장하기이고, 이것은 적은 글을 다듬는 보조 조작이다.
 * 눌러도 화면이 바뀌지 않으므로 선택 상태가 아니라 누르는 조작으로 쓴다.
 */
@Composable
private fun OrganizeRow(state: VisitNoteUiState, onOrganizeClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateChip(
            label = stringResource(R.string.visit_note_organize),
            selected = false,
            onClick = onOrganizeClick,
            enabled = state.note.isNotBlank() && !state.organizing,
        )
        // 툴팁 트리거는 블록 우측 끝에 둔다(Figma 툴팁 배치 규칙).
        MedicalMateTooltip(
            text = stringResource(R.string.visit_note_organize_tooltip),
            contentDescription = stringResource(R.string.visit_note_organize_tooltip_label),
        )
    }
}

/** 시안의 카운터가 `71 / 300`이다. */
private const val NOTE_MAX_LENGTH = 300

@MedicalMateScreenPreviews
@Composable
private fun VisitNoteScreenPreview() {
    MedicalMateTheme {
        VisitNoteScreen(
            state = VisitNoteUiState(visit = previewVisitHeadline, note = PREVIEW_VISIT_NOTE),
            callbacks = VisitNoteCallbacks(),
        )
    }
}
