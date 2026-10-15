package com.mist.medicalmate.intake.ui

import android.provider.Settings
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
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
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceInputMate
import com.mist.medicalmate.core.voice.rememberVoiceLazyListState
import com.mist.medicalmate.core.voice.voicePanelInset

/**
 * 3단계 증상 문답과 추천 질문 고르기. Figma 1c-2 `3663:7181`, 1c-3 `3663:7196`, 1c-D~D4 `3663:7218`~`3663:7276`.
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
 *
 * **말로 할 때도 같다**(#190). 말하는 중인 글은 마디 수를 늘리지 않고 자라기만 하고, 음성
 * 패널은 키보드보다 높아서 입력칸일 때보다 더 가린다. 그 둘을 함께 봐야 말하는 글이 패널 뒤로
 * 들어가지 않는다.
 *
 * **문답이 끝나면 같은 대화 아래에 추천 질문이 선다**(#310). AI 말로 무엇을 하라는지 알리고, `Question Row`를
 * 눌러 담을 것을 체크하며, 맨 아래 빈 칸에 직접 적는다. 하나라도 담으면 아래에 몇 개를 담는지 AI 말이 한 번 더
 * 선다. 질문 줄이 늘거나 적기 시작하면 끝으로 보낸다.
 */
@Composable
internal fun ChatStep(state: IntakeUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    val listState = rememberVoiceLazyListState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(
        state.messages.size,
        state.awaitingReply,
        state.speaking,
        state.inputMode,
        imeBottom,
        state.questionRows.size,
        state.writingQuestion,
    ) {
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
            bottom = MedicalMateSpace.s16 + voicePanelInset(),
        ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        item(key = PROGRESS_KEY) { IntakeProgress(state.step) }
        state.bodyPart?.let { part -> item(key = CONTEXT_KEY) { BodyPartContext(part) } }
        items(state.messages, key = { it.id }) { message -> MessageRow(message) }
        // 말하는 중인 글은 대화에 미리 세운다. 음성일 때는 입력칸이 패널로 바뀌어 있어서
        // 적힌 글을 볼 자리가 거기밖에 없다. 보내면 진짜 마디가 그 자리를 대신한다.
        state.speaking?.let { text -> item(key = SPEAKING_KEY) { MessageRow(text) } }
        if (state.awaitingReply) {
            item(key = TYPING_KEY) { TypingRow() }
        }
        if (state.chatFinished) questionItems(state = state, callbacks = callbacks)
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

/** 말하는 중인 글. 아직 보내지 않아 id가 없다. */
@Composable
private fun MessageRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        MedicalMateBubble(text = text, sender = MedicalMateBubbleSender.PATIENT)
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
 * **점이 차례로 떠오른다**(#228). 멈춰 있는 점 셋은 답을 기다리는 중인지 거기서 끝난 것인지
 * 구별되지 않는다. 세 점이 같은 주기를 나눠 갖고 어긋나게 오르내린다.
 *
 * **AI의 말이 설 자리에 선다.** 왼쪽이고 말풍선 면이 없다. 오른쪽은 환자가 보낸 말의
 * 자리다 — 거기에 두면 방금 보낸 것이 아직 안 갔다는 뜻으로 읽힌다. AI의 말도 면 없이
 * 글만 서므로 점도 면을 두르지 않는다.
 *
 * 기기에서 애니메이션을 껐으면 움직이지 않는다. 개발자 옵션의 배율이 0이면 전환 효과를 끈
 * 것이고, 그 설정을 무시하는 애니메이션은 두지 않는다.
 *
 * 점만으로는 스크린 리더에 아무것도 전달되지 않아 접근성 이름을 붙인다.
 */
@Composable
private fun TypingRow() {
    val typingLabel = stringResource(R.string.intake_chat_waiting)
    val context = LocalContext.current
    val animated =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }
    val cycle = rememberInfiniteTransition(label = "typing")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            modifier =
            Modifier
                .padding(vertical = MedicalMateSpace.s14)
                .semantics { contentDescription = typingLabel },
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
        ) {
            repeat(TYPING_DOTS) { index ->
                // 한 주기를 셋이 나눠 갖는다. 점마다 시작을 늦춰 물결처럼 이어진다.
                val lift by cycle.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec =
                    infiniteRepeatable(
                        animation =
                        keyframes {
                            durationMillis = TYPING_CYCLE_MILLIS
                            0f at 0
                            1f at TYPING_CYCLE_MILLIS / 6
                            0f at TYPING_CYCLE_MILLIS / 3
                        },
                        initialStartOffset = StartOffset(TYPING_CYCLE_MILLIS / TYPING_DOTS * index),
                    ),
                    label = "dot",
                )
                val moving = if (animated) lift else 0f
                Box(
                    modifier =
                    Modifier
                        .offset(y = TypingDotLift * -moving)
                        .size(TypingDotSize)
                        .alpha(TYPING_DOT_REST + (1f - TYPING_DOT_REST) * moving)
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
 * 하단 입력. 시안 1c-2 · 1c-3의 `Text Field` 하나이고, 안쪽 오른쪽에 마이크와 보내기가 선다.
 *
 * **받아쓰는 중에는 입력칸 위에 `Voice Input · Mate` 카드가 선다**(#318). 시안에 문답의 듣는 중 화면이 없어 진료 후
 * 메모(1p-T)와 같은 카드를 쓴다(사용자 결정). 입력칸은 그대로 남는다 — 마이크를 한 번 더 누르거나 카드를 누르면
 * 듣기를 끝내고, 입력칸을 누르면 받아쓰기를 끝내고 손으로 적는다. 조용한 곳이 아니거나 목소리가 잘 안 나오는
 * 환자에게 음성만 남기면 앱을 쓸 수 없다.
 *
 * 빈 칸에 질문을 적을 때도 이 입력이다(1c-D2 · D3). 열리면 입력칸으로 바로 옮겨 키보드를 올린다. 시안의 그
 * 입력칸에는 안내 글이 없다.
 */
@Composable
internal fun ChatInput(state: IntakeUiState, callbacks: IntakeCallbacks) {
    val field = remember { FocusRequester() }
    val listening = state.inputMode == IntakeInputMode.VOICE
    LaunchedEffect(state.writingQuestion) {
        if (state.writingQuestion && !listening) field.requestFocus()
    }
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16)) {
        if (listening) {
            MedicalMateVoiceInputMate(state = state.voice, onClick = callbacks.onMicClick)
        }
        MedicalMateTextField(
            value = state.draft,
            onValueChange = callbacks.onDraftChange,
            modifier =
            Modifier
                .focusRequester(field)
                .onFocusChanged { if (it.isFocused && listening) callbacks.onTypeInsteadClick() },
            placeholder = stringResource(R.string.intake_chat_placeholder).takeUnless { state.writingQuestion },
            onSend = callbacks.onSendClick,
            trailing = { ChatInputActions(state = state, listening = listening, callbacks = callbacks) },
        )
    }
}

/**
 * 입력칸 안쪽의 `Actions` 슬롯. 버튼끼리는 4로 붙인다. 듣는 중에는 마이크가 "말하기 끝내기"다.
 *
 * 음성을 쓸 수 없는 기기에서는 마이크를 그리지 않는다. 직접 입력은 그대로라 막히는 흐름이 없다.
 */
@Composable
private fun ChatInputActions(state: IntakeUiState, listening: Boolean, callbacks: IntakeCallbacks) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        if (state.voiceAvailable) {
            MedicalMateIconButton(
                onClick = if (listening) callbacks.onMicClick else callbacks.onVoiceClick,
                icon = MedicalMateIcons.Mic,
                contentDescription = stringResource(
                    if (listening) R.string.intake_chat_voice_stop else R.string.intake_chat_voice,
                ),
            )
        }
        MedicalMateIconButton(
            onClick = callbacks.onSendClick,
            icon = MedicalMateIcons.ArrowUp,
            contentDescription = stringResource(R.string.intake_chat_send),
            style = MedicalMateIconButtonStyle.GHOST,
            enabled = if (state.writingQuestion) state.canAddQuestion else state.canSend,
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

private const val SPEAKING_KEY = "speaking"

private const val TYPING_DOTS = 3

private val TypingDotSize = 8.dp

/** 점이 떠오르는 높이와 쉬고 있을 때의 흐릿함. */
private val TypingDotLift = 4.dp

private const val TYPING_DOT_REST = 0.4f

/** 세 점이 한 바퀴 도는 시간. */
private const val TYPING_CYCLE_MILLIS = 900

/** Figma 환자 버블의 반경과 같다. */
