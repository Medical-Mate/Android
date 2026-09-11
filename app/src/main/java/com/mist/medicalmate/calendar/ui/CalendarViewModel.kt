package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

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
}
