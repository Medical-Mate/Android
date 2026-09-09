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

    /** 인체도 단계의 조작. 왜 나눴는지는 [BodyMapActions]에 있다. */
    val bodyMap = BodyMapActions { transform ->
        mutableUiState.update { it.copy(bodyMap = transform(it.bodyMap)) }
    }

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
     * 아픈 부위를 지날 때 고른 부위 이름을 [IntakeUiState.bodyPart]에 넣고 대화를 연다.
     * 문답의 첫 마디가 부위를 부르며 시작하고, 통증 강도 화면의 물음도 그 이름을 쓴다.
     */
    fun onNext() {
        val state = mutableUiState.value
        when {
            state.step == IntakeStep.BODY_PART -> {
                // 부위를 다 고르기 전에는 문답을 열지 않는다. 화면의 다음 버튼도 꺼져 있다.
                val part = state.bodyMap.selection?.takeIf { state.canLeaveBodyPart }?.title()
                if (part != null) {
                    mutableUiState.update {
                        it.copy(
                            bodyPart = part,
                            messages =
                            listOf(IntakeMessage(nextMessageId++, IntakeMessage.Sender.AI, openingLine(part))),
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
     * 인체도에서 구역을 고르는 중이면 단계를 내리는 대신 앵커 선택으로 돌아간다. 화면이
     * 바뀌지 않고 같은 단계 안에서 깊어진 상태라 뒤로 가는 곳도 그 안이다.
     */
    fun onBack() {
        val state = mutableUiState.value
        if (state.step == IntakeStep.BODY_PART) {
            if (state.bodyMap.selection != null) bodyMap.onAnchorReset()
            return
        }
        mutableUiState.update { it.copy(step = IntakeStep.entries[it.step.ordinal - 1]) }
    }

    private companion object {
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

/**
 * 문답의 첫 마디. Figma 1c-1의 문장에 고른 부위를 끼운다.
 *
 * LLM이 붙으면 사라진다. 그때까지도 부위 이름은 고른 값이어야 한다. "복부가"로 고정해
 * 두면 무릎을 짚고도 복부를 묻는다.
 */
private fun openingLine(part: String): String = "$part${subjectParticle(part)} 불편하시군요. 언제부터 그러셨어요? 정확하지 않아도 괜찮아요."

/**
 * 주격 조사. 받침이 있으면 "이", 없으면 "가"다.
 *
 * 부위 이름 25개에 "무릎"과 "머리"가 함께 있어서 하나로 고정할 수 없다. 이름 끝에
 * 괄호가 붙는 것도 있어("가슴 옆(갈비)") 마지막 한글 음절을 찾아서 본다.
 */
private fun subjectParticle(word: String): String {
    val syllable = word.lastOrNull { it.code in HANGUL_FIRST..HANGUL_LAST } ?: return "이"
    val hasFinalConsonant = (syllable.code - HANGUL_FIRST) % HANGUL_FINAL_COUNT != 0
    return if (hasFinalConsonant) "이" else "가"
}

/** 한글 음절 영역과 종성 개수. 유니코드가 초성·중성·종성 순서로 음절을 나열한다. */
private const val HANGUL_FIRST = 0xAC00
private const val HANGUL_LAST = 0xD7A3
private const val HANGUL_FINAL_COUNT = 28
