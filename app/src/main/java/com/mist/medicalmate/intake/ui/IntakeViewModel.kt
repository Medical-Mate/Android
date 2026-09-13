package com.mist.medicalmate.intake.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.speech.Dictation
import com.mist.medicalmate.core.speech.SpeechToText
import com.mist.medicalmate.intake.data.IntakeSessionMessage
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
internal constructor(
    private val repository: SessionRepository,
    private val speech: SpeechToText,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = mutableUiState.asStateFlow()

    /** 4단계 질문 목록 조작. 왜 나눴는지는 [IntakeQuestionActions]에 있다. */
    val question = IntakeQuestionActions { transform -> mutableUiState.update(transform) }

    /** 인체도 단계의 조작. 왜 나눴는지는 [BodyMapActions]에 있다. */
    val bodyMap = BodyMapActions { transform ->
        mutableUiState.update { it.copy(bodyMap = transform(it.bodyMap)) }
    }

    /** 서버와 주고받는 부분. 왜 나눴는지는 [IntakeSessionActions]에 있다. */
    internal val session = IntakeSessionActions(repository, viewModelScope) { transform ->
        mutableUiState.update(transform)
    }

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
        val sessionId = state.sessionId
        if (!state.canSend || sessionId == null) return

        val text = state.draft.trim()
        val byVoice = state.inputMode == IntakeInputMode.VOICE
        mutableUiState.update {
            it.copy(
                messages = it.messages + it.newMessage(IntakeMessage.Sender.PATIENT, text),
                draft = "",
                awaitingReply = true,
            )
        }
        viewModelScope.launch {
            when (val result = repository.send(sessionId, text, byVoice)) {
                is ApiResult.Success ->
                    mutableUiState.update { current ->
                        current.copy(
                            messages = result.value.messages.map { it.toMessage() },
                            awaitingReply = false,
                            chatFinished = result.value.ended,
                        )
                    }

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    // 보낸 말은 화면에 남긴다. 지우면 다시 적어야 한다.
                    mutableUiState.update { it.copy(awaitingReply = false, sendFailed = true) }
            }
        }
    }

    /** 글과 음성을 바꾼다. 바꿀 때 음성 상태를 대기로 되돌린다. */
    fun onInputModeChange(mode: IntakeInputMode) {
        if (mode == IntakeInputMode.TEXT) dictation.stop()
        mutableUiState.update { it.copy(inputMode = mode, voice = MedicalMateVoiceState.IDLE) }
    }

    /**
     * 받아쓰기.
     *
     * 받아쓴 글은 입력칸에 들어간다. 보내는 것은 환자가 한다 — 말이 끝나자마자 나가면 잘못
     * 알아들은 것을 고칠 자리가 없다.
     */
    private val dictation =
        Dictation(
            speech = speech,
            scope = viewModelScope,
            onVoice = { voice -> mutableUiState.update { it.copy(voice = voice ?: MedicalMateVoiceState.IDLE) } },
            onDictated = { draft -> mutableUiState.update { it.copy(draft = draft) } },
        )

    /**
     * 입력칸의 마이크. 권한이 확인된 뒤에 불린다.
     *
     * 음성으로 바꾸면서 **바로 듣기 시작한다.** 누른 사람은 패널이 뜨자마자 말한다. 한 번 더
     * 눌러야 듣기 시작하면 그 사이에 한 말이 사라진다.
     */
    fun onVoiceMode() {
        mutableUiState.update { it.copy(inputMode = IntakeInputMode.VOICE, voice = MedicalMateVoiceState.IDLE) }
        dictation.start(mutableUiState.value.draft)
    }

    /** 패널 안의 마이크. 듣는 중이면 멈추고 아니면 다시 듣는다. */
    fun onMicClick() {
        dictation.toggle(mutableUiState.value.draft)
    }

    /** 마이크 권한을 거부했다. */
    fun onMicDenied() {
        dictation.onDenied()
    }

    /** 이 기기에서 음성을 쓸 수 있는지. 쓸 수 없으면 마이크를 그리지 않는다. */
    fun checkVoice() {
        viewModelScope.launch {
            val usable = speech.available()
            mutableUiState.update { it.copy(voiceAvailable = usable) }
        }
    }

    fun onSeverityChange(severity: MedicalMateSeverity) {
        mutableUiState.update { it.copy(severity = severity) }
    }

    /**
     * 다음 단계로. 마지막 단계에서는 흐름을 마친다.
     *
     * 아픈 부위를 지날 때 고른 부위 이름을 [IntakeUiState.bodyPart]에 넣고 대화를 연다.
     * 문답의 첫 마디가 부위를 부르며 시작하고, 통증 강도 화면의 물음도 그 이름을 쓴다.
     */
    fun onNext(severityLabel: String) {
        val state = mutableUiState.value
        // 단계를 떠날 때 그 단계의 값을 서버에 남긴다. 고를 때마다 보내면 슬라이더를 끄는
        // 동안 요청이 줄줄이 나간다. 둘 다 끝난 문답에도 보낼 수 있어 시점이 자유롭다.
        when (state.step) {
            IntakeStep.SEVERITY -> session.saveSeverity(state, severityLabel)
            IntakeStep.QUESTIONS -> session.saveQuestions(state)
            else -> Unit
        }
        when {
            state.step == IntakeStep.BODY_PART -> {
                // 부위를 다 고르기 전에는 문답을 열지 않는다. 화면의 다음 버튼도 꺼져 있다.
                val selection = state.bodyMap.selection?.takeIf { state.canLeaveBodyPart }
                val part = selection?.title()
                if (selection != null && part != null) {
                    // 서버에 세션을 연다. 여기서부터 임시저장이 남는다.
                    session.start(selection, part)
                    mutableUiState.update {
                        it.copy(
                            bodyPart = part,
                            messages =
                            listOf(it.newMessage(IntakeMessage.Sender.AI, intakeOpeningLine(part))),
                            step = IntakeStep.SYMPTOM_CHAT,
                        )
                    }
                }
            }

            state.step.isLast -> mutableUiState.update { it.copy(completed = true) }

            else -> mutableUiState.update { it.copy(step = IntakeStep.entries[it.step.ordinal + 1]) }
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
        if (state.step == IntakeStep.BODY_PART) {
            if (state.bodyMap.focus != null) bodyMap.onFocusClear()
            return
        }
        mutableUiState.update { it.copy(step = IntakeStep.entries[it.step.ordinal - 1]) }
    }
}

/**
 * 문답의 첫 마디. Figma 1c-1의 문장에 고른 부위를 끼운다.
 *
 * LLM이 붙으면 사라진다. 그때까지도 부위 이름은 고른 값이어야 한다. "복부가"로 고정해
 * 두면 무릎을 짚고도 복부를 묻는다.
 */
internal fun intakeOpeningLine(part: String): String = "${withSubjectParticle(part)} 불편하시군요. 언제부터 그러셨어요? 정확하지 않아도 괜찮아요."

/** 서버가 준 마디를 화면 마디로. `seq`를 id로 쓴다. */
private fun IntakeSessionMessage.toMessage() = IntakeMessage(
    id = seq,
    sender = if (fromPatient) IntakeMessage.Sender.PATIENT else IntakeMessage.Sender.AI,
    text = text,
)
