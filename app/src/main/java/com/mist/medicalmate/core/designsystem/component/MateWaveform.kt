package com.mist.medicalmate.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * 파형의 모양. Figma 원본 좌표를 그대로 옮긴 것이고, 움직임은 모양과 상관없이 같다.
 *
 * 막대 하나는 폭 [barWidth]의 알약(양 끝이 반원)이고 [barHeights]는 끝까지 포함한 높이다.
 */
class MateWaveformShape private constructor(
    internal val gridWidth: Float,
    internal val gridHeight: Float,
    internal val barCenterX: FloatArray,
    internal val barWidth: Float,
    internal val barHeights: FloatArray,
    val defaultSize: Dp,
) {
    companion object {
        /**
         * Figma `302:719` Icon/waveform (= `R.drawable.ic_waveform`). 24 격자, 획 1.75,
         * 선 길이 3.6·10.4·15.6·10.4·3.6에 둥근 끝을 더한 높이. 메이트 떠 있는 카드(40dp)용.
         */
        val Icon = MateWaveformShape(
            gridWidth = 24f,
            gridHeight = 24f,
            barCenterX = floatArrayOf(4f, 8f, 12f, 16f, 20f),
            barWidth = 1.75f,
            barHeights = floatArrayOf(5.35f, 12.15f, 17.35f, 12.15f, 5.35f),
            defaultSize = 40.dp,
        )

        /**
         * Figma `3539:5588` [제안] Waveform · Size=L, State=말하는 중 (세트 `3539:5606`).
         * 72×72, 막대 폭 8 · 간격 8 · 반경 4, 높이 26·46·62·42·26(4번째가 2번째보다 짧음).
         * 음성 입력 섹션 "Voice Input · Mate"(1p-T 등)의 큰 아이콘용.
         */
        val Large = MateWaveformShape(
            gridWidth = 72f,
            gridHeight = 72f,
            barCenterX = floatArrayOf(4f, 20f, 36f, 52f, 68f),
            barWidth = 8f,
            barHeights = floatArrayOf(26f, 46f, 62f, 42f, 26f),
            defaultSize = 72.dp,
        )
    }
}

/**
 * 메이트 음성의 움직이는 파형.
 *
 * [shape]의 Figma 좌표대로 막대 5개를 그리고 **막대 높이만** 움직인다. 크기를 바꾸면 막대
 * 폭도 같은 비율로 바뀐다.
 *
 * **움직임** (회의 캔버스 7-B v2 · Figma `3539:5606` 설명과 같은 값)
 * - 말소리가 없을 때(듣는 중): 1.8초 주기로 느리게 숨 쉰다. 높이 45~75%, 불투명도 85%.
 * - 말할 때: 1.1초 주기, 높이 45↔100%. [level]이 클수록 100%에 가깝다.
 * - 두 상태는 끊기지 않고 [level]에 따라 섞인다. 막대마다 박자가 조금씩 늦어 물결처럼 보인다
 *   (지연 0 · -0.2 · -0.45 · -0.7 · -0.9초).
 * - 곡선은 cubic-bezier(.45, 0, .55, 1).
 * - 시스템 "애니메이션 끄기"(애니메이터 길이 배율 0)면 Figma 모양 그대로 멈춘다.
 *
 * 원형 배경은 없다. 장식이라 접근성 설명을 달지 않는다(상태는 옆 글자가 전한다).
 *
 * @param level 지금 말소리의 크기 0~1. 그리기 단계에서만 읽으므로 재구성을 일으키지 않는다.
 *   값이 튀어도 안에서 부드럽게 따라간다(올라갈 때 빠르게, 내려갈 때 천천히).
 *   음량을 못 받으면 [rememberSpeechActivityLevel]로 부분 인식 글자에서 만든다.
 * @param shape [MateWaveformShape.Icon](떠 있는 카드) 또는 [MateWaveformShape.Large](음성 입력 섹션).
 */
@Composable
fun MateWaveform(
    level: () -> Float,
    modifier: Modifier = Modifier,
    shape: MateWaveformShape = MateWaveformShape.Icon,
    size: Dp = shape.defaultSize,
    color: Color = MedicalMateTheme.colors.bgPrimary,
) {
    val animate = rememberAnimationsEnabled()
    val currentLevel by rememberUpdatedState(level)
    val elapsed = remember { mutableFloatStateOf(0f) }
    val smoothed = remember { mutableFloatStateOf(0f) }

    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            var last = start
            while (true) {
                withFrameNanos { now ->
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    elapsed.floatValue = (now - start) / 1_000_000_000f
                    val target = currentLevel().coerceIn(0f, 1f)
                    val tau = if (target > smoothed.floatValue) ATTACK_SEC else RELEASE_SEC
                    smoothed.floatValue += (target - smoothed.floatValue) * (1f - exp(-dt / tau))
                }
            }
        }
    }

    Canvas(modifier.size(size)) {
        val unit = min(this.size.width / shape.gridWidth, this.size.height / shape.gridHeight)
        val left = (this.size.width - shape.gridWidth * unit) / 2f
        val cy = center.y
        val barW = shape.barWidth * unit
        val t = elapsed.floatValue
        val lv = smoothed.floatValue
        val alpha = if (animate) IDLE_ALPHA + (1f - IDLE_ALPHA) * lv else 1f
        for (i in shape.barCenterX.indices) {
            val scale =
                if (!animate) {
                    1f
                } else {
                    val idle = IDLE_MIN + (IDLE_MAX - IDLE_MIN) * pulse(t, IDLE_PERIOD_SEC, PHASE[i])
                    val speak = SPEAK_MIN + (1f - SPEAK_MIN) * pulse(t, SPEAK_PERIOD_SEC, PHASE[i])
                    idle + (speak - idle) * lv
                }
            // 둥근 끝(반지름 barW/2)이 위아래로 붙으므로 선 길이는 전체 높이에서 막대 폭을 뺀 값
            val half = max(shape.barHeights[i] * unit * scale - barW, 0f) / 2f
            val x = left + shape.barCenterX[i] * unit
            drawLine(
                color = color,
                start = Offset(x, cy - half),
                end = Offset(x, cy + half),
                strokeWidth = barW,
                cap = StrokeCap.Round,
                alpha = alpha,
            )
        }
    }
}

/**
 * 음량을 받을 수 없을 때 쓰는 말소리 신호.
 *
 * 지금 받아쓰기는 ML Kit GenAI `AudioSource.fromMic()`이 마이크를 잡고 있어 음량(RMS)을
 * 따로 주지 않는다. 대신 부분 인식 글자가 새로 들어올 때마다 신호를 0.9로 올렸다가 0.7초에
 * 걸쳐 내린다. 말을 이어 하면 글자가 계속 들어와 높게 유지되고, 멈추면 숨 쉬는 상태로 돌아간다.
 *
 * @param partialText 지금까지 들어온 부분 인식 글자(발화가 끊기면 빈 글자로).
 */
@Composable
fun rememberSpeechActivityLevel(partialText: String): () -> Float {
    val level = remember { Animatable(0f) }
    LaunchedEffect(partialText) {
        if (partialText.isBlank()) {
            level.animateTo(0f, tween(durationMillis = 300))
            return@LaunchedEffect
        }
        level.snapTo(maxOf(level.value, 0.9f))
        level.animateTo(0f, tween(durationMillis = 700, easing = LinearOutSlowInEasing))
    }
    return { level.value }
}

/** 0~1~0을 한 주기로 오가는 값. 막대마다 [phase]만큼 앞당긴다. */
private fun pulse(t: Float, period: Float, phase: Float): Float {
    val p = ((t / period + phase) % 1f + 1f) % 1f
    val tri = if (p < 0.5f) p * 2f else (1f - p) * 2f
    return EASE.transform(tri)
}

@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

// 움직임 (회의 캔버스 7-B v2 CSS keyframes · Figma 3539:5606 설명과 같은 값)
private const val SPEAK_PERIOD_SEC = 1.1f
private const val IDLE_PERIOD_SEC = 1.8f
private const val SPEAK_MIN = 0.45f
private const val IDLE_MIN = 0.45f
private const val IDLE_MAX = 0.75f
private const val IDLE_ALPHA = 0.85f

/** CSS animation-delay 0 · -0.2 · -0.45 · -0.7 · -0.9초를 1.1초 주기에 대한 비율로 */
private val PHASE = floatArrayOf(0f, 0.18f, 0.41f, 0.64f, 0.82f)
private val EASE = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private const val ATTACK_SEC = 0.06f
private const val RELEASE_SEC = 0.25f

@Preview(name = "카드 · 말소리 없음", widthDp = 64, heightDp = 64)
@Composable
private fun MateWaveformIdlePreview() {
    MedicalMateTheme { MateWaveform(level = { 0f }) }
}

@Preview(name = "카드 · 말하는 중", widthDp = 64, heightDp = 64)
@Composable
private fun MateWaveformSpeakingPreview() {
    MedicalMateTheme { MateWaveform(level = { 0.8f }) }
}

@Preview(name = "음성 입력 섹션 L · 말하는 중", widthDp = 96, heightDp = 96)
@Composable
private fun MateWaveformLargePreview() {
    MedicalMateTheme { MateWaveform(level = { 0.8f }, shape = MateWaveformShape.Large) }
}
