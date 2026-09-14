package com.mist.medicalmate.calendar.ui
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentCard
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.calendar.data.NewAppointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.ui.FakeCardRepository
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.VisitFollowUp
import com.mist.medicalmate.visit.data.VisitListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 오늘이 있는 달을 열고 오늘을 고른다`() {
        // 서버가 어느 달을 보여줄지 정하지 않는다. 화면이 열린 날이 기준이다.
        val state = monthViewModel().uiState.value

        assertEquals(java.time.YearMonth.of(2026, 9), state.month)
        assertEquals(LocalDate.of(2026, 9, 11), state.selected)
    }

    @Test
    fun `카드를 쓴 날에 채운 점이 찍힌다`() {
        // 채운 점은 기록, 빈 원은 앞으로의 일정이다. 서버가 그 둘을 다른 경로로 준다.
        val state = monthViewModel().uiState.value

        assertEquals(setOf(4), state.recordDays)
        assertEquals(setOf(12), state.plannedDays)
    }

    @Test
    fun `확정하지 않은 재방문도 예정으로 찍힌다`() {
        // 뽑아 두기만 한 날은 일정에 없다. 달력에 안 보이면 확정하러 갈 길이 없다(#186).
        val revisit =
            VisitListItem(
                id = "9",
                cardId = 1,
                cardTitle = "갈비뼈",
                clinic = null,
                visitedOn = LocalDate.of(2026, 9, 5),
                followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 26)),
            )

        val state = monthViewModel(visits = listOf(revisit)).uiState.value

        assertEquals(setOf(12, 26), state.plannedDays)
    }

    @Test
    fun `지난 재방문은 찍지 않는다`() {
        val past =
            VisitListItem(
                id = "9",
                cardId = 1,
                cardTitle = "갈비뼈",
                clinic = null,
                visitedOn = LocalDate.of(2026, 9, 1),
                followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 2)),
            )

        assertEquals(setOf(12), monthViewModel(visits = listOf(past)).uiState.value.plannedDays)
    }

    @Test
    fun `달을 넘기면 고른 날은 그대로 둔다`() {
        val viewModel = monthViewModel()
        val selected = viewModel.uiState.value.selected

        viewModel.onNextMonth()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.month)
        assertEquals(selected, viewModel.uiState.value.selected)

        viewModel.onPreviousMonth()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.month)
    }

    @Test
    fun `일정 없는 날을 고르면 목록이 빈다`() {
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertEquals(LocalDate.of(2026, 9, 13), viewModel.uiState.value.selected)
        assertEquals(emptyList<CalendarSchedule>(), viewModel.uiState.value.schedules)
    }

    @Test
    fun `카드만 쓴 날을 고르면 시트가 뜬다`() {
        // 일정이 없고 카드만 쓴 날은 갈 화면이 없다.
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        assertEquals("1", viewModel.uiState.value.cardSheet?.id)
    }

    @Test
    fun `일정이 있는 날은 시트를 띄우지 않는다`() {
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 12))

        assertTrue(viewModel.uiState.value.schedules.isNotEmpty())
        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `카드도 일정도 없는 날은 시트를 띄우지 않는다`() {
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `시트를 닫으면 고른 날은 그대로 둔다`() {
        val viewModel = monthViewModel()
        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        viewModel.onCardSheetDismiss()

        assertNull(viewModel.uiState.value.cardSheet)
        assertEquals(LocalDate.of(2026, 9, 4), viewModel.uiState.value.selected)
    }

    @Test
    fun `다른 날로 옮기면 앞서 뜬 시트가 닫힌다`() {
        val viewModel = monthViewModel()
        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        viewModel.onDaySelect(LocalDate.of(2026, 9, 13))

        assertNull(viewModel.uiState.value.cardSheet)
    }

    @Test
    fun `다녀온 날은 할 일이 없고 기록과 다음 일정이 붙는다`() {
        // 진료일이 지난 시점을 만들 수 없어(오늘은 기기가 준다) 픽스처 상태로 확인한다.
        val day = previewCalendarDayVisitedState

        assertTrue(day.visited)
        assertEquals(emptyList<DayTodo>(), day.todos)
        assertEquals("진료 후 기록", day.records.single().title)
        assertEquals("9월 26일 (토)", day.nextEvent?.chip)
        assertNull(day.nextEvent?.at)
    }

    @Test
    fun `카드 줄의 배지는 진료를 다녀왔는지로 갈린다`() {
        // 시안 `1r-2`가 카드 줄에 늘 배지를 두고, 다녀온 카드만 "진료 완료"로 바꾼다.
        // 확정 여부가 아니라 `visited`다 — 확정만 하고 안 간 카드에 완료가 붙으면 안 된다.
        assertTrue(previewCalendarDayVisitedState.card?.visited == true)
        assertTrue(previewCalendarDayState.card?.visited == false)
    }

    @Test
    fun `다음 일정이 확정되면 칩과 시간이 바뀐다`() {
        val next = previewCalendarDayConfirmedState.nextEvent

        assertEquals("D-14", next?.chip)
        assertEquals("9월 26일 (토) 오전 10:30", next?.at)
    }

    @Test
    fun `진료 예정일의 일자 화면에는 일정과 카드와 할 일이 있다`() {
        val day = dayState(LocalDate.of(2026, 9, 12))

        assertNotNull(day.schedule)
        assertNotNull(day.card)
        assertEquals(3, day.todos.size)
        assertEquals(emptyList<DayRecord>(), day.records)
    }

    @Test
    fun `다른 날의 일자 화면은 비어 있다`() {
        val day = dayState(LocalDate.of(2026, 9, 13))

        assertNull(day.schedule)
        assertNull(day.card)
        assertEquals(emptyList<DayTodo>(), day.todos)
    }

    @Test
    fun `카드로 이미 일정을 만들었으면 시트가 그 날을 들고 있다`() {
        // 9월 4일 카드로 9월 12일 일정을 만든 상태다. 4일을 누르면 또 만들라고 하던 자리다.
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        assertEquals(LocalDate.of(2026, 9, 12), viewModel.uiState.value.cardSheet?.scheduledOn)
    }

    @Test
    fun `일정이 걸리지 않은 카드의 시트에는 날이 없다`() {
        val viewModel = monthViewModel(appointments = emptyList())

        viewModel.onDaySelect(LocalDate.of(2026, 9, 4))

        assertNull(viewModel.uiState.value.cardSheet?.scheduledOn)
    }

    @Test
    fun `D-day는 오늘을 기준으로 센다`() {
        val viewModel = monthViewModel()

        viewModel.onDaySelect(LocalDate.of(2026, 9, 12))

        assertEquals(1L, viewModel.uiState.value.schedules.single().dday)
    }
}

/** 9월 12일 하나를 돌려주는 저장소로 세운다. 오늘은 9월 11일이다. */
private fun monthViewModel(
    appointments: List<Appointment> = listOf(monthAppointment),
    visits: List<VisitListItem> = emptyList(),
) = CalendarViewModel(
    repository = FakeMonthRepository(appointments),
    cardRepository = FakeCardRepository(list = ApiResult.Success(monthCards)),
    visitRepository = FakeVisitRepository(list = ApiResult.Success(visits)),
    clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneId.of("Asia/Seoul")),
).apply { load() }

/** 9월 4일에 쓴 카드 한 장. 그 날 일정은 없다. */
private val monthCards =
    listOf(
        CardListItem(
            id = "1",
            title = "복부 통증 · 3주",
            confirmed = true,
            visited = false,
            clinic = "서울OO병원 내과",
            writtenOn = LocalDate.of(2026, 9, 4),
        ),
    )

private val monthAppointment =
    Appointment(
        id = 1,
        title = "서울OO병원 내과 재진",
        on = LocalDate.of(2026, 9, 12),
        time = LocalTime.of(10, 30),
        status = AppointmentStatus.SCHEDULED,
        cards = listOf(AppointmentCard(id = 1, title = "복부 통증 · 3주")),
    )

private class FakeMonthRepository(private val appointments: List<Appointment>) :
    com.mist.medicalmate.calendar.data.AppointmentRepository {
    override suspend fun month(month: java.time.YearMonth) =
        com.mist.medicalmate.core.network.ApiResult.Success(appointments)

    override suspend fun day(date: java.time.LocalDate) =
        com.mist.medicalmate.core.network.ApiResult.Success(appointments)

    override suspend fun upcoming() = com.mist.medicalmate.core.network.ApiResult.Success(appointments)

    override suspend fun create(appointment: NewAppointment) =
        com.mist.medicalmate.core.network.ApiResult.Success(appointments.first())

    override suspend fun update(id: Long, edit: AppointmentEdit) =
        com.mist.medicalmate.core.network.ApiResult.Success(appointments.first())

    override suspend fun delete(id: Long) = com.mist.medicalmate.core.network.ApiResult.Success(Unit)
}
