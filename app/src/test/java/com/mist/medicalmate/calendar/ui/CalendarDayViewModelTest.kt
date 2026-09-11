package com.mist.medicalmate.calendar.ui
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarDayViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loaded(): CalendarDayViewModel = dayViewModel().apply { load(VisitDate) }

    private fun CalendarDayViewModel.state(): CalendarDayUiState = uiState.value!!

    @Test
    fun `불러오기 전에는 상태가 없다`() {
        assertNull(dayViewModel().uiState.value)
    }

    @Test
    fun `처음에는 편집이 아니다`() {
        val state = loaded().state()

        assertFalse(state.editing)
        assertNull(state.todoDraft)
        assertEquals(3, state.shownTodos.size)
    }

    @Test
    fun `편집으로 들어가면 사본이 본값과 같다`() {
        val viewModel = loaded()

        viewModel.onEditStart()

        assertTrue(viewModel.state().editing)
        assertEquals(viewModel.state().todos, viewModel.state().todoDraft)
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `할 일을 지우면 바뀐 것이 있다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onTodoDelete("todo-2")

        assertTrue(viewModel.state().changed)
        assertEquals(2, viewModel.state().shownTodos.size)
        // 본값은 확인을 누르기 전까지 그대로다.
        assertEquals(3, viewModel.state().todos.size)
    }

    @Test
    fun `취소하면 지운 것이 돌아온다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onTodoDelete("todo-2")

        viewModel.onEditCancel()

        assertFalse(viewModel.state().editing)
        assertEquals(3, viewModel.state().shownTodos.size)
    }

    @Test
    fun `확인하면 지운 것이 본값이 된다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onTodoDelete("todo-2")

        viewModel.onEditDone()

        assertFalse(viewModel.state().editing)
        assertEquals(listOf("todo-1", "todo-3"), viewModel.state().todos.map { it.id })
    }

    @Test
    fun `체크는 바뀐 것으로 세지 않는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onTodoToggle("todo-2", true)

        // 체크는 편집 밖에서도 누를 수 있는 조작이라 편집으로 바꾼 것이 아니다.
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `편집이 아닐 때도 체크는 켜고 끈다`() {
        val viewModel = loaded()

        viewModel.onTodoToggle("todo-2", true)

        assertTrue(viewModel.state().todos.first { it.id == "todo-2" }.done)
    }

    @Test
    fun `일정 삭제는 확인을 묻는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onScheduleDeleteClick()
        assertTrue(viewModel.state().deleteRequested)

        viewModel.onScheduleDeleteDismiss()
        assertFalse(viewModel.state().deleteRequested)
    }

    @Test
    fun `취소하면 삭제 확인도 닫힌다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onScheduleDeleteClick()

        viewModel.onEditCancel()

        assertFalse(viewModel.state().deleteRequested)
        assertFalse(viewModel.state().editing)
    }

    @Test
    fun `다녀온 날은 할 일이 없어 편집해도 지울 줄이 없다`() {
        val viewModel = dayViewModel().apply { load(PastVisitDate) }

        viewModel.onEditStart()

        assertTrue(viewModel.state().editing)
        assertEquals(emptyList<DayTodo>(), viewModel.state().shownTodos)
    }

    @Test
    fun `그 날 일정을 서버에서 읽는다`() {
        val viewModel = dayViewModel()

        viewModel.load(LocalDate.of(2026, 9, 12))

        val schedule = requireNotNull(viewModel.uiState.value?.schedule)
        assertEquals("1", schedule.id)
        assertEquals("서울OO병원 내과 재진", schedule.title)
        assertEquals(1L, schedule.dday)
    }

    @Test
    fun `그 날 일정이 없으면 비운다`() {
        // 없는 일정을 남겨 두면 삭제를 눌렀을 때 지울 것이 없다.
        val viewModel = dayViewModel(emptyList())

        viewModel.load(LocalDate.of(2026, 9, 12))

        assertNull(viewModel.uiState.value?.schedule)
    }

    @Test
    fun `삭제를 확정하면 그 일정을 서버에서 지운다`() {
        val repository = FakeAppointmentRepository(listOf(testAppointment))
        val viewModel = CalendarDayViewModel(repository, fixedClock)
        viewModel.load(LocalDate.of(2026, 9, 12))
        var left = false

        viewModel.onScheduleDeleteConfirm { left = true }

        assertEquals(1L, repository.deletedId)
        assertTrue(left)
    }

    @Test
    fun `다음 일정에 재방문할 병원이 함께 온다`() {
        // 시간을 확정하러 갈 때 일정 추가의 병원 필드가 이 값으로 채워진다(1r-4-B).
        // 제목에서 떼어내지 않는다. "서울OO병원 내과 재방문"처럼 말이 붙어 있다.
        val viewModel = dayViewModel()

        viewModel.load(PastVisitDate)

        assertEquals("서울OO병원 내과", viewModel.uiState.value?.nextEvent?.clinic)
    }

    @Test
    fun `시간이 정해지면 확정 조작이 사라진다`() {
        // 1r-2-A는 시간이 비어 조작이 붙고, A2는 확정된 상태다.
        val viewModel = dayViewModel()

        viewModel.load(PastVisitDate)

        assertNull(viewModel.uiState.value?.nextEvent?.at)
    }
}

/** 그 날 일정을 하나 돌려주는 저장소. */
private fun dayViewModel(appointments: List<Appointment> = listOf(testAppointment)) =
    CalendarDayViewModel(FakeAppointmentRepository(appointments), fixedClock)

private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneId.of("Asia/Seoul"))

private val testAppointment =
    Appointment(
        id = 1,
        title = "서울OO병원 내과 재진",
        at = LocalDateTime.of(2026, 9, 12, 10, 30),
        status = AppointmentStatus.SCHEDULED,
        cardId = 1,
        cardTitle = "복부 통증 · 3주",
    )

private class FakeAppointmentRepository(private val appointments: List<Appointment>) : AppointmentRepository {
    var deletedId: Long? = null

    override suspend fun month(month: java.time.YearMonth) = ApiResult.Success(appointments)

    override suspend fun day(date: java.time.LocalDate) = ApiResult.Success(appointments)

    override suspend fun upcoming() = ApiResult.Success(appointments)

    override suspend fun create(
        clinicName: String?,
        department: String?,
        purpose: String?,
        at: LocalDateTime,
        cardId: Long?,
    ) = ApiResult.Success(appointments.first())

    override suspend fun update(id: Long, at: LocalDateTime?, purpose: String?, cardId: Long?, clearCard: Boolean) =
        ApiResult.Success(appointments.first())

    override suspend fun delete(id: Long): ApiResult<Unit> {
        deletedId = id
        return ApiResult.Success(Unit)
    }
}
