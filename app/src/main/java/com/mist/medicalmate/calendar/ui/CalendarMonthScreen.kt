package com.mist.medicalmate.calendar.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateCell
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateCellSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateMarker
import com.mist.medicalmate.core.designsystem.component.MedicalMateFab
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.designsystem.component.MedicalMateTabBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 와이어프레임 1r-1. Figma `406:2310`.
 *
 * 하단 탭의 캘린더다. 월 격자에서 날을 고르면 아래에 그 날의 일정이 나오고, 일정을 누르면
 * 일자 화면(1r-2)으로 들어간다.
 *
 * 추가 버튼을 화면 우하단에 띄운다. Figma 수정사항이 상단 탭바의 일정 추가 버튼을 없애고
 * 캘린더·일자 페이지의 + 버튼으로 정리했다. 마스터는 `FAB`이다 — 월 화면의 세 프레임이
 * 모두 그 인스턴스를 둔다. 본문 위에 떠서 격자를 가리는 조작이라 그림자로 층을 알려야 하고,
 * 그림자가 없는 `Icon Button`의 `SOLID`는 격자에 눌러붙어 보인다. 일자 화면(1r-2)에는
 * 이 버튼이 없다.
 *
 * 1Depth 화면이라 하단 탭을 함께 그린다.
 */
@Composable
fun CalendarMonthScreen(
    state: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onScheduleClick: (String) -> Unit,
    onCardOpenClick: (String) -> Unit,
    onCardSheetDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.calendar_title),
            leading = MedicalMateNavLeading.NONE,
        )
        Box(modifier = Modifier.weight(1f)) {
            MonthContent(
                state = state,
                onPreviousMonthClick = onPreviousMonthClick,
                onNextMonthClick = onNextMonthClick,
                onDayClick = onDayClick,
                onScheduleClick = onScheduleClick,
            )
            MedicalMateFab(
                onClick = onAddClick,
                icon = MedicalMateIcons.Plus,
                contentDescription = stringResource(R.string.calendar_add_schedule),
                modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MedicalMateSize.gutter),
            )
        }
        MedicalMateTabBar(selected = MedicalMateTab.CALENDAR, onSelect = onTabSelect)
    }

    state.cardSheet?.let { card ->
        CardSheet(
            date = state.selected,
            card = card,
            onCardOpenClick = onCardOpenClick,
            onScheduleClick = onAddClick,
            onDismissRequest = onCardSheetDismiss,
        )
    }
}

/**
 * 카드만 있는 날 시트. Figma 1r-1-S `1226:4669`.
 *
 * 일정이 없고 카드만 쓴 날을 눌렀을 때 뜬다. 갈 화면이 없는 날이라 그 자리에서 무엇이
 * 있는지 보여주고, 일정을 만들면 그때부터 일자 화면이 열린다.
 *
 * 날짜와 섹션 머리와 줄을 한 `Column`으로 묶는다. 시트가 자식 사이에 12를 두는데 시안은
 * 이 셋을 붙여 두고 `Section Header`의 안쪽 여백으로만 띄운다.
 *
 * 일정 만들기는 캘린더의 + 버튼과 같은 곳으로 간다. 그 화면(1r-4)이 아직 없어서 둘 다
 * 목적지가 비어 있다.
 *
 * `ModalBottomSheet`이 실험 API라 [MedicalMateBottomSheet]의 기본 `sheetState`도 그 타입을
 * 드러낸다. 컴포넌트 쪽과 같은 이유로 호출부에도 붙인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardSheet(
    date: LocalDate,
    card: DayCard,
    onCardOpenClick: (String) -> Unit,
    onScheduleClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    MedicalMateBottomSheet(onDismissRequest = onDismissRequest) {
        Column {
            Text(
                text = date.format(dayFormat),
                style = MedicalMateTheme.typography.headingS,
                color = MedicalMateTheme.colors.fgDefault,
            )
            MedicalMateSectionHeader(title = stringResource(R.string.calendar_card_sheet_section))
            MedicalMateListRow(
                title = card.title,
                meta = card.meta,
                badge = card.status,
                type = MedicalMateListRowType.BADGE,
                onClick = { onCardOpenClick(card.id) },
            )
        }
        Text(
            text = stringResource(R.string.calendar_card_sheet_hint),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        MedicalMateButton(
            label = stringResource(R.string.calendar_card_sheet_schedule),
            onClick = onScheduleClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MonthContent(
    state: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onScheduleClick: (String) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        MonthHeader(
            month = state.month,
            onPreviousMonthClick = onPreviousMonthClick,
            onNextMonthClick = onNextMonthClick,
        )
        MonthGrid(state = state, onDayClick = onDayClick)
        SelectedDay(state = state, onScheduleClick = onScheduleClick)
    }
}

/** 월 이동. 가운데에 연·월을 두고 좌우에 화살표다. */
@Composable
private fun MonthHeader(month: YearMonth, onPreviousMonthClick: () -> Unit, onNextMonthClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateIconButton(
            onClick = onPreviousMonthClick,
            icon = MedicalMateIcons.ChevronLeft,
            contentDescription = stringResource(R.string.calendar_previous_month),
        )
        Text(
            text = stringResource(R.string.calendar_month, month.year, month.monthValue),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        MedicalMateIconButton(
            onClick = onNextMonthClick,
            icon = MedicalMateIcons.ChevronRight,
            contentDescription = stringResource(R.string.calendar_next_month),
        )
    }
}

/**
 * 월 격자.
 *
 * 한 주가 일요일에서 시작한다. 첫 주의 빈 칸은 그 달 1일의 요일만큼 비운다.
 *
 * 날짜 칸은 [MedicalMateDateCell]이 그린다. 오늘, 고른 날, 일정 있는 날의 표시가 그
 * 컴포넌트에 있다.
 */
@Composable
private fun MonthGrid(state: CalendarUiState, onDayClick: (LocalDate) -> Unit) {
    val firstDayOffset = state.month.atDay(1).dayOfWeek.value % DAYS_IN_WEEK
    val lastDay = state.month.lengthOfMonth()

    MedicalMateCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels().forEach { label ->
                Text(
                    text = label,
                    style = MedicalMateTheme.typography.labelS,
                    color = MedicalMateTheme.colors.fgSubtle,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        var day = 1 - firstDayOffset
        while (day <= lastDay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(DAYS_IN_WEEK) { index ->
                    val current = day + index
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (current in 1..lastDay) {
                            val date = state.month.atDay(current)
                            MedicalMateDateCell(
                                day = current,
                                selected = date == state.selected,
                                isToday = date == state.today,
                                marker = state.markerOn(current),
                                onClick = { onDayClick(date) },
                            )
                        } else {
                            // 빈 칸도 자리를 차지해야 요일이 어긋나지 않는다.
                            Box(modifier = Modifier.size(MedicalMateDateCellSize))
                        }
                    }
                }
            }
            day += DAYS_IN_WEEK
        }
        MonthLegend()
    }
}

/**
 * 범례. 채운 점과 빈 원이 각각 무엇인지 적는다.
 *
 * 두 표시가 5px이라 모양 차이만으로는 처음 보는 사람이 알 수 없다. 시안이 격자와 같은
 * 판 안에 두었다.
 */
@Composable
private fun MonthLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MedicalMateSpace.s8),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(marker = MedicalMateDateMarker.RECORD, labelRes = R.string.calendar_legend_record)
        LegendItem(marker = MedicalMateDateMarker.PLANNED, labelRes = R.string.calendar_legend_planned)
    }
}

@Composable
private fun LegendItem(marker: MedicalMateDateMarker, @StringRes labelRes: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier
                .size(LegendDotSize)
                .then(
                    if (marker == MedicalMateDateMarker.PLANNED) {
                        Modifier.border(
                            width = LegendRingWidth,
                            color = MedicalMateTheme.colors.bgPrimary,
                            shape = MedicalMateRadius.full,
                        )
                    } else {
                        Modifier.background(
                            color = MedicalMateTheme.colors.bgPrimary,
                            shape = MedicalMateRadius.full,
                        )
                    },
                ),
        )
        Text(
            text = stringResource(labelRes),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 날짜 칸의 표시와 같은 크기여야 같은 것으로 읽힌다. */
private val LegendDotSize = 5.dp
private val LegendRingWidth = 1.dp

/** 고른 날과 그 날의 일정. 일정이 없으면 없다고 적는다. */
/**
 * 고른 날의 일정.
 *
 * 시안(`406:2310`)은 이 자리에 `List Row`를 쓰는데 그 컴포넌트는 제목과 메타 한 줄까지다.
 * 이 줄은 제목 옆에 D-day 배지가 붙고 메타가 따로 있어서 1j-1의 기록 줄과 같은 이유로
 * 카드로 짠다. 오른쪽 이동 표시는 `List Row`와 같게 둔다.
 */
@Composable
private fun ColumnScope.SelectedDay(state: CalendarUiState, onScheduleClick: (String) -> Unit) {
    Text(
        text = state.selected.format(dayFormat),
        style = MedicalMateTheme.typography.headingS,
        color = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.padding(top = MedicalMateSpace.s10),
    )
    // 시안 `1185:13797`이 일정 없는 날에 제목만 두고 아래를 비운다. 그 날 무엇을 할지는
    // 아래 + 버튼이 이미 말하고 있어서 "일정이 없어요"를 한 줄 더 적을 자리가 아니다.
    if (state.schedules.isEmpty()) return
    state.schedules.forEach { schedule ->
        MedicalMateCard(onClick = { onScheduleClick(schedule.id) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = schedule.title,
                            style = MedicalMateTheme.typography.bodyLStrong,
                            color = MedicalMateTheme.colors.fgDefault,
                        )
                        if (schedule.dday >= 0) {
                            MedicalMateBadge(
                                label = stringResource(R.string.calendar_day_dday, schedule.dday),
                                tone = MedicalMateBadgeTone.BRAND,
                            )
                        }
                    }
                    Text(
                        text =
                        listOfNotNull(
                            schedule.time ?: stringResource(R.string.calendar_time_unset),
                            schedule.detail.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MedicalMateTheme.typography.bodyS,
                        color = MedicalMateTheme.colors.fgSubtle,
                    )
                }
                Icon(
                    painter = painterResource(MedicalMateIcons.ChevronRight),
                    contentDescription = null,
                    tint = MedicalMateTheme.colors.fgMuted,
                )
            }
        }
    }
}

/**
 * 일요일부터의 요일 머리글.
 *
 * `DayOfWeek.getDisplayName`을 쓰지 않는다. 컴포저블 안에서 기기 로케일을 직접 읽으면
 * 언어가 바뀌어도 다시 그리지 않고, lint가 그것을 잡는다. 번역 대상이기도 해서 리소스에
 * 둔다.
 *
 * 날짜 선택 시트(1r-4-D)의 격자도 같은 머리글을 쓴다.
 */
@Composable
internal fun weekdayLabels(): List<String> = stringArrayResource(R.array.calendar_weekdays).toList()

private const val DAYS_IN_WEEK = 7

/** "9월 12일 (금)" 형식. 홈의 일정 줄과 같은 형식이다. */
private val dayFormat = DateTimeFormatter.ofPattern("M월 d일 (E)")

@MedicalMateScreenPreviews
@Composable
private fun CalendarMonthScreenPreview() {
    MedicalMateTheme {
        CalendarMonthScreen(
            state = previewCalendarState,
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDayClick = {},
            onScheduleClick = {},
            onCardOpenClick = {},
            onCardSheetDismiss = {},
            onAddClick = {},
            onTabSelect = {},
        )
    }
}
