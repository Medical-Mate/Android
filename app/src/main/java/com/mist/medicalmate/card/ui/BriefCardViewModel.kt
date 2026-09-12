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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 브리핑 카드 상태 보유자.
 *
 * `GET /api/cards/{id}`가 카드를 준다. 알러지는 AI가 만드는 값이 아니라 신상정보에 저장된
 * 것이 카드에 실려 온다.
 *
 * 편집은 원본을 두고 사본을 따로 들고 있다가 확인할 때 서버로 보낸다. 취소하면 사본만
 * 버린다. 사본을 고치는 조작은 [editActions]에 있다.
 *
 * **확정한 카드를 고치면 서버가 새 버전을 만든다.** 응답의 `cardId`가 달라지므로 확인 뒤에는
 * 응답으로 온 카드로 갈아탄다. 옛 id를 들고 있으면 다음 수정이 엉뚱한 카드로 간다.
 */
@HiltViewModel
class BriefCardViewModel
@Inject
internal constructor(private val repository: CardRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardUiState>(BriefCardUiState.Loading)
    val uiState: StateFlow<BriefCardUiState> = mutableUiState.asStateFlow()

    /** 편집 모드가 아니면 아무것도 하지 않는다. 사본이 없는데 고칠 수는 없다. */
    val editActions =
        BriefCardEditActions { change ->
            mutableUiState.update { state ->
                val draft = (state as? BriefCardUiState.Content)?.draft ?: return@update state
                state.copy(draft = change(draft))
            }
        }

    /**
     * 카드를 연다. 없으면 문답으로 만든다.
     *
     * 문답을 마치면(1c-5) 아직 카드가 없다. 병원을 먼저 찾고 오는 길과 바로 오는 길이 여기서
     * 만나고, 만드는 자리가 하나라 어느 쪽으로 와도 같은 카드가 나온다.
     *
     * 카드는 `DRAFT`로 만들어진다. 확정은 진료실에서 보여줄 때 한다. 검증에 걸린 필드가
     * 있어도 만들기 자체는 성공한다. 통째로 실패시키면 환자가 답한 문답이 날아간다.
     *
     * [hospital]은 라우트가 들고 온 값이다. 진료 전 병원 찾기(1m-B)에서 고른 것이다.
     * 서버 카드 응답에는 병원이 없다. 목록 응답에만 `clinicName`이 있어서 상세를 열면 그
     * 값을 채울 곳이 없다(#139). 그래서 지금은 방금 고른 것만 얹는다.
     */
    fun open(cardId: Long?, sessionId: Long? = null, hospital: BriefCardHospital? = null) {
        // 이미 만든 카드를 다시 만들지 않는다. 화면이 다시 조합되면 이 호출이 한 번 더 온다.
        if (cardId == null && mutableUiState.value is BriefCardUiState.Content) return
        mutableUiState.value = BriefCardUiState.Loading
        viewModelScope.launch {
            val result =
                when {
                    cardId != null -> repository.card(cardId)
                    sessionId != null -> repository.createFromSession(sessionId)
                    else -> null
                }
            mutableUiState.value =
                when (result) {
                    is ApiResult.Success -> BriefCardUiState.Content(card = result.value.copy(hospital = hospital))
                    else -> BriefCardUiState.Failed
                }
        }
    }

    /**
     * 병원 `변경`에서 골라 돌아왔다.
     *
     * 주소는 함께 오지 않는다. 이름만으로 그 자리를 채우고, 주소는 서버가 카드에 병원을
     * 싣기 시작하면 응답에서 온다(#139).
     */
    fun onHospitalPicked(name: String?) {
        if (name.isNullOrBlank()) return
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) {
                state
            } else {
                state.copy(card = state.card.copy(hospital = BriefCardHospital(name = name)))
            }
        }
    }

    /** Nav 우측 `편집`. 카드 안의 모든 값을 한 번에 연다. */
    fun onEditClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(draft = BriefCardDraft.of(state.card))
        }
    }

    /**
     * Nav 우측 `확인`. 고친 값을 보내고 편집 모드를 닫는다.
     *
     * **응답으로 온 카드로 갈아탄다.** 확정된 카드를 고치면 서버가 새 버전을 만들고 id가
     * 달라진다. 화면이 옛 id를 들고 있으면 다음 수정이 엉뚱한 카드로 간다.
     *
     * **바뀐 축만 보낸다.** PATCH라 보낸 것만 바뀌고, 손대지 않은 축까지 실어 보내면 서버가
     * 그것도 환자가 고친 값(`PATIENT_EDIT`)으로 남긴다.
     *
     * 축에서 오지 않은 줄은 보낼 곳이 없어 건너뛴다. 지금은 그런 줄이 없지만 카드에 축 밖의
     * 값이 생기면 조용히 사라지는 대신 화면에만 남는다.
     */
    fun onEditDoneClick() {
        val state = mutableUiState.value as? BriefCardUiState.Content ?: return
        val draft = state.draft
        val cardId = state.card.id.toLongOrNull()
        if (draft == null || cardId == null) return

        viewModelScope.launch {
            when (val result = repository.update(cardId, state.changedAxes(), draft.questions)) {
                is ApiResult.Success ->
                    mutableUiState.value =
                        BriefCardUiState.Content(
                            card = result.value.copy(items = draft.items, hospital = state.card.hospital),
                        )

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    mutableUiState.value = state.copy(saveFailed = true)
            }
        }
    }

    /**
     * 하단 `진료실에서 보여주기`. 전달 화면으로 가기 전에 카드를 확정한다.
     *
     * 전달 경로는 확정한 카드만 연다. 초안이면 400이다. 이미 확정한 카드를 다시 확정해도
     * 400이므로 그때는 그냥 넘어간다. 확정 상태를 화면이 들고 있어서 다시 부를 일이 없다.
     *
     * [onConfirmed]는 화면 이동이다. 확정에 실패하면 부르지 않는다. 전달 화면이 열리자마자
     * 400으로 비어 버리는 것보다 여기서 멈추는 편이 낫다.
     */
    fun onHandoffClick(onConfirmed: (String) -> Unit) {
        val state = mutableUiState.value as? BriefCardUiState.Content ?: return
        val cardId = state.card.id.toLongOrNull()
        if (cardId == null || state.card.status == BriefCard.Status.CONFIRMED) {
            // 이미 확정했으면 그대로 간다. 다시 확정하면 400이다.
            if (cardId != null) onConfirmed(state.card.id)
            return
        }

        viewModelScope.launch {
            when (val result = repository.confirm(cardId)) {
                is ApiResult.Success -> {
                    mutableUiState.value = state.copy(card = result.value.copy(hospital = state.card.hospital))
                    onConfirmed(result.value.id)
                }

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    mutableUiState.value = state.copy(saveFailed = true)
            }
        }
    }

    /** Nav 우측 `취소`. 사본을 버리고 원래 값으로 돌아간다. */
    fun onCancelClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(draft = null)
        }
    }

    /**
     * 하단 `브리핑 카드 삭제`. 대화상자를 띄우는 것까지만 한다.
     *
     * 개체를 통째로 지우는 것이라 문서가 확인을 필수로 둔다. 안의 항목을 지우는 ×와 다르다.
     */
    fun onDeleteClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(deleteRequested = true)
        }
    }

    fun onDeleteDismiss() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(deleteRequested = false)
        }
    }

    /**
     * 대화상자의 `삭제`. 화면을 떠나는 것은 호출자가 한다.
     *
     * 서버 삭제는 아직 없다. `DELETE /api/cards/{cardId}`를 붙이면 여기서 호출한다. 지금은
     * 대화상자를 닫기만 하고, 화면 이동은 `BriefCardRoute`가 상태를 보고 처리한다.
     */
    /**
     * 대화상자의 `삭제`. `DELETE /api/cards/{cardId}`.
     *
     * 지운 뒤에 화면을 나간다. 지운 카드의 화면에 남을 수 없다. 실패하면 그대로 있는다 —
     * 나가 버리면 안 지워진 카드를 지운 것으로 알게 된다.
     *
     * **문답과 진료 기록이 함께 지워진다.** 되돌릴 수 없다. 확인 대화상자가 그 사실을 적는다.
     */
    fun onDeleteConfirm(onDeleted: () -> Unit) {
        val cardId = (mutableUiState.value as? BriefCardUiState.Content)?.card?.id?.toLongOrNull()
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(deleteRequested = false, draft = null)
        }
        if (cardId == null) return
        viewModelScope.launch {
            if (repository.delete(cardId) is ApiResult.Success) onDeleted()
        }
    }
}
