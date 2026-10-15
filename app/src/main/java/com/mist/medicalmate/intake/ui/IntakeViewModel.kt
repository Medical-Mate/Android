package com.mist.medicalmate.intake.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.speech.SpeechToText
import com.mist.medicalmate.intake.data.IntakeSessionMessage
import com.mist.medicalmate.intake.data.IntakeSeverity
import com.mist.medicalmate.intake.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 증상 정리 상태 보유자.
 *
 * 문답이 서버에서 진행된다. 보낸 말과 다음 질문이 `POST /api/sessions/{id}/messages`로
 * 오간다. 응답이 대화 전체를 주므로 화면은 그것으로 갈아 끼운다.
 *
 * 보내는 동안 [IntakeUiState.awaitingReply]가 선다. 1c-4의 점 세 개가 그 상태다.
 *
 * **음성 인식도 붙이지 않았다.** [onMicClick]이 대기와 듣는 중을 오갈 뿐이고 받아쓴 글은
 * 없다. STT가 들어오면 멈출 때 그 결과를 [IntakeUiState.draft]에 넣고 보내면 된다.
 */
@HiltViewModel
class IntakeViewModel
@Inject
internal constructor(private val repository: SessionRepository, speech: SpeechToText) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = mutableUiState.asStateFlow()

    /** 추천 질문 고르기 조작. 왜 나눴는지는 [IntakeQuestionActions]에 있다. */
    val question = IntakeQuestionActions { transform -> mutableUiState.update(transform) }

    /** 인체도 단계의 조작. 왜 나눴는지는 [BodyMapActions]에 있다. */
    val bodyMap = BodyMapActions { transform ->
        mutableUiState.update { it.copy(bodyMap = transform(it.bodyMap)) }
    }

    /** 서버와 주고받는 부분. 왜 나눴는지는 [IntakeSessionActions]에 있다. */
    internal val session = IntakeSessionActions(repository, viewModelScope) { transform ->
        mutableUiState.update(transform)
    }

    /**
     * 지금 세션을 연 부위. 부위를 바꿔 다시 오면 그 세션은 다른 부위의 문답이라 새로 연다. 이어서 하기로
     * 들어온 세션은 부위를 모르므로 비어 있고, 그때 부위를 다시 고르면 새 문답이다.
     */
    private var startedWith: BodyMapSelection? = null

    fun onDraftChange(draft: String) {
        mutableUiState.update { it.copy(draft = draft) }
    }

    /**
     * 환자의 말을 보내고 다음 질문을 받는다.
     *
     * 보낸 말을 먼저 화면에 붙인다. 서버 응답을 기다렸다 한꺼번에 그리면 방금 누른 것이
     * 사라진 것처럼 보인다.
     *
     * 응답이 대화 전체를 주므로 그것으로 갈아 끼운다. 우리가 붙인 줄과 서버가 센 줄이
     * 어긋나는 일이 없다.
     *
     * 세션이 없으면 보내지 않는다. 세션 만들기가 실패했다는 뜻이고, 보낼 곳이 없다.
     */
    fun onSend() {
        val state = mutableUiState.value
        // 빈 칸에 적던 질문이면 문답으로 보내지 않고 질문 줄로 붙인다(1c-D3 → D4).
        if (state.writingQuestion) {
            question.onAdd()
            return
        }
        send(byVoice = state.inputMode == IntakeInputMode.VOICE)
    }

    private fun send(byVoice: Boolean) {
        val state = mutableUiState.value
        val sessionId = state.sessionId
        if (!state.canSend || sessionId == null) return

        val text = state.draft.trim()
        mutableUiState.update {
            it.copy(
                messages = it.messages + it.newMessage(IntakeMessage.Sender.PATIENT, text),
                draft = "",
                awaitingReply = true,
            )
        }
        viewModelScope.launch {
            when (val result = repository.send(sessionId, text, byVoice)) {
                is ApiResult.Success -> {
                    mutableUiState.update { current ->
                        current.copy(
                            messages = result.value.messages.map { it.toMessage() },
                            awaitingReply = false,
                            chatFinished = result.value.ended,
                            // 문답이 끝난 턴에 추천 질문이 실려 온다(Backend#169). 이미 줄이 있으면 덮지 않는다 —
                            // 끝난 뒤에 또 보낸 말에도 같은 후보가 오는데, 그 사이의 체크가 사라진다.
                            questionRows =
                            current.questionRows.ifEmpty { questionRowsOf(result.value.questionCandidates) },
                        )
                    }
                    voice.onReplied()
                }

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    // 보낸 말은 화면에 남긴다. 지우면 다시 적어야 한다.
                    mutableUiState.update { it.copy(awaitingReply = false, sendFailed = true) }
            }
        }
    }

    /** 글과 음성을 바꾼다. 바꿀 때 음성 상태를 대기로 되돌린다. */
    fun onInputModeChange(mode: IntakeInputMode) {
        if (mode == IntakeInputMode.TEXT) voice.stop()
        mutableUiState.update {
            // 패널을 닫으면 받아쓴 말은 버리고 음성을 켜기 전에 적어 둔 글로 돌아간다(#318). 받아쓴 말이 입력칸에
            // 남지 않는다.
            val leavingVoice = mode == IntakeInputMode.TEXT && it.inputMode == IntakeInputMode.VOICE
            it.copy(
                inputMode = mode,
                voice = MedicalMateVoiceState.IDLE,
                draft = if (leavingVoice) it.typedBefore else it.draft,
            )
        }
    }

    /** 음성 조작. 왜 나눴는지는 [IntakeVoiceActions]에 있다. */
    val voice =
        IntakeVoiceActions(
            speech = speech,
            scope = viewModelScope,
            state = { mutableUiState.value },
            update = { transform -> mutableUiState.update(transform) },
            onCommand = ::onVoiceCommand,
        )

    /**
     * "헤이 메이트" 다음의 말(#284). 세 단계를 말로 한다. 문답에서 한 말은 답으로 보낸다 — 말로 한
     * 답이라 서버에 음성으로 적는다.
     */
    internal val voiceCommands =
        IntakeVoiceCommands(
            state = { mutableUiState.value },
            update = { transform -> mutableUiState.update(transform) },
            send = { text ->
                onDraftChange(text)
                send(byVoice = true)
            },
            back = ::onBack,
        )

    /** 말로 한 조작. 보내거나 다음으로 간다. */
    private fun onVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.SEND -> onSend()
            // 문답이 끝난 뒤에만 오는 명령이라 강도 라벨을 쓰지 않는다.
            VoiceCommand.NEXT -> onNext("")
            VoiceCommand.CLOSE -> onInputModeChange(IntakeInputMode.TEXT)
        }
    }

    fun onSeverityChange(severity: MedicalMateSeverity) {
        mutableUiState.update { it.copy(severity = severity) }
    }

    /**
     * 다음 단계로. 마지막 단계에서는 흐름을 마친다.
     *
     * 아픈 부위를 지날 때 고른 부위 이름을 [IntakeUiState.bodyPart]에 넣는다. 통증 강도 화면의 물음과
     * 문답의 첫 마디가 그 이름을 쓴다.
     *
     * @param severityLabel 고른 강도의 표시 문구. 리소스라 화면이 풀어서 넘긴다. 강도 단계에서만 쓴다.
     */
    fun onNext(severityLabel: String) {
        val state = mutableUiState.value
        leaveChat(state)
        when (state.step) {
            IntakeStep.BODY_PART -> leaveBodyPart(state)
            IntakeStep.SEVERITY -> leaveSeverity(state, IntakeSeverity(state.severity.level, severityLabel))
            IntakeStep.SYMPTOM_CHAT -> {
                // 문답이 끝나야 다음이 있다. 화면의 다음 버튼도 그때 선다. 질문을 적는 중에는 하단이
                // 입력칸이라 다음 버튼이 없다.
                if (!state.chatFinished || state.writingQuestion) return
                session.saveQuestions(state)
                mutableUiState.update { it.copy(completed = true) }
            }
        }
    }

    /**
     * 부위를 다 고르기 전에는 강도로 가지 않는다. 화면의 다음 버튼도 꺼져 있다.
     *
     * 세션을 연 부위와 다른 부위를 골랐으면 그 문답은 버린다. 다른 부위의 대화가 이어지면 안 된다.
     */
    private fun leaveBodyPart(state: IntakeUiState) {
        val selection = state.bodyMap.selection?.takeIf { state.canLeaveBodyPart } ?: return
        val part = selection.title() ?: return
        val changed = selection != startedWith
        mutableUiState.update {
            val moved = it.copy(bodyPart = part, step = IntakeStep.SEVERITY)
            if (changed) moved.withoutSession() else moved
        }
    }

    /**
     * 강도를 고르고 문답으로. 세션이 없으면 부위와 강도로 연다(Backend#169). 이미 열려 있으면 강도로
     * 돌아와 다시 고른 것이라 그 값만 고친다.
     */
    private fun leaveSeverity(state: IntakeUiState, severity: IntakeSeverity) {
        val selection = state.bodyMap.selection
        val part = state.bodyPart
        when {
            state.sessionId != null -> {
                session.saveSeverity(state, severity)
                mutableUiState.update { it.copy(step = IntakeStep.SYMPTOM_CHAT) }
            }

            selection != null && part != null -> {
                startedWith = selection
                // 첫 질문은 서버가 세션을 만들면서 준다(#259). 오기 전까지는 답을 기다리는 점 셋만 두고
                // 보내기를 막는다. 앱 문장을 먼저 세우면 응답이 올 때 바뀐다.
                mutableUiState.update {
                    it.copy(messages = emptyList(), awaitingReply = true, step = IntakeStep.SYMPTOM_CHAT)
                }
                // 서버에 세션을 연다. 여기서부터 임시저장이 남는다.
                session.start(selection, part, severity)
            }
        }
    }

    /**
     * 이전으로.
     *
     * 인체도에서 구역을 고르는 중이면 단계를 내리는 대신 앵커 화면으로 돌아간다. 화면이
     * 바뀌지 않고 같은 단계 안에서 깊어진 상태라 뒤로 가는 곳도 그 안이다. 고른 부위는
     * 남는다.
     */
    fun onBack() {
        val state = mutableUiState.value
        // 빈 칸에 질문을 적던 중이면 적던 것만 거둔다(1c-D2 → D1). 단계는 그대로다.
        if (state.writingQuestion) {
            question.onCancel()
            return
        }
        leaveChat(state)
        if (state.step == IntakeStep.BODY_PART) {
            if (state.bodyMap.focus != null) bodyMap.onFocusClear()
            return
        }
        mutableUiState.update { it.copy(step = IntakeStep.entries[it.step.ordinal - 1]) }
    }
}

/**
 * 문답 단계를 떠날 때 듣던 것을 멈춘다(#304).
 *
 * 세 단계가 한 목적지 안에서 넘어가서 화면이 가려지지 않는다. 멈추지 않으면 강도 단계에서도 마이크를 잡고
 * 있어 "헤이 메이트"가 듣히지 않는다.
 */
private fun IntakeViewModel.leaveChat(state: IntakeUiState) {
    if (state.step == IntakeStep.SYMPTOM_CHAT) voice.stop()
}

/**
 * 문답의 첫 마디. Figma 1c-1의 문장에 고른 부위를 끼운다.
 *
 * LLM이 붙으면 사라진다. 그때까지도 부위 이름은 고른 값이어야 한다. "복부가"로 고정해
 * 두면 무릎을 짚고도 복부를 묻는다.
 */
internal fun intakeOpeningLine(part: String): String = "${withSubjectParticle(part)} 불편하시군요. 언제부터 그러셨어요? 정확하지 않아도 괜찮아요."

/** 열었던 문답을 버린다. 대화와 추천 질문이 그 세션 것이라 함께 비운다. */
private fun IntakeUiState.withoutSession(): IntakeUiState = copy(
    sessionId = null,
    messages = emptyList(),
    chatFinished = false,
    questionRows = emptyList(),
    writingQuestion = false,
    draft = "",
)

/** 서버가 준 마디를 화면 마디로. `seq`를 id로 쓴다. */
private fun IntakeSessionMessage.toMessage() = IntakeMessage(
    id = seq,
    sender = if (fromPatient) IntakeMessage.Sender.PATIENT else IntakeMessage.Sender.AI,
    text = text,
)
