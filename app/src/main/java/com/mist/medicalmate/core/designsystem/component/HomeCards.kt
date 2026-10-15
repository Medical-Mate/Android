package com.mist.medicalmate.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import kotlinx.coroutines.launch

/*
 * 홈 시트의 카드 셋(#312). Figma `추가 · 새 컴포넌트` 02 홈(`3313:4910`)의 `Card / Action` · `Card / Resume` ·
 * `Card / Schedule`이다. 모두 폭 Fill에 반경 20이다.
 */

/**
 * `Card / Action`(`3313:4946`). 홈 "증상 정리 시작하기".
 *
 * `bg/primary` 면 + 오른쪽 아이콘 원(`bg/on-dark`) + `Elevation/Primary`. 카드 전체가 버튼이다.
 */
@Composable
fun MedicalMateActionCard(label: String, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .dropShadow(MedicalMateRadius.lg, MedicalMateEffects.elevationPrimary)
            .clip(MedicalMateRadius.lg)
            .background(MedicalMateTheme.colors.bgPrimary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MedicalMateSpace.s20),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
            Text(text = label, style = MedicalMateTheme.typography.bodyS, color = MedicalMateTheme.colors.fgOnPrimary)
            Text(
                text = title,
                style = MedicalMateTheme.typography.headingM,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
        }
        Box(
            modifier =
            Modifier
                .size(OrbSize)
                .background(color = MedicalMateTheme.colors.bgOnDark, shape = MedicalMateRadius.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(MedicalMateIcons.Stethoscope),
                contentDescription = null,
                tint = MedicalMateTheme.colors.fgOnPrimary,
                modifier = Modifier.size(MedicalMateSize.iconMd),
            )
        }
    }
}

/**
 * `Card / Resume`(`3313:4957`). 홈 "이어서 하기".
 *
 * 진행 원이 12시에서 시계 방향으로 [done] / [total]만큼 찬다. 숫자는 `Label/XS`다. 원은 색만으로 진행을
 * 알리지 않게 숫자를 함께 둔다(D11).
 */
@Composable
fun MedicalMateResumeCard(
    done: Int,
    total: Int,
    label: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(onClick = onClick, modifier = modifier) {
        ProgressRing(done = done, total = total)
        TwoLines(label = label, title = title, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(MedicalMateIcons.ChevronRight),
            contentDescription = null,
            tint = MedicalMateTheme.colors.fgMuted,
            modifier = Modifier.size(MedicalMateSize.iconMd),
        )
    }
}

/**
 * `Card / Schedule`(`3313:4967`). 홈 "다가오는 일정".
 *
 * 왼쪽 날짜 블록(`bg/primary-faint`, 44×48)에 월과 일, 가운데에 메타와 제목, 오른쪽에 D-day `Badge`(Brand).
 */
@Composable
fun MedicalMateScheduleCard(
    month: String,
    day: String,
    meta: String,
    title: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier =
            Modifier
                .size(width = DateWidth, height = DateHeight)
                .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.sm),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = month, style = MedicalMateTheme.typography.labelS, color = MedicalMateTheme.colors.fgSubtle)
            Text(text = day, style = MedicalMateTheme.typography.numericM, color = MedicalMateTheme.colors.fgPrimary)
        }
        TwoLines(label = meta, title = title, modifier = Modifier.weight(1f))
        MedicalMateBadge(label = badge, tone = MedicalMateBadgeTone.BRAND)
    }
}

/** 흰 카드 면. 반경 20 · 좌우 18 · 위아래 16 · 간격 14 · `Elevation/Card`. */
@Composable
private fun WhiteCard(onClick: () -> Unit, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .background(color = MedicalMateTheme.colors.bgSurface, shape = MedicalMateRadius.lg)
            .clip(MedicalMateRadius.lg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = CardPaddingHorizontal, vertical = MedicalMateSpace.s16),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 위 `Body/S` 보조 글, 아래 `Body/L Strong` 제목. 사이 2. */
@Composable
private fun TwoLines(label: String, title: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 진행 원. 지름 44 · 굵기 3.96(지름의 9%) · 빈 칸 `bg/subtle` · 채움 `bg/primary`. 끝은 각지다.
 *
 * **카드가 뜨면 호가 0에서 [done]까지 찬다**(#318). 홈 프로토타입(`3677:11740`)의 `Progress (열린 호 · 0→1/3)`가 0.75초
 * 뒤에 0.8초 동안 ease-out으로 그려지고, 숫자(`Step`)는 1초 뒤에 0.4초 동안 ease-out으로 나타난다. 진행이 바뀌면
 * 다시 찬다. 기기에서 애니메이션을 껐으면 바로 다 찬 모양이다.
 */
@Composable
private fun ProgressRing(done: Int, total: Int) {
    val track = MedicalMateTheme.colors.bgSubtle
    val fill = MedicalMateTheme.colors.bgPrimary
    val spoken = stringResource(R.string.progress_step, done, total)
    val drawn = remember(done, total) { Animatable(0f) }
    val stepAlpha = remember(done, total) { Animatable(0f) }
    LaunchedEffect(drawn, stepAlpha) {
        launch { drawn.animateTo(1f, tween(RING_FILL_MS, delayMillis = RING_FILL_DELAY_MS, easing = CssEaseOut)) }
        stepAlpha.animateTo(1f, tween(STEP_FADE_MS, delayMillis = STEP_FADE_DELAY_MS, easing = CssEaseOut))
    }
    Box(
        modifier = Modifier.size(RingSize).clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val width = size.minDimension * RING_STROKE_RATIO
            val inset = width / 2
            val arcSize = Size(size.width - width, size.height - width)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, FULL_TURN, false, topLeft, arcSize, style = Stroke(width))
            if (total > 0 && done > 0) {
                val sweep = FULL_TURN * done / total * drawn.value
                drawArc(fill, START_ANGLE, sweep, false, topLeft, arcSize, style = Stroke(width))
            }
        }
        Text(
            text = "$done/$total",
            style = MedicalMateTheme.typography.labelXS,
            color = MedicalMateTheme.colors.fgPrimary,
            modifier = Modifier.graphicsLayer { alpha = stepAlpha.value },
        )
    }
}

/** CSS `ease-out`. 프로토타입이 이 곡선이다. */
private val CssEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private const val RING_FILL_DELAY_MS = 750
private const val RING_FILL_MS = 800
private const val STEP_FADE_DELAY_MS = 1_000
private const val STEP_FADE_MS = 400

/** 마스터의 좌우 18. 간격 스케일 밖 값이다. */
private val CardPaddingHorizontal = 18.dp

private val OrbSize = 44.dp
private val RingSize = 44.dp
private val DateWidth = 44.dp
private val DateHeight = 48.dp

/** 마스터 원의 굵기 3.96 ÷ 지름 44. */
private const val RING_STROKE_RATIO = 0.09f
private const val FULL_TURN = 360f

/** 12시에서 시작한다. */
private const val START_ANGLE = -90f
