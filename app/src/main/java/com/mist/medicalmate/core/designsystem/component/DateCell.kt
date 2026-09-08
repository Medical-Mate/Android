package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.2 `Date Cell`.
 *
 * 7열 캘린더의 한 칸이다. 시각 규격은 46x46이다.
 *
 * 세 가지가 서로 다른 뜻이라 표시 방법도 다르다(문서 8.2).
 * [hasRecord]는 그 날 기록이 있다는 사실이라 5 점으로 알린다.
 * [isToday]는 오늘이라는 상태라 테두리로 알린다.
 * [selected]는 사용자가 고른 칸이라 채움으로 알린다.
 * 셋이 겹칠 수 있어 한 칸에 함께 나타난다.
 *
 * 46은 접근성 기준 48보다 작다. 문서 11.4에 따라 hit area는 48을 맞춰야 하는데, 7열
 * 캘린더에서 칸마다 48을 넣으면 390 폭에 들어가지 않는다. 그래서 이 컴포넌트는 시각
 * 크기만 담당하고, **캘린더 그리드가 칸 사이 여백까지 포함해 hit test를 하도록 호출자가
 * 배치한다.** 문서도 같은 방법을 적어 두었다.
 *
 * 접근성 이름은 날짜와 상태를 한 번에 읽는다. 점만 두면 스크린 리더에 기록 유무가
 * 전달되지 않는다.
 */
@Composable
fun MedicalMateDateCell(
    day: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    hasRecord: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors
    val spoken = dateCellDescription(day = day, isToday = isToday, hasRecord = hasRecord)
    val content =
        when {
            !enabled -> colors.fgDisabled
            selected -> colors.fgOnPrimary
            else -> colors.fgDefault
        }

    Box(
        modifier =
        modifier
            .size(CellSize)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .background(
                color = if (selected && enabled) colors.bgPrimary else colors.bgSurface,
                shape = MedicalMateRadius.full,
            )
            .then(todayBorder(isToday = isToday, selected = selected, enabled = enabled)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
        ) {
            Text(
                text = day.toString(),
                style = MedicalMateTheme.typography.bodyM,
                color = content,
            )
            if (hasRecord) {
                RecordDot(selected = selected)
            }
        }
    }
}

/** 날짜와 상태를 한 번에 읽는다. 점만 두면 스크린 리더에 기록 유무가 전달되지 않는다. */
@Composable
private fun dateCellDescription(day: Int, isToday: Boolean, hasRecord: Boolean): String {
    val recordLabel = stringResource(R.string.date_cell_has_record)
    val todayLabel = stringResource(R.string.date_cell_today)
    return buildString {
        append(day)
        if (isToday) append(", $todayLabel")
        if (hasRecord) append(", $recordLabel")
    }
}

/** 오늘 표시는 고른 칸에서 생략한다. 채움이 이미 그 칸을 가리킨다. */
@Composable
private fun todayBorder(isToday: Boolean, selected: Boolean, enabled: Boolean): Modifier =
    if (isToday && !selected && enabled) {
        Modifier.border(
            width = TodayBorderWidth,
            color = MedicalMateTheme.colors.borderPrimary,
            shape = MedicalMateRadius.full,
        )
    } else {
        Modifier
    }

@Composable
private fun RecordDot(selected: Boolean) {
    Box(
        modifier =
        Modifier
            .size(RecordDotSize)
            .background(
                color =
                if (selected) {
                    MedicalMateTheme.colors.fgOnPrimary
                } else {
                    MedicalMateTheme.colors.bgPrimary
                },
                shape = MedicalMateRadius.full,
            ),
    )
}

/** 문서 8.2가 지정한 시각 크기. hit area는 그리드가 맡는다. */
private val CellSize = 46.dp

/** 문서 8.2의 "5px dot". */
private val RecordDotSize = 5.dp

private val TodayBorderWidth = 1.5.dp
