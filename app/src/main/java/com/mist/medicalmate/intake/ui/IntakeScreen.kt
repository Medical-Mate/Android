package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateProgressIndicator
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1l·1c·1d·1i. 증상 정리 네 단계를 한 화면이 그린다.
 *
 * 단계마다 내용이 크게 다르지만 상단 내비와 진행 표시, 하단 영역은 같다. 목적지를 넷으로
 * 나누면 그 껍데기를 네 번 적고 진행 표시가 어긋난다.
 *
 * 진행 표시는 본문 안에 있다. Figma가 각 화면의 `Content` 첫 줄에 뒀다.
 *
 * 하단은 단계마다 다르다. 문답은 입력창이나 음성이고 나머지는 다음 버튼이다.
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
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        Box(modifier = Modifier.weight(1f)) {
            when (state.step) {
                IntakeStep.BODY_PART -> BodyPartStep(state = state, callbacks = callbacks)
                IntakeStep.SYMPTOM_CHAT -> ChatStep(state = state)
                IntakeStep.SEVERITY ->
                    SeverityStep(state = state, onSeverityChange = callbacks.onSeverityChange)

                IntakeStep.QUESTIONS -> QuestionsStep(state = state, callbacks = callbacks)
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
    val onBodyPartToggle: (BodyMapSelection) -> Unit = {},
    val onBodyFocusClear: () -> Unit = {},
    val onBodyListModeToggle: () -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onSendClick: () -> Unit = {},
    val onVoiceClick: () -> Unit = {},
    val onMicClick: () -> Unit = {},
    val onTypeInsteadClick: () -> Unit = {},
    val onSeverityChange: (MedicalMateSeverity) -> Unit = {},
    val onQuestionDraftChange: (String) -> Unit = {},
    val onAddQuestionClick: () -> Unit = {},
    val onRemoveQuestionClick: (Int) -> Unit = {},
)

/**
 * 진행 표시. 네 단계 모두 본문 첫 줄에 같은 형태로 둔다.
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
            IntakeMessage(4, IntakeMessage.Sender.AI, "명치가 얼마나 아프세요?"),
        ),
    )

/**
 * 하단 영역.
 *
 * 문답 단계만 입력이 붙고 나머지는 다음 버튼이다. 문답에서도 물어볼 것이 남지 않으면
 * 다음 버튼이 함께 나온다. **시안에 문답을 끝내는 조작이 없다.** 그대로 두면 통증
 * 강도로 갈 방법이 없어서 넣었고 디자인 트랙에 남겼다(#69).
 */
@Composable
private fun IntakeFooter(state: IntakeUiState, callbacks: IntakeCallbacks) {
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
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        when (state.step) {
            IntakeStep.SYMPTOM_CHAT -> {
                if (state.chatFinished) {
                    NextButton(onClick = callbacks.onNextClick)
                }
                ChatInput(state = state, callbacks = callbacks)
            }

            IntakeStep.BODY_PART ->
                NextButton(onClick = callbacks.onNextClick, enabled = state.canLeaveBodyPart)

            else -> NextButton(onClick = callbacks.onNextClick)
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
private fun IntakeQuestionsPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state =
            previewChatState.copy(
                step = IntakeStep.QUESTIONS,
                questions =
                listOf(
                    "검사를 받아야 하나요?",
                    "지금 진통제 계속 먹어도 되나요?",
                    "어떤 증상이면 바로 다시 와야 하나요?",
                ),
            ),
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
                    selected =
                    listOf(
                        BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT),
                        BodyMapSelection("ANC:014", "SUR:097", BodyMapSide.LEFT),
                    ),
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
