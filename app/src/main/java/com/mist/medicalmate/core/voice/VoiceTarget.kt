package com.mist.medicalmate.core.voice

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 지금 화면이 알아듣는 말(#282).
 *
 * "헤이 메이트"로 부른 뒤의 말은 먼저 지금 화면에 간다. 화면이 모르는 말이면 전역 명령(홈·캘린더
 * 같은 화면 이동)으로 넘어간다. 화면마다 알아듣는 말이 달라서, 일정 추가에서는 "10월 20일"이 날짜
 * 칸이 되고 병원 찾기에서는 "첫 번째"가 첫 줄이 된다.
 *
 * **새 동작을 만들지 않는다.** 화면의 버튼을 누른 것과 같은 일을 말로 부른다. 그래서 화면 로직과
 * 그 시험을 그대로 쓴다.
 *
 * 화면이 모든 말을 받아 가면 "뒤로"·"홈" 같은 전역 명령이 막힌다. 자기 화면의 말이 아니면
 * [VoiceReply.NotMine]을 돌려준다.
 */
fun interface VoiceTarget {
    suspend fun handle(text: String): VoiceReply
}

/** 화면이 받아쓴 말을 어떻게 처리했는지. 문구는 리소스 id로 돌려주고 패널이 그린다. */
sealed interface VoiceReply {
    /** 이 화면이 모르는 말. 전역 명령으로 넘긴다. */
    data object NotMine : VoiceReply

    /**
     * 처리했다. 패널은 닫지 않고 다음 말을 기다린다.
     *
     * @param args 문구에 끼울 값. 날짜·병원처럼 화면이 이미 글로 만든 것이다.
     */
    data class Done(@StringRes val message: Int, val args: List<String> = emptyList()) : VoiceReply

    /**
     * 되돌리기 어려운 일이라 한 번 더 묻는다. 다음 말이 "네"면 [onConfirm]을 부른다.
     *
     * 저장처럼 서버에 남는 일이 여기다. 화면 이동은 뒤로가기로 되돌릴 수 있어 묻지 않는다.
     */
    data class Confirm(@StringRes val prompt: Int, val args: List<String> = emptyList(), val onConfirm: () -> Unit) :
        VoiceReply
}

/**
 * 등록된 화면들. 맨 나중에 등록한 것이 지금 화면이다.
 *
 * 화면이 바뀌는 동안에는 나가는 화면과 들어오는 화면이 잠깐 함께 조합된다. 들어오는 쪽이 나중에
 * 등록하므로 맨 위가 된다. 나가는 쪽이 빠지면서 자기만 지운다.
 */
class VoiceTargets {
    private val stack = mutableListOf<VoiceTarget>()

    val current: VoiceTarget? get() = synchronized(stack) { stack.lastOrNull() }

    fun register(target: VoiceTarget) {
        synchronized(stack) { stack += target }
    }

    fun unregister(target: VoiceTarget) {
        synchronized(stack) { stack.remove(target) }
    }
}

/** 그래프 전체에 하나. `VoiceCommandViewModel`이 들고 있고 `navigation`이 내려 준다. */
val LocalVoiceTargets = staticCompositionLocalOf<VoiceTargets?> { null }

/**
 * 이 화면이 알아듣는 말을 등록한다. 화면이 컴포지션을 떠나면 지운다.
 *
 * [target]은 매번 새로 만들어도 된다. 등록은 한 번만 하고 부를 때 가장 최근 것을 쓴다.
 */
@Composable
fun VoiceTargetEffect(target: VoiceTarget) {
    val targets = LocalVoiceTargets.current ?: return
    val latest by rememberUpdatedState(target)
    DisposableEffect(targets) {
        val registered = VoiceTarget { text -> latest.handle(text) }
        targets.register(registered)
        onDispose { targets.unregister(registered) }
    }
}
