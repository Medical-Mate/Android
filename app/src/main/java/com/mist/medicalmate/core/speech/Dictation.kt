package com.mist.medicalmate.core.speech

import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 받아쓰기 한 자리.
 *
 * 두 화면(1c-3·1c-4, 1p)이 같은 일을 한다. ViewModel마다 같은 코드를 두면 한쪽만 고쳐지는
 * 자리가 생긴다. [ScheduleAddTodoActions][com.mist.medicalmate.calendar.ui.ScheduleAddTodoActions]와
 * 같은 방식으로 조작만 따로 든다.
 *
 * **누를 때의 글을 기억해 두고 그 뒤에 붙인다.** 부분 결과는 말하는 중에 계속 갱신되는
 * 값이라 이어 붙이면 같은 말이 여러 번 쌓인다. 기억해 둔 글에 매번 새로 붙여야 한다.
 *
 * **패널이 뜨면 이미 듣고 있다.** 마이크를 누른 사람은 바로 말한다. 패널을 보고 한 번 더
 * 눌러야 듣기 시작하면 그 사이에 한 말이 사라진다. 그래서 `말씀해 주세요`(IDLE)는 "아직
 * 아무것도 못 들었다"이고 `듣고 있어요`(LISTENING)는 "말소리가 들어오기 시작했다"다.
 *
 * @param onVoice 패널 상태.
 * @param onDictated 받아쓴 결과를 얹은 글 전체.
 */
class Dictation(
    private val speech: SpeechToText,
    private val scope: CoroutineScope,
    private val onVoice: (MedicalMateVoiceState) -> Unit,
    private val onDictated: (String) -> Unit,
) {
    private var job: Job? = null

    /** 듣는 중인지. 패널 상태가 아니라 이것으로 판단한다 — IDLE이어도 듣고 있을 수 있다. */
    val listening: Boolean get() = job?.isActive == true

    /**
     * 듣기 시작한다.
     *
     * @param base 지금까지 적어 둔 글. 받아쓴 것이 이 뒤에 붙는다.
     */
    fun start(base: String) {
        if (listening) return
        onVoice(MedicalMateVoiceState.IDLE)
        job = scope.launch {
            speech.listen().collect { chunk ->
                when (chunk) {
                    // 모델을 처음 받는 중이다. 다 받으면 다시 기다리는 상태로 돌아온다.
                    SpeechChunk.Preparing -> onVoice(MedicalMateVoiceState.PROCESSING)

                    is SpeechChunk.Partial -> {
                        onVoice(MedicalMateVoiceState.LISTENING)
                        onDictated(base + chunk.text)
                    }

                    is SpeechChunk.Final -> {
                        onVoice(MedicalMateVoiceState.LISTENING)
                        onDictated(base + chunk.text)
                    }

                    // 적던 글은 그대로 두고 상태만 되돌린다. 못 알아들었다고 적은 것을 지우지 않는다.
                    SpeechChunk.Failed -> onVoice(MedicalMateVoiceState.IDLE)
                }
            }
            onVoice(MedicalMateVoiceState.IDLE)
        }
    }

    /** 패널 안의 마이크. 듣는 중이면 멈추고 아니면 다시 듣는다. */
    fun toggle(base: String) {
        if (listening) stop() else start(base)
    }

    /** 마이크 권한을 거부했다. 패널은 열어 두고 왜 안 되는지를 적는다. */
    fun onDenied() {
        onVoice(MedicalMateVoiceState.DENIED)
    }

    /** 패널을 닫거나 화면을 떠날 때. 듣던 것을 멈춘다. */
    fun stop() {
        job?.cancel()
        job = null
        scope.launch { speech.stop() }
        onVoice(MedicalMateVoiceState.IDLE)
    }
}
