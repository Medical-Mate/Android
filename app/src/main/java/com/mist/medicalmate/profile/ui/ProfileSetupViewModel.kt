package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.HealthEdit
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
internal constructor(private val repository: HealthProfileRepository) :
    ViewModel() {
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

    /**
     * 세 단계를 한 번에 저장한다. `PUT /api/me/health-profile`.
     *
     * 단계마다 보내지 않는다. 서버가 부분 갱신을 주지 않고, 그 전에 이 흐름은 중간에 나가면
     * 아무것도 남지 않는 자리다. 다른 화면과 달리 임시저장이 없다.
     *
     * 저장은 통째로 덮어쓰기라 먼저 읽는다. 이름·출생연도·성별을 그대로 되돌려 보내야 하는데
     * 그 셋은 카카오에서 오고 이 화면이 묻지 않는다.
     *
     * 성공해야 [ProfileSetupUiState.completed]가 선다. 저장되지 않았는데 완료 화면(1b-4)이
     * 나오면 등록됐다고 읽힌다.
     */
    private fun complete() {
        if (mutableUiState.value.saving) return
        mutableUiState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = save()
            mutableUiState.update {
                it.copy(saving = false, completed = saved, saveFailed = !saved)
            }
        }
    }

    private suspend fun save(): Boolean {
        val profile = (repository.profile() as? ApiResult.Success)?.value ?: return false
        val saved = repository.save(profile, mutableUiState.value.toHealthEdit())
        return saved is ApiResult.Success
    }

    private fun updateAnswer(transform: (ProfileSetupAnswer) -> ProfileSetupAnswer) {
        mutableUiState.update { state ->
            val next = transform(state.answer)
            state.copy(answers = state.answers + (state.step to next))
        }
    }
}

/** 세 단계의 답을 서버가 받는 모양으로. */
private fun ProfileSetupUiState.toHealthEdit() = HealthEdit(
    medications = field(ProfileSetupStep.MEDICATIONS),
    conditions = field(ProfileSetupStep.CONDITIONS),
    allergies = field(ProfileSetupStep.ALLERGIES),
)

/**
 * 고른 칩과 직접 적은 것을 한 목록으로.
 *
 * 직접 입력 칸은 쉼표로 나눈다. "혈압약, 아스피린"을 한 항목으로 두면 목록에서 약 하나로
 * 세어진다. 읽어 올 때도 같은 규칙으로 되돌린다.
 */
private fun ProfileSetupUiState.field(step: ProfileSetupStep): HealthField {
    val answer = answers[step] ?: return HealthField()
    val written = answer.note.split(",").map(String::trim).filter(String::isNotEmpty)
    return HealthField((answer.chosen + written).toList())
}
