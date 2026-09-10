package com.mist.medicalmate.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateCell
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateCellSizeCompact
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * 일정 추가 화면에 뜨는 두 시트. Figma 1r-4-D `1063:3131`, 1r-4-T `1063:3372`.
 *
 * 고른 값은 시트 안에 둔다. 두 시트 모두 아래에 `확인`이 있어서 닫기 전까지는 정해진 값이
 * 아니다. 화면 상태로 올리면 끌어내려 닫은 것과 확인한 것이 구별되지 않는다.
 */
@Composable
internal fun ScheduleAddSheets(state: ScheduleAddUiState, callbacks: ScheduleAddCallbacks) {
    when (state.sheet) {
        ScheduleAddSheet.NONE -> Unit
        ScheduleAddSheet.DATE ->
            DateSheet(
                initial = state.date,
                onConfirm = callbacks.onDateConfirm,
                onDismissRequest = callbacks.onSheetDismiss,
            )

        ScheduleAddSheet.TIME ->
            TimeSheet(
                initial = state.time,
                onConfirm = callbacks.onTimeConfirm,
                onDismissRequest = callbacks.onSheetDismiss,
            )
    }
}

/**
 * 날짜 선택 시트.
 *
 * 격자는 월 화면과 같은 [MedicalMateDateCell]인데 크기가 34다. 시트에 제목과 버튼까지
 * 들어가서 42로는 한 달이 다 보이지 않는다.
 *
 * 기록·예정 점은 찍지 않는다. 여기서는 앞으로의 날을 고르는 것이고, 지난 기록이 있는지는
 * 이 판단에 쓰이지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSheet(initial: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismissRequest: () -> Unit) {
    val today = remember { LocalDate.now() }
    var picked by remember { mutableStateOf(initial ?: today) }
    var month by remember { mutableStateOf(YearMonth.from(picked)) }

    MedicalMateBottomSheet(onDismissRequest = onDismissRequest) {
        SheetTitle(stringResource(R.string.schedule_add_date_sheet))
        MonthHead(
            month = month,
            onPreviousClick = { month = month.minusMonths(1) },
            onNextClick = { month = month.plusMonths(1) },
        )
        DateGrid(
            month = month,
            picked = picked,
            today = today,
            onDayClick = { picked = it },
        )
        MedicalMateButton(
            label = stringResource(R.string.schedule_add_sheet_confirm),
            onClick = { onConfirm(picked) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 월 이동. 월 화면과 같은 짜임인데 화살표가 S다. 시트 안이라 32 줄에 들어간다. */
@Composable
private fun MonthHead(month: YearMonth, onPreviousClick: () -> Unit, onNextClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MedicalMateIconButton(
            onClick = onPreviousClick,
            icon = MedicalMateIcons.ChevronLeft,
            contentDescription = stringResource(R.string.calendar_previous_month),
            size = MedicalMateIconButtonSize.S,
        )
        Text(
            text = stringResource(R.string.calendar_month, month.year, month.monthValue),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        MedicalMateIconButton(
            onClick = onNextClick,
            icon = MedicalMateIcons.ChevronRight,
            contentDescription = stringResource(R.string.calendar_next_month),
            size = MedicalMateIconButtonSize.S,
        )
    }
}

/** 한 주가 일요일에서 시작한다. 첫 주의 빈 칸은 그 달 1일의 요일만큼 비운다. */
@Composable
private fun DateGrid(month: YearMonth, picked: LocalDate, today: LocalDate, onDayClick: (LocalDate) -> Unit) {
    val firstDayOffset = month.atDay(1).dayOfWeek.value % SHEET_DAYS_IN_WEEK
    val lastDay = month.lengthOfMonth()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels().forEach { label ->
                Text(
                    text = label,
                    style = MedicalMateTheme.typography.labelS,
                    color = MedicalMateTheme.colors.fgSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        var day = 1 - firstDayOffset
        while (day <= lastDay) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                repeat(SHEET_DAYS_IN_WEEK) { index ->
                    val current = day + index
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (current in 1..lastDay) {
                            val date = month.atDay(current)
                            MedicalMateDateCell(
                                day = current,
                                selected = date == picked,
                                isToday = date == today,
                                onClick = { onDayClick(date) },
                                size = MedicalMateDateCellSizeCompact,
                            )
                        } else {
                            // 빈 칸도 자리를 차지해야 요일이 어긋나지 않는다.
                            Box(modifier = Modifier.size(MedicalMateDateCellSizeCompact))
                        }
                    }
                }
            }
            day += SHEET_DAYS_IN_WEEK
        }
    }
}

/**
 * 시간 선택 시트.
 *
 * 분은 00과 30만 둔다. 시안이 그 둘만 그렸고, 진료 예약이 분 단위로 잡히지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSheet(initial: LocalTime?, onConfirm: (LocalTime) -> Unit, onDismissRequest: () -> Unit) {
    val start = initial ?: DefaultTime
    var afternoon by remember { mutableStateOf(start.hour >= NOON) }
    var hour by remember { mutableStateOf(start.hour % NOON) }
    var minute by remember { mutableStateOf(if (start.minute < HALF_HOUR) 0 else HALF_HOUR) }

    MedicalMateBottomSheet(onDismissRequest = onDismissRequest) {
        SheetTitle(stringResource(R.string.schedule_add_time_sheet))
        TimeWheel(
            afternoon = afternoon,
            hour = hour,
            minute = minute,
            onAfternoonChange = { afternoon = it },
            onHourChange = { hour = it },
            onMinuteChange = { minute = it },
        )
        MedicalMateButton(
            label = stringResource(R.string.schedule_add_sheet_confirm),
            onClick = { onConfirm(LocalTime.of(hour % NOON + if (afternoon) NOON else 0, minute)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 세 열의 휠.
 *
 * 가운데 밴드가 고른 값을 표시한다. 밴드를 열 뒤에 깔아서 세 열이 같은 줄에 맞는다.
 * 12시는 0으로 담고 표시할 때만 12로 바꾼다. `LocalTime`의 시가 0~23이라 그쪽에 맞춘다.
 */
@Composable
private fun TimeWheel(
    afternoon: Boolean,
    hour: Int,
    minute: Int,
    onAfternoonChange: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    val meridiem = listOf(stringResource(R.string.schedule_add_time_am), stringResource(R.string.schedule_add_time_pm))
    val hours = (0 until NOON).map { if (it == 0) NOON else it }
    val minutes = listOf(0, HALF_HOUR)

    Box(modifier = Modifier.fillMaxWidth().height(WheelHeight), contentAlignment = Alignment.Center) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(BandHeight)
                .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.sm),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            WheelColumn(
                labels = meridiem,
                selected = if (afternoon) 1 else 0,
                onSelect = { onAfternoonChange(it == 1) },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                labels = hours.map { it.toString() },
                selected = hours.indexOf(if (hour == 0) NOON else hour).coerceAtLeast(0),
                onSelect = { onHourChange(hours[it] % NOON) },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                labels = minutes.map { "%02d".format(it) },
                selected = minutes.indexOf(minute).coerceAtLeast(0),
                onSelect = { onMinuteChange(minutes[it]) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 휠 한 열.
 *
 * 위아래로 한 칸씩 여백을 둬서 첫 항목과 마지막 항목도 가운데 밴드에 올 수 있다.
 * 멈춘 자리의 항목을 고른 값으로 올린다. 스크롤 중에는 올리지 않는다. 지나가는 값마다
 * 상태가 바뀌면 목록이 스스로 되감긴다.
 */
@Composable
private fun WheelColumn(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    ReportSettledItem(listState = listState, onSettle = onSelect)

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = WheelItemHeight),
        modifier = modifier,
    ) {
        items(items = labels) { label ->
            val index = labels.indexOf(label)
            Box(
                modifier = Modifier.fillMaxWidth().height(WheelItemHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style =
                    if (index == selected) {
                        MedicalMateTheme.typography.bodyLStrong
                    } else {
                        MedicalMateTheme.typography.bodyL
                    },
                    color =
                    if (index == selected) MedicalMateTheme.colors.fgDefault else MedicalMateTheme.colors.fgSubtle,
                )
            }
        }
    }
}

/** 스크롤이 멈춘 뒤 가운데 항목을 한 번만 올린다. */
@Composable
private fun ReportSettledItem(listState: LazyListState, onSettle: (Int) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) -> if (!scrolling) onSettle(index) }
    }
}

/** 시트 제목. 두 시트가 같은 자리에 같은 크기로 둔다. */
@Composable
private fun ColumnScope.SheetTitle(text: String) {
    Text(
        text = text,
        style = MedicalMateTheme.typography.bodyLStrong,
        color = MedicalMateTheme.colors.fgDefault,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 시안의 휠 156과 항목 52. 세 칸이 보인다. */
private val WheelHeight = 156.dp

private val WheelItemHeight = 52.dp

/** 가운데 밴드 48. 항목 52보다 낮아서 위아래 항목이 밴드 밖으로 나온다. */
private val BandHeight = 48.dp

private const val SHEET_DAYS_IN_WEEK = 7

private const val NOON = 12

private const val HALF_HOUR = 30

private val DefaultTime: LocalTime = LocalTime.of(10, 0)
