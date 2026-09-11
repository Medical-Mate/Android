package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.HealthEdit
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthProfile
import com.mist.medicalmate.profile.data.HealthProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 건강 정보 수정 상태 보유자.
 *
 * 고르고 빼는 규칙이 여기 있다. 저장은 `PUT /api/me/health-profile`이고 세 갈래를 한 번에
 * 보낸다.
 *
 * 직접 추가한 항목은 [HealthEditUiState.extras]에 쌓고 넣는 동시에 고른 것으로 표시한다.
 * 적어 넣고 다시 눌러야 선택되는 것은 한 번에 두 번 시키는 일이다.
 *
 * 읽어 온 값 중 목록에 없는 것도 [HealthEditUiState.extras]로 들어간다. 1b의 직접 입력으로
 * 넣었거나 칩 목록이 바뀐 경우인데, 칩으로 그리지 않으면 저장할 때 조용히 사라진다.
 */
@HiltViewModel
class HealthEditViewModel
@Inject
internal constructor(private val repository: HealthProfileRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HealthEditUiState())
    val uiState: StateFlow<HealthEditUiState> = mutableUiState.asStateFlow()

    /**
     * 읽어 온 이름·출생연도·성별.
     *
     * 저장이 통째로 덮어쓰기라 그대로 되돌려 보내야 한다. 화면이 쓰지 않는 값이라 상태에
     * 두지 않는다.
     */
    private var profile: HealthProfile? = null

    /** 지금 저장된 값으로 화면을 채운다. */
    fun load() {
        mutableUiState.update { it.copy(loading = true, saveFailed = false) }
        viewModelScope.launch {
            val result = repository.profile()
            profile = (result as? ApiResult.Success)?.value
            mutableUiState.value = (result as? ApiResult.Success)?.value?.health.toUiState()
        }
    }

    /**
     * 저장하고 나간다. 나가는 판단은 호출자가 한다.
     *
     * 실패하면 화면에 남는다. 나가 버리면 고친 것이 사라지고 무엇이 저장됐는지 알 수 없다.
     */
    fun onSaveClick(onSaved: () -> Unit) {
        val base = profile ?: return
        if (mutableUiState.value.saving) return
        mutableUiState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = repository.save(base, mutableUiState.value.toHealthEdit())
            mutableUiState.update { it.copy(saving = false, saveFailed = saved !is ApiResult.Success) }
            if (saved is ApiResult.Success) onSaved()
        }
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

/**
 * 읽어 온 값을 화면 상태로.
 *
 * 읽어 온 값을 전부 [HealthEditUiState.extras]에 넣는다. 기본 목록(`optionsRes`)에 있는지
 * 여기서 가릴 수 없다. `Context`가 없어 문자열 배열을 읽지 못하고, 목록에 없다고 버리면
 * 환자가 1b에서 적어 넣은 것이 화면에서 사라진 채 저장돼 지워진다. 기본 목록과 겹치는
 * 값은 화면이 `distinct()`로 한 번만 그린다.
 */
private fun HealthEdit?.toUiState(): HealthEditUiState {
    val edit = this ?: HealthEdit()
    val values =
        mapOf(
            ProfileSetupStep.MEDICATIONS to edit.medications.items,
            ProfileSetupStep.CONDITIONS to edit.conditions.items,
            ProfileSetupStep.ALLERGIES to edit.allergies.items,
        )
    return HealthEditUiState(
        chosen = values.mapValues { (_, items) -> items.toSet() },
        extras = values,
        loading = false,
    )
}

/** 화면에서 고른 것을 서버가 받는 모양으로. */
private fun HealthEditUiState.toHealthEdit() = HealthEdit(
    medications = HealthField(chosenIn(ProfileSetupStep.MEDICATIONS).toList()),
    conditions = HealthField(chosenIn(ProfileSetupStep.CONDITIONS).toList()),
    allergies = HealthField(chosenIn(ProfileSetupStep.ALLERGIES).toList()),
)

/** Figma 1s-2(407:2650)에서 골라져 있는 값. Preview가 쓴다. */
internal val previewHealthEdit =
    HealthEditUiState(
        chosen =
        mapOf(
            ProfileSetupStep.MEDICATIONS to setOf("혈압약", "진통제"),
            ProfileSetupStep.CONDITIONS to setOf("고혈압"),
            ProfileSetupStep.ALLERGIES to setOf("페니실린"),
        ),
    )
