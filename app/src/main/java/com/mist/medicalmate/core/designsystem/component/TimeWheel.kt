package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs

/**
 * DESIGN.md의 `Time Wheel`(`3645:2393`, #316). 오전·오후 · 시 · 분 세 열이고 다섯 줄이 보인다. 1s-3 알림 시간.
 *
 * 높이 240 가운데에 `bg/primary-faint` 띠(48 · 반경 14)를 깔고, 고른 줄은 `Heading/S` `fg/default`, 바로 옆 줄은
 * `Body/L` `fg/muted`, 그 바깥 줄은 `Body/L` `fg/disabled`다. 줄 간격은 40이다(글자 26 + 사이 14).
 *
 * 멈춘 자리의 값을 고른 값으로 올린다. 굴리는 동안에는 올리지 않는다 — 지나가는 값마다 상태가 바뀌면 목록이 스스로
 * 되감긴다. 보이는 값을 눌러도 그 자리로 굴러가 고른다.
 *
 * 분 눈금은 [minuteStep]이다. 시안은 00 · 30만 그렸지만 일정 추가 휠과 같은 10분 단위로 연다(사용자 결정).
 *
 * @param meridiemLabels "오전" · "오후". 문구는 화면이 리소스에서 넘긴다.
 */
@Composable
fun MedicalMateTimeWheel(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    meridiemLabels: List<String>,
    modifier: Modifier = Modifier,
    minuteStep: Int = DEFAULT_MINUTE_STEP,
) {
    val afternoon = time.hour >= NOON
    val hours = (0 until NOON).map { if (it == 0) NOON else it }
    val minutes = (0 until MINUTES_IN_HOUR step minuteStep).toList()
    val minute = time.minute / minuteStep * minuteStep

    Box(modifier = modifier.fillMaxWidth().height(WheelHeight), contentAlignment = Alignment.Center) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(BandHeight)
                .background(MedicalMateTheme.colors.bgPrimaryFaint, MedicalMateRadius.buttonM),
        )
        Row(modifier = Modifier.fillMaxWidth().height(VisibleHeight)) {
            WheelColumn(
                labels = meridiemLabels,
                selected = if (afternoon) 1 else 0,
                onSelect = { onTimeChange(time.withHour(time.hour % NOON + if (it == 1) NOON else 0)) },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                labels = hours.map { it.toString() },
                selected = hours.indexOf(if (time.hour % NOON == 0) NOON else time.hour % NOON),
                onSelect = { onTimeChange(time.withHour(hours[it] % NOON + if (afternoon) NOON else 0)) },
                modifier = Modifier.weight(1f),
            )
            WheelColumn(
                labels = minutes.map { "%02d".format(it) },
                selected = minutes.indexOf(minute).coerceAtLeast(0),
                onSelect = { onTimeChange(time.withMinute(minutes[it])) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 한 열. 위아래로 두 줄씩 비워 첫 값과 끝 값도 가운데 띠에 올 수 있다. */
@Composable
private fun WheelColumn(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val scope = rememberCoroutineScope()
    ReportSettledItem(listState = listState, onSettle = { if (it != selected) onSelect(it) })

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = RowHeight * 2),
        modifier = modifier,
    ) {
        items(count = labels.size) { index ->
            WheelItem(
                label = labels[index],
                distance = abs(index - selected),
                onClick = { scope.launch { listState.animateScrollToItem(index) } },
            )
        }
    }
}

/** 고른 줄과의 거리로 글자를 고른다. 0은 고른 줄, 1은 바로 옆, 그 밖은 흐린 줄이다. */
@Composable
private fun WheelItem(label: String, distance: Int, onClick: () -> Unit) {
    val colors = MedicalMateTheme.colors
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (distance == 0) MedicalMateTheme.typography.headingS else MedicalMateTheme.typography.bodyL,
            color =
            when (distance) {
                0 -> colors.fgDefault
                1 -> colors.fgMuted
                else -> colors.fgDisabled
            },
        )
    }
}

/** 스크롤이 멈춘 뒤 가운데 항목을 한 번만 올린다. 위에 두 줄을 비워 두어 첫 보이는 항목이 곧 가운데다. */
@Composable
private fun ReportSettledItem(listState: LazyListState, onSettle: (Int) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) -> if (!scrolling) onSettle(index) }
    }
}

/** 마스터 높이 240. 보이는 다섯 줄(200)이 그 가운데에 선다. */
private val WheelHeight = 240.dp

private val RowHeight = 40.dp

private val VisibleHeight = RowHeight * 5

private val BandHeight = 48.dp

private const val NOON = 12

private const val MINUTES_IN_HOUR = 60

private const val DEFAULT_MINUTE_STEP = 10
