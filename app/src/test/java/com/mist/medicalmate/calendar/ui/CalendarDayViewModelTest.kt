package com.mist.medicalmate.calendar.ui
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentCard
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.calendar.data.AppointmentTodo
import com.mist.medicalmate.calendar.data.NewAppointment
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    @Test
    fun `누른 일정을 연다`() {
        // 하루에 둘 이상일 수 있다. 날짜만으로 열면 첫 건이 나와서 누른 것과 달랐다(#179).
        val second = testAppointment.copy(id = 7, title = "킴벨피부과병원")
        val repository = FakeAppointmentRepository(listOf(testAppointment, second))

        val viewModel = dayViewModel(repository = repository).apply { load(VisitDate, appointmentId = 7) }

        assertEquals("킴벨피부과병원", viewModel.uiState.value?.schedule?.title)
    }

    @Test
    fun `누른 일정이 그 날에 없으면 첫 일정을 연다`() {
        // 지운 일정으로 돌아오는 경우다. 빈 화면보다 그 날 첫 일정이 낫다.
        val repository = FakeAppointmentRepository(listOf(testAppointment))

        val viewModel = dayViewModel(repository = repository).apply { load(VisitDate, appointmentId = 999) }

        assertEquals(testAppointment.title, viewModel.uiState.value?.schedule?.title)
    }

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

        viewModel.todo.onEditStart()

        assertTrue(viewModel.state().editing)
        assertEquals(viewModel.state().todos, viewModel.state().todoDraft)
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `할 일을 지우면 바뀐 것이 있다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()

        viewModel.todo.onDelete("todo-2")

        assertTrue(viewModel.state().changed)
        assertEquals(2, viewModel.state().shownTodos.size)
        // 본값은 확인을 누르기 전까지 그대로다.
        assertEquals(3, viewModel.state().todos.size)
    }

    @Test
    fun `취소하면 지운 것이 돌아온다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()
        viewModel.todo.onDelete("todo-2")

        viewModel.todo.onEditCancel()

        assertFalse(viewModel.state().editing)
        assertEquals(3, viewModel.state().shownTodos.size)
    }

    @Test
    fun `확인하면 지운 것이 본값이 된다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()
        viewModel.todo.onDelete("todo-2")

        viewModel.todo.onEditDone()

        assertFalse(viewModel.state().editing)
        assertEquals(listOf("todo-1", "todo-3"), viewModel.state().todos.map { it.id })
    }

    @Test
    fun `체크는 바뀐 것으로 세지 않는다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()

        viewModel.todo.onToggle("todo-2", true)

        // 체크는 편집 밖에서도 누를 수 있는 조작이라 편집으로 바꾼 것이 아니다.
        assertFalse(viewModel.state().changed)
    }

    @Test
    fun `편집이 아닐 때도 체크는 켜고 끈다`() {
        val viewModel = loaded()

        viewModel.todo.onToggle("todo-2", true)

        assertTrue(viewModel.state().todos.first { it.id == "todo-2" }.done)
    }

    @Test
    fun `할 일은 그 날 일정에서 온다`() {
        // 픽스처가 아니다(#187). 서버가 일정에 매달아 준다.
        val todos = loaded().state().todos

        assertEquals(
            listOf("달라진 증상 있으면 카드 수정", "복용 중인 약 챙기기", "지난 검사 결과 사진 준비"),
            todos.map { it.label },
        )
        assertTrue(todos.first().done)
    }

    @Test
    fun `체크하면 서버로 나간다`() {
        val repository = FakeAppointmentRepository(listOf(testAppointment))
        val viewModel = dayViewModel(repository = repository).apply { load(VisitDate) }

        viewModel.todo.onToggle("todo-2", true)

        assertEquals(
            listOf(true, true, false),
            repository.savedTodos?.map { it.done },
        )
    }

    @Test
    fun `편집 중 체크는 서버로 나가지 않는다`() {
        // 취소가 실행 취소를 대신하는데 이미 보냈으면 되돌릴 것이 없다.
        val repository = FakeAppointmentRepository(listOf(testAppointment))
        val viewModel = dayViewModel(repository = repository).apply { load(VisitDate) }
        viewModel.todo.onEditStart()

        viewModel.todo.onToggle("todo-2", true)

        assertNull(repository.savedTodos)
    }

    @Test
    fun `편집을 마치면 남은 줄을 통째로 보낸다`() {
        // 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다.
        val repository = FakeAppointmentRepository(listOf(testAppointment))
        val viewModel = dayViewModel(repository = repository).apply { load(VisitDate) }
        viewModel.todo.onEditStart()
        viewModel.todo.onDelete("todo-2")

        viewModel.todo.onEditDone()

        assertEquals(
            listOf("달라진 증상 있으면 카드 수정", "지난 검사 결과 사진 준비"),
            repository.savedTodos?.map { it.text },
        )
    }

    @Test
    fun `일정 삭제는 확인을 묻는다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()

        viewModel.onScheduleDeleteClick()
        assertTrue(viewModel.state().deleteRequested)

        viewModel.onScheduleDeleteDismiss()
        assertFalse(viewModel.state().deleteRequested)
    }

    @Test
    fun `취소하면 삭제 확인도 닫힌다`() {
        val viewModel = loaded()
        viewModel.todo.onEditStart()
        viewModel.onScheduleDeleteClick()

        viewModel.todo.onEditCancel()

        assertFalse(viewModel.state().deleteRequested)
        assertFalse(viewModel.state().editing)
    }

    @Test
    fun `다녀온 날은 할 일이 없어 편집해도 지울 줄이 없다`() {
        val viewModel = dayViewModel().apply { load(PastVisitDate) }

        viewModel.todo.onEditStart()

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
        val viewModel = CalendarDayViewModel(repository, FakeVisitRepository(), FakeCardRepository(), fixedClock)
        viewModel.load(LocalDate.of(2026, 9, 12))
        var left = false

        viewModel.onScheduleDeleteConfirm { left = true }

        assertEquals(1L, repository.deletedId)
        assertTrue(left)
    }

    @Test
    fun `그 날 기록이 있으면 줄로 선다`() {
        val viewModel = dayViewModel(visits = listOf(testVisit))

        viewModel.load(VisitDate)

        val record = viewModel.state().records.single()
        assertEquals("2", record.id)
        assertEquals("갈비뼈 · 일주일", record.title)
        assertTrue(viewModel.state().visited)
    }

    @Test
    fun `다른 날 기록은 끌어오지 않는다`() {
        val viewModel = dayViewModel(visits = listOf(testVisit.copy(visitedOn = PastVisitDate)))

        viewModel.load(VisitDate)

        assertEquals(emptyList<DayRecord>(), viewModel.state().records)
        assertFalse(viewModel.state().visited)
    }

    @Test
    fun `진료가 끝난 날에는 진료 전 할 일을 두지 않는다`() {
        // 시안 1r-2-A에도 없다. 다녀온 뒤에 챙길 것을 알릴 이유가 없다.
        val viewModel = dayViewModel(visits = listOf(testVisit))

        viewModel.load(VisitDate)

        assertTrue(viewModel.state().todos.isEmpty())
    }

    @Test
    fun `기록이 있으면 다음 일정이 붙는다`() {
        val viewModel =
            dayViewModel(appointments = listOf(testAppointment, nextAppointment), visits = listOf(testVisit))

        viewModel.load(VisitDate)

        val next = viewModel.state().nextEvent
        assertEquals("서울OO병원 내과 재방문", next?.title)
        assertEquals("D-15", next?.chip)
        // 서버 일정에는 시각이 늘 있다. 시간을 정하는 조작이 붙는 1r-2-A는 Preview에만 남는다.
        assertEquals("9월 26일 (토) 오전 10:30", next?.at)
    }

    @Test
    fun `기록이 없으면 다음 일정을 붙이지 않는다`() {
        // 아직 오지 않은 날에 붙이면 "이 날 일정"과 나란히 서서 어느 쪽이 오늘 갈 곳인지 흐려진다.
        val viewModel = dayViewModel(appointments = listOf(testAppointment, nextAppointment))

        viewModel.load(VisitDate)

        assertNull(viewModel.state().nextEvent)
    }

    @Test
    fun `잡아 둔 일정이 없으면 뽑아 둔 재방문이 선다`() {
        // 시각이 없는 1r-2-A 상태다. 거기서 확정하면 일정 추가가 열려 일정이 만들어진다(#186).
        val withRevisit = testVisit.copy(followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 26)))
        val viewModel = dayViewModel(appointments = listOf(testAppointment), visits = listOf(withRevisit))

        viewModel.load(VisitDate)

        val next = viewModel.state().nextEvent
        assertEquals("9월 26일 (토)", next?.chip)
        assertNull(next?.at)
    }

    @Test
    fun `잡아 둔 일정이 뽑아 둔 재방문을 이긴다`() {
        // 확정한 것이 뽑아 둔 것보다 정확하다.
        val withRevisit = testVisit.copy(followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 30)))
        val viewModel =
            dayViewModel(appointments = listOf(testAppointment, nextAppointment), visits = listOf(withRevisit))

        viewModel.load(VisitDate)

        assertEquals("D-15", viewModel.state().nextEvent?.chip)
    }

    @Test
    fun `지난 재방문은 다음 일정이 되지 않는다`() {
        val past = testVisit.copy(followUp = VisitFollowUp(date = VisitDate.minusDays(1)))
        val viewModel = dayViewModel(appointments = listOf(testAppointment), visits = listOf(past))

        viewModel.load(VisitDate)

        assertNull(viewModel.state().nextEvent)
    }

    @Test
    fun `그 날 일정은 다음 일정이 되지 않는다`() {
        // 위에 이미 "이 날 일정"으로 서 있다.
        val viewModel = dayViewModel(appointments = listOf(testAppointment), visits = listOf(testVisit))

        viewModel.load(VisitDate)

        assertNull(viewModel.state().nextEvent)
    }
}

/** 그 날 일정과 기록을 돌려주는 저장소 둘. */
private fun dayViewModel(
    appointments: List<Appointment> = listOf(testAppointment),
    visits: List<VisitListItem> = emptyList(),
    repository: FakeAppointmentRepository = FakeAppointmentRepository(appointments),
) = CalendarDayViewModel(
    repository,
    FakeVisitRepository(list = ApiResult.Success(visits)),
    FakeCardRepository(),
    fixedClock,
)

/** 그 날 남긴 기록. 목록 응답의 한 줄이다. */
private val testVisit =
    VisitListItem(
        id = "2",
        cardId = 1L,
        cardTitle = "갈비뼈 · 일주일",
        clinic = "서울OO병원 내과",
        visitedOn = VisitDate,
    )

/** 진료 다음에 잡힌 일정. */
private val nextAppointment =
    Appointment(
        id = 2,
        title = "서울OO병원 내과 재방문",
        on = LocalDate.of(2026, 9, 26),
        time = LocalTime.of(10, 30),
        status = AppointmentStatus.SCHEDULED,
    )

private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneId.of("Asia/Seoul"))

private val testAppointment =
    Appointment(
        id = 1,
        title = "서울OO병원 내과 재진",
        on = LocalDate.of(2026, 9, 12),
        time = LocalTime.of(10, 30),
        status = AppointmentStatus.SCHEDULED,
        cards = listOf(AppointmentCard(id = 1, title = "복부 통증 · 3주")),
        // 진료 전 할 일은 일정에 매달린다(#187). 전에는 픽스처 세 줄이었다.
        todos =
        listOf(
            AppointmentTodo(text = "달라진 증상 있으면 카드 수정", done = true),
            AppointmentTodo(text = "복용 중인 약 챙기기"),
            AppointmentTodo(text = "지난 검사 결과 사진 준비"),
        ),
    )

private class FakeAppointmentRepository(private val appointments: List<Appointment>) : AppointmentRepository {
    var deletedId: Long? = null

    override suspend fun month(month: java.time.YearMonth) = ApiResult.Success(appointments)

    override suspend fun day(date: java.time.LocalDate) = ApiResult.Success(appointments.filter { it.on == date })

    override suspend fun upcoming() = ApiResult.Success(appointments)

    override suspend fun create(appointment: NewAppointment) = ApiResult.Success(appointments.first())

    /** 저장하라고 받은 할 일. 체크가 서버로 나가는지가 관심사다. */
    var savedTodos: List<AppointmentTodo>? = null

    override suspend fun update(id: Long, edit: AppointmentEdit): ApiResult<Appointment> {
        edit.todos?.let { savedTodos = it }
        return ApiResult.Success(appointments.first())
    }

    override suspend fun delete(id: Long): ApiResult<Unit> {
        deletedId = id
        return ApiResult.Success(Unit)
    }
}
