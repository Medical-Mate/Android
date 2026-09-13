package com.mist.medicalmate.calendar.ui
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentCard
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.calendar.data.AppointmentTodo
import com.mist.medicalmate.calendar.data.NewAppointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.ui.FakeCardRepository
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleAddViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 카드 후보만 있고 아무것도 고르지 않았다`() {
        val state = addViewModel().uiState.value

        assertEquals(2, state.cards.size)
        assertEquals(0, state.pickedCardCount)
        assertNull(state.hospital)
        assertEquals(emptyList<ScheduleAddTodo>(), state.todos)
        assertEquals(ScheduleAddSheet.NONE, state.sheet)
    }

    @Test
    fun `병원과 날짜가 있어야 저장할 수 있다`() {
        // 시간은 필수가 아니다. 진료 시각을 아직 모르고 날짜만 잡아 두는 일이 흔하다.
        val viewModel = addViewModel()

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onHospitalPicked("서울OO병원 내과")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `카드와 할 일은 없어도 저장할 수 있다`() {
        // 필수는 병원과 날짜뿐이다. 나머지는 선택이다.
        val viewModel = filled()

        assertTrue(viewModel.uiState.value.cards.none { it.picked })
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `확인하면 시트가 닫히고 값이 남는다`() {
        val viewModel = addViewModel()
        viewModel.onSheetOpen(ScheduleAddSheet.DATE)

        assertEquals(ScheduleAddSheet.DATE, viewModel.uiState.value.sheet)

        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))

        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
        assertEquals(LocalDate.of(2026, 9, 26), viewModel.uiState.value.date)
    }

    @Test
    fun `끌어내려 닫으면 값이 그대로다`() {
        val viewModel = addViewModel()
        viewModel.onTimeConfirm(LocalTime.of(9, 0))
        viewModel.onSheetOpen(ScheduleAddSheet.TIME)

        viewModel.onSheetDismiss()

        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
        assertEquals(LocalTime.of(9, 0), viewModel.uiState.value.time)
    }

    @Test
    fun `빈 이름으로 돌아오면 병원을 지우지 않는다`() {
        val viewModel = addViewModel()
        viewModel.onHospitalPicked("서울OO병원 내과")

        viewModel.onHospitalPicked(null)
        viewModel.onHospitalPicked("")

        assertEquals("서울OO병원 내과", viewModel.uiState.value.hospital)
    }

    @Test
    fun `카드를 고르면 개수가 오른다`() {
        val viewModel = addViewModel()

        viewModel.onCardPickChange("1", true)
        assertEquals(1, viewModel.uiState.value.pickedCardCount)

        viewModel.onCardPickChange("2", true)
        assertEquals(2, viewModel.uiState.value.pickedCardCount)

        viewModel.onCardPickChange("1", false)
        assertEquals(1, viewModel.uiState.value.pickedCardCount)
    }

    @Test
    fun `카드 목록을 다시 읽어도 고른 것은 그대로다`() {
        // 병원을 고르러 나갔다 돌아오면 화면이 다시 조합되면서 목록을 한 번 더 읽는다.
        // 그때 고른 것이 풀리면 카드가 안 걸린 일정이 저장되고, 그 일정으로는 진료 후
        // 기록을 남길 수 없다.
        val viewModel = addViewModel()
        viewModel.onCardPickChange("1", true)

        viewModel.load()

        assertEquals(1, viewModel.uiState.value.pickedCardCount)
        assertTrue(viewModel.uiState.value.cards.first { it.id == "1" }.picked)
    }

    @Test
    fun `할 일을 추가하면 적는 중인 빈 줄이 생긴다`() {
        val viewModel = addViewModel()

        viewModel.todo.onAddClick()

        val todo = viewModel.uiState.value.todos.single()
        assertEquals("", todo.label)
        assertTrue(todo.editing)
    }

    @Test
    fun `적고 마치면 줄이 남는다`() {
        val viewModel = addViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onLabelChange(id, "복용 중인 약 챙기기")
        viewModel.todo.onEditDone(id)

        val todo = viewModel.uiState.value.todos.single()
        assertEquals("복용 중인 약 챙기기", todo.label)
        assertFalse(todo.editing)
    }

    @Test
    fun `한 글자도 없이 마치면 빈 줄이 사라진다`() {
        val viewModel = addViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onEditDone(id)

        assertEquals(emptyList<ScheduleAddTodo>(), viewModel.uiState.value.todos)
    }

    @Test
    fun `공백만 적고 마쳐도 사라진다`() {
        val viewModel = addViewModel()
        viewModel.todo.onAddClick()
        val id = viewModel.uiState.value.todos.single().id

        viewModel.todo.onLabelChange(id, "   ")
        viewModel.todo.onEditDone(id)

        assertEquals(emptyList<ScheduleAddTodo>(), viewModel.uiState.value.todos)
    }

    @Test
    fun `지운 뒤 추가해도 id가 겹치지 않는다`() {
        val viewModel = addViewModel()
        viewModel.todo.onAddClick()
        val first = viewModel.uiState.value.todos.single().id
        viewModel.todo.onDeleteClick(first)

        viewModel.todo.onAddClick()

        assertEquals(1, viewModel.uiState.value.todos.size)
        assertTrue(first != viewModel.uiState.value.todos.single().id)
    }

    @Test
    fun `체크는 그 줄만 바꾼다`() {
        val viewModel = addViewModel()
        viewModel.todo.onAddClick()
        viewModel.todo.onAddClick()
        val (first, second) = viewModel.uiState.value.todos.map { it.id }

        viewModel.todo.onToggle(second, true)

        val todos = viewModel.uiState.value.todos.associateBy { it.id }
        assertFalse(todos.getValue(first).done)
        assertTrue(todos.getValue(second).done)
    }

    @Test
    fun `저장하면 고른 날짜와 시각으로 일정을 만든다`() {
        val repository = RecordingAppointmentRepository()
        val viewModel = filled(repository)

        viewModel.onSaveClick {}

        assertEquals(LocalDate.of(2026, 9, 26), repository.createdOn)
        assertEquals(LocalTime.of(10, 30), repository.createdTime)
        assertEquals("서울OO병원 내과", repository.createdClinic)
    }

    @Test
    fun `적어 둔 할 일도 함께 보낸다`() {
        // 전에는 화면 안에서만 살고 저장되지 않았다(#187).
        val repository = RecordingAppointmentRepository()
        val viewModel = filled(repository)
        viewModel.todo.onAddClick()
        viewModel.todo.onLabelChange(viewModel.uiState.value.todos.last().id, "보험 서류 챙기기")

        viewModel.onSaveClick {}

        assertEquals(listOf("보험 서류 챙기기"), repository.createdTodos.map { it.text })
    }

    @Test
    fun `비운 할 일 줄은 보내지 않는다`() {
        // 적지 않은 것과 빈 줄은 다르다.
        val repository = RecordingAppointmentRepository()
        val viewModel = filled(repository)
        viewModel.todo.onAddClick()

        viewModel.onSaveClick {}

        assertEquals(emptyList<String>(), repository.createdTodos.map { it.text })
    }

    @Test
    fun `필수 칸이 비면 보내지 않는다`() {
        val repository = RecordingAppointmentRepository()

        addViewModel(repository).onSaveClick {}

        assertEquals(0, repository.createCount)
    }

    @Test
    fun `필수 칸이 비면 어느 칸인지 알린다`() {
        // 비활성 버튼으로 막지 않는다. 눌러야 무엇이 비었는지 알 수 있다.
        val viewModel = addViewModel()

        viewModel.onSaveClick {}

        val state = viewModel.uiState.value
        assertTrue(state.hospitalMissing)
        assertTrue(state.dateMissing)
    }

    @Test
    fun `병원만 비면 그 칸만 알린다`() {
        val viewModel = addViewModel()
        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))
        viewModel.onTimeConfirm(LocalTime.of(10, 30))

        viewModel.onSaveClick {}

        val state = viewModel.uiState.value
        assertTrue(state.hospitalMissing)
        assertFalse(state.dateMissing)
    }

    @Test
    fun `화면을 열자마자 붉히지 않는다`() {
        // 채울 기회가 없었는데 틀렸다고 하는 셈이다.
        val state = addViewModel().uiState.value

        assertFalse(state.hospitalMissing)
        assertFalse(state.dateMissing)
    }

    @Test
    fun `시간을 안 골라도 저장한다`() {
        // 진료 시각을 아직 모르고 날짜만 잡아 두는 일이 흔하다. 시안도 필수 표시를 두지 않는다.
        val repository = RecordingAppointmentRepository()
        val viewModel = addViewModel(repository)
        viewModel.onHospitalPicked("서울OO병원 내과")
        viewModel.onDateConfirm(LocalDate.of(2026, 9, 26))

        viewModel.onSaveClick {}

        // 시각 없이 나간다. 서버가 "시간 미정"으로 만든다(#202). 전에는 자리가 없어 오전
        // 9시로 박았고, 환자가 고르지 않은 시각이 일자 화면에 그대로 떴다.
        assertEquals(LocalDate.of(2026, 9, 26), repository.createdOn)
        assertNull(repository.createdTime)
    }

    @Test
    fun `저장이 끝나야 화면을 옮긴다`() {
        var saved = false

        filled().onSaveClick { saved = true }

        assertTrue(saved)
    }

    /** 필수 셋을 채운 상태. */
    private fun filled(repository: RecordingAppointmentRepository = RecordingAppointmentRepository()) =
        addViewModel(repository).apply {
            onHospitalPicked("서울OO병원 내과")
            onDateConfirm(LocalDate.of(2026, 9, 26))
            onTimeConfirm(LocalTime.of(10, 30))
        }
}
private fun addViewModel(repository: RecordingAppointmentRepository = RecordingAppointmentRepository()) =
    ScheduleAddViewModel(repository, FakeCardRepository(list = ApiResult.Success(pickableCards)))
        .apply { load() }

/** 고를 수 있는 카드 둘. 화면이 열릴 때 서버에서 받는다. */
private val pickableCards =
    listOf(
        CardListItem(
            id = "1",
            title = "복부 통증 · 3주",
            confirmed = true,
            visited = false,
            clinic = "서울OO병원 내과",
            writtenOn = java.time.LocalDate.of(2026, 9, 4),
        ),
        CardListItem(
            id = "2",
            title = "두통 · 잦은 어지러움",
            confirmed = false,
            visited = false,
            clinic = null,
            writtenOn = java.time.LocalDate.of(2026, 8, 21),
        ),
    )

/** 무엇을 보냈는지 기록한다. */
internal class RecordingAppointmentRepository : AppointmentRepository {
    var createdOn: LocalDate? = null

    var createdTime: LocalTime? = null
    var createdClinic: String? = null
    var createdCardIds: List<Long> = emptyList()
    var createCount = 0

    override suspend fun month(month: YearMonth) = ApiResult.Success(emptyList<Appointment>())

    override suspend fun day(date: java.time.LocalDate) = ApiResult.Success(emptyList<Appointment>())

    override suspend fun upcoming() = ApiResult.Success(emptyList<Appointment>())

    /** 저장하라고 받은 할 일. 적어 둔 줄이 함께 나가는지가 관심사다. */
    var createdTodos: List<AppointmentTodo> = emptyList()

    override suspend fun create(appointment: NewAppointment): ApiResult<Appointment> {
        createCount += 1
        createdOn = appointment.on
        createdTime = appointment.time
        createdClinic = appointment.clinicName
        createdCardIds = appointment.cardIds
        createdTodos = appointment.todos
        return ApiResult.Success(
            Appointment(
                id = 1,
                title = appointment.clinicName.orEmpty(),
                on = appointment.on,
                time = appointment.time,
                status = AppointmentStatus.SCHEDULED,
                cards = appointment.cardIds.map { AppointmentCard(id = it, title = null) },
            ),
        )
    }

    override suspend fun update(id: Long, edit: AppointmentEdit) = create(
        NewAppointment(
            clinicName = null,
            on = edit.on ?: LocalDate.now(),
            time = edit.time,
            purpose = edit.purpose,
            cardIds = edit.cardIds.orEmpty(),
            todos = edit.todos.orEmpty(),
        ),
    )

    override suspend fun delete(id: Long) = ApiResult.Success(Unit)
}
