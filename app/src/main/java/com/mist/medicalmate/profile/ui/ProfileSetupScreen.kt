package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateProgressIndicator
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextField
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltip

/**
 * 와이어프레임 1b-1·1b-2·1b-3. Figma `398:1225`, `398:1285`, `398:1341`.
 *
 * 세 화면이 같은 배치에 문구와 칩 목록만 다르다. [ProfileSetupStep]이 그 차이를 들고
 * 있고 이 화면은 하나다.
 *
 * 마지막 단계의 주 버튼만 "완료"다. 나머지는 "다음"이다.
 *
 * 본문에 스크롤을 준다. 칩이 여러 줄로 흐르고 그 아래 입력 칸까지 있어서 좁은 화면이나
 * 큰 글꼴에서 넘친다.
 */
@Composable
fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    onOptionToggle: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.profile_setup_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MedicalMateSize.gutter,
                    end = MedicalMateSize.gutter,
                    top = MedicalMateSpace.s12,
                    bottom = MedicalMateSpace.s16,
                ),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20),
        ) {
            MedicalMateProgressIndicator(
                current = state.step.number,
                total = ProfileSetupStep.total,
                label = stringResource(R.string.profile_setup_title),
            )
            Question(step = state.step)
            Options(
                step = state.step,
                chosen = state.answer.chosen,
                onOptionToggle = onOptionToggle,
            )
            MedicalMateTextField(
                value = state.answer.note,
                onValueChange = onNoteChange,
                label = stringResource(R.string.profile_setup_other_label),
                placeholder = stringResource(R.string.profile_setup_other_placeholder),
                helperText = stringResource(R.string.profile_setup_other_helper),
            )
        }
        Footer(state = state, onNextClick = onNextClick)
    }
}

/**
 * 질문과 설명. Figma `398:1244`.
 *
 * 알러지 단계에만 툴팁이 붙는다. 왜 묻는지가 그 단계에서만 설명이 필요하다. 브리핑 카드
 * 맨 위에 항상 표시되는 항목이라서다.
 *
 * 말풍선은 [MedicalMateTooltip]이 별도 창에 띄운다. 같은 레이아웃에서 겹치면 감싸는 상자가
 * 커져서 아래 칩들이 밀려 내려간다(#71).
 */
@Composable
private fun Question(step: ProfileSetupStep) {
    Column(
        modifier = Modifier.padding(top = MedicalMateSpace.s8),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(step.questionRes),
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
                modifier = Modifier.weight(1f),
            )
            if (step == ProfileSetupStep.ALLERGIES) {
                MedicalMateTooltip(
                    text = stringResource(R.string.profile_setup_allergies_tooltip),
                    contentDescription = stringResource(R.string.profile_setup_allergies_tooltip_open),
                )
            }
        }
        Text(
            text = stringResource(step.descriptionRes),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 고를 항목들. Figma `398:1247`. 줄이 넘치면 다음 줄로 흐른다. */
@Composable
private fun Options(step: ProfileSetupStep, chosen: Set<String>, onOptionToggle: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        stringArrayResource(step.optionsRes).forEach { option ->
            MedicalMateChip(
                label = option,
                selected = option in chosen,
                onClick = { onOptionToggle(option) },
            )
        }
    }
}

/**
 * 하단 버튼. Figma `398:1275`.
 *
 * **Figma에는 "잘 모르겠어요 · 없어요" 보조 버튼이 하나 더 있는데 넣지 않았다.** 고를 것이
 * 없으면 아무것도 고르지 않고 다음을 누르면 되고, 같은 뜻의 길이 두 개면 어느 쪽을
 * 눌러야 하는지 고민하게 된다. 시안과 어긋나는 지점이라 디자인 트랙에 남겼다(#67).
 *
 * 그 판단을 다시 봐야 한다. 서버가 "없다"와 "모른다"를 다른 값으로 받는데(`NONE`·`UNKNOWN`)
 * 지금은 둘을 구별할 자리가 없어 빈 답을 모두 모른다로 보낸다. 시안의 보조 버튼도 한 개에
 * 두 뜻을 묶어 둬서 그대로 넣어서는 갈리지 않는다(#150).
 *
 * 저장에 실패하면 버튼 위에 한 줄이 붙는다. 마지막 단계에는 다음 화면이 없어서, 조용히
 * 실패하면 버튼이 죽은 것으로 보인다.
 */
@Composable
private fun Footer(state: ProfileSetupUiState, onNextClick: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s8,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.profile_setup_save_failed),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgDanger,
            )
        }
        MedicalMateButton(
            onClick = onNextClick,
            label =
            stringResource(
                if (state.step.isLast) R.string.profile_setup_done else R.string.profile_setup_next,
            ),
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun ProfileSetupScreenPreview() {
    MedicalMateTheme {
        ProfileSetupScreen(
            state =
            ProfileSetupUiState(
                step = ProfileSetupStep.MEDICATIONS,
                answers =
                mapOf(
                    ProfileSetupStep.MEDICATIONS to
                        ProfileSetupAnswer(chosen = setOf("혈압약", "진통제")),
                ),
            ),
            onOptionToggle = {},
            onNoteChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
