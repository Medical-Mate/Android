package com.mist.medicalmate.voice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.SpeechChunk
import com.mist.medicalmate.core.speech.SpeechToText
import com.mist.medicalmate.core.speech.VoiceWake
import com.mist.medicalmate.core.speech.VoiceWakeTrace
import com.mist.medicalmate.core.speech.WakeWordDetector
import com.mist.medicalmate.core.speech.WakeWordModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 음성 명령 패널.
 *
 * @param heard 지금까지 알아들은 말. 말하는 중에 계속 바뀐다.
 * @param missed 한 번 말했는데 명령으로 못 알아들었다. 패널은 열어 두고 다시 듣는다.
 */
data class VoicePanel(
    val voice: MedicalMateVoiceState = MedicalMateVoiceState.IDLE,
    val heard: String = "",
    val missed: Boolean = false,
)

/**
 * @param command 알아들은 명령. 그래프가 화면을 옮기기 **전에** [VoiceCommandViewModel.onCommandHandled]로
 *   비운다.
 */
data class VoiceCommandUiState(val panel: VoicePanel? = null, val command: VoiceCommand? = null)

/**
 * "메이트야"로 부르고 명령을 듣는다(#273).
 *
 * **Activity 스코프다.** 특정 화면에 속한 상태가 아니고, 화면을 옮겨도 듣기가 끊기면 안 된다.
 * [SessionViewModel][com.mist.medicalmate.auth.ui.SessionViewModel]과 같은 이유다.
 *
 * **두 단계로 듣는다.** 호출어는 Vosk가, 그 뒤의 명령은 받아쓰기(ML Kit)가 듣는다. Vosk의
 * 한국어 받아쓰기는 틀리는 말이 많아(WER 28%) 명령까지 맡기기 어렵다. 한 마이크를 둘이 나눠
 * 쓰므로 감지기를 완전히 거둔 뒤에 받아쓰기를 연다.
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
        trace.log { "호출됨. 명령을 듣는다" }
        val detecting = detectorJob
        detecting?.cancel()
        mutableUiState.update { it.copy(panel = VoicePanel()) }
        commandJob =
            viewModelScope.launch {
                detecting?.join()
                val command = withTimeoutOrNull(LISTEN_TIMEOUT_MS) { awaitCommand() }
                trace.log { "명령: ${command ?: "없음 (시간이 다 됐거나 받아쓰기가 실패했다)"}" }
                speech.stop()
                commandJob = null
                mutableUiState.update { it.copy(panel = null, command = command ?: it.command) }
            }
    }

    /**
     * 명령 하나를 알아들을 때까지 듣는다.
     *
     * 못 알아들은 말이면 패널에 그 말을 남기고 계속 듣는다. 받아쓰기가 실패하면 그만둔다.
     */
    private suspend fun awaitCommand(): VoiceCommand? {
        var command: VoiceCommand? = null
        speech.listen()
            .takeWhile { chunk ->
                when (chunk) {
                    SpeechChunk.Preparing -> {
                        updatePanel { it.copy(voice = MedicalMateVoiceState.PROCESSING) }
                        true
                    }

                    is SpeechChunk.Partial -> {
                        updatePanel { it.copy(voice = MedicalMateVoiceState.LISTENING, heard = chunk.text) }
                        true
                    }

                    is SpeechChunk.Final -> {
                        command = parseVoiceCommand(chunk.text)
                        trace.log { "명령 받아쓰기: \"${chunk.text}\" → ${command ?: "모르는 말"}" }
                        if (command == null) {
                            updatePanel {
                                it.copy(voice = MedicalMateVoiceState.IDLE, heard = chunk.text, missed = true)
                            }
                        }
                        command == null
                    }

                    SpeechChunk.Failed -> {
                        trace.log { "명령 받아쓰기: 실패" }
                        false
                    }
                }
            }
            .collect()
        return command
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
        /** 부르고 이만큼 아무 명령도 없으면 닫는다. 못 알아들어 다시 말하는 것까지 넣은 길이다. */
        const val LISTEN_TIMEOUT_MS = 12_000L
    }
}
