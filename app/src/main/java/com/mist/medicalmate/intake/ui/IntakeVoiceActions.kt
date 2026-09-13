package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.Dictation
import com.mist.medicalmate.core.speech.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 문답의 음성 조작.
 *
 * [IntakeViewModel]에 얹지 않은 이유는 [BodyMapActions]·[IntakeQuestionActions]와 같다. 그
 * 클래스가 네 단계를 한 번에 들고 있어서 단계마다 조작을 더하면 한 클래스가 너무 많은 일을
 * 한다.
 *
 * **말로 하는 대화의 한 바퀴를 여기가 돈다.** 마이크를 누르면 듣고, 다 말하고 누르면 그
 * 자리에서 보내고, AI가 답하면 다시 듣는다. 매번 마이크를 누르게 하면 말로 하는 흐름이
 * 끊긴다. 문답이 끝난 뒤에는 "다음"을 듣는다.
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
        if (state().chatFinished && latest.isNextCommand()) {
            dictation.stop()
            onCommand(VoiceCommand.NEXT)
            return
        }
        update { it.copy(draft = text) }
    }

    /**
     * 입력칸의 마이크. 권한이 확인된 뒤에 불린다.
     *
     * 음성으로 바꾸면서 **바로 듣기 시작한다.** 누른 사람은 패널이 뜨자마자 말한다. 한 번 더
     * 눌러야 듣기 시작하면 그 사이에 한 말이 사라진다.
     */
    fun onVoiceMode() {
        update { it.copy(inputMode = IntakeInputMode.VOICE, voice = MedicalMateVoiceState.IDLE) }
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
            dictation.start(state().draft)
            return
        }
        dictation.stop()
        onCommand(VoiceCommand.SEND)
    }

    /**
     * AI가 답했다. 다시 듣는다.
     *
     * 글로 답하는 중이거나 문답이 끝났으면 다시 듣지 않는다 — 끝난 뒤에도 켜 두면 다음
     * 화면으로 가는 동안 녹음이 남는다.
     */
    fun onReplied() {
        if (state().inputMode != IntakeInputMode.VOICE) return
        dictation.start("")
    }

    /** 마이크 권한을 거부했다. */
    fun onDenied() {
        dictation.onDenied()
    }

    /** 글로 바꾸거나 화면을 떠날 때. 듣던 것을 멈춘다. */
    fun stop() {
        dictation.stop()
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

/** "다음" 두 자에 조사·어미가 붙는 만큼. */
private const val NEXT_COMMAND_MAX = 6

private const val PUNCTUATION = ".,!?"
