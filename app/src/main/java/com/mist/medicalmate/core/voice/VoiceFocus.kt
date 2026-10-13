package com.mist.medicalmate.core.voice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlinx.coroutines.delay

/**
 * 말로 바뀐 자리들(#282·#284). 화면이 첫 자리로 옮겨 가고 모두에 잠깐 테를 두른다. 말로 조작하는 동안
 * 무엇이 바뀌었는지 눈으로 따라가게 하려는 것이다.
 *
 * [seq]는 같은 자리를 두 번 말해도 다시 보이게 하려고 둔다. 화면은 마지막으로 보인 [seq]를 저장해 두어
 * 다른 화면에 다녀왔을 때 지난 것을 다시 띄우지 않는다.
 *
 * [T]는 화면마다 정한다. 일정 추가는 칸·카드·할 일, 증상 정리는 인체도·강도·질문이다.
 */
data class VoiceFocus<T>(val targets: List<T>, val seq: Long = System.nanoTime())

/**
 * 지금 보여 줄 말로 바뀐 자리(#282). 새로 온 것만 [VOICE_FOCUS_HOLD_MS] 동안 내준다.
 *
 * 마지막으로 보인 [VoiceFocus.seq]를 저장해 둔다. 다른 화면에 다녀오면 이 화면이 다시 조합되는데,
 * 그때 상태에 남은 지난 자리가 또 번쩍이면 안 된다.
 */
@Composable
fun <T> rememberVoiceFocus(focus: VoiceFocus<T>?): VoiceFocus<T>? {
    var seen by rememberSaveable { mutableStateOf<Long?>(null) }
    var shown by remember { mutableStateOf<VoiceFocus<T>?>(null) }
    LaunchedEffect(focus) {
        if (focus == null || focus.seq == seen) return@LaunchedEffect
        seen = focus.seq
        shown = focus
        delay(VOICE_FOCUS_HOLD_MS)
        shown = null
    }
    return shown
}

/**
 * 말로 바뀐 자리를 감싼다(#282). [focus]의 첫 자리면 화면을 옮겨 보이게 하고, 들어 있으면 테를 두른다.
 *
 * **옮길 때 아래에 막대 높이만큼 둔다.** 음성 막대가 화면 아래에 겹쳐 뜬다. 딱 맞춰 들이면 그 자리가
 * 막대 뒤에 들어간다. 스크롤 끝에도 그만큼 빈자리가 붙어 있어 맨 아래 칸도 막대 위로 올라온다(#284).
 *
 * 테는 내용 위에 그린다. 칸마다 면색이 달라 뒤에 깐 면은 가려진다. 색이 나타났다 사라지는 것이라
 * 기기에서 애니메이션을 꺼도 테는 선다. 막대의 윗줄이 무엇을 했는지 함께 적으므로 색만으로 알리지 않는다.
 */
@Composable
fun <T> VoiceFocusFrame(
    focus: VoiceFocus<T>?,
    target: T,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val above = with(density) { MedicalMateSpace.s16.toPx() }
    val below = with(density) { (voicePanelInset() + MedicalMateSpace.s16).toPx() }
    val stroke = with(density) { MedicalMateSpace.s2.toPx() }

    LaunchedEffect(focus) {
        if (focus?.targets?.firstOrNull() != target) return@LaunchedEffect
        // 말로 막 더한 줄은 아직 자리를 잡기 전이다. 한 프레임 기다려 크기를 읽는다.
        withFrameNanos { }
        requester.bringIntoView(Rect(0f, -above, size.width.toFloat(), size.height + below))
    }

    val active = focus?.targets?.contains(target) == true
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(VOICE_FOCUS_FADE_MS),
        label = "voiceFocus",
    )
    val color = MedicalMateTheme.colors.borderFocus
    Box(
        modifier =
        modifier
            .bringIntoViewRequester(requester)
            .onSizeChanged { size = it }
            .drawWithContent {
                drawContent()
                if (alpha > 0f) {
                    drawOutline(
                        outline = shape.createOutline(this.size, layoutDirection, this),
                        color = color,
                        alpha = alpha,
                        style = Stroke(width = stroke),
                    )
                }
            },
    ) {
        content()
    }
}

/** 테를 두르고 있는 시간. 막대의 윗줄을 읽고 칸을 찾아볼 만큼이다. */
private const val VOICE_FOCUS_HOLD_MS = 1_800L

private const val VOICE_FOCUS_FADE_MS = 200
