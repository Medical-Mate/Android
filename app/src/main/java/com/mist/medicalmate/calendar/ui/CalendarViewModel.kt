package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 캘린더 상태 보유자.
 *
 * 달을 넘길 때마다 그 달을 다시 읽는다. 서버가 `year`·`month`로 잘라 준다.
 *
 * **D-day를 여기서 센다.** 문서가 "서버가 계산하면 사용자 시간대와 어긋날 때 하루
 * 틀립니다"라고 적었고 실제로 남은 날수가 오지 않는다. 화면이 열린 날 기준이어야 해서
 * 미리 만들어 두지도 않는다.
 *
 * 점 두 가지가 서로 다른 곳에서 온다. 빈 원(앞으로의 일정)은 일정에서, 채운 점(기록 있음)은
 * 그 날 쓴 카드에서 온다. 시안이 지난 기록과 앞으로의 일정을 다르게 찍고, 서버도 그 둘을
 * 다른 경로로 준다. 취소된 일정은 찍지 않는다.
 */
@HiltViewModel
class CalendarViewModel
@Inject
internal constructor(
    private val repository: AppointmentRepository,
    private val cardRepository: CardRepository,
    private val visitRepository: VisitRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(emptyState(LocalDate.now(clock)))
    val uiState: StateFlow<CalendarUiState> = mutableUiState.asStateFlow()

    fun load() {
        loadMonth(mutableUiState.value.month)
    }

    fun onPreviousMonth() {
        loadMonth(mutableUiState.value.month.minusMonths(1))
    }

    fun onNextMonth() {
        loadMonth(mutableUiState.value.month.plusMonths(1))
    }

    /**
     * 날을 골랐다. 그 날의 일정만 남긴다.
     *
     * 이미 그 달을 읽어 뒀으므로 다시 부르지 않는다. 일자 화면이 열릴 때 그 날짜로 한 번 더
     * 읽는다.
     */
    fun onDaySelect(date: LocalDate) {
        mutableUiState.update { state ->
            val schedules = state.appointments.filter { it.at.toLocalDate() == date }
            state.copy(
                selected = date,
                schedules = schedules.map { it.toSchedule(state.today) },
                // 일정이 없고 카드만 쓴 날은 갈 화면이 없다. 그 자리에서 시트로 보여준다.
                cardSheet = if (schedules.isEmpty()) state.cards.cardOn(date) else null,
            )
        }
    }

    /** 시트를 닫았다. */
    fun onCardSheetDismiss() {
        mutableUiState.update { it.copy(cardSheet = null) }
    }

    private fun loadMonth(month: YearMonth) {
        val today = LocalDate.now(clock)
        mutableUiState.update { it.copy(month = month, today = today) }
        viewModelScope.launch {
            val appointments = (repository.month(month) as? ApiResult.Success)?.value.orEmpty()
            val cards = (cardRepository.cards() as? ApiResult.Success)?.value.orEmpty()
            // 확정하지 않은 재방문도 달력에 찍는다. 뽑아 두기만 한 날은 일정에 없다(#186).
            val revisits = (visitRepository.visits() as? ApiResult.Success)?.value.orEmpty()
            mutableUiState.update { it.withMonth(appointments, cards, revisits, month, today) }
        }
    }
}

private fun CalendarUiState.withMonth(
    appointments: List<Appointment>,
    cards: List<CardListItem>,
    visits: List<VisitListItem>,
    month: YearMonth,
    today: LocalDate,
): CalendarUiState {
    val live = appointments.filter { it.status != AppointmentStatus.CANCELED }
    val monthCards = cards.filter { YearMonth.from(it.writtenOn) == month }
    val onSelected = live.filter { it.at.toLocalDate() == selected }
    return copy(
        appointments = live,
        cards = monthCards,
        recordDays = monthCards.map { it.writtenOn.dayOfMonth }.toSet(),
        plannedDays = plannedDays(live, visits, month, today),
        schedules = onSelected.map { it.toSchedule(today) },
        cardSheet = if (onSelected.isEmpty()) monthCards.cardOn(selected) else null,
    )
}

/**
 * 예정 표시를 찍을 날.
 *
 * 잡아 둔 일정과 **아직 확정하지 않은 재방문**을 합친다(#186). 재방문은 진료 후 기록에서 AI가
 * 뽑아 둔 날이고 일정으로는 아직 없다. 그 날이 달력에 안 보이면 환자가 확정하러 갈 길이 없다.
 *
 * 지난 날은 빼고 그 달의 것만 남긴다.
 */
private fun plannedDays(
    appointments: List<Appointment>,
    visits: List<VisitListItem>,
    month: YearMonth,
    today: LocalDate,
): Set<Int> {
    val scheduled = appointments.map { it.at.toLocalDate() }
    val revisits = visits.mapNotNull { it.followUpDate }
    return (scheduled + revisits)
        .filter { it >= today && YearMonth.from(it) == month }
        .map { it.dayOfMonth }
        .toSet()
}

/** 그 날 쓴 카드. 여럿이면 첫 장이다. 시안의 시트도 한 장을 보여준다. */
private fun List<CardListItem>.cardOn(date: LocalDate): DayCard? =
    firstOrNull { it.writtenOn == date }?.let { DayCard(id = it.id, title = it.title, meta = it.clinic.orEmpty()) }

private fun emptyState(today: LocalDate) = CalendarUiState(
    month = YearMonth.from(today),
    today = today,
    selected = today,
)

/**
 * 일정 한 줄.
 *
 * 보조 문구는 가져갈 카드다. 없으면 비운다. 서버가 그 줄을 만들어 주지 않고, 카드 제목이
 * 그 자리에 들어가는 유일한 값이다.
 */
private fun Appointment.toSchedule(today: LocalDate) = CalendarSchedule(
    id = id.toString(),
    title = title,
    time = at.toLocalTime().format(TIME_FORMAT),
    detail = cardTitle.orEmpty(),
    dday = at.toLocalDate().toEpochDay() - today.toEpochDay(),
)

/** 시안의 "오전 10:30". 기기 언어가 한국어가 아니어도 한 줄 안에서 언어가 갈리지 않게 한다. */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
