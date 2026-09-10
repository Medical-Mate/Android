package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.designsystem.component.MedicalMateDateMarker
import java.time.LocalDate
import java.time.YearMonth

/**
 * 캘린더 월 화면의 상태. Figma 1r-1 `406:2310`.
 *
 * [recordDays]는 기록이 있는 날, [plannedDays]는 앞으로 일정이 있는 날이다. 날짜 칸이 각각
 * 채운 점과 빈 원을 찍는다. 지난 기록과 앞으로의 일정을 색만으로 가르면 구별되지 않아서
 * 두 집합으로 나눠 갖는다. 어떤 일정인지는 그 날을 눌러야 나오므로 여기서는 날짜만 갖는다.
 *
 * [selected]와 [today]를 나눠 갖는다. 오늘은 옅은 테두리, 고른 날은 채움이라 표시가 다르고,
 * 오늘이 아닌 날을 골라 볼 수 있어야 한다.
 *
 * [cardSheet]가 있으면 카드만 있는 날 시트(1r-1-S `1226:4669`)가 떠 있다. 일정이 있는
 * 날은 일자 화면으로 넘어가고, 카드만 쓴 날은 갈 화면이 없어서 그 자리에서 시트로 보여준다.
 */
data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    val recordDays: Set<Int> = emptySet(),
    val plannedDays: Set<Int> = emptySet(),
    val schedules: List<CalendarSchedule> = emptyList(),
    val cardSheet: DayCard? = null,
)

/**
 * 고른 날의 일정 한 줄.
 *
 * [dday]는 오늘로부터 남은 일수다. 화면이 열린 날 기준이라 저장해 두지 않고 계산해서
 * 넣는다. 지난 일정은 음수가 되고 화면이 표시를 감춘다.
 */
data class CalendarSchedule(val id: String, val title: String, val time: String, val detail: String, val dday: Long)

/**
 * 캘린더 일자 화면의 상태. Figma 1r-2 `406:2514`.
 *
 * 네 덩어리다. 이 날 일정, 가져갈 브리핑 카드, 진료 전 할 일, 이 날 기록. 각 덩어리의
 * 머리에 수정·열기·추가가 붙는다.
 *
 * [record]가 없으면 아직 진료 전이라는 빈 상태가 나온다.
 */
data class CalendarDayUiState(
    val date: LocalDate,
    val schedule: CalendarSchedule?,
    val card: DayCard?,
    val todos: List<DayTodo> = emptyList(),
    val record: String? = null,
)

/** 그 진료에 가져갈 카드. */
data class DayCard(val id: String, val title: String, val status: String, val meta: String)

/** 진료 전 할 일 한 줄. */
data class DayTodo(val id: String, val label: String, val done: Boolean)

/** 그 날에 찍을 표시. 기록이 예정보다 앞선다. 이미 지난 일은 사실이고 예정은 계획이다. */
internal fun CalendarUiState.markerOn(day: Int): MedicalMateDateMarker = when (day) {
    in recordDays -> MedicalMateDateMarker.RECORD
    in plannedDays -> MedicalMateDateMarker.PLANNED
    else -> MedicalMateDateMarker.NONE
}
