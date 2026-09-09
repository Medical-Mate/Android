package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 건강 정보 수정 상태 보유자.
 *
 * 고르고 빼는 규칙이 여기 있다. `PUT /api/me/health-profile`이 붙으면 저장할 때 세 갈래를
 * 한 번에 보낸다.
 *
 * 직접 추가한 항목은 [HealthEditUiState.extras]에 쌓고 넣는 동시에 고른 것으로 표시한다.
 * 적어 넣고 다시 눌러야 선택되는 것은 한 번에 두 번 시키는 일이다.
 */
@HiltViewModel
class HealthEditViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(HealthEditUiState())
    val uiState: StateFlow<HealthEditUiState> = mutableUiState.asStateFlow()

    /** 지금 저장된 값으로 화면을 채운다. 서버 연동에서는 응답을 받아 같은 모양으로 넣는다. */
    fun load() {
        mutableUiState.value = previewHealthEdit
    }

    fun onOptionClick(step: ProfileSetupStep, option: String) {
        mutableUiState.update { state ->
            val chosen = state.chosenIn(step)
            val next = if (option in chosen) chosen - option else chosen + option
            state.copy(chosen = state.chosen + (step to next))
        }
    }

    /**
     * 직접 추가 칩을 눌렀을 때.
     *
     * 이미 열려 있던 갈래를 다시 누르면 닫는다. 열어 둔 것을 닫을 방법이 없으면 적다 만
     * 입력 칸이 화면에 남는다.
     */
    fun onAddClick(step: ProfileSetupStep) {
        mutableUiState.update { state ->
            if (state.adding == step) {
                state.copy(adding = null, draft = "")
            } else {
                state.copy(adding = step, draft = "")
            }
        }
    }

    fun onDraftChange(draft: String) {
        mutableUiState.update { it.copy(draft = draft) }
    }

    /** 빈 값과 이미 있는 항목은 넣지 않는다. 같은 칩이 두 개 생기면 어느 쪽이 골라졌는지 알 수 없다. */
    fun onDraftSubmit() {
        mutableUiState.update { state ->
            val step = state.adding ?: return@update state
            val value = state.draft.trim()
            if (value.isEmpty()) return@update state
            val extras = state.extrasIn(step)
            if (value in extras) return@update state.copy(draft = "")
            state.copy(
                extras = state.extras + (step to extras + value),
                chosen = state.chosen + (step to state.chosenIn(step) + value),
                draft = "",
            )
        }
    }
}

/** Figma 1s-2(407:2650)에서 골라져 있는 값. Preview와 픽스처가 함께 쓴다. */
internal val previewHealthEdit =
    HealthEditUiState(
        chosen =
        mapOf(
            ProfileSetupStep.MEDICATIONS to setOf("혈압약", "진통제"),
            ProfileSetupStep.CONDITIONS to setOf("고혈압"),
            ProfileSetupStep.ALLERGIES to setOf("페니실린"),
        ),
    )
