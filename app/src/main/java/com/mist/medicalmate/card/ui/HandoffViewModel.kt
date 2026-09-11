package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 진료실 화면(1f-1) 상태 보유자.
 *
 * 카드 조회로 대신하지 않고 `GET /api/cards/{id}/handoff`를 부른다. **여는 순간이 전달
 * 시각으로 기록된다.** 나중에 "진료 어떠셨어요"를 물을 근거가 그 값이다.
 *
 * 초안이면 400이라 들어오기 전에 확정돼 있어야 한다. 그 확정은 카드 화면이 한다.
 */
@HiltViewModel
class HandoffViewModel
@Inject
internal constructor(private val repository: CardRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardUiState>(BriefCardUiState.Loading)
    val uiState: StateFlow<BriefCardUiState> = mutableUiState.asStateFlow()

    fun load(cardId: Long) {
        mutableUiState.value = BriefCardUiState.Loading
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.handoff(cardId)) {
                    is ApiResult.Success -> BriefCardUiState.Content(card = result.value)
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> BriefCardUiState.Failed
                }
        }
    }
}
