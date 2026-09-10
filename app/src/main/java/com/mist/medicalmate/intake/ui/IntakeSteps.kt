package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateSeveritySlider
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextField

/**
 * 단계별 본문. [IntakeScreen]에서만 쓴다.
 *
 * 네 단계가 상단 내비와 하단 영역을 공유하고 이 파일이 그 사이를 채운다.
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
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .verticalScroll(key(scrollKey) { rememberScrollState() })
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        IntakeProgress(step)
        content()
    }
}

/**
 * 3단계 통증 강도. Figma 1d `402:1934`.
 *
 * 판독 카드는 [MedicalMateSeveritySlider]가 안에 그린다. 고른 단계의 숫자와 낱말, 상황
 * 설명이 거기 함께 나온다. 화면에서 [MedicalMateSeverityReadout]을 따로 얹지 않는다.
 * 그것은 브리핑 카드에서 값을 보여주는 출력 전용이고(문서 8.3), 함께 두면 같은 수치가
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
    StepContent(step = state.step, modifier = modifier) {
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
        MedicalMateSeveritySlider(severity = state.severity, onSeverityChange = onSeverityChange)
        MedicalMateNotice(
            title = stringResource(R.string.intake_severity_notice_title),
            body = stringResource(R.string.intake_severity_notice_body),
        )
    }
}

/**
 * 4단계 추가 질문. Figma 1i `489:5606`.
 *
 * 적은 질문이 브리핑 카드 맨 아래에 함께 담긴다. 진료실에서 잊고 못 꺼내는 것을 막는
 * 자리다.
 */
@Composable
internal fun QuestionsStep(state: IntakeUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    StepContent(step = state.step, modifier = modifier) {
        Text(
            text = stringResource(R.string.intake_questions_question),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = stringResource(R.string.intake_questions_description),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        MedicalMateTextField(
            value = state.questionDraft,
            onValueChange = callbacks.onQuestionDraftChange,
            placeholder = stringResource(R.string.intake_questions_placeholder),
            trailing = {
                MedicalMateIconButton(
                    onClick = callbacks.onAddQuestionClick,
                    icon = MedicalMateIcons.Plus,
                    contentDescription = stringResource(R.string.intake_questions_add),
                    enabled = state.canAddQuestion,
                )
            },
        )
        if (state.questions.isNotEmpty()) {
            QuestionsHeader(count = state.questions.size)
            Text(
                text = stringResource(R.string.intake_questions_ai_hint),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
            state.questions.forEachIndexed { index, question ->
                QuestionRow(
                    number = index + 1,
                    question = question,
                    onRemoveClick = { callbacks.onRemoveQuestionClick(index) },
                )
            }
        }
    }
}

/**
 * 적어둔 질문 목록의 머리. 개수는 누를 수 없는 표시라 `Section Header`의 액션 자리에 두지
 * 않는다. 그 자리는 눌리는 라벨이고, 개수를 넣으면 눌러 볼 것처럼 보인다.
 */
@Composable
private fun QuestionsHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.intake_questions_saved),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.intake_questions_count, count),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 적어둔 질문 한 줄.
 *
 * 번호를 원 안에 둔다. 진료실에서 순서대로 꺼내는 목록이라 몇 번째인지가 보여야 한다.
 * 지우기는 아이콘만 두고 접근성 이름에 몇 번째 질문인지 담는다.
 */
@Composable
private fun QuestionRow(number: Int, question: String, onRemoveClick: () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.sm)
            .padding(
                start = MedicalMateSpace.s12,
                end = MedicalMateSpace.s4,
                top = MedicalMateSpace.s4,
                bottom = MedicalMateSpace.s4,
            ),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier
                .size(QuestionNumberSize)
                .background(color = MedicalMateTheme.colors.bgPrimary, shape = MedicalMateRadius.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MedicalMateTheme.typography.labelS,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
        }
        Text(
            text = question,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        MedicalMateIconButton(
            onClick = onRemoveClick,
            icon = MedicalMateIcons.Close,
            contentDescription = stringResource(R.string.intake_questions_remove, number),
        )
    }
}

/** 번호 원. Figma 1i의 원이 22다. */
private val QuestionNumberSize = 22.dp
