package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlin.math.roundToInt

/**
 * DESIGN.md의 `Severity Slider`.
 *
 * 기본 통증 입력이다. 5단계로 붙고 drag와 정지점 tap을 모두 받는다.
 *
 * **끄는 동안에는 손가락을 그대로 따라가고 놓을 때 단계로 붙는다**(#228). 끄는 중에도 다섯
 * 지점으로만 뛰면 손가락과 손잡이가 따로 놀아서 뻑뻑하게 읽힌다. 값은 다섯 그대로다 — 지나는
 * 단계마다 판독 카드와 트랙 색이 바뀐다.
 *
 * 트랙과 정지점은 마스터(`339:1293`) 값이다. 트랙 10, 정지점 6, 손잡이 28에 브랜드 링 4다.
 * 정지점은 채움 위에 오면 흰색, 밖이면 테두리 색이다 — 마스터가 그 둘을 다른 그림으로
 * 그려 뒀고, 채움과 같은 색으로 두면 지나온 정지점이 사라진다.
 *
 * Material3 `Slider` 위에 얹었다. 문서의 컴포넌트 규격이 요구하는 키보드 ←/→와 Home/End를 그쪽이
 * 이미 처리한다(material3 1.4.0의 `slideOnKeyEvents`). 직접 만들면 그 네 가지를
 * 빠뜨리기 쉽다.
 *
 * 스크린 리더에는 "3단계, 꽤 아파요"로 읽힌다. 기본 슬라이더 값은 "3.0"으로 읽혀서
 * 무엇을 고른 것인지 알 수 없다.
 *
 * 단계를 색만으로 전달하지 않는다. 위쪽 판독 영역에 숫자, 낱말, 상황 설명이 함께 나온다
 * (문서의 D11).
 *
 * [showNrs]는 문서의 `Show NRS` boolean이다. 의료진용 표기라 기본은 끈다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMateSeveritySlider(
    severity: MedicalMateSeverity,
    onSeverityChange: (MedicalMateSeverity) -> Unit,
    modifier: Modifier = Modifier,
    showNrs: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(severity.labelRes)
    val spoken = stringResource(R.string.severity_level_content_description, severity.level, label)
    val last = MedicalMateSeverity.entries.size.toFloat()
    val dragging by interactionSource.collectIsDraggedAsState()
    var position by remember { mutableFloatStateOf(severity.level.toFloat()) }

    // 놓으면 고른 단계에 붙인다. 끄는 중에는 붙이지 않는다 — 붙이면 손잡이가 손가락을 떠난다.
    LaunchedEffect(severity, dragging) {
        if (!dragging) position = severity.level.toFloat()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        SeverityReadoutCard(severity = severity, showNrs = showNrs)
        Slider(
            value = position,
            onValueChange = { raw ->
                position = raw
                val level = raw.roundToInt()
                if (level != severity.level) onSeverityChange(MedicalMateSeverity.ofLevel(level))
            },
            valueRange = 1f..last,
            enabled = enabled,
            interactionSource = interactionSource,
            thumb = { SliderThumb() },
            track = { SeverityTrack(fraction = (position - 1f) / (last - 1f), fill = severity.base) },
            modifier = Modifier.semantics { stateDescription = spoken },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.severity_scale_low),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
            Text(
                text = stringResource(R.string.severity_scale_high),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
    }
}

/**
 * 트랙과 정지점.
 *
 * M3 기본 트랙을 쓰지 않는다. 마스터의 트랙이 10이고 정지점이 6인데 기본값은 그보다 얇고,
 * 무엇보다 지나온 정지점을 채움과 같은 색으로 그려서 보이지 않는다.
 */
@Composable
private fun SeverityTrack(fraction: Float, fill: Color) {
    val base = MedicalMateTheme.colors.bgSubtle
    val passed = MedicalMateTheme.colors.bgSurface
    val ahead = MedicalMateTheme.colors.borderDefault
    val stops = MedicalMateSeverity.entries.size

    Canvas(modifier = Modifier.fillMaxWidth().height(TrackHeight)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = base, size = size, cornerRadius = radius)

        // 손잡이가 오갈 수 있는 폭만 쓴다. 정지점도 손잡이가 서는 자리에 찍혀야 맞는다.
        val inset = ThumbSize.toPx() / 2
        val travel = (size.width - inset * 2).coerceAtLeast(0f)
        val head = inset + travel * fraction
        drawRoundRect(color = fill, size = Size(head, size.height), cornerRadius = radius)

        repeat(stops) { index ->
            val x = inset + travel * index / (stops - 1f)
            drawCircle(
                color = if (x <= head) passed else ahead,
                radius = StopSize.toPx() / 2,
                center = Offset(x, size.height / 2),
            )
        }
    }
}

/** 흰 원에 브랜드 링. 채움 트랙 색이 단계마다 바뀌어도 손잡이 위치가 또렷하다. */
@Composable
private fun SliderThumb() {
    Box(
        modifier =
        Modifier
            .size(ThumbSize)
            .background(MedicalMateTheme.colors.bgSurface, MedicalMateRadius.full)
            .border(ThumbBorderWidth, MedicalMateTheme.colors.borderPrimary, MedicalMateRadius.full),
    )
}

/** 판독 영역. 문서의 원시 팔레트에 따라 tint를 면에, base를 숫자 칩에 쓴다. */
@Composable
private fun SeverityReadoutCard(severity: MedicalMateSeverity, showNrs: Boolean) {
    Surface(
        shape = MedicalMateRadius.buttonM,
        color = severity.tint,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MedicalMateSpace.s12),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            SeverityLevelChip(severity)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
            ) {
                Text(
                    text = stringResource(severity.labelRes),
                    style = MedicalMateTheme.typography.bodyLStrong,
                )
                Text(
                    text = stringResource(severity.descriptionRes),
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
            if (showNrs) {
                Text(
                    text = stringResource(R.string.severity_nrs, severity.nrsFirst, severity.nrsLast),
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
        }
    }
}

/**
 * 단계 숫자 칩.
 *
 * 글자색을 `fg/default`로 고정한다. severity base가 옅은 1~2단계에서 흰 글자를 쓰면
 * 대비가 무너진다.
 */
@Composable
internal fun SeverityLevelChip(severity: MedicalMateSeverity) {
    Surface(
        shape = MedicalMateRadius.xs,
        color = severity.base,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.size(LevelChipSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = severity.level.toString(), style = MedicalMateTheme.typography.labelM)
        }
    }
}

/**
 * DESIGN.md의 `Severity Select`.
 *
 * 설명을 모두 펼친 세로 카드다. 단독 화면에 배치한다(문서의 컴포넌트 규격). 다섯 단계의 상황 설명을
 * 한 번에 읽고 고르는 방식이라, 다른 입력과 같은 화면에 두면 화면이 이것만으로 찬다.
 */
@Composable
fun MedicalMateSeveritySelect(
    severity: MedicalMateSeverity?,
    onSeverityChange: (MedicalMateSeverity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        MedicalMateSeverity.entries.forEach { level ->
            SelectCard(
                level = level,
                selected = level == severity,
                onClick = { onSeverityChange(level) },
            )
        }
    }
}

@Composable
private fun SelectCard(level: MedicalMateSeverity, selected: Boolean, onClick: () -> Unit) {
    val colors = MedicalMateTheme.colors

    Surface(
        onClick = onClick,
        shape = MedicalMateRadius.buttonM,
        color = if (selected) level.tint else colors.bgSubtle,
        contentColor = colors.fgDefault,
        border = if (selected) BorderStroke(SelectedWidth, colors.borderPrimary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MedicalMateSpace.s12),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            SeverityLevelChip(level)
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
                Text(
                    text = stringResource(level.labelRes),
                    style = MedicalMateTheme.typography.bodyLStrong,
                )
                Text(
                    text = stringResource(level.descriptionRes),
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgSubtle,
                )
            }
        }
    }
}

private val ThumbSize = 28.dp

private val ThumbBorderWidth = 4.dp

/** 마스터의 트랙 10과 정지점 6. */
private val TrackHeight = 10.dp

private val StopSize = 6.dp

private val LevelChipSize = 28.dp

/** Chip과 Source Quote의 선택 경계와 같은 두께다. */
private val SelectedWidth = 1.5.dp
