package com.mist.medicalmate.core.voice

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 말로 화면을 굴린다(#284). "아래로", "내려 줘", "맨 위로".
 *
 * 화면이 길어 아래가 안 보일 때, 손을 쓰지 않고 말로 하는 흐름이 거기서 끊기면 안 된다. 어느 화면이든
 * 같은 말로 굴린다. 화면마다 따로 받지 않고 대화가 먼저 본다.
 */
enum class VoiceScroll { DOWN, UP, BOTTOM, TOP }

/**
 * 굴리라는 말인지. 아니면 null이다.
 *
 * **말머리에서만 본다.** "배 위로 아파요"·"맨 아래 거"(목록의 마지막) 같은 말이 걸리면 안 된다. "로"나
 * 동사까지 붙어야 굴리는 말이고, 앞의 "화면", "좀", "스크롤"은 떼고 본다.
 */
fun parseVoiceScroll(text: String): VoiceScroll? {
    var heard = VoiceWords.compact(text)
    while (true) {
        val lead = SCROLL_LEAD.firstOrNull { heard.startsWith(it) } ?: break
        heard = heard.removePrefix(lead)
    }
    if (heard.length > SCROLL_MAX_LENGTH) return null
    return SCROLL_WORDS.firstOrNull { (_, words) -> words.any { heard.startsWith(it) } }?.first
}

/**
 * 지금 화면에서 굴릴 수 있는 것. [VoiceTargets]에 화면이 등록하고, 맨 나중에 등록한 것이 지금 화면이다.
 */
fun interface VoiceScroller {
    /** 굴린다. 이미 끝이라 움직일 수 없으면 false다. */
    fun scroll(direction: VoiceScroll): Boolean
}

/**
 * 말로 굴릴 수 있는 [ScrollState]. 화면의 `rememberScrollState()` 자리에 쓴다.
 *
 * @param register 말로 굴릴 것으로 등록할지. 음성 막대의 고를 목록(#336)은 넘칠 때만 등록한다 — 늘 등록하면 넘치지
 *   않는 목록이 "아래로"를 받아 뒤의 화면이 굴러가지 않는다.
 */
@Composable
fun rememberVoiceScrollState(register: Boolean = true): ScrollState {
    val state = rememberScrollState()
    val scope = rememberCoroutineScope()
    if (register) VoiceScrollerEffect(state) { direction -> state.scrollBy(direction, scope) }
    return state
}

/** 말로 굴릴 수 있는 [LazyListState]. 목록의 `rememberLazyListState()` 자리에 쓴다. */
@Composable
fun rememberVoiceLazyListState(): LazyListState {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    VoiceScrollerEffect(state) { direction -> state.scrollBy(direction, scope) }
    return state
}

/** 굴릴 것을 등록한다. 화면이 컴포지션을 떠나면 지운다. */
@Composable
private fun VoiceScrollerEffect(key: Any, scroller: VoiceScroller) {
    val targets = LocalVoiceTargets.current ?: return
    DisposableEffect(targets, key) {
        targets.registerScroller(scroller)
        onDispose { targets.unregisterScroller(scroller) }
    }
}

/** 한 번에 굴리는 양. 보이는 높이의 이만큼이다. 앞에서 보던 줄이 조금 남아 어디서 이어지는지 보인다. */
private const val PAGE = 0.7f

private fun ScrollState.scrollBy(direction: VoiceScroll, scope: CoroutineScope): Boolean {
    if (!can(direction, canScrollForward, canScrollBackward)) return false
    scope.launch {
        when (direction) {
            VoiceScroll.DOWN -> animateScrollBy(viewportSize * PAGE)
            VoiceScroll.UP -> animateScrollBy(-viewportSize * PAGE)
            VoiceScroll.BOTTOM -> animateScrollTo(maxValue)
            VoiceScroll.TOP -> animateScrollTo(0)
        }
    }
    return true
}

private fun LazyListState.scrollBy(direction: VoiceScroll, scope: CoroutineScope): Boolean {
    if (!can(direction, canScrollForward, canScrollBackward)) return false
    val page = layoutInfo.viewportSize.height * PAGE
    scope.launch {
        when (direction) {
            VoiceScroll.DOWN -> animateScrollBy(page)
            VoiceScroll.UP -> animateScrollBy(-page)
            VoiceScroll.BOTTOM -> animateScrollToItem((layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            VoiceScroll.TOP -> animateScrollToItem(0)
        }
    }
    return true
}

private fun can(direction: VoiceScroll, forward: Boolean, backward: Boolean): Boolean =
    if (direction == VoiceScroll.DOWN || direction == VoiceScroll.BOTTOM) forward else backward

/** 굴리라는 말 앞에 붙는 군말. 떼고 본다. */
private val SCROLL_LEAD = listOf("화면", "스크롤", "좀", "조금", "더", "쭉", "한번", "을", "를")

private const val SCROLL_MAX_LENGTH = 8

/** 차례가 우선순위다. "맨 아래로"가 "아래로"보다 먼저다. */
private val SCROLL_WORDS =
    listOf(
        VoiceScroll.BOTTOM to listOf("맨아래로", "맨밑으로", "맨끝으로", "끝까지", "제일아래로", "제일밑으로"),
        VoiceScroll.TOP to listOf("맨위로", "맨처음으로", "제일위로", "처음으로올려"),
        VoiceScroll.DOWN to listOf("아래로", "밑으로", "내려", "다음내용"),
        VoiceScroll.UP to listOf("위로", "올려"),
    )

/**
 * 화면 아래에 뜬 음성 막대의 높이(#284). 막대가 닫혀 있으면 0이다.
 *
 * 막대는 화면 위에 겹쳐 뜬다. 화면을 그만큼 줄이면 막대가 뜰 때마다 화면 전체가 들썩여서, 화면은 그대로
 * 두고 스크롤 끝에만 이만큼 빈자리를 붙인다. 맨 아래 내용도 막대 위로 올려 볼 수 있다.
 */
val LocalVoicePanelInset = compositionLocalOf { 0.dp }

/**
 * 스크롤 끝에 막대 높이만큼 빈자리를 둔다. `verticalScroll(...)` 바로 뒤에 붙인다. 그 뒤의 여백은 내용
 * 안쪽이라 스크롤 길이가 늘어난다.
 */
fun Modifier.voicePanelSpace(): Modifier = composed { padding(bottom = LocalVoicePanelInset.current) }

/** 지금 막대 높이. 목록의 `contentPadding` 아래 값에 더한다. */
@Composable
fun voicePanelInset(): Dp = LocalVoicePanelInset.current
