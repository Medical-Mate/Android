package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.model.DEFAULT_UNTIMED_REMINDER
import com.mist.medicalmate.core.model.VisitReminders
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * 알림 시간(1s-3) 상태(#316).
 *
 * @param time 시간 미정 일정의 알림 시각. 휠이 고른 값이고 `확인`을 눌러야 저장된다.
 * @param loaded 서버 값을 읽었는지. 읽기 전에 휠을 그리면 기본값에서 시작했다가 튄다.
 * @param saveFailed 저장이 안 됐다. 화면에 남아 하단에 이유를 적는다.
 */
data class ReminderTimeUiState(
    val time: LocalTime = DEFAULT_UNTIMED_REMINDER,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
)

/**
 * 알림 시간 상태 보유자(#316).
 *
 * 시간 미정 일정의 전날 · 당일 알림 시각을 하나로 고른다(사용자 결정). 서버에는 두 칸이지만 휠이 하나다. 처음
 * 열 때는 진료 전 알림 시각을 보인다.
 *
 * 저장하면 알림 예약을 다시 맞춘다. 못 읽으면 기본값(오후 7시)으로 연다.
 */
@HiltViewModel
class ReminderTimeViewModel
@Inject
internal constructor(
    private val settings: SettingsRepository,
    private val reminders: VisitReminders,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReminderTimeUiState())
    val uiState: StateFlow<ReminderTimeUiState> = mutableUiState.asStateFlow()

    fun load() {
        if (mutableUiState.value.loaded) return
        viewModelScope.launch {
            val time = (settings.reminders() as? ApiResult.Success)?.value?.beforeVisitTime ?: DEFAULT_UNTIMED_REMINDER
            mutableUiState.update { it.copy(time = time, loaded = true) }
        }
    }

    fun onTimeChange(time: LocalTime) {
        mutableUiState.update { it.copy(time = time, saveFailed = false) }
    }

    /** `확인`. 저장되면 [onSaved]로 나간다. 실패하면 남는다 — 나가 버리면 안 바뀐 시각을 바뀐 것으로 안다. */
    fun onConfirm(onSaved: () -> Unit) {
        val state = mutableUiState.value
        if (state.saving) return
        mutableUiState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = settings.update(untimedTime = state.time) is ApiResult.Success
            mutableUiState.update { it.copy(saving = false, saveFailed = !saved) }
            if (saved) {
                reminders.refresh()
                onSaved()
            }
        }
    }
}
