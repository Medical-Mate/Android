package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.speech.Dictation
import com.mist.medicalmate.core.speech.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 문답의 음성 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 [BodyMapActions]·[IntakeQuestionActions]와 같다. 그
 * 클래스가 네 단계를 한 번에 들고 있어서 단계마다 조작을 더하면 한 클래스가 너무 많은 일을
 * 한다.
 *
 * **말로 하는 대화의 한 바퀴를 여기가 돈다.** 마이크를 누르면 듣고, 말이 1초 끊기면 그 자리에서 보내고(#318),
 * AI가 답하면 다시 듣는다. 매번 마이크를 누르게 하면 말로 하는 흐름이 끊긴다. 다 말하고 직접 눌러도 보낸다.
 * 듣기 시작하고 5초 동안 아무 말이 없으면 패널을 닫는다(#318). 문답이 끝난 뒤에는 "다음"을 듣는다.
 *
 * **받아쓴 말은 입력칸에 적지 않는다**(#318, 사용자 결정). 패널이 떠 있는 동안 입력칸은 그 전 글을 보이고, 패널이 닫히면
 * 음성을 켜기 전에 손으로 적어 둔 글([IntakeUiState.typedBefore])로 되돌린다.
 *
 * @param onCommand 말이 끝났을 때 할 일. 보내는 것도 다음으로 가는 것도 세션과 단계를 아는
 *   ViewModel이 한다.
 */
class IntakeVoiceActions(
    private val speech: SpeechToText,
    private val scope: CoroutineScope,
    private val state: () -> IntakeUiState,
    private val update: ((IntakeUiState) -> IntakeUiState) -> Unit,
    private val onCommand: (VoiceCommand) -> Unit,
) {
    private val dictation =
        Dictation(
            speech = speech,
            scope = scope,
            onVoice = { voice -> update { it.copy(voice = voice) } },
            onDictated = ::onDictated,
        )

    /**
     * 화면이 보이는지(#304). 다음 화면으로 가도 이 ViewModel은 백스택에 남아 있어, AI 답이 그 뒤에 와도 다시
     * 듣기 시작했다. 안 보이는 화면이 마이크를 잡고 있으면 호출어 감지기가 쉬어서 "헤이 메이트"가 듣히지 않는다.
     */
    private var shown = true

    /** 조용한 시간을 세는 일(#318). 말이 들리면 1초 뒤 보내기로, 듣기 시작하면 5초 뒤 닫기로 다시 센다. */
    private var silence: Job? = null

    /**
     * 받아쓴 글이 왔다.
     *
     * **문답이 끝난 뒤에는 "다음"을 듣는다.** 더 할 말이 없는 자리라 남은 조작이 다음으로
     * 가는 것뿐이고, 말로 하던 사람에게 거기서만 손을 쓰게 할 이유가 없다. 그 밖의 말은
     * 적히기만 하고 아무 일도 하지 않는다.
     *
     * 방금 들어온 발화만 본다. 이어 붙인 글로 보면 앞에 다른 말을 한 뒤에는 "다음"이라고
     * 해도 걸리지 않는다.
     */
    private fun onDictated(text: String, latest: String) {
        // 빈 칸에 질문을 적는 중이면 "다음"도 질문의 글이다.
        if (state().chatFinished && !state().writingQuestion && latest.isNextCommand()) {
            stop()
            onCommand(VoiceCommand.NEXT)
            return
        }
        update { it.copy(draft = text) }
        armSilence(heardSpeech = true)
    }

    /**
     * 조용한 시간을 센다(#318, 사용자 결정).
     *
     * [heardSpeech]면 **말이 1초 끊길 때 보낸다.** 전에는 다 말하고 패널을 한 번 더 눌러야 보내졌다. 받아쓴 글이 들어올
     * 때마다 처음부터 다시 세서 말하는 동안에는 보내지 않는다. 문답이 끝난 뒤에는 보내지 않는다 — 그 자리에서는
     * "다음"만 듣는다.
     *
     * 아니면 듣기를 막 시작한 것이고 **5초 동안 아무 말이 없으면 패널을 닫는다.** 켜 둔 채 잊은 마이크가 계속 듣지 않게
     * 한다. 인식기가 그 사이 스스로 끝났어도 같다. 글로 바꿨거나 화면을 떠났으면 아무것도 하지 않는다.
     */
    private fun armSilence(heardSpeech: Boolean) {
        silence?.cancel()
        // 문답이 끝난 뒤에는 빈 칸에 질문을 적을 때만 보낸다(#320). 그 말이 질문으로 붙는다.
        if (heardSpeech && state().chatFinished && !state().writingQuestion) return
        silence =
            scope.launch {
                delay(if (heardSpeech) SILENCE_SEND_MS else SILENCE_CLOSE_MS)
                val current = state()
                if (!shown || current.inputMode != IntakeInputMode.VOICE) return@launch
                when {
                    heardSpeech && current.draft.isNotBlank() -> {
                        update { it.copy(typedBefore = "") }
                        stop()
                        onCommand(VoiceCommand.SEND)
                    }
                    !heardSpeech -> onCommand(VoiceCommand.CLOSE)
                }
            }
    }

    /**
     * 입력칸의 마이크. 권한이 확인된 뒤에 불린다.
     *
     * 음성으로 바꾸면서 **바로 듣기 시작한다.** 누른 사람은 패널이 뜨자마자 말한다. 한 번 더
     * 눌러야 듣기 시작하면 그 사이에 한 말이 사라진다.
     */
    fun onVoiceMode() {
        update {
            it.copy(inputMode = IntakeInputMode.VOICE, voice = MedicalMateVoiceState.IDLE, typedBefore = it.draft)
        }
        // 닫기를 먼저 건다. 듣자마자 말이 오면 그 말이 보내기로 바꿔 건다.
        armSilence(heardSpeech = false)
        dictation.start(state().draft)
    }

    /**
     * 패널 안의 마이크.
     *
     * **듣는 중이면 멈추고 그 자리에서 보낸다.** 문구가 "다 말씀하시면 버튼을 다시
     * 눌러주세요"라고 적혀 있고, 누른 사람이 기대하는 것은 말이 전달되는 것이다. 멈추기만
     * 하면 한 번 더 눌러야 대화가 이어진다.
     *
     * 듣는 중이 아니면 다시 듣는다. 인식이 스스로 끝났거나 멈춰 둔 자리다.
     */
    fun onMicClick() {
        if (!dictation.listening) {
            armSilence(heardSpeech = false)
            dictation.start(state().draft)
            return
        }
        update { it.copy(typedBefore = "") }
        stop()
        onCommand(VoiceCommand.SEND)
    }

    /**
     * AI가 답했다. 다시 듣는다.
     *
     * 글로 답하는 중이거나 문답이 끝났으면 다시 듣지 않는다 — 끝난 뒤에도 켜 두면 다음
     * 화면으로 가는 동안 녹음이 남는다.
     */
    fun onReplied() {
        // 답이 늦게 와서 그 사이 문답 단계를 떠났으면 듣지 않는다(#304).
        if (!shown || state().step != IntakeStep.SYMPTOM_CHAT || state().inputMode != IntakeInputMode.VOICE) return
        // 마지막 답이었으면 받아쓰기를 접는다(#320). 하단이 다음 버튼으로 바뀌어 카드가 보이지 않는데 마이크가 남아
        // "헤이 메이트"가 듣히지 않았고, 그 사이 "다음"이라고 하면 정리가 끝났다.
        if (state().chatFinished) {
            stop()
            onCommand(VoiceCommand.CLOSE)
            return
        }
        armSilence(heardSpeech = false)
        dictation.start("")
    }

    /** 마이크 권한을 거부했다. */
    fun onDenied() {
        dictation.onDenied()
    }

    /** 글로 바꿀 때. 듣던 것을 멈춘다. */
    fun stop() {
        silence?.cancel()
        dictation.stop()
    }

    /**
     * 화면이 가려졌다(#304). 다음 화면으로 갔거나 앱이 뒤로 갔다. 듣던 것을 멈추고, 돌아오기 전에는 다시 듣지 않는다.
     *
     * 패널은 그대로 둔다. 돌아와서 마이크를 누르면 다시 듣는다.
     */
    fun onHidden() {
        shown = false
        stop()
    }

    /** 화면이 다시 보인다. */
    fun onShown() {
        shown = true
    }

    /** 이 기기에서 음성을 쓸 수 있는지. 쓸 수 없으면 화면이 마이크를 그리지 않는다. */
    fun check() {
        scope.launch {
            val usable = speech.available()
            update { it.copy(voiceAvailable = usable) }
        }
    }
}

/** 말이 끝났을 때 할 일. */
enum class VoiceCommand {
    /** 적힌 글을 보낸다. */
    SEND,

    /** 다음 단계로 간다. 문답이 끝난 뒤에만 나온다. */
    NEXT,

    /** 패널을 닫고 글로 돌아간다. 듣기 시작하고 5초 동안 아무 말이 없을 때다(#318). */
    CLOSE,
}

/**
 * "다음"으로 들리는 말인지.
 *
 * 앞이 "다음"이고 짧을 때만 본다. 그냥 포함 여부로 보면 "다음 주에 다시 올게요" 같은 말에도
 * 걸린다. 조사나 어미가 붙는 것("다음이요"·"다음으로")은 받아들인다.
 */
private fun String.isNextCommand(): Boolean {
    val word = filterNot { it.isWhitespace() || it in PUNCTUATION }
    return word.startsWith(NEXT_WORD) && word.length <= NEXT_COMMAND_MAX
}

private const val NEXT_WORD = "다음"

/** 말이 끊긴 뒤 보내기까지 기다리는 시간(#318, 사용자 결정). */
private const val SILENCE_SEND_MS = 1_000L

/** 아무 말이 없을 때 패널을 닫기까지 기다리는 시간(#318, 사용자 결정). */
private const val SILENCE_CLOSE_MS = 5_000L

/** "다음" 두 자에 조사·어미가 붙는 만큼. */
private const val NEXT_COMMAND_MAX = 6

private const val PUNCTUATION = ".,!?"
