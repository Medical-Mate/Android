package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 신상정보 입력 상태 보유자.
 *
 * 세 단계를 한 목적지 안에서 넘긴다. 단계마다 목적지를 두면 뒤로 가기와 저장 시점이
 * 흩어지고, 마지막에 한 번 저장하는 흐름과 맞지 않는다.
 *
 * **서버 저장은 아직 없다.** [complete]가 상태만 바꾼다. `PUT /api/me/health-profile`을
 * 연결하면 그 자리에서 호출하고 결과에 따라 실패 상태를 하나 더 둔다. 지금 세 단계의
 * 답을 [ProfileSetupAnswer]로 들고 있는 것이 그때 요청 본문이 된다.
 */
@HiltViewModel
class ProfileSetupViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = mutableUiState.asStateFlow()

    /** 칩을 눌렀다. 고른 것은 빠지고 안 고른 것은 들어간다. */
    fun onOptionToggle(option: String) {
        updateAnswer { answer ->
            val chosen = if (option in answer.chosen) answer.chosen - option else answer.chosen + option
            answer.copy(chosen = chosen)
        }
    }

    fun onNoteChange(note: String) {
        updateAnswer { answer -> answer.copy(note = note) }
    }

    /** 다음 단계로. 마지막 단계에서는 [complete]를 부른다. */
    fun onNext() {
        val state = mutableUiState.value
        if (state.step.isLast) {
            complete()
            return
        }
        mutableUiState.update { it.copy(step = ProfileSetupStep.entries[it.step.ordinal + 1]) }
    }

    /** 이전 단계로. 첫 단계에서는 아무 일도 하지 않는다. 화면을 벗어나는 것은 호출자가 한다. */
    fun onBack() {
        mutableUiState.update {
            if (it.canGoBack) it.copy(step = ProfileSetupStep.entries[it.step.ordinal - 1]) else it
        }
    }

    private fun complete() {
        mutableUiState.update { it.copy(completed = true) }
    }

    private fun updateAnswer(transform: (ProfileSetupAnswer) -> ProfileSetupAnswer) {
        mutableUiState.update { state ->
            val next = transform(state.answer)
            state.copy(answers = state.answers + (state.step to next))
        }
    }
}
