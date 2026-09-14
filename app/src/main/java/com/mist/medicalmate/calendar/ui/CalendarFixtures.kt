package com.mist.medicalmate.calendar.ui

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 캘린더 픽스처. 서버 연동 시 이 파일을 삭제한다.
 *
 * 월 화면과 일자 화면이 함께 쓴다. `CalendarViewModel`의 companion에 있던 것을 옮겼다.
 * 일자 화면이 편집 상태를 들면서 상태 보유자가 둘로 갈렸고, 두 곳이 같은 날짜와 같은
 * 카드를 봐야 한다.
 */
/** Figma가 그린 진료 예정일. */
internal val VisitDate: LocalDate = LocalDate.of(2026, 9, 12)

/** Figma가 그린 카드 작성일. 1r-1-S가 이 날의 시트다. */
internal val CardDate: LocalDate = LocalDate.of(2026, 9, 4)

/**
 * 이미 다녀온 진료일. 1r-2-A가 이 날의 화면이다.
 *
 * 시안에 날짜가 없어 9월 2일로 잡았다. 다녀온 날의 화면은 진료일이 지나야 나오는데
 * 픽스처의 진료일(9월 12일)이 앞으로의 날이라 그 상태를 열어 볼 길이 없었다.
 */
internal val PastVisitDate: LocalDate = LocalDate.of(2026, 9, 2)

/** 기록이 있는 날. 2일에 다녀왔고 4일에 브리핑 카드를 썼다. */
internal val RecordDays = setOf(2, 4)

/** 앞으로 일정이 있는 날. 12일 진료, 26일 재방문이다. */
internal val PlannedDays = setOf(12, 26)

internal val fixtureCard =
    DayCard(
        id = "card-1",
        title = "복부 통증 · 3주",
        meta = "2026.09.04 작성 · 5항목",
        status = "진료 전",
    )

/** 다녀온 뒤의 같은 카드. 가져갈 일이 끝나 배지가 없고 언제 보여줬는지가 온다. */
internal val fixtureVisitedCard =
    DayCard(
        id = "card-1",
        title = "복부 통증 · 3주",
        meta = "브리핑 카드 · 진료실에서 보여줌",
    )

internal val fixtureRecord =
    DayRecord(
        id = "card-1",
        title = "진료 후 기록",
        meta = "위염 초기 · 2주 약 · 09.26 재방문",
    )

/** 시간이 아직 정해지지 않은 재방문. 확정하면 칩이 D-day로 바뀐다(1r-2-A2). */
internal val fixtureNextEvent =
    DayNextEvent(
        chip = "9월 26일 (토)",
        title = "재방문 예정",
        on = LocalDate.of(2026, 9, 26),
        clinic = "서울OO병원 내과",
    )

/** Preview에서만 쓰는 진료 전 할 일. 서버에 붙은 뒤로 화면은 일정의 것을 그린다(#187). */
internal val fixtureTodos =
    listOf(
        DayTodo(id = "todo-1", label = "달라진 증상 있으면 카드 수정", done = true),
        DayTodo(id = "todo-2", label = "복용 중인 약 챙기기", done = false),
        DayTodo(id = "todo-3", label = "지난 검사 결과 사진 준비", done = false),
    )

internal fun initialState(): CalendarUiState {
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
internal fun cardOn(date: LocalDate): DayCard? = if (date == CardDate || date == VisitDate) fixtureCard else null

internal fun schedulesOn(date: LocalDate, today: LocalDate): List<CalendarSchedule> = when (date) {
    VisitDate ->
        listOf(
            CalendarSchedule(
                id = "visit-1",
                title = "서울OO병원 내과 재진",
                time = "오전 10:30",
                detail = "복부 통증 브리핑 카드를 가져가요",
                dday = ChronoUnit.DAYS.between(today, date),
            ),
        )

    PastVisitDate ->
        listOf(
            CalendarSchedule(
                id = "visit-0",
                title = "서울OO병원 내과 초진",
                time = "오전 9:30",
                detail = "복부 통증 브리핑 카드를 가져갔어요",
                dday = ChronoUnit.DAYS.between(today, date),
            ),
        )

    else -> emptyList()
}

/**
 * 고른 날의 일자 화면 상태.
 *
 * 진료를 다녀왔는지로 화면이 갈린다(1r-2-A). 지난 진료일은 할 일이 없어지고 기록과 다음
 * 일정이 붙는다. 서버가 붙으면 그 날의 기록 유무가 이 판단을 대신한다.
 */
internal fun dayState(date: LocalDate): CalendarDayUiState {
    val today = LocalDate.now()
    return when (date) {
        PastVisitDate ->
            CalendarDayUiState(
                date = date,
                schedule = schedulesOn(date, today).firstOrNull(),
                card = fixtureVisitedCard,
                records = listOf(fixtureRecord),
                nextEvent = fixtureNextEvent,
            )

        VisitDate ->
            CalendarDayUiState(
                date = date,
                schedule = schedulesOn(date, today).firstOrNull(),
                card = fixtureCard,
                todos = fixtureTodos,
            )

        else -> CalendarDayUiState(date = date, schedule = null, card = null)
    }
}

/** Preview용 일자 상태. */
internal val previewCalendarDayState = dayState(VisitDate)

/** Preview용 다녀온 날. 1r-2-A처럼 시간이 아직 정해지지 않은 재방문이 붙는다. */
internal val previewCalendarDayVisitedState = dayState(PastVisitDate)

/** Preview용 다음 일정 확정. 1r-2-A2다. 칩이 남은 날수로 바뀌고 날짜와 시간이 온다. */
internal val previewCalendarDayConfirmedState =
    previewCalendarDayVisitedState.copy(
        nextEvent =
        DayNextEvent(
            chip = "D-14",
            title = "서울OO병원 내과 재방문",
            on = LocalDate.of(2026, 9, 26),
            at = "9월 26일 (토) 오전 10:30",
        ),
    )

/** Preview용 편집 상태. 1r-2-E다. 할 일 줄에 x가 붙고 하단에 일정 삭제가 선다. */
internal val previewCalendarDayEditingState =
    previewCalendarDayState.copy(todoDraft = previewCalendarDayState.todos)

/** Preview용 월 상태. */
internal val previewCalendarState =
    CalendarUiState(
        month = java.time.YearMonth.of(2026, 9),
        today = LocalDate.of(2026, 9, 7),
        selected = VisitDate,
        recordDays = RecordDays,
        plannedDays = PlannedDays,
        schedules = schedulesOn(VisitDate, LocalDate.of(2026, 9, 7)),
    )
