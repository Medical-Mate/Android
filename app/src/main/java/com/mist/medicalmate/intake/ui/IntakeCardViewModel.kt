package com.mist.medicalmate.intake.ui

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
 * 1c-5에서 문답을 카드로 만든다.
 *
 * 카드는 `DRAFT`로 만들어진다. 확정은 진료실에서 보여줄 때 한다. 검증에 걸린 필드가 있어도
 * 만들기 자체는 성공한다. 통째로 실패시키면 환자가 답한 문답이 날아가기 때문이다.
 *
 * 만들어진 id만 흘려보낸다. 카드 내용은 다음 화면이 다시 불러온다. 여기서 들고 가면 그
 * 화면이 두 갈래(넘겨받은 것과 불러온 것)를 다뤄야 한다.
 */
@HiltViewModel
class IntakeCardViewModel
@Inject
internal constructor(private val repository: CardRepository) : ViewModel() {
    private val mutableCreatedCardId = MutableStateFlow<String?>(null)
    val createdCardId: StateFlow<String?> = mutableCreatedCardId.asStateFlow()

    private val mutableFailed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = mutableFailed.asStateFlow()

    fun create(sessionId: Long) {
        if (mutableCreatedCardId.value != null) return
        viewModelScope.launch {
            when (val result = repository.createFromSession(sessionId)) {
                is ApiResult.Success -> mutableCreatedCardId.value = result.value.id
                is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> mutableFailed.value = true
            }
        }
    }
}
