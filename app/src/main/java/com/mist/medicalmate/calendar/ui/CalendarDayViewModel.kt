package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.core.network.ApiResult
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
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<CalendarDayUiState?>(null)
    val uiState: StateFlow<CalendarDayUiState?> = mutableUiState.asStateFlow()

    /**
     * 그 날을 읽는다.
     *
     * **일정과 그 일정에 걸린 카드가 서버 값이다.** 진료 후 기록·다음 일정·진료 전 할 일은
     * 아직 픽스처다. 기록과 다음 일정은 기록 상세를 함께 읽어야 나오고, 할 일은 서버에
     * 자리가 없다(#141). 그것들까지 지우면 화면이 일정 한 줄만 남는다.
     *
     * 서버에 그 날 일정이 없으면 픽스처의 일정도 지운다. 없는 일정을 남겨 두면 삭제를
     * 눌렀을 때 지울 것이 없다.
     */
    fun load(date: LocalDate) {
        viewModelScope.launch {
            val appointment =
                (repository.day(date) as? ApiResult.Success)
                    ?.value
                    ?.firstOrNull { it.status != AppointmentStatus.CANCELED }
            mutableUiState.value =
                dayState(date).copy(
                    schedule = appointment?.toDaySchedule(LocalDate.now(clock)),
                    card = appointment?.toDayCard(),
                )
        }
    }

    /** 할 일 체크. 편집 중이면 사본을 고친다. */
    fun onTodoToggle(id: String, done: Boolean) {
        mutableUiState.update { state ->
            state?.copy(todos = state.todos.map { if (it.id == id) it.copy(done = done) else it })
        }
    }

    fun onEditStart() {
        mutableUiState.update { it?.copy(todoDraft = it.todos) }
    }

    /** 편집을 버린다. 사본을 지우면 본값이 그대로 남는다. */
    fun onEditCancel() {
        mutableUiState.update { it?.copy(todoDraft = null, deleteRequested = false) }
    }

    /** 편집을 마친다. 사본이 본값이 된다. */
    fun onEditDone() {
        mutableUiState.update { state ->
            state?.copy(todos = state.todoDraft ?: state.todos, todoDraft = null)
        }
    }

    /**
     * 할 일 줄의 ×.
     *
     * 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고, 편집을 벗어나기 전이면 취소가
     * 실행 취소를 대신한다.
     */
    fun onTodoDelete(id: String) {
        mutableUiState.update { state ->
            state?.copy(todoDraft = state.todoDraft?.filterNot { it.id == id })
        }
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
    val id = cardId ?: return null
    return DayCard(id = id.toString(), title = cardTitle.orEmpty())
}

private fun Appointment.toDaySchedule(today: LocalDate) = CalendarSchedule(
    id = id.toString(),
    title = title,
    time = at.toLocalTime().format(DAY_TIME_FORMAT),
    detail = cardTitle.orEmpty(),
    dday = at.toLocalDate().toEpochDay() - today.toEpochDay(),
)

private val DAY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
