package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateSeveritySlider
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.voice.VoiceFocusFrame
import com.mist.medicalmate.core.voice.rememberVoiceFocus
import com.mist.medicalmate.core.voice.rememberVoiceScrollState
import com.mist.medicalmate.core.voice.voicePanelSpace

/**
 * 단계별 본문. [IntakeScreen]에서만 쓴다.
 *
 * 세 단계가 상단 내비와 하단 영역을 공유하고 이 파일이 그 사이를 채운다. 3단계 문답은 `IntakeChat.kt`에 있다.
 */

/**
 * 단계 본문의 공통 껍데기. 여백과 간격, 스크롤이 네 단계에서 같다.
 *
 * [scrollKey]가 바뀌면 스크롤이 맨 위로 돌아간다. 한 단계 안에서 화면이 갈리는 곳이
 * 있어서(인체도의 앵커·확대·목록) 하나의 스크롤 상태를 공유하면, 아래로 내려 부위를 짚은
 * 뒤 확대 화면이 그 위치로 열려 제목이 잘린다.
 */
@Composable
internal fun StepContent(
    step: IntakeStep,
    modifier: Modifier = Modifier,
    scrollKey: Any? = Unit,
    spacing: Dp = MedicalMateSpace.s14,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .verticalScroll(key(scrollKey) { rememberVoiceScrollState() }).voicePanelSpace()
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        IntakeProgress(step)
        content()
    }
}

/**
 * 2단계 통증 강도. Figma 1d `3663:7170`. 블록 간격이 20이고 물음과 설명이 위 8 · 사이 8로 묶인다.
 *
 * 판독 카드는 [MedicalMateSeveritySlider]가 안에 그린다. 고른 단계의 숫자와 낱말, 상황
 * 설명이 거기 함께 나온다. 화면에서 [MedicalMateSeverityReadout]을 따로 얹지 않는다.
 * 그것은 브리핑 카드에서 값을 보여주는 출력 전용이고(문서의 컴포넌트 규격), 함께 두면 같은 수치가
 * 두 곳에 나온다.
 *
 * 안내는 숫자가 어디에 쓰이는지 알려준다. NRS 등가를 카드에 함께 싣는 것은 의사가 읽는
 * 값이고, 환자는 낱말로 고르면 된다는 뜻이다.
 */
@Composable
internal fun SeverityStep(
    state: IntakeUiState,
    onSeverityChange: (MedicalMateSeverity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = rememberVoiceFocus(state.voiceFocus)
    StepContent(step = state.step, modifier = modifier, spacing = MedicalMateSpace.s20) {
        Column(
            modifier = Modifier.padding(top = MedicalMateSpace.s8),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        ) {
            Text(
                text = stringResource(R.string.intake_severity_question, state.bodyPartSubject),
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
            )
            Text(
                text = stringResource(R.string.intake_severity_description),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        VoiceFocusFrame(focus, IntakeFocusTarget.Severity, MedicalMateRadius.md) {
            MedicalMateSeveritySlider(severity = state.severity, onSeverityChange = onSeverityChange)
        }
        MedicalMateNotice(
            title = stringResource(R.string.intake_severity_notice_title),
            body = stringResource(R.string.intake_severity_notice_body),
        )
    }
}
