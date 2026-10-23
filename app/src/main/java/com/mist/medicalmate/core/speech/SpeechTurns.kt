package com.mist.medicalmate.core.speech

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield

/**
 * 받아쓰기를 여는 쪽(#348). 인식기는 하나라 한 번에 한쪽만 연다.
 *
 * [SCREEN]은 화면이 연 받아쓰기다(증상 문답 · 진료 후 메모의 마이크). [PANEL]은 "헤이 메이트" 막대다.
 */
enum class SpeechOwner { SCREEN, PANEL }

/**
 * 인식기를 누가 쓰는지 가른다(#348). 전에는 `stop()`이 누가 부르든 지금 도는 인식을 끊고, 듣는 중인지가 Boolean
 * 하나여서 화면이 받아쓰기를 멈추면 막대의 인식이 끊기고, 막대가 닫히면 화면의 받아쓰기가 끊겼다.
 *
 * - 한 번에 한쪽만 연다. 앞 차례가 다 끝나야 다음이 연다.
 * - **화면이 먼저다.** 사람이 화면의 마이크를 눌렀다는 뜻이라, 막대가 듣고 있으면 그 인식을 멈추게 하고([interrupt])
 *   막대가 놓으면 연다. 막대는 다시 들으려 할 때 화면이 다 쓸 때까지 기다린다.
 * - 멈추라는 말은 지금 쓰는 쪽이 그쪽일 때만 듣는다([owns]).
 */
internal class SpeechTurns(private val interrupt: suspend () -> Unit) {
    private val holder = MutableStateFlow<SpeechOwner?>(null)
    private val turn = Mutex()

    @Volatile
    private var screenWaiting = false

    private val listening = MutableStateFlow(false)

    /** 누군가 받아쓰고 있는지. 호출어 감지기가 이것을 보고 마이크를 내준다. */
    val active: StateFlow<Boolean> = listening.asStateFlow()

    /** 차례를 받는다. 받을 때까지 기다린다. 받으면 반드시 [release]로 놓는다. */
    suspend fun take(owner: SpeechOwner) {
        if (owner == SpeechOwner.SCREEN) {
            screenWaiting = true
            try {
                if (holder.value == SpeechOwner.PANEL) interrupt()
                turn.lock()
            } finally {
                screenWaiting = false
            }
        } else {
            while (true) {
                holder.first { it == null }
                turn.lock()
                if (!screenWaiting) break
                // 화면이 기다리고 있다. 그쪽에 넘기고 다시 기다린다.
                turn.unlock()
                yield()
            }
        }
        holder.value = owner
        listening.value = true
    }

    fun release() {
        holder.value = null
        listening.value = false
        turn.unlock()
    }

    /** 지금 [owner]가 쓰고 있는지. 멈추라는 말을 들을지 정한다. */
    fun owns(owner: SpeechOwner): Boolean = holder.value == owner
}
