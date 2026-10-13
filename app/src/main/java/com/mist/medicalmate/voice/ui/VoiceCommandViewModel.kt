package com.mist.medicalmate.voice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.SpeechChunk
import com.mist.medicalmate.core.speech.SpeechToText
import com.mist.medicalmate.core.speech.VoiceWake
import com.mist.medicalmate.core.speech.VoiceWakeTrace
import com.mist.medicalmate.core.speech.WakeWordDetector
import com.mist.medicalmate.core.speech.WakeWordModelState
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceTargets
import com.mist.medicalmate.core.voice.VoiceText
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** 막대에 적을 한 줄. 화면이 돌려주는 [VoiceText]와 같은 것이다. */
typealias VoiceMessage = VoiceText

/**
 * 음성 명령 패널.
 *
 * @param heard 지금 말하고 있는 글. 말하는 중에 계속 바뀌고, 말이 끝나면 비운다.
 * @param message 방금 한 일이나 물을 것. 처음에는 말할 수 있는 예다.
 * @param confirming [message]가 "할까요?"이고 "네"를 기다린다.
 * @param question 화면이 되묻는 것(#282). 빠진 칸이나 골라야 할 것이 있을 때 사용자가 무엇을
 *   말하면 되는지 적는다. 말하는 중에는 들리는 글이 이 자리를 잠깐 쓴다.
 */
data class VoicePanel(
    val voice: MedicalMateVoiceState = MedicalMateVoiceState.IDLE,
    val heard: String = "",
    val message: VoiceMessage = VoiceMessage(R.string.voice_command_hint),
    val confirming: Boolean = false,
    val question: VoiceMessage? = null,
)

/** 알아들은 전역 명령과 그 말 전체. 일정 추가는 말 속의 날짜·병원까지 들고 간다. */
data class VoiceCommandRequest(val command: VoiceCommand, val text: String)

/**
 * @param command 알아들은 명령. 그래프가 화면을 옮기기 **전에** [VoiceCommandViewModel.onCommandHandled]로
 *   비운다.
 */
data class VoiceCommandUiState(val panel: VoicePanel? = null, val command: VoiceCommandRequest? = null)

/**
 * "헤이 메이트"로 부르고 명령을 듣는다(#273).
 *
 * **Activity 스코프다.** 특정 화면에 속한 상태가 아니고, 화면을 옮겨도 듣기가 끊기면 안 된다.
 * [SessionViewModel][com.mist.medicalmate.auth.ui.SessionViewModel]과 같은 이유다.
 *
 * **두 단계로 듣는다.** 호출어는 Vosk가, 그 뒤의 명령은 받아쓰기(ML Kit)가 듣는다. Vosk는
 * 영어 모델을 "hey mate"와 그 밖의 말 둘로 좁혀 두어 한국어 명령을 받아쓸 수 없다. 한
 * 마이크를 둘이 나눠 쓰므로 감지기를 완전히 거둔 뒤에 받아쓰기를 연다.
 *
 * **부르면 대화가 이어진다**(#282). 명령 하나를 처리해도 패널을 닫지 않고 다음 말을 기다린다.
 * 한 흐름을 말로 끝까지 하려면 단계마다 다시 부르지 않아야 한다. 받아쓴 말은 먼저 지금 화면에
 * 가고([VoiceTargets]), 화면이 모르는 말이면 전역 명령으로 넘어간다. 저장처럼 되돌리기 어려운
 * 일은 화면이 [VoiceReply.Confirm]을 돌려주고, 다음 말이 "네"일 때만 한다. 확인한 일을 하고
 * 나면 패널을 닫는다. 흐름이 끝난 자리다. 12초 동안 아무것도 들리지 않거나 "그만"이라고 하면
 * 닫는다.
 *
 * 감지기는 다음이 모두 참일 때만 돈다. 하나라도 거짓이 되면 마이크를 놓는다.
 * - 설정에서 켰고 모델을 다 받았다
 * - 앱이 화면에 있다 — 뒤에서 마이크를 잡는 앱이 되지 않는다
 * - 홈에 갈 수 있는 세션이고 마이크 권한이 있다
 * - 다른 화면이 받아쓰고 있지 않고 패널이 닫혀 있다
 */
@HiltViewModel
class VoiceCommandViewModel
@Inject
internal constructor(
    voiceWake: VoiceWake,
    private val detector: WakeWordDetector,
    private val speech: SpeechToText,
    private val trace: VoiceWakeTrace,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VoiceCommandUiState())
    val uiState: StateFlow<VoiceCommandUiState> = mutableUiState.asStateFlow()

    private val foreground = MutableStateFlow(false)
    private val allowed = MutableStateFlow(false)

    private var detectorJob: Job? = null
    private var commandJob: Job? = null

    /** 화면들이 알아듣는 말을 등록하는 자리. `navigation`이 그래프 전체에 내려 준다. */
    val targets = VoiceTargets()

    init {
        val setting = combine(voiceWake.enabled, voiceWake.model, ::Pair)
        val screen = combine(foreground, allowed, ::Pair)
        val idle = combine(speech.active, mutableUiState.map { it.panel != null }, ::Pair)
        viewModelScope.launch {
            combine(setting, screen, idle) { (enabled, model), (front, allow), (dictating, panel) ->
                ListenGate(enabled, model, front, allow, dictating, panel)
            }
                .distinctUntilChanged()
                .collect { gate ->
                    trace.log { "듣기 조건: $gate" }
                    if (gate.open) startDetector() else detectorJob?.cancel()
                }
        }
    }

    /** 앱이 화면에 올라오거나 내려갔다. 내려가면 듣던 명령도 거둔다. */
    fun setForeground(value: Boolean) {
        foreground.value = value
        if (!value) dismiss()
    }

    /**
     * 홈에 갈 수 있는 세션인지와 마이크 권한이 있는지. 둘 다 있어야 듣는다. 로그아웃하면 듣던
     * 명령도 거둔다. 로그에서 어느 쪽이 빠졌는지 보이도록 따로 받는다.
     */
    fun setAllowed(signedIn: Boolean, micGranted: Boolean) {
        trace.log { "세션: 홈에 갈 수 있음=$signedIn · 마이크 권한=$micGranted" }
        val value = signedIn && micGranted
        allowed.value = value
        if (!value) dismiss()
    }

    /** 패널을 닫았다. 마이크를 누른 것도 같다 — 듣는 중의 마이크는 "말하기 끝내기"다. */
    fun dismiss() {
        val listening = commandJob ?: return
        commandJob = null
        listening.cancel()
        viewModelScope.launch { speech.stop() }
        mutableUiState.update { it.copy(panel = null) }
    }

    fun onCommandHandled() {
        mutableUiState.update { it.copy(command = null) }
    }

    private fun startDetector() {
        if (detectorJob?.isActive == true) return
        detectorJob = viewModelScope.launch { detector.detections().collect { onWake() } }
    }

    /**
     * 불렸다.
     *
     * 이 호출은 감지기의 흐름 안에서 온다. 감지기를 거두고, 그것이 마이크를 놓을 때까지
     * 기다린 다음 받아쓰기를 연다.
     */
    private fun onWake() {
        trace.log { "호출됨. 대화를 시작한다" }
        val detecting = detectorJob
        detecting?.cancel()
        mutableUiState.update { it.copy(panel = VoicePanel()) }
        commandJob =
            viewModelScope.launch {
                detecting?.join()
                converse()
                speech.stop()
                commandJob = null
                mutableUiState.update { it.copy(panel = null) }
            }
    }

    /**
     * 말이 끊길 때까지 듣고 답한다.
     *
     * 받아쓰기가 실패하거나, [IDLE_TIMEOUT_MS] 동안 아무것도 안 들리거나, 그만하라고 하거나,
     * 확인한 일을 마치면 끝난다.
     *
     * **화면이 먼저 건 말도 함께 받는다**(#282). 병원 찾기가 결과를 받아 "같은 이름이 3곳 있어요"라고
     * 묻는 것처럼, 사용자가 말하지 않았는데 막대에 적을 것이 생긴다. 그것도 들린 것처럼 쳐서
     * 12초를 다시 센다. 물어 놓고 바로 닫히면 답할 틈이 없다.
     */
    @OptIn(FlowPreview::class)
    private suspend fun converse() {
        val conversation =
            VoiceConversation(
                targets = targets,
                trace = trace,
                say = { message, confirming, question ->
                    updatePanel { it.copy(message = message, confirming = confirming, question = question) }
                },
                command = { request -> mutableUiState.update { it.copy(command = request) } },
                shown = { mutableUiState.value.panel },
            )
        val heard = speech.listen().map<SpeechChunk, Heard>(Heard::Spoken).onCompletion { emit(Heard.Ended) }
        merge(heard, targets.prompts.map(Heard::Asked))
            .timeout(IDLE_TIMEOUT_MS.milliseconds)
            .catch { error ->
                if (error !is TimeoutCancellationException) throw error
                trace.log { "대화: ${IDLE_TIMEOUT_MS}ms 동안 들린 것이 없어 닫는다" }
            }
            .takeWhile { event -> hear(event, conversation) }
            .collect()
    }

    /** 들린 것 하나를 막대에 옮기고 답한다. 대화를 이어 갈지 돌려준다. */
    private suspend fun hear(event: Heard, conversation: VoiceConversation): Boolean = when (event) {
        is Heard.Asked -> {
            val prompt = event.prompt
            updatePanel { it.copy(message = prompt.message ?: it.message, question = prompt.question) }
            true
        }

        Heard.Ended -> false
        is Heard.Spoken ->
            when (val chunk = event.chunk) {
                SpeechChunk.Preparing -> {
                    updatePanel { it.copy(voice = MedicalMateVoiceState.PROCESSING) }
                    true
                }

                is SpeechChunk.Partial -> {
                    updatePanel { it.copy(voice = MedicalMateVoiceState.LISTENING, heard = chunk.text) }
                    true
                }

                is SpeechChunk.Final -> {
                    updatePanel { it.copy(voice = MedicalMateVoiceState.LISTENING, heard = "") }
                    conversation.respond(chunk.text)
                }

                SpeechChunk.Failed -> {
                    trace.log { "대화: 받아쓰기 실패" }
                    false
                }
            }
    }

    /** 대화가 듣는 것. 받아쓴 말, 화면이 건 말, 받아쓰기가 끝난 것. */
    private sealed interface Heard {
        data class Spoken(val chunk: SpeechChunk) : Heard

        data class Asked(val prompt: VoicePrompt) : Heard

        data object Ended : Heard
    }

    /**
     * 감지기가 돌 조건 여섯. 로그에 무엇이 빠졌는지 그대로 찍히도록 값을 모두 든다.
     *
     * @param allowed 홈에 갈 수 있는 세션이고 마이크 권한이 있다.
     * @param dictating 다른 화면(또는 명령 듣기)이 받아쓰는 중이다.
     */
    private data class ListenGate(
        val enabled: Boolean,
        val model: WakeWordModelState,
        val foreground: Boolean,
        val allowed: Boolean,
        val dictating: Boolean,
        val panel: Boolean,
    ) {
        val open: Boolean
            get() = enabled && model == WakeWordModelState.Ready && foreground && allowed && !dictating && !panel

        override fun toString(): String =
            "켬=$enabled 모델=$model 화면=$foreground 세션·권한=$allowed 받아쓰기중=$dictating 패널=$panel" +
                " → ${if (open) "듣는다" else "쉰다"}"
    }

    private fun updatePanel(change: (VoicePanel) -> VoicePanel) {
        mutableUiState.update { state -> state.copy(panel = state.panel?.let(change)) }
    }

    private companion object {
        /** 이만큼 아무것도 들리지 않으면 닫는다. 말하는 중의 글도 들린 것으로 친다. */
        const val IDLE_TIMEOUT_MS = 12_000L
    }
}
