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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 날짜 아래 표시의 뜻.
 *
 * [RECORD]는 그 날 기록이 있다는 사실, [PLANNED]는 앞으로의 일정이다. 캘린더 범례가
 * `기록 있음` · `예정`으로 둘을 나눠 적는다.
 */
enum class MedicalMateDateMarker { NONE, RECORD, PLANNED }

/**
 * DESIGN.md의 `Date Cell`.
 *
 * 7열 캘린더의 한 칸이다. 시각 규격은 42x42다. 문서는 시트 안에서 34를 쓰라고 하는데
 * 아직 그 자리가 없다.
 *
 * 세 가지가 서로 다른 뜻이라 표시 방법도 다르다(문서의 컴포넌트 규격).
 * [marker]는 그 날 기록이 있거나 일정이 있다는 사실이라 5 점으로 알린다.
 * [isToday]는 오늘이라는 상태라 **옅은 면과 테두리**로 알린다. 채움이 아니다.
 * [selected]는 사용자가 고른 칸이라 채움으로 알린다.
 * 셋이 겹칠 수 있어 한 칸에 함께 나타난다.
 *
 * 칸은 원이 아니라 반경 13의 둥근 사각형이다(마스터 `335:1188`). 문서 3.0이 13을 스케일
 * 밖 값으로 잡고 `radius/sm` 12로 통일하겠다고 적었다. Figma가 바뀌면 함께 바꾼다.
 *
 * 42는 접근성 기준 48보다 작다. 문서의 접근성 기준이 hit area 48을 요구하는데, 7열
 * 캘린더에서 칸마다 48을 넣으면 360 폭에 들어가지 않는다. 그래서 이 컴포넌트는 시각
 * 크기만 담당하고, **캘린더 그리드가 칸 사이 여백까지 포함해 hit test를 하도록 호출자가
 * 배치한다.** 문서도 같은 방법을 적어 두었다.
 *
 * 접근성 이름은 날짜와 상태를 한 번에 읽는다. 점만 두면 스크린 리더에 기록 유무가
 * 전달되지 않는다.
 *
 * [size]는 시트에 들어가는 작은 격자를 위해 열어 뒀다. 1r-4-D의 날짜 선택 시트가 34를
 * 쓴다. 시트는 가로 폭이 화면과 같은데 위아래로 버튼과 제목까지 들어가서 42로는 한 달이
 * 다 보이지 않는다.
 */
@Composable
fun MedicalMateDateCell(
    day: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    marker: MedicalMateDateMarker = MedicalMateDateMarker.NONE,
    enabled: Boolean = true,
    size: Dp = MedicalMateDateCellSize,
) {
    val colors = MedicalMateTheme.colors
    val spoken = dateCellDescription(day = day, isToday = isToday, marker = marker)
    val content =
        when {
            !enabled -> colors.fgDisabled
            selected -> colors.fgOnPrimary
            else -> colors.fgDefault
        }

    Box(
        modifier =
        modifier
            .size(size)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .background(color = cellBackground(selected, isToday, enabled), shape = MedicalMateRadius.dateCell)
            .then(todayBorder(isToday = isToday, selected = selected, enabled = enabled)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DotGap),
        ) {
            Text(
                text = day.toString(),
                style = MedicalMateTheme.typography.bodyMStrong,
                color = content,
            )
            if (marker != MedicalMateDateMarker.NONE) {
                DayMarker(marker = marker, selected = selected)
            }
        }
    }
}

/** 날짜와 상태를 한 번에 읽는다. 점만 두면 스크린 리더에 표시의 뜻이 전달되지 않는다. */
@Composable
private fun dateCellDescription(day: Int, isToday: Boolean, marker: MedicalMateDateMarker): String {
    val todayLabel = stringResource(R.string.date_cell_today)
    val markerLabel =
        when (marker) {
            MedicalMateDateMarker.NONE -> null
            MedicalMateDateMarker.RECORD -> stringResource(R.string.date_cell_has_record)
            MedicalMateDateMarker.PLANNED -> stringResource(R.string.date_cell_planned)
        }
    return buildString {
        append(day)
        if (isToday) append(", $todayLabel")
        markerLabel?.let { append(", $it") }
    }
}

/**
 * 칸의 면.
 *
 * 오늘은 채움이 아니라 **옅은 면**이다. 마스터 설명이 "오늘은 상태이지 선택이 아니다"라고
 * 적어 둔 구분이고, 채움을 주면 고른 칸과 헷갈린다.
 */
@Composable
private fun cellBackground(selected: Boolean, isToday: Boolean, enabled: Boolean) = when {
    selected && enabled -> MedicalMateTheme.colors.bgPrimary
    isToday && enabled -> MedicalMateTheme.colors.bgPrimaryFaint
    else -> MedicalMateTheme.colors.bgSurface
}

/** 오늘 표시는 고른 칸에서 생략한다. 채움이 이미 그 칸을 가리킨다. */
@Composable
private fun todayBorder(isToday: Boolean, selected: Boolean, enabled: Boolean): Modifier =
    if (isToday && !selected && enabled) {
        Modifier.border(
            width = TodayBorderWidth,
            color = MedicalMateTheme.colors.borderPrimary,
            shape = MedicalMateRadius.dateCell,
        )
    } else {
        Modifier
    }

/**
 * 날짜 아래 표시.
 *
 * 기록은 **채운 점**, 예정은 **빈 원**이다. 둘을 색으로만 가르면 캘린더에서 지난 기록과
 * 앞으로의 일정이 구별되지 않는다.
 *
 * 예정의 빈 원은 Date Cell 마스터에 없다. 시안 `1r-1`이 그렇게 쓰고 범례까지 두고 있어서
 * 화면을 따랐고, variant 추가는 디자인 트랙에 넘겼다.
 */
@Composable
private fun DayMarker(marker: MedicalMateDateMarker, selected: Boolean) {
    val tint = if (selected) MedicalMateTheme.colors.fgOnPrimary else MedicalMateTheme.colors.bgPrimary
    Box(
        modifier =
        Modifier
            .size(RecordDotSize)
            .then(
                if (marker == MedicalMateDateMarker.PLANNED) {
                    Modifier.border(width = PlannedRingWidth, color = tint, shape = MedicalMateRadius.full)
                } else {
                    Modifier.background(color = tint, shape = MedicalMateRadius.full)
                },
            ),
    )
}

/**
 * Figma 마스터의 시각 크기. hit area는 그리드가 맡는다.
 *
 * 360 재단에서 46에서 42로 줄었다. 한 주가 콘텐츠 폭 320을 일곱으로 나눠 45.7이라
 * 46이면 칸을 넘친다. 격자의 빈 칸도 같은 값을 써야 요일이 어긋나지 않아 공개한다.
 */
val MedicalMateDateCellSize = 42.dp

/** 시트 안의 작은 격자. 1r-4-D의 마스터 값이다. */
val MedicalMateDateCellSizeCompact = 34.dp

/** 문서의 "5px dot". */
private val RecordDotSize = 5.dp

/** Date Cell v2가 더한 오늘 표시 링. 색만으로 상태를 구분하던 것을 테두리로 보강한다. */
private val TodayBorderWidth = 1.dp

/** 마스터의 숫자와 점 사이 `gap-[3px]`. 문서의 간격 토큰에 없는 값이다. */
private val DotGap = 3.dp

/** 빈 원의 테두리. 5px 안에서 채움과 구별되려면 이보다 두꺼울 수 없다. */
private val PlannedRingWidth = 1.dp
