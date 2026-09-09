package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextField

/**
 * 와이어프레임 1s-2. Figma `407:2650`.
 *
 * 복용약·기저질환·알러지를 다시 고른다. 세 갈래를 한 화면에 두는 것이 1b와 다른 점이다.
 * 처음 등록할 때는 한 번에 하나씩 물어야 하지만, 고칠 때는 무엇이 들어 있는지 한눈에
 * 보이는 쪽이 낫다.
 *
 * 뒤로가 아니라 닫기다. 흐름을 한 단계 되돌리는 것이 아니라 수정 자체를 그만두는 것이다.
 *
 * 맨 위 안내가 "다음 브리핑 카드부터 반영된다"고 알린다. 이미 진료실에서 보여준 카드가
 * 바뀌면 의사가 본 내용과 기록이 달라진다.
 */
@Composable
fun HealthEditScreen(state: HealthEditUiState, callbacks: HealthEditCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.health_edit_title),
            leading = MedicalMateNavLeading.CLOSE,
            onLeadingClick = callbacks.onCloseClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        EditContent(state = state, callbacks = callbacks)
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.health_edit_save),
                onClick = callbacks.onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            )
            MedicalMateButton(
                label = stringResource(R.string.health_edit_cancel),
                onClick = callbacks.onCloseClick,
                type = MedicalMateButtonType.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 수정 화면의 조작. 저장과 닫기가 나가는 길이다. */
data class HealthEditCallbacks(
    val onCloseClick: () -> Unit = {},
    val onOptionClick: (ProfileSetupStep, String) -> Unit = { _, _ -> },
    val onAddClick: (ProfileSetupStep) -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onDraftSubmit: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

@Composable
private fun ColumnScope.EditContent(state: HealthEditUiState, callbacks: HealthEditCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s24),
    ) {
        MedicalMateNotice(
            title = stringResource(R.string.health_edit_notice_title),
            body = stringResource(R.string.health_edit_notice_body),
        )
        ProfileSetupStep.entries.forEach { step ->
            EditGroup(step = step, state = state, callbacks = callbacks)
        }
    }
}

@Composable
private fun EditGroup(step: ProfileSetupStep, state: HealthEditUiState, callbacks: HealthEditCallbacks) {
    val chosen = state.chosenIn(step)
    val options = stringArrayResource(step.optionsRes).toList() + state.extrasIn(step)

    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10)) {
        Text(
            text = stringResource(step.labelRes),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        ) {
            options.forEach { option ->
                MedicalMateChip(
                    label = option,
                    selected = option in chosen,
                    onClick = { callbacks.onOptionClick(step, option) },
                )
            }
            MedicalMateChip(
                label = stringResource(R.string.health_edit_add),
                selected = false,
                onClick = { callbacks.onAddClick(step) },
            )
        }
        if (state.adding == step) {
            AddField(state = state, callbacks = callbacks)
        }
    }
}

/**
 * 직접 추가.
 *
 * 시안에는 "+ 직접 추가" 칩만 있고 누른 뒤가 그려져 있지 않다. 추가 질문(1i)이 입력 칸과
 * 더하기 버튼으로 목록을 늘리므로 같은 방식으로 뒀다. 같은 일을 하는 두 화면이 다르게
 * 동작하면 익힐 것이 늘어난다.
 */
@Composable
private fun AddField(state: HealthEditUiState, callbacks: HealthEditCallbacks) {
    MedicalMateTextField(
        value = state.draft,
        onValueChange = callbacks.onDraftChange,
        placeholder = stringResource(R.string.health_edit_add_placeholder),
        trailing = {
            MedicalMateIconButton(
                onClick = callbacks.onDraftSubmit,
                icon = MedicalMateIcons.Plus,
                contentDescription = stringResource(R.string.health_edit_add_submit),
                enabled = state.canAddDraft,
            )
        },
    )
}

@MedicalMateScreenPreviews
@Composable
private fun HealthEditScreenPreview() {
    MedicalMateTheme {
        HealthEditScreen(state = previewHealthEdit, callbacks = HealthEditCallbacks())
    }
}
