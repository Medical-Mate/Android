package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.Amber100
import com.mist.medicalmate.core.designsystem.Amber500
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** `Calendar Day`의 `State`. 빈 칸(`Empty`)은 [MedicalMateCalendarGrid]가 자리만 비운다. */
enum class MedicalMateCalendarDayState {
    DEFAULT,

    /** 오늘. 옅은 면과 테두리다 — 상태이지 선택이 아니다. */
    TODAY,

    /** 고른 날. 채움이다. 오늘이어도 이쪽이 이긴다. */
    SELECTED,

    /** 다른 달이나 고를 수 없는 지난 칸. */
    DISABLED,
}

/**
 * 10-15의 `Calendar Day`(`3778:1867`, #326). 캘린더 날짜 한 칸이고 [MedicalMateCalendarGrid] 안에서 쓴다.
 *
 * 칸은 폭을 채우고 높이 46이며 안쪽 면이 42 · 반경 13이다. **숫자 자리는 점이 있든 없든 같다** — 위 7 · 숫자 24 ·
 * 간격 2 · 점 5다. 예전 `Date Cell`은 점이 켜지면 숫자가 올라갔다. 토요일은 `fg/link`, 일요일은 `fg/danger`다.
 *
 * 점은 기록이 `bg/primary`, 예정이 `amber/500`이다. 고른 날은 숫자와 기록 점이 흰색, 예정 점은 `amber/100`이고
 * 고를 수 없는 칸은 숫자가 `fg/disabled`, 점이 40%다. 예정 점은 마스터가 시맨틱 토큰이 아니라 팔레트 `amber/500`
 * · `amber/100`을 바로 묶어 그대로 쓴다.
 *
 * 누르는 자리는 안쪽 면이 아니라 칸 전체다 — 42는 접근성 기준 48보다 작고, 일곱 칸이 콘텐츠 폭을 나눠 가져 칸
 * 사이 여백까지 누를 수 있게 둔다.
 */
@Composable
fun MedicalMateCalendarDay(
    day: Int,
    weekday: DayOfWeek,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: MedicalMateCalendarDayState = MedicalMateCalendarDayState.DEFAULT,
    marker: MedicalMateDateMarker = MedicalMateDateMarker.NONE,
) {
    val colors = MedicalMateTheme.colors
    val selected = state == MedicalMateCalendarDayState.SELECTED
    val enabled = state != MedicalMateCalendarDayState.DISABLED
    val today = state == MedicalMateCalendarDayState.TODAY
    val spoken = dateCellDescription(day = day, isToday = today, marker = marker)
    val number =
        when {
            !enabled -> colors.fgDisabled
            selected -> colors.fgOnPrimary
            weekday == DayOfWeek.SATURDAY -> colors.fgLink
            weekday == DayOfWeek.SUNDAY -> colors.fgDanger
            else -> colors.fgDefault
        }
    Box(
        modifier =
        modifier
            .height(CalendarDayHeight)
            .selectable(selected = selected, enabled = enabled, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
            Modifier
                .size(CalendarDayFace)
                .clip(MedicalMateRadius.dateCell)
                .background(
                    when {
                        selected -> colors.bgPrimary
                        today -> colors.bgPrimaryFaint
                        else -> Color.Transparent
                    },
                )
                .then(
                    if (today) {
                        Modifier.border(CalendarTodayBorder, colors.borderPrimary, MedicalMateRadius.dateCell)
                    } else {
                        Modifier
                    },
                )
                .padding(top = CalendarDayTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
        ) {
            Text(text = day.toString(), style = MedicalMateTheme.typography.bodyMStrong, color = number)
            if (marker != MedicalMateDateMarker.NONE) {
                Box(
                    modifier =
                    Modifier
                        .size(CalendarDotSize)
                        .background(dotColor(marker, selected, enabled), MedicalMateRadius.full),
                )
            }
        }
    }
}

@Composable
private fun dotColor(marker: MedicalMateDateMarker, selected: Boolean, enabled: Boolean): Color {
    val base =
        when {
            marker == MedicalMateDateMarker.PLANNED && selected -> Amber100
            marker == MedicalMateDateMarker.PLANNED -> Amber500
            selected -> MedicalMateTheme.colors.fgOnPrimary
            else -> MedicalMateTheme.colors.bgPrimary
        }
    return if (enabled) base else base.copy(alpha = DISABLED_DOT_ALPHA)
}

/**
 * 10-15의 `Calendar Grid`(`3778:1868`, #326). 한 달 캘린더다.
 *
 * 면은 `bg/surface` · 반경 20 · `Elevation/Card` 두 겹이고 안쪽 여백이 위아래 12 · 좌우 8, 줄 사이가 4다. 맨 위
 * 요일 줄(`Label/S`, 일 `fg/danger` · 토 `fg/link` · 나머지 `fg/subtle`, 아래 4) 아래로 주마다 일곱 칸이 서고, 맨
 * 아래 범례(기록 있음 · 예정)다. 한 주는 일요일에서 시작하고 그 달에 든 주만큼 줄이 선다 — 마스터의 `Show Week 6`이
 * 6주인 달에만 켜진다. 그 달이 아닌 칸은 비운다(`Empty`).
 */
@Composable
fun MedicalMateCalendarGrid(
    month: YearMonth,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    stateOf: (LocalDate) -> MedicalMateCalendarDayState = { MedicalMateCalendarDayState.DEFAULT },
    markerOf: (LocalDate) -> MedicalMateDateMarker = { MedicalMateDateMarker.NONE },
) {
    val colors = MedicalMateTheme.colors
    val shape = MedicalMateRadius.lg
    val offset = month.atDay(1).dayOfWeek.value % DAYS_IN_WEEK
    val last = month.lengthOfMonth()
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .dropShadow(shape, MedicalMateEffects.cardWide)
            .dropShadow(shape, MedicalMateEffects.cardTight)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(horizontal = MedicalMateSpace.s8, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = MedicalMateSpace.s4)) {
            stringArrayResource(R.array.calendar_weekdays).forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MedicalMateTheme.typography.labelS,
                    color =
                    when (index) {
                        0 -> colors.fgDanger
                        DAYS_IN_WEEK - 1 -> colors.fgLink
                        else -> colors.fgSubtle
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        var first = 1 - offset
        while (first <= last) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DAYS_IN_WEEK) { index ->
                    val day = first + index
                    if (day in 1..last) {
                        val date = month.atDay(day)
                        MedicalMateCalendarDay(
                            day = day,
                            weekday = date.dayOfWeek,
                            state = stateOf(date),
                            marker = markerOf(date),
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(CalendarDayHeight))
                    }
                }
            }
            first += DAYS_IN_WEEK
        }
        CalendarLegend()
    }
}

/** 범례. 위 10 · 아래 2 · 좌우 10, 항목 사이 14, 점과 글자 사이 5다. 점은 6이다. */
@Composable
private fun CalendarLegend() {
    Row(
        modifier =
        Modifier.padding(
            start = MedicalMateSpace.s10,
            end = MedicalMateSpace.s10,
            top = MedicalMateSpace.s10,
            bottom = MedicalMateSpace.s2,
        ),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = MedicalMateTheme.colors.bgPrimary, label = stringResource(R.string.calendar_legend_record))
        LegendItem(color = Amber500, label = stringResource(R.string.calendar_legend_planned))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LegendGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(LegendDotSize).background(color, MedicalMateRadius.full))
        Text(text = label, style = MedicalMateTheme.typography.labelS, color = MedicalMateTheme.colors.fgSubtle)
    }
}

private const val DAYS_IN_WEEK = 7

/** 고를 수 없는 칸의 점. 마스터가 40%로 묶었다. */
private const val DISABLED_DOT_ALPHA = 0.4f

private val CalendarDayHeight = 46.dp
private val CalendarDayFace = 42.dp
private val CalendarDayTop = 7.dp
private val CalendarDotSize = 5.dp
private val CalendarTodayBorder = 1.dp
private val LegendDotSize = 6.dp
private val LegendGap = 5.dp
