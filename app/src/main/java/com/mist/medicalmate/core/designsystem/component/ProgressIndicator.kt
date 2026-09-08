package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.4 `Progress Indicator`.
 *
 * 단계별 칸과 `n/총` 숫자를 함께 보여준다. 칸만 두면 몇 단계가 남았는지 세어야 하고,
 * 숫자만 두면 진행 정도가 한눈에 안 들어온다.
 *
 * [total]이 6을 넘으면 칸을 나누지 않고 연속 막대로 바꾼다(문서 8.4). 칸이 잘게 쪼개지면
 * 390 폭에서 각 칸이 몇 픽셀이 되어 진행이 보이지 않는다.
 *
 * 접근성 트리에서는 "3/4 단계"로 한 번만 읽는다. 칸을 하나씩 읽으면 소리만 길어진다.
 */
@Composable
fun MedicalMateProgressIndicator(current: Int, modifier: Modifier = Modifier, total: Int = DEFAULT_STEPS) {
    require(isValidStep(current, total)) { "현재 단계는 1..$total 범위여야 합니다. 받은 값: $current" }
    val spoken = stringResource(R.string.progress_step, current, total)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = ProgressHeight)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shouldUseContinuousBar(total)) {
            ContinuousBar(current = current, total = total, modifier = Modifier.weight(1f))
        } else {
            SegmentedBar(current = current, total = total, modifier = Modifier.weight(1f))
        }
        Text(
            text = spoken,
            style = MedicalMateTheme.typography.labelM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

@Composable
private fun SegmentedBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        repeat(total) { index ->
            Track(
                filled = index < current,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContinuousBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = MedicalMateRadius.full,
        color = MedicalMateTheme.colors.bgSubtle,
        modifier = modifier.height(TrackHeight),
    ) {
        Row {
            Track(filled = true, modifier = Modifier.weight(current.toFloat()))
            // 남은 폭을 0으로 만들면 weight가 예외를 낸다. 마지막 단계에서는 채움만 남는다.
            if (current < total) {
                Track(filled = false, modifier = Modifier.weight((total - current).toFloat()))
            }
        }
    }
}

@Composable
private fun Track(filled: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = MedicalMateRadius.full,
        color =
        if (filled) {
            MedicalMateTheme.colors.bgPrimary
        } else {
            MedicalMateTheme.colors.bgSubtle
        },
        modifier = modifier.height(TrackHeight),
        content = {},
    )
}

/** 문서 8.4의 `Step=1/2/3/4`. */
private const val DEFAULT_STEPS = 4

/** 이 수를 넘으면 칸이 너무 잘게 쪼개져 연속 막대로 바꾼다. */
private const val SEGMENT_LIMIT = 6

/**
 * 아래 셋은 그리는 코드가 아니라 판단이라 컴포저블 밖에 둔다. 컴포저블 안에 두면
 * 계측 테스트 없이는 확인할 수 없다(CLAUDE.md 3장).
 */
internal fun progressDefaultSteps(): Int = DEFAULT_STEPS

internal fun shouldUseContinuousBar(total: Int): Boolean = total > SEGMENT_LIMIT

internal fun isValidStep(current: Int, total: Int): Boolean = current in 1..total

private val ProgressHeight = 34.dp

private val TrackHeight = 6.dp
