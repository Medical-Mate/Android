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
 * @param onVoice 패널 상태. null이면 패널을 닫는다.
 * @param onDictated 받아쓴 결과를 얹은 글 전체.
 */
class Dictation(
    private val speech: SpeechToText,
    private val scope: CoroutineScope,
    private val onVoice: (MedicalMateVoiceState?) -> Unit,
    private val onDictated: (String) -> Unit,
) {
    private var job: Job? = null

    /**
     * 마이크를 눌렀다.
     *
     * @param listening 지금 듣는 중인지. 듣는 중이면 멈춘다.
     * @param base 지금까지 적어 둔 글. 받아쓴 것이 이 뒤에 붙는다.
     */
    fun onMicClick(listening: Boolean, base: String) {
        if (listening) {
            stop()
            return
        }
        onVoice(MedicalMateVoiceState.LISTENING)
        job = scope.launch {
            speech.listen().collect { chunk ->
                when (chunk) {
                    // 모델을 처음 받는 중이다. 다 받으면 다시 듣는 중으로 돌아온다.
                    SpeechChunk.Preparing -> onVoice(MedicalMateVoiceState.PROCESSING)

                    is SpeechChunk.Partial -> {
                        onVoice(MedicalMateVoiceState.LISTENING)
                        onDictated(base + chunk.text)
                    }

                    is SpeechChunk.Final -> onDictated(base + chunk.text)

                    // 적던 글은 그대로 두고 패널만 닫는다. 못 알아들었다고 적은 것을 지우지 않는다.
                    SpeechChunk.Failed -> onVoice(MedicalMateVoiceState.IDLE)
                }
            }
            onVoice(MedicalMateVoiceState.IDLE)
        }
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
