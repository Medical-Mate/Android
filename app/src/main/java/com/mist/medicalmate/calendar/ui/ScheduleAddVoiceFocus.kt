package com.mist.medicalmate.calendar.ui

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
 * 지금 보여 줄 말로 바뀐 자리(#282). 새로 온 것만 [VOICE_FOCUS_HOLD_MS] 동안 내준다.
 *
 * 마지막으로 보인 [ScheduleAddFocus.seq]를 저장해 둔다. 병원 찾기에 다녀오면 이 화면이 다시 조합되는데,
 * 그때 상태에 남은 지난 자리가 또 번쩍이면 안 된다.
 */
@Composable
internal fun rememberVoiceFocus(focus: ScheduleAddFocus?): ScheduleAddFocus? {
    var seen by rememberSaveable { mutableStateOf<Long?>(null) }
    var shown by remember { mutableStateOf<ScheduleAddFocus?>(null) }
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
 * 옮길 때 위아래를 조금 둔다. 화면 끝에 딱 붙으면 테가 잘려 보인다. 음성 막대는 화면을 덮지 않고
 * 화면이 그만큼 줄어든다(#284).
 *
 * 테는 내용 위에 그린다. 칸마다 면색이 달라 뒤에 깐 면은 가려진다. 색이 나타났다 사라지는 것이라
 * 기기에서 애니메이션을 꺼도 테는 선다. 막대의 윗줄이 무엇을 했는지 함께 적으므로 색만으로 알리지 않는다.
 */
@Composable
internal fun VoiceFocusFrame(
    focus: ScheduleAddFocus?,
    target: ScheduleAddFocusTarget,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val above = with(density) { MedicalMateSpace.s16.toPx() }
    val below = with(density) { MedicalMateSpace.s16.toPx() }
    val stroke = with(density) { MedicalMateSpace.s2.toPx() }

    LaunchedEffect(focus) {
        if (focus?.targets?.firstOrNull() != target) return@LaunchedEffect
        // 말로 막 더한 할 일은 아직 자리를 잡기 전이다. 한 프레임 기다려 크기를 읽는다.
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
