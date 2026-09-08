package com.mist.medicalmate.intake.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 증상 정리 상태 보유자.
 *
 * **AI 응답이 픽스처다.** 환자가 보내면 [REPLY_SCRIPT]의 다음 줄이 나온다. Figma 1c-1의
 * 대화를 그대로 옮긴 것이고, LLM을 붙이면 이 자리가 실제 호출로 바뀐다. 화면과 상태
 * 모델은 그대로 쓴다.
 *
 * 응답 전에 [IntakeUiState.awaitingReply]를 세우고 잠깐 기다린다. 1c-4의 점 세 개가 그
 * 상태이고, 기다림이 없으면 그 표시를 볼 수 없다. 실제 호출이 들어오면 이 지연은 사라지고
 * 응답이 올 때까지가 그 자리를 대신한다.
 *
 * **음성 인식도 붙이지 않았다.** [onMicClick]이 대기와 듣는 중을 오갈 뿐이고 받아쓴 글은
 * 없다. STT가 들어오면 멈출 때 그 결과를 [IntakeUiState.draft]에 넣고 보내면 된다.
 */
@HiltViewModel
class IntakeViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = mutableUiState.asStateFlow()

    private var nextMessageId = 0L

    fun onDraftChange(draft: String) {
        mutableUiState.update { it.copy(draft = draft) }
    }

    /** 환자의 말을 붙이고 다음 AI 마디를 기다린다. */
    fun onSend() {
        val state = mutableUiState.value
        if (!state.canSend) return

        mutableUiState.update {
            it.copy(
                messages = it.messages + IntakeMessage(nextMessageId++, IntakeMessage.Sender.PATIENT, it.draft.trim()),
                draft = "",
                awaitingReply = true,
            )
        }
        viewModelScope.launch {
            delay(REPLY_DELAY_MILLIS)
            mutableUiState.update { current ->
                val line = REPLY_SCRIPT.getOrNull(current.messages.count { it.sender == IntakeMessage.Sender.AI } - 1)
                current.copy(
                    messages =
                    if (line == null) {
                        current.messages
                    } else {
                        current.messages + IntakeMessage(nextMessageId++, IntakeMessage.Sender.AI, line)
                    },
                    awaitingReply = false,
                    // 물어볼 것이 남지 않았다. 시안에 문답을 끝내는 조작이 없어서 이 시점에
                    // 다음으로 가는 버튼을 띄운다. LLM이 붙으면 그쪽이 끝을 알린다(#69).
                    chatFinished = line == null,
                )
            }
        }
    }

    /** 글과 음성을 바꾼다. 바꿀 때 음성 상태를 대기로 되돌린다. */
    fun onInputModeChange(mode: IntakeInputMode) {
        mutableUiState.update { it.copy(inputMode = mode, voice = MedicalMateVoiceState.IDLE) }
    }

    /**
     * 마이크를 눌렀다. 대기와 듣는 중을 오간다.
     *
     * 받아쓴 글은 아직 없다. 멈춰도 대화에 아무것도 붙지 않는다.
     */
    fun onMicClick() {
        mutableUiState.update {
            val next =
                if (it.voice == MedicalMateVoiceState.LISTENING) {
                    MedicalMateVoiceState.IDLE
                } else {
                    MedicalMateVoiceState.LISTENING
                }
            it.copy(voice = next)
        }
    }

    fun onSeverityChange(severity: MedicalMateSeverity) {
        mutableUiState.update { it.copy(severity = severity) }
    }

    fun onQuestionDraftChange(draft: String) {
        mutableUiState.update { it.copy(questionDraft = draft) }
    }

    fun onAddQuestion() {
        mutableUiState.update {
            if (!it.canAddQuestion) {
                it
            } else {
                it.copy(
                    questions = it.questions + it.questionDraft.trim(),
                    questionDraft = "",
                )
            }
        }
    }

    fun onRemoveQuestion(index: Int) {
        mutableUiState.update {
            if (index !in
                it.questions.indices
            ) {
                it
            } else {
                it.copy(questions = it.questions.filterIndexed { i, _ -> i != index })
            }
        }
    }

    /**
     * 다음 단계로. 마지막 단계에서는 흐름을 마친다.
     *
     * 아픈 부위를 지날 때 부위를 임시 값으로 채우고 대화를 열어 둔다. 인체도가 없어서
     * 고를 것이 없고, 문답의 첫 마디가 부위를 부르며 시작한다.
     */
    fun onNext() {
        val state = mutableUiState.value
        when {
            state.step == IntakeStep.BODY_PART ->
                mutableUiState.update {
                    it.copy(
                        bodyPart = PLACEHOLDER_BODY_PART,
                        messages = listOf(IntakeMessage(nextMessageId++, IntakeMessage.Sender.AI, OPENING_LINE)),
                        step = IntakeStep.SYMPTOM_CHAT,
                    )
                }

            state.step.isLast -> mutableUiState.update { it.copy(completed = true) }

            else -> mutableUiState.update { it.copy(step = IntakeStep.entries[it.step.ordinal + 1]) }
        }
    }

    /** 이전 단계로. 첫 단계에서는 아무 일도 하지 않는다. */
    fun onBack() {
        mutableUiState.update {
            if (it.canGoBack) it.copy(step = IntakeStep.entries[it.step.ordinal - 1]) else it
        }
    }

    private companion object {
        /** 인체도가 없어서 쓰는 임시 부위. 시안이 오면 화면이 고른 값으로 바뀐다. */
        const val PLACEHOLDER_BODY_PART = "복부"

        /** Figma 1c-1의 첫 마디. */
        const val OPENING_LINE = "복부가 불편하시군요. 언제부터 그러셨어요? 정확하지 않아도 괜찮아요."

        /** 그 다음 마디들. LLM이 붙으면 사라진다. */
        val REPLY_SCRIPT =
            listOf(
                "3주 전부터 점점 심해지셨네요. 어떨 때 더 아프세요?",
                "명치가 얼마나 아프세요?",
                "알겠어요. 조금 더 알려주실 것이 있으면 말씀해 주세요.",
            )

        /** 응답을 기다리는 시간. 픽스처가 즉시 답하면 기다리는 표시를 볼 수 없다. */
        const val REPLY_DELAY_MILLIS = 700L
    }
}
