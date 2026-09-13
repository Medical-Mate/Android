package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.calendar.data.AppointmentTodo
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.VisitListItem
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 캘린더 일자 화면 상태 보유자.
 *
 * 월 화면과 나눈다. 일자 화면이 편집 상태를 들면서 한 클래스가 두 화면의 조작을 다 갖게
 * 됐고, 픽스처는 [CalendarFixtures][VisitDate]에서 함께 본다.
 *
 * 편집은 문서의 CRUD 규칙을 따른다. 편집 중에는 사본을 고치고 취소하면 버린다. 확인은
 * 사본을 본값으로 옮긴다. 서버가 붙으면 그 자리에서 저장 호출이 나간다.
 */
@HiltViewModel
class CalendarDayViewModel
@Inject
internal constructor(
    private val repository: AppointmentRepository,
    private val visitRepository: VisitRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<CalendarDayUiState?>(null)
    val uiState: StateFlow<CalendarDayUiState?> = mutableUiState.asStateFlow()

    /**
     * 그 날을 읽는다.
     *
     * 일정·카드·기록·다음 일정이 서버 값이다. **진료 전 할 일만 아직 픽스처다.** 서버에
     * 자리가 없다(#141).
     *
     * 서버에 없는 것은 픽스처의 것도 지운다. 없는 일정을 남겨 두면 삭제를 눌렀을 때 지울
     * 것이 없고, 없는 기록을 남겨 두면 눌렀을 때 빈 화면이 나온다.
     *
     * 세 호출을 하나씩 기다린다. 화면 하나를 그리는 데 셋이 다 있어야 하고, 일자 화면은
     * 캘린더에서 한 번 들어올 때만 읽는다.
     */
    fun load(date: LocalDate) {
        viewModelScope.launch {
            val appointment =
                (repository.day(date) as? ApiResult.Success)
                    ?.value
                    ?.firstOrNull { it.status != AppointmentStatus.CANCELED }
            val record = (visitRepository.visits() as? ApiResult.Success)?.value?.firstOrNull { it.visitedOn == date }
            mutableUiState.value =
                dayState(date).copy(
                    schedule = appointment?.toDaySchedule(LocalDate.now(clock)),
                    card = appointment?.toDayCard(),
                    record = record?.toDayRecord(),
                    // 진료가 끝난 날에는 진료 전 할 일을 두지 않는다. 시안 1r-2-A도 그렇다.
                    todos = if (record == null) appointment?.todos.toDayTodos() else emptyList(),
                    nextEvent = record?.let { nextEvent(date, it) },
                )
        }
    }

    /**
     * 이 진료 다음에 올 일정.
     *
     * **기록이 있는 날에만 붙인다.** 진료를 다녀온 뒤 다음이 언제인지를 알리는 자리다(1r-2-A).
     * 아직 오지 않은 날에 붙이면 같은 화면에 "이 날 일정"과 "다음 일정"이 나란히 서서 어느
     * 쪽이 오늘 갈 곳인지 흐려진다.
     *
     * 그 날 자신의 일정은 뺀다. 위에 이미 "이 날 일정"으로 서 있다.
     *
     * **잡아 둔 일정이 없으면 뽑아 둔 재방문을 세운다**(#186). 진료 후 기록에서 AI가 꺼낸
     * 날짜이고 아직 일정이 아니다. 시각이 없는 그 상태가 시안 1r-2-A이고, 거기서 확정하면
     * 일정 추가(1r-4-B)가 열려 일정이 만들어진다.
     *
     * 잡아 둔 것이 있으면 그것이 이긴다. 확정한 것이 뽑아 둔 것보다 정확하다.
     */
    private suspend fun nextEvent(date: LocalDate, record: VisitListItem): DayNextEvent? {
        val today = LocalDate.now(clock)
        val booked = (repository.upcoming() as? ApiResult.Success)
            ?.value
            ?.firstOrNull { it.status != AppointmentStatus.CANCELED && it.on > date }
        if (booked != null) return booked.toNextEvent(today)
        return record.followUpDate?.takeIf { it > date }?.toRevisit(record.clinic)
    }

    /** 할 일 조작. 다섯 가지라 여기 얹으면 한 클래스가 너무 많은 일을 한다. */
    val todo =
        CalendarDayTodoActions(
            editing = { mutableUiState.value?.todoDraft != null },
            update = { change -> mutableUiState.update { it?.change() } },
            save = ::saveTodos,
        )

    /**
     * 할 일을 일정에 남긴다.
     *
     * **통째로 갈아끼운다.** 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다.
     *
     * 일정이 없으면 보낼 곳이 없다. 할 일은 일정에 매달린 값이라 그 날 일정이 없으면 화면에도
     * 할 일이 없다.
     */
    private fun saveTodos() {
        val state = mutableUiState.value ?: return
        val id = state.schedule?.id?.toLongOrNull() ?: return
        val todos = state.todos.map { AppointmentTodo(text = it.label, done = it.done) }
        viewModelScope.launch { repository.update(id = id, edit = AppointmentEdit(todos = todos)) }
    }

    fun onScheduleDeleteClick() {
        mutableUiState.update { it?.copy(deleteRequested = true) }
    }

    fun onScheduleDeleteDismiss() {
        mutableUiState.update { it?.copy(deleteRequested = false) }
    }

    /**
     * 대화상자의 `삭제`. 서버에서 지우고 화면을 떠난다.
     *
     * [onDeleted]는 실패하면 부르지 않는다. 지워지지 않았는데 화면을 닫으면 캘린더로 돌아가
     * 그 일정이 그대로 있다.
     */
    fun onScheduleDeleteConfirm(onDeleted: () -> Unit) {
        val id = mutableUiState.value?.schedule?.id?.toLongOrNull() ?: return
        viewModelScope.launch {
            if (repository.delete(id) is ApiResult.Success) {
                mutableUiState.update { it?.copy(deleteRequested = false) }
                onDeleted()
            }
        }
    }
}

/** 일자 화면의 일정 줄. 월 화면과 같은 모양이라 값만 옮긴다. */
/**
 * 일정에 걸린 카드.
 *
 * 픽스처 카드를 쓰지 않는다. 그 id로는 카드를 열 수 없어서 눌러도 빈 화면이 나왔다.
 *
 * 작성일·항목 수·상태는 일정 응답에 없다. 카드를 따로 읽어야 나오는데, 이 줄은 카드로
 * 들어가는 길이라 그 값들이 없어도 제 일을 한다.
 */
private fun Appointment.toDayCard(): DayCard? {
    // 서버가 카드를 목록으로 준다(#202). 이 줄은 한 장을 그리므로 첫 장만 쓴다.
    val card = cards.firstOrNull() ?: return null
    return DayCard(id = card.id.toString(), title = card.title.orEmpty())
}

/**
 * 그 날 남긴 진료 후 기록.
 *
 * 무엇을 들었는지는 목록 응답에 없다. 상세를 따로 읽어야 나오는데, 이 줄은 기록 상세로
 * 들어가는 길이라 그 값이 없어도 제 일을 한다. 기록 목록(1j-1)의 줄과 같은 모양이다.
 */
private fun VisitListItem.toDayRecord() = DayRecord(
    id = id,
    title = cardTitle.ifBlank { clinic.orEmpty() },
    meta = clinic.orEmpty(),
)

/**
 * 다음 일정 카드.
 *
 * **시각이 없을 수 있다**(#202). 서버가 시간 미정을 담을 자리를 열면서 1r-2-A가 실제로
 * 온다. 그때 [DayNextEvent.at]이 없고 화면이 "시간 정하고 확정하기"를 둔다. 전에는 일정에
 * 시각이 필수라 늘 정해진 쪽(1r-2-A2)이었고 그 상태는 Preview에만 있었다.
 */
private fun Appointment.toNextEvent(today: LocalDate) = DayNextEvent(
    chip = "D-${on.toEpochDay() - today.toEpochDay()}",
    title = title,
    at = time?.let { on.atTime(it).format(NEXT_EVENT_FORMAT) },
    clinic = title,
)

/**
 * 서버의 할 일을 화면 줄로.
 *
 * 서버가 id를 매기지 않아 차례로 만든다. 같은 글이 두 줄 있을 수 있어 글을 key로 쓸 수 없다.
 */
private fun List<AppointmentTodo>?.toDayTodos(): List<DayTodo> =
    orEmpty().mapIndexed { index, todo -> DayTodo(id = "todo-${index + 1}", label = todo.text, done = todo.done) }

/**
 * 확정 전 재방문.
 *
 * 시각이 없다. 칩에 D-day 대신 날짜를 적는다 — 아직 일정이 아니라서 "며칠 남았다"가 아니라
 * "이 날쯤"이 맞는 말이다. 화면은 [DayNextEvent.at]이 없으면 "시간 정하고 확정하기"를 둔다.
 */
private fun LocalDate.toRevisit(clinic: String?) = DayNextEvent(
    chip = format(REVISIT_CHIP),
    title = clinic?.let { "$it 재방문" } ?: "재방문 예정",
    clinic = clinic,
)

private fun Appointment.toDaySchedule(today: LocalDate) = CalendarSchedule(
    id = id.toString(),
    title = title,
    time = time?.format(DAY_TIME_FORMAT),
    detail = cards.firstOrNull()?.title.orEmpty(),
    dday = on.toEpochDay() - today.toEpochDay(),
)

/** 시안 1r-2-A의 "9월 26일 (토)". */
private val REVISIT_CHIP: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

private val DAY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/** 다음 일정의 "9월 26일 (토) 오전 10:30". */
private val NEXT_EVENT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 (E) a h:mm", Locale.KOREAN)
