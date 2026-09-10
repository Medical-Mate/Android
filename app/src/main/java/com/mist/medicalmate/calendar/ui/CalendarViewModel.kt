package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 캘린더 상태 보유자.
 *
 * 일정이 픽스처다. Figma 1r-1의 9월 내용을 그대로 옮겼다. `GET /api/visits`가 붙으면
 * [load]가 그 달의 일정을 받는다.
 *
 * D-day를 여기서 계산한다. 화면이 열린 날 기준이어야 하고, 미리 만들어 두면 자정을 넘긴
 * 뒤에도 옛 값이 남는다.
 */
@HiltViewModel
class CalendarViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState())
    val uiState: StateFlow<CalendarUiState> = mutableUiState.asStateFlow()

    fun onPreviousMonth() {
        mutableUiState.update { it.copy(month = it.month.minusMonths(1)) }
    }

    fun onNextMonth() {
        mutableUiState.update { it.copy(month = it.month.plusMonths(1)) }
    }

    /**
     * 날을 골랐다. 그 날의 일정만 남긴다.
     *
     * 일정이 없고 카드만 쓴 날이면 시트를 함께 띄운다. 그런 날은 일자 화면으로 넘어갈
     * 것이 없는데, 쓴 카드가 있다는 것은 알려야 한다.
     */
    fun onDaySelect(date: LocalDate) {
        mutableUiState.update { state ->
            val schedules = schedulesOn(date, state.today)
            state.copy(
                selected = date,
                schedules = schedules,
                cardSheet = if (schedules.isEmpty()) cardOn(date) else null,
            )
        }
    }

    /** 시트를 닫았다. */
    fun onCardSheetDismiss() {
        mutableUiState.update { it.copy(cardSheet = null) }
    }

    /** 고른 날의 일자 화면 상태. 목적지가 열릴 때 화면이 받는다. */
    fun dayState(date: LocalDate): CalendarDayUiState {
        val today = mutableUiState.value.today
        return CalendarDayUiState(
            date = date,
            schedule = schedulesOn(date, today).firstOrNull(),
            card = if (date == VisitDate) fixtureCard else null,
            todos = if (date == VisitDate) fixtureTodos else emptyList(),
        )
    }

    private companion object {
        /** Figma가 그린 진료 예정일. */
        val VisitDate: LocalDate = LocalDate.of(2026, 9, 12)

        /** Figma가 그린 카드 작성일. 1r-1-S가 이 날의 시트다. */
        val CardDate: LocalDate = LocalDate.of(2026, 9, 4)

        /** 기록이 있는 날. 9월 4일에 브리핑 카드를 썼다. */
        val RecordDays = setOf(4)

        /** 앞으로 일정이 있는 날. 12일 진료, 26일 재방문이다. */
        val PlannedDays = setOf(12, 26)

        val fixtureCard =
            DayCard(
                id = "card-1",
                title = "복부 통증 · 3주",
                status = "진료 전",
                meta = "2026.09.04 작성 · 5항목",
            )

        val fixtureTodos =
            listOf(
                DayTodo(id = "todo-1", label = "달라진 증상 있으면 카드 수정", done = true),
                DayTodo(id = "todo-2", label = "복용 중인 약 챙기기", done = false),
                DayTodo(id = "todo-3", label = "지난 검사 결과 사진 준비", done = false),
            )

        fun initialState(): CalendarUiState {
            val today = LocalDate.now()
            return CalendarUiState(
                month = YearMonth.from(VisitDate),
                today = today,
                selected = VisitDate,
                recordDays = RecordDays,
                plannedDays = PlannedDays,
                schedules = schedulesOn(VisitDate, today),
            )
        }

        /** 그 날에 걸린 카드. 쓴 날과 가져갈 날 양쪽에서 같은 카드가 나온다. */
        fun cardOn(date: LocalDate): DayCard? = if (date == CardDate || date == VisitDate) fixtureCard else null

        fun schedulesOn(date: LocalDate, today: LocalDate): List<CalendarSchedule> = if (date != VisitDate) {
            emptyList()
        } else {
            listOf(
                CalendarSchedule(
                    id = "visit-1",
                    title = "서울OO병원 내과 재진",
                    time = "오전 10:30",
                    detail = "복부 통증 브리핑 카드",
                    dday = ChronoUnit.DAYS.between(today, date),
                ),
            )
        }
    }
}

/** Preview용 월 상태. */
internal val previewCalendarState =
    CalendarUiState(
        month = YearMonth.of(2026, 9),
        today = LocalDate.of(2026, 9, 7),
        selected = LocalDate.of(2026, 9, 12),
        recordDays = setOf(4),
        plannedDays = setOf(12, 26),
        schedules =
        listOf(
            CalendarSchedule(
                id = "visit-1",
                title = "서울OO병원 내과 재진",
                time = "오전 10:30",
                detail = "복부 통증 브리핑 카드",
                dday = 5,
            ),
        ),
    )

/** Preview용 일자 상태. */
internal val previewCalendarDayState =
    CalendarDayUiState(
        date = LocalDate.of(2026, 9, 12),
        schedule = previewCalendarState.schedules.first(),
        card =
        DayCard(
            id = "card-1",
            title = "복부 통증 · 3주",
            status = "진료 전",
            meta = "2026.09.04 작성 · 5항목",
        ),
        todos =
        listOf(
            DayTodo(id = "todo-1", label = "달라진 증상 있으면 카드 수정", done = true),
            DayTodo(id = "todo-2", label = "복용 중인 약 챙기기", done = false),
            DayTodo(id = "todo-3", label = "지난 검사 결과 사진 준비", done = false),
        ),
    )
