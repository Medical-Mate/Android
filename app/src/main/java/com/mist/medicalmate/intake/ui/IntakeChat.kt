package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBubble
import com.mist.medicalmate.core.designsystem.component.MedicalMateBubbleSender
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextField
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceInput

/**
 * 2단계 증상 문답. Figma 1c-1 `402:1629`, 1c-2 `402:1506`.
 *
 * AI와 주고받는 대화다. AI는 면색도 테두리도 없이 라벨과 본문만, 환자는 유색 면에 우하
 * 꼬리다. 화면에서 유색 큰 면을 가진 유일한 요소가 환자의 말이다(문서의 컴포넌트 규격).
 *
 * 맨 위에 짚은 부위를 칩으로 남긴다. 대화가 길어져도 무엇에 대한 문답인지 남아 있어야 한다.
 *
 * **마디가 붙으면 끝으로 보낸다**(#176). 그러지 않으면 방금 보낸 말과 AI의 답이 입력창 뒤에
 * 남는다. 키보드 높이를 함께 보는 이유는, 마디가 늘지 않아도 키보드가 올라오면 보이는 높이가
 * 줄어 마지막 마디가 가리기 때문이다. 인셋 자체는 `MainActivity`가 `safeDrawing`으로 합쳐
 * 두어 입력창은 제 자리에 서 있고, 가리는 것은 본문이다.
 */
@Composable
internal fun ChatStep(state: IntakeUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(state.messages.size, state.awaitingReply, imeBottom) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding =
        PaddingValues(
            start = MedicalMateSize.gutter,
            end = MedicalMateSize.gutter,
            top = MedicalMateSpace.s12,
            bottom = MedicalMateSpace.s16,
        ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        item(key = PROGRESS_KEY) { IntakeProgress(state.step) }
        state.bodyPart?.let { part -> item(key = CONTEXT_KEY) { BodyPartContext(part) } }
        items(state.messages, key = { it.id }) { message -> MessageRow(message) }
        if (state.awaitingReply) {
            item(key = TYPING_KEY) { TypingRow() }
        }
    }
}

/** 짚은 부위. Figma `402:1651`. 고른 결과라서 선택된 칩으로 둔다. */
@Composable
private fun BodyPartContext(bodyPart: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.intake_chat_context),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        MedicalMateChip(label = bodyPart, selected = true, onClick = {})
    }
}

/** 한 마디. 환자의 말만 오른쪽으로 붙는다. */
@Composable
private fun MessageRow(message: IntakeMessage) {
    val patient = message.sender == IntakeMessage.Sender.PATIENT

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (patient) Arrangement.End else Arrangement.Start,
    ) {
        MedicalMateBubble(
            text = message.text,
            sender = if (patient) MedicalMateBubbleSender.PATIENT else MedicalMateBubbleSender.AI,
            senderLabel = if (patient) null else stringResource(R.string.intake_chat_ai),
            modifier = if (patient) Modifier else Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 응답을 기다리는 표시. Figma 1c-4의 점 세 개다.
 *
 * 점만으로는 스크린 리더에 아무것도 전달되지 않아 접근성 이름을 붙인다. 점이 움직이지
 * 않는 것은 지금 애니메이션을 넣지 않았기 때문이다.
 */
@Composable
private fun TypingRow() {
    val typingLabel = stringResource(R.string.intake_chat_waiting)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row(
            modifier =
            Modifier
                .background(
                    color = MedicalMateTheme.colors.bgPrimarySubtle,
                    shape = RoundedCornerShape(TypingRadius),
                )
                .padding(horizontal = MedicalMateSpace.s16, vertical = MedicalMateSpace.s14)
                .semantics { contentDescription = typingLabel },
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
        ) {
            repeat(TYPING_DOTS) {
                Box(
                    modifier =
                    Modifier
                        .size(TypingDotSize)
                        .background(
                            color = MedicalMateTheme.colors.fgPrimary,
                            shape = RoundedCornerShape(TypingDotSize / 2),
                        ),
                )
            }
        }
    }
}

/**
 * 하단 입력. 글과 음성 두 가지다.
 *
 * 음성에서도 "직접 입력할게요"가 같은 자리에 남는다. 문서의 컴포넌트 규격과 9절이 요구하는 것이고,
 * 조용한 곳이 아니거나 목소리가 잘 안 나오는 환자에게 음성만 남기면 앱을 쓸 수 없다.
 */
@Composable
internal fun ChatInput(state: IntakeUiState, callbacks: IntakeCallbacks) {
    when (state.inputMode) {
        IntakeInputMode.VOICE ->
            MedicalMateVoiceInput(
                state = state.voice,
                onMicClick = callbacks.onMicClick,
                onTypeInsteadClick = callbacks.onTypeInsteadClick,
            )

        IntakeInputMode.TEXT ->
            MedicalMateTextField(
                value = state.draft,
                onValueChange = callbacks.onDraftChange,
                placeholder = stringResource(R.string.intake_chat_placeholder),
                onSend = callbacks.onSendClick,
                trailing = {
                    // Actions 슬롯. 버튼끼리는 4로 붙인다.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
                    ) {
                        MedicalMateIconButton(
                            onClick = callbacks.onVoiceClick,
                            icon = MedicalMateIcons.Mic,
                            contentDescription = stringResource(R.string.intake_chat_voice),
                        )
                        MedicalMateIconButton(
                            onClick = callbacks.onSendClick,
                            icon = MedicalMateIcons.ArrowUp,
                            contentDescription = stringResource(R.string.intake_chat_send),
                            style = MedicalMateIconButtonStyle.GHOST,
                            enabled = state.canSend,
                        )
                    }
                },
            )
    }
}

/**
 * 고정 항목의 key.
 *
 * 마디 key는 서버의 `seq`라 숫자다. 고정 항목을 숫자로 두면 겹칠 수 있어 문자열로 둔다.
 */
private const val PROGRESS_KEY = "progress"

private const val CONTEXT_KEY = "context"

private const val TYPING_KEY = "typing"

private const val TYPING_DOTS = 3

private val TypingDotSize = 8.dp

/** Figma 환자 버블의 반경과 같다. */
private val TypingRadius = 18.dp
