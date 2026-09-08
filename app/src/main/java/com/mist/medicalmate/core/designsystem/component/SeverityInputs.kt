package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.2 `Severity Slider`.
 *
 * 기본 통증 입력이다. 5단계로 붙고 drag와 정지점 tap을 모두 받는다.
 *
 * Material3 `Slider` 위에 얹었다. 문서 8.2가 요구하는 키보드 ←/→와 Home/End를 그쪽이
 * 이미 처리한다(material3 1.4.0의 `slideOnKeyEvents`). 직접 만들면 그 네 가지를
 * 빠뜨리기 쉽다.
 *
 * 스크린 리더에는 "3단계, 꽤 아파요"로 읽힌다. 기본 슬라이더 값은 "3.0"으로 읽혀서
 * 무엇을 고른 것인지 알 수 없다.
 *
 * 단계를 색만으로 전달하지 않는다. 위쪽 판독 영역에 숫자, 낱말, 상황 설명이 함께 나온다
 * (문서 1항 6번).
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        SeverityReadoutCard(severity = severity, showNrs = showNrs)
        Slider(
            value = severity.level.toFloat(),
            onValueChange = { onSeverityChange(MedicalMateSeverity.ofLevel(it.toInt())) },
            valueRange = 1f..MedicalMateSeverity.entries.size.toFloat(),
            steps = MedicalMateSeverity.entries.size - 2,
            enabled = enabled,
            interactionSource = interactionSource,
            colors =
            SliderDefaults.colors(
                activeTrackColor = severity.base,
                inactiveTrackColor = MedicalMateTheme.colors.bgSubtle,
                activeTickColor = severity.base,
                inactiveTickColor = MedicalMateTheme.colors.borderDefault,
                thumbColor = MedicalMateTheme.colors.bgSurface,
            ),
            thumb = { SliderThumb() },
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

/** 흰 원에 브랜드 테두리. 채움 트랙 색이 단계마다 바뀌어도 손잡이 위치가 또렷하다. */
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

/** 판독 영역. 문서 2.2에 따라 tint를 면에, base를 숫자 칩에 쓴다. */
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
 * DESIGN.md 8.2 `Severity Scale`.
 *
 * 슬라이더의 대안이다. 글자 배율이 크거나 drag가 어려울 때 쓴다(문서 8.2). 손이 떨리는
 * 환자에게 drag만 남기면 통증을 입력할 수 없다.
 *
 * 다섯 칸을 각각 눌러 고른다. 고른 칸은 채움 색과 테두리로 함께 표시하고, 아래에 낱말과
 * 상황 설명이 나온다.
 */
@Composable
fun MedicalMateSeverityScale(
    severity: MedicalMateSeverity,
    onSeverityChange: (MedicalMateSeverity) -> Unit,
    modifier: Modifier = Modifier,
    showNrs: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            MedicalMateSeverity.entries.forEach { level ->
                ScaleCell(
                    level = level,
                    selected = level == severity,
                    onClick = { onSeverityChange(level) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SeverityReadoutCard(severity = severity, showNrs = showNrs)
    }
}

@Composable
private fun ScaleCell(
    level: MedicalMateSeverity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MedicalMateTheme.colors
    val label = stringResource(level.labelRes)
    val spoken = stringResource(R.string.severity_level_content_description, level.level, label)

    Surface(
        onClick = onClick,
        shape = MedicalMateRadius.sm,
        color = if (selected) level.base else colors.bgSubtle,
        contentColor = colors.fgDefault,
        border = if (selected) BorderStroke(SelectedWidth, colors.borderPrimary) else null,
        modifier =
        modifier
            .height(MedicalMateSize.controlMd)
            .semantics { stateDescription = spoken },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = level.level.toString(), style = MedicalMateTheme.typography.bodyLStrong)
        }
    }
}

/**
 * DESIGN.md 8.2 `Severity Select`.
 *
 * 설명을 모두 펼친 세로 카드다. 단독 화면에 배치한다(문서 8.2). 다섯 단계의 상황 설명을
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

private val ThumbSize = 24.dp

private val ThumbBorderWidth = 2.dp

private val LevelChipSize = 28.dp

/** Chip과 Source Quote의 선택 경계와 같은 두께다. */
private val SelectedWidth = 1.5.dp
