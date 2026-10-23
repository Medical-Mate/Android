package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateFooter
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateProgressIndicator
import com.mist.medicalmate.core.model.IntakeStep

/**
 * 증상 정리 세 단계를 한 화면이 그린다(#310). 1단계 부위(앱의 3D 인체도), 2단계 통증 강도(1d), 3단계
 * 문답과 추천 질문 고르기(1c-2 · 1c-3 · 1c-D~D4)다.
 *
 * 단계마다 내용이 크게 다르지만 상단 내비와 진행 표시, 하단 영역은 같다. 목적지를 셋으로
 * 나누면 그 껍데기를 세 번 적고 진행 표시가 어긋난다.
 *
 * 진행 표시는 본문 안에 있다. Figma가 각 화면의 `Content` 첫 줄에 뒀다.
 *
 * 상단 내비는 불투명이다. 10-15 시안의 Nav Bar 면이 `bg/surface` 100%다.
 */
@Composable
fun IntakeScreen(state: IntakeUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.intake_title),
            onLeadingClick = callbacks.onBackClick,
        )
        Box(modifier = Modifier.weight(1f)) {
            when (state.step) {
                IntakeStep.BODY_PART -> BodyPartStep(state = state, callbacks = callbacks)
                IntakeStep.SEVERITY ->
                    SeverityStep(state = state, onSeverityChange = callbacks.onSeverityChange)

                IntakeStep.SYMPTOM_CHAT -> ChatStep(state = state, callbacks = callbacks)
            }
        }
        IntakeFooter(state = state, callbacks = callbacks)
    }
}

/**
 * 단계에서 나가는 길들.
 *
 * 파라미터로 하나씩 받으면 열 개가 넘는다. 단계마다 쓰는 것이 다르고 화면 하나가 전부를
 * 들고 있어야 해서 한 덩어리로 묶었다.
 */
data class IntakeCallbacks(
    val onBackClick: () -> Unit = {},
    val onNextClick: () -> Unit = {},
    val onBodyViewChange: (BodyMapView) -> Unit = {},
    val onBodyDotClick: (String) -> Unit = {},
    val onBodySideAnchorClick: (String) -> Unit = {},
    val onBodyPartSelect: (BodyMapSelection) -> Unit = {},
    /** 3D 판에서 세부 부위를 짚었다. 고른 곳을 다시 짚으면 푼다(#331). */
    val onBodyPartToggle: (BodyMapSelection) -> Unit = {},
    val onBodyAnchorFocus: (BodyMapSelection) -> Unit = {},
    val onBodyFocusClear: () -> Unit = {},
    val onBodyListModeToggle: () -> Unit = {},
    val onBodyMap3dToggle: () -> Unit = {},
    val onBodyReset: () -> Unit = {},
    val onBodySearchChange: (String) -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onSendClick: () -> Unit = {},
    val onVoiceClick: () -> Unit = {},
    val onMicClick: () -> Unit = {},
    val onTypeInsteadClick: () -> Unit = {},
    val onSeverityChange: (MedicalMateSeverity) -> Unit = {},
    val onQuestionToggle: (Int) -> Unit = {},
    val onQuestionAddClick: () -> Unit = {},
)

/**
 * 진행 표시. 세 단계 모두 본문 첫 줄에 같은 형태로 둔다.
 *
 * 화면 껍데기에 두지 않고 각 단계가 부르는 이유는 Figma가 `Content` 안에 뒀기 때문이다.
 * 본문이 스크롤되면 함께 올라간다.
 */
@Composable
internal fun IntakeProgress(step: IntakeStep) {
    MedicalMateProgressIndicator(
        current = step.number,
        total = IntakeStep.total,
        label = stringResource(R.string.intake_progress_label),
    )
}

/** Preview용 대화. Figma 1c-1의 내용이다. */
private val previewChatState =
    IntakeUiState(
        step = IntakeStep.SYMPTOM_CHAT,
        bodyPart = "복부",
        messages =
        listOf(
            IntakeMessage(0, IntakeMessage.Sender.AI, "복부가 불편하시군요. 언제부터 그러셨어요? 정확하지 않아도 괜찮아요."),
            IntakeMessage(1, IntakeMessage.Sender.PATIENT, "한 3주쯤 됐어요. 요즘 더 아파요."),
            IntakeMessage(2, IntakeMessage.Sender.AI, "3주 전부터 점점 심해지셨네요. 어떨 때 더 아프세요?"),
            IntakeMessage(3, IntakeMessage.Sender.PATIENT, "밥 먹고 30분쯤 지나면 명치가 쓰려요."),
        ),
    )

/**
 * 하단 영역. 10-15 시안의 `Footer`(그림자 없음)다.
 *
 * 문답 중에는 입력이고(1c-2 · 1c-3), 문답이 끝나면 다음 버튼만 남는다(1c-D). 빈 칸을 눌러 질문을 적는
 * 동안은 다시 입력이 선다(1c-D2 · D3). 나머지 단계는 다음 버튼이다.
 */
@Composable
private fun IntakeFooter(state: IntakeUiState, callbacks: IntakeCallbacks) {
    MedicalMateFooter {
        when {
            state.step == IntakeStep.BODY_PART ->
                NextButton(onClick = callbacks.onNextClick, enabled = state.canLeaveBodyPart)

            state.step == IntakeStep.SYMPTOM_CHAT && (!state.chatFinished || state.writingQuestion) ->
                ChatInput(state = state, callbacks = callbacks)

            else -> {
                // 질문 저장이 안 되면 버튼 위에 알린다(#344, 사용자 결정 — 시안에 없다). 카드 편집 실패와 같은 문구 · 자리다.
                if (state.questionsSaveFailed) {
                    Text(
                        text = stringResource(R.string.intake_questions_save_failed),
                        style = MedicalMateTheme.typography.bodyS,
                        color = MedicalMateTheme.colors.fgDanger,
                    )
                }
                NextButton(onClick = callbacks.onNextClick, enabled = !state.savingQuestions)
            }
        }
    }
}

@Composable
private fun NextButton(onClick: () -> Unit, enabled: Boolean = true) {
    MedicalMateButton(
        onClick = onClick,
        label = stringResource(R.string.intake_next),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@MedicalMateScreenPreviews
@Composable
private fun IntakeChatPreview() {
    MedicalMateTheme {
        IntakeScreen(state = previewChatState, callbacks = IntakeCallbacks())
    }
}

@MedicalMateScreenPreviews
@Composable
private fun IntakeSeverityPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state = previewChatState.copy(step = IntakeStep.SEVERITY),
            callbacks = IntakeCallbacks(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun IntakeBodyAnchorPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state = IntakeUiState(step = IntakeStep.BODY_PART),
            callbacks = IntakeCallbacks(),
        )
    }
}

/** 무릎을 짚기 직전. 다리 앵커를 왼쪽으로 골라 확대한 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun IntakeBodyZonePreview() {
    MedicalMateTheme {
        IntakeScreen(
            state =
            IntakeUiState(
                step = IntakeStep.BODY_PART,
                bodyMap =
                BodyMapUiState(
                    focus = BodyMapSelection("ANC:014", side = BodyMapSide.LEFT),
                    selection = BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT),
                ),
            ),
            callbacks = IntakeCallbacks(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun IntakeBodyListPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state = IntakeUiState(step = IntakeStep.BODY_PART, bodyMap = BodyMapUiState(byList = true)),
            callbacks = IntakeCallbacks(),
        )
    }
}
