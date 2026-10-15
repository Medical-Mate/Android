package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBubble
import com.mist.medicalmate.core.designsystem.component.MedicalMateBubbleSender
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuestionRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuestionRowState
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.voice.VoiceFocusFrame
import com.mist.medicalmate.core.voice.rememberVoiceFocus

/*
 * 3단계 문답이 끝난 자리의 추천 질문 고르기(1c-D~D4, #310). 문답 목록([ChatStep])이 대화 아래에 잇는다.
 */

/**
 * 추천 질문 고르기(1c-D~D4). 문답 대화 아래에 잇는다.
 *
 * 추천이 없으면(후보를 못 만듦) 소개 말 없이 빈 칸만 둔다. 줄 번호는 위에서부터 차례이고 빈 칸이 그다음
 * 번호다. 담을 수 있는 수가 차면 빈 칸을 감춘다(#264).
 */
internal fun LazyListScope.questionItems(state: IntakeUiState, callbacks: IntakeCallbacks) {
    val suggested = state.questionRows.count { !it.custom }
    if (suggested > 0) {
        item(key = QUESTIONS_INTRO_KEY) { AiLine(countString(suggested, R.string.intake_questions_intro)) }
    }
    item(key = QUESTIONS_KEY) { QuestionRows(state = state, callbacks = callbacks) }
    val picked = state.questions.size
    if (picked > 0) {
        val summary =
            if (state.questionRows.any { it.custom }) {
                R.string.intake_questions_summary_done
            } else {
                R.string.intake_questions_summary_more
            }
        item(key = QUESTIONS_SUMMARY_KEY) { AiLine(countString(picked, summary)) }
    }
}

/** 리소스의 `%1$d`에 수를 넣는다. 한국어라 수에 따라 문장이 갈리지 않는다. */
@Composable
private fun countString(count: Int, id: Int): String = stringResource(id, count)

/** 앱이 거는 AI 말. 서버 대화의 마디가 아니라서 id가 없다. 모양은 AI 마디와 같다. */
@Composable
private fun AiLine(text: String) {
    MedicalMateBubble(
        text = text,
        sender = MedicalMateBubbleSender.AI,
        senderLabel = stringResource(R.string.intake_chat_ai),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 질문 줄과 맨 아래 빈 칸. 줄 사이는 8이다. */
@Composable
private fun QuestionRows(state: IntakeUiState, callbacks: IntakeCallbacks) {
    // 말로 넣은 질문, 지운 뒤의 목록으로 화면이 옮겨 가고 잠깐 테를 두른다(#284).
    val focus = rememberVoiceFocus(state.voiceFocus)
    VoiceFocusFrame(focus, IntakeFocusTarget.Questions, MedicalMateRadius.md) {
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            state.questionRows.forEachIndexed { index, row ->
                VoiceFocusFrame(focus, IntakeFocusTarget.Question(index), MedicalMateRadius.md) {
                    MedicalMateQuestionRow(
                        number = index + 1,
                        question = row.text,
                        state =
                        if (row.selected) MedicalMateQuestionRowState.SELECTED else MedicalMateQuestionRowState.DEFAULT,
                        onClick = { callbacks.onQuestionToggle(index) },
                    )
                }
            }
            BlankRow(state = state, number = state.questionRows.size + 1, onClick = callbacks.onQuestionAddClick)
        }
    }
}

/** 맨 아래 빈 칸. 적는 중이면 그 상태로 선다(1c-D2 · D3). 다 찼으면 없다. */
@Composable
private fun BlankRow(state: IntakeUiState, number: Int, onClick: () -> Unit) {
    when {
        state.writingQuestion ->
            MedicalMateQuestionRow(
                number = number,
                question = stringResource(R.string.intake_questions_writing),
                state =
                if (state.draft.isBlank()) MedicalMateQuestionRowState.FOCUS else MedicalMateQuestionRowState.WRITING,
            )

        !state.questionsFull ->
            MedicalMateQuestionRow(
                number = number,
                question = stringResource(R.string.intake_questions_add_row),
                state = MedicalMateQuestionRowState.ADD,
                onClick = onClick,
            )
    }
}

private const val QUESTIONS_INTRO_KEY = "questions-intro"

private const val QUESTIONS_KEY = "questions"

private const val QUESTIONS_SUMMARY_KEY = "questions-summary"

/** Preview용. 1c-D의 마지막 대화 한 줄이다. */
private val previewQuestionsState =
    IntakeUiState(
        step = IntakeStep.SYMPTOM_CHAT,
        bodyPart = "복부",
        messages = listOf(IntakeMessage(0, IntakeMessage.Sender.PATIENT, "혈압약 먹고 있고, 속이 더부룩하고 트림이 자주 나요.")),
    )

/** 1c-D1. 추천 셋 가운데 둘을 골랐다. */
@MedicalMateScreenPreviews
@Composable
private fun IntakeQuestionsPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state =
            previewQuestionsState.copy(
                chatFinished = true,
                questionRows =
                listOf(
                    IntakeQuestion("검사를 받아야 하나요?", selected = true),
                    IntakeQuestion("지금 진통제 계속 먹어도 되나요?", selected = false),
                    IntakeQuestion("어떤 증상이면 바로 다시 와야 하나요?", selected = true),
                ),
            ),
            callbacks = IntakeCallbacks(),
        )
    }
}

/** 1c-D3. 빈 칸에 질문을 적는 중이다. */
@MedicalMateScreenPreviews
@Composable
private fun IntakeQuestionWritingPreview() {
    MedicalMateTheme {
        IntakeScreen(
            state =
            previewQuestionsState.copy(
                chatFinished = true,
                writingQuestion = true,
                draft = "공복으로 가야 하나요?",
                questionRows =
                listOf(
                    IntakeQuestion("검사를 받아야 하나요?", selected = true),
                    IntakeQuestion("지금 진통제 계속 먹어도 되나요?", selected = false),
                    IntakeQuestion("어떤 증상이면 바로 다시 와야 하나요?", selected = true),
                ),
            ),
            callbacks = IntakeCallbacks(),
        )
    }
}
