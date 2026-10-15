package com.mist.medicalmate.core.voice

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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

/** 막대에 적을 한 줄. 문구는 리소스이고 [args]가 그 안에 들어간다. */
data class VoiceText(@StringRes val text: Int, val args: List<String> = emptyList())

/** 화면이 받아쓴 말을 어떻게 처리했는지. 문구는 리소스 id로 돌려주고 막대가 그린다. */
sealed interface VoiceReply {
    /** 이 화면이 모르는 말. 전역 명령으로 넘긴다. */
    data object NotMine : VoiceReply

    /**
     * 처리했다. 막대는 닫지 않고 다음 말을 기다린다.
     *
     * @param args 문구에 끼울 값. 날짜·병원처럼 화면이 이미 글로 만든 것이다.
     * @param question 이어서 물을 것. 빠진 칸이나 골라야 할 것이 있으면 화면이 되묻는다.
     *   사용자는 막대를 보고 다시 말한다.
     */
    data class Done(
        @StringRes val message: Int,
        val args: List<String> = emptyList(),
        val question: VoiceText? = null,
    ) : VoiceReply

    /**
     * 되돌리기 어려운 일이라 한 번 더 묻는다. 다음 말이 "네"면 [onConfirm]을 부른다.
     *
     * 저장처럼 서버에 남는 일이 여기다. 화면 이동은 뒤로가기로 되돌릴 수 있어 묻지 않는다.
     */
    data class Confirm(@StringRes val prompt: Int, val args: List<String> = emptyList(), val onConfirm: () -> Unit) :
        VoiceReply
}

/**
 * 화면이 먼저 막대에 거는 말. 사용자가 말하지 않았는데 알릴 것이나 물을 것이 생겼을 때다.
 *
 * 병원 찾기가 결과를 받아 "같은 이름이 3곳 있어요"라고 묻거나, 일정 추가가 열리면서 "어느
 * 병원인가요?"라고 묻는 것이 여기다. 막대가 닫혀 있으면 버려진다. 말로 하는 중이 아니면 물을
 * 이유가 없다.
 *
 * @param message 막대 윗줄. 비우면 지금 줄을 그대로 둔다.
 * @param question 막대 아랫줄의 물음.
 */
data class VoicePrompt(val message: VoiceText? = null, val question: VoiceText? = null)

/**
 * 등록된 화면들. 맨 나중에 등록한 것이 지금 화면이다.
 *
 * 화면이 바뀌는 동안에는 나가는 화면과 들어오는 화면이 잠깐 함께 조합된다. 들어오는 쪽이 나중에
 * 등록하므로 맨 위가 된다. 나가는 쪽이 빠지면서 자기만 지운다.
 */
class VoiceTargets {
    private val stack = mutableListOf<VoiceTarget>()

    /** 화면마다 추천하는 말(#292). 화면의 상태에 따라 달라질 수 있어 부를 때 읽는다. */
    private val suggestionsOf = mutableMapOf<VoiceTarget, () -> List<VoiceSuggestion>>()

    private val scrollers = mutableListOf<VoiceScroller>()

    private var handoff: Pair<String, Long>? = null

    /**
     * 돌아갈 화면이 있는지(#298). 그래프가 화면을 옮길 때마다 알려 준다. 알려 주기 전에는 있다고 본다.
     */
    @Volatile
    var canGoBack: Boolean = true

    /**
     * 호출어 없이 메이트를 부른다(#312). 홈 배너의 "메이트에게 말해보기"가 이 길이다. 듣는 쪽(`voice`)이 채운다.
     * 마이크 권한은 부르는 화면이 먼저 받는다.
     */
    @Volatile
    var summon: () -> Unit = {}

    private val asked = MutableSharedFlow<VoicePrompt>(extraBufferCapacity = PROMPT_BUFFER)

    val current: VoiceTarget? get() = synchronized(stack) { stack.lastOrNull() }

    /** 지금 화면이 추천하는 말. 없으면 비어 있고, 막대가 공통 예시로 채운다. */
    val suggestions: List<VoiceSuggestion>
        get() = synchronized(stack) { stack.lastOrNull()?.let { suggestionsOf[it]?.invoke() }.orEmpty() }

    /** 지금 화면에서 말로 굴릴 것(#284). 시트처럼 화면 위에 뜬 것이 나중에 등록해 맨 위가 된다. */
    val currentScroller: VoiceScroller? get() = synchronized(scrollers) { scrollers.lastOrNull() }

    /** 화면이 먼저 건 말. 대화가 이어지는 동안만 받는다. */
    val prompts: SharedFlow<VoicePrompt> = asked.asSharedFlow()

    fun register(target: VoiceTarget, suggestions: () -> List<VoiceSuggestion> = { emptyList() }) {
        synchronized(stack) {
            stack += target
            suggestionsOf[target] = suggestions
        }
    }

    fun unregister(target: VoiceTarget) {
        synchronized(stack) {
            stack.remove(target)
            suggestionsOf.remove(target)
        }
    }

    /**
     * 화면을 옮긴 말을 옮겨 간 화면에 넘긴다(#286). "10월 20일 일정 보여줘"가 캘린더를 열고, 열린 캘린더가 그
     * 말로 그 날을 연다. 다음에 등록하는 화면 하나가 받는다. 그 사이가 길면 버린다 — 옮겨 가지 않았으면
     * 엉뚱한 화면이 나중에 받게 된다.
     */
    fun handOff(text: String) {
        synchronized(stack) { handoff = text to System.currentTimeMillis() }
    }

    /** 넘겨받은 말. 한 번만 준다. */
    fun takeHandoff(): String? = synchronized(stack) {
        val taken = handoff
        handoff = null
        taken?.takeIf { System.currentTimeMillis() - it.second <= HANDOFF_MS }?.first
    }

    fun registerScroller(scroller: VoiceScroller) {
        synchronized(scrollers) { scrollers += scroller }
    }

    fun unregisterScroller(scroller: VoiceScroller) {
        synchronized(scrollers) { scrollers.remove(scroller) }
    }

    /** 막대에 말을 건다. 대화 중이 아니면 아무도 받지 않아 사라진다. */
    fun ask(prompt: VoicePrompt) {
        asked.tryEmit(prompt)
    }

    private companion object {
        const val PROMPT_BUFFER = 4

        const val HANDOFF_MS = 3_000L
    }
}

/** 그래프 전체에 하나. `VoiceCommandViewModel`이 들고 있고 `navigation`이 내려 준다. */
val LocalVoiceTargets = staticCompositionLocalOf<VoiceTargets?> { null }

/**
 * 이 화면이 알아듣는 말을 등록한다. 화면이 컴포지션을 떠나면 지운다.
 *
 * [target]은 매번 새로 만들어도 된다. 등록은 한 번만 하고 부를 때 가장 최근 것을 쓴다.
 *
 * @param suggestions 이 화면에서 추천하는 말(#292). 부를 때와 못 알아들었을 때 막대 위에 선다. 화면의
 *   단계에 따라 바뀌어도 된다. 부를 때 가장 최근 것을 읽는다.
 */
@Composable
fun VoiceTargetEffect(suggestions: List<VoiceSuggestion> = emptyList(), target: VoiceTarget) {
    val targets = LocalVoiceTargets.current ?: return
    val latest by rememberUpdatedState(target)
    val latestSuggestions by rememberUpdatedState(suggestions)
    DisposableEffect(targets) {
        val registered = VoiceTarget { text -> latest.handle(text) }
        targets.register(registered) { latestSuggestions }
        onDispose { targets.unregister(registered) }
    }
    // 이 화면을 연 말이 넘어왔으면 받아 처리하고 그 답을 막대에 건다(#286).
    LaunchedEffect(targets) {
        val text = targets.takeHandoff() ?: return@LaunchedEffect
        val reply = latest.handle(text) as? VoiceReply.Done ?: return@LaunchedEffect
        targets.ask(VoicePrompt(message = VoiceText(reply.message, reply.args), question = reply.question))
    }
}

/** 화면이 막대에 먼저 말을 거는 길. 그래프 밖(미리보기 등)에서는 아무 일도 하지 않는다. */
@Composable
fun rememberVoicePrompt(): (VoicePrompt) -> Unit {
    val targets = LocalVoiceTargets.current
    return remember(targets) { { prompt -> targets?.ask(prompt) } }
}
