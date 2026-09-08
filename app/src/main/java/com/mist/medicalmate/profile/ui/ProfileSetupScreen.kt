package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltipBubble
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltipTrigger

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
        Footer(isLast = state.step.isLast, onNextClick = onNextClick)
    }
}

/**
 * 질문과 설명. Figma `398:1244`.
 *
 * 알러지 단계에만 툴팁 트리거가 붙는다. 왜 묻는지가 그 단계에서만 설명이 필요하다.
 * 브리핑 카드 맨 위에 항상 표시되는 항목이라서다.
 */
@Composable
private fun Question(step: ProfileSetupStep) {
    var tooltipOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(top = MedicalMateSpace.s8)) {
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(step.questionRes),
                    style = MedicalMateTheme.typography.headingL,
                    color = MedicalMateTheme.colors.fgDefault,
                    modifier = Modifier.weight(1f),
                )
                if (step == ProfileSetupStep.ALLERGIES) {
                    MedicalMateTooltipTrigger(
                        active = tooltipOpen,
                        onClick = { tooltipOpen = !tooltipOpen },
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
        if (step == ProfileSetupStep.ALLERGIES && tooltipOpen) {
            // 말풍선을 흐름에 넣지 않고 얹는다. 흐름에 넣으면 열고 닫을 때마다 아래 내용이
            // 밀린다. Figma도 설명 문구 위에 겹쳐 뒀다.
            MedicalMateTooltipBubble(
                text = stringResource(R.string.profile_setup_allergies_tooltip),
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = TooltipTop),
            )
        }
    }
}

/** 질문 한 줄(행간 34) 아래에 붙는다. 트리거 바로 밑이다. */
private val TooltipTop = 38.dp

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
 */
@Composable
private fun Footer(isLast: Boolean, onNextClick: () -> Unit) {
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
    ) {
        MedicalMateButton(
            onClick = onNextClick,
            label =
            stringResource(
                if (isLast) R.string.profile_setup_done else R.string.profile_setup_next,
            ),
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
