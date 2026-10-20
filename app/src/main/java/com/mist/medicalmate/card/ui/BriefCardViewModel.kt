package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.card.data.AxisEdit
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.card.data.latestCardId
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
     * 카드는 만들 때부터 확정돼 있다(Backend#181). 검증에 걸린 필드가 있어도 만들기 자체는 성공한다. 통째로
     * 실패시키면 환자가 답한 문답이 날아간다.
     *
     * **문답에서 막 만들었으면 진료 날짜를 묻는 시트(1e-1-S)를 띄운다**(#338, 사용자 결정). 전에는 처음 "저장하기"를
     * 누를 때 떴는데 저장 단계가 없어지면서 카드를 처음 여는 이때로 옮겼다. 그 카드로 잡은 일정이 있으면 띄우지
     * 않는다. 이미 있는 카드를 열 때는 띄우지 않는다.
     *
     * [hospital]은 진료 전 병원 찾기(1m-B)에서 고른 것이다. **만들 때만 쓴다** — 서버에
     * 함께 보내면 카드가 그 값을 들고, 다음부터는 응답에서 온다(Backend#101). 전에는 카드
     * 응답에 병원이 없어 라우트로 날라 화면에만 얹었고, 캘린더에서 연 카드는 비어 있었다.
     */
    fun open(cardId: Long?, sessionId: Long? = null, hospital: BriefCardHospital? = null) {
        // **한 번 연 카드는 다시 읽지 않는다.** 병원을 고르러 갔다 오면 이 화면의 조합이 다시
        // 시작되면서 이 호출이 한 번 더 온다. 이미 만든 카드를 다시 만들지 않는 것도 같은
        // 이유다.
        //
        // 들고 있는 것과 같은 id일 때만 막으면 모자란다(#219). 카드를 고치면 서버가 새 버전을
        // 만들어 id가 달라지는데 라우트는 누를 때의 옛 id를 그대로 들고 있다. 그 둘을 견주면
        // 다르니 옛 버전을 다시 읽어 오고, 거기서 또 고치면 같은 부모에서 버전이 가지를 친다.
        // 가지에 들어간 편집은 목록이 최신 한 장만 내면서 영영 보이지 않는다(Backend#114).
        //
        // ViewModel이 목적지 엔트리에 묶여 있어 한 화면이 사는 동안 라우트의 id는 바뀌지
        // 않는다. 화면이 든 카드가 그 id에서 나온 최신본이라 다시 읽을 이유가 없다.
        if (mutableUiState.value is BriefCardUiState.Content) return
        mutableUiState.value = BriefCardUiState.Loading
        viewModelScope.launch {
            val result =
                when {
                    cardId != null -> repository.card(cardId)
                    sessionId != null -> repository.createFromSession(sessionId, hospital)
                    else -> null
                }
            val card = (result as? ApiResult.Success)?.value
            mutableUiState.value =
                if (card == null) {
                    BriefCardUiState.Failed
                } else {
                    BriefCardUiState.Content(card = card, scheduleSheet = cardId == null && card.appointment == null)
                }
        }
    }

    /**
     * 병원 `변경`에서 골라 돌아왔다.
     *
     * **서버에 보낸다**(Backend#101). 카드가 병원을 들게 되면서 `PATCH`로 남길 곳이 생겼다.
     * 전에는 화면에만 얹어서 다시 열면 사라졌다.
     *
     * 주소가 함께 온다. 같은 이름의 다른 지점을 가르는 값이라 이름만 남기면 어느 곳을
     * 골랐는지 알 수 없다. 심평원에 주소가 없는 곳은 비어 있고, 그때는 블록이 이름만 그린다.
     *
     * 실패하면 카드는 그대로 둔다. 화면에만 바꿔 두면 다시 열었을 때 되돌아가 있다.
     */
    fun onHospitalPicked(name: String?, address: String?) {
        val state = mutableUiState.value as? BriefCardUiState.Content ?: return
        // 고른 것이 없거나 부를 수 없는 id면 보낼 것이 없다. 화면은 그대로 둔다.
        val cardId = state.card.id.toLongOrNull()?.takeIf { !name.isNullOrBlank() } ?: return
        val picked = BriefCardHospital(name = name.orEmpty(), address = address?.takeIf { it.isNotBlank() })

        viewModelScope.launch {
            when (
                val result = repository.patch(
                    cardId,
                    axes = emptyList(),
                    questions = state.card.questions,
                    clinic = picked,
                )
            ) {
                is ApiResult.Success ->
                    mutableUiState.value = BriefCardUiState.Content(card = result.value)

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    mutableUiState.value = state.copy(saveFailed = true)
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
            when (val result = repository.patch(cardId, state.changedAxes(), draft.questions)) {
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

    /** 떠 있는 것을 닫는다. 삭제 확인 대화상자와 일정 시트(1e-1-S)는 함께 뜨지 않는다. */
    fun onOverlayDismiss() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) {
                state
            } else {
                state.copy(
                    deleteRequested = false,
                    scheduleSheet = false,
                )
            }
        }
    }

    /**
     * 화면에 돌아왔을 때 그 카드로 잡은 일정만 다시 읽는다(#314). 하단의 "진료 일정 잡기"로 일정을 잡고 오면
     * "진료 일정 보기"로 바뀌어야 한다. 고치는 중이면 건드리지 않고, 못 읽으면 그대로 둔다.
     */
    fun refreshSchedule() {
        val state = mutableUiState.value as? BriefCardUiState.Content ?: return
        val cardId = state.card.id.toLongOrNull()?.takeIf { !state.editing } ?: return
        viewModelScope.launch {
            val fresh = (repository.card(cardId) as? ApiResult.Success)?.value ?: return@launch
            mutableUiState.update { current ->
                if (current !is BriefCardUiState.Content || current.card.id != state.card.id) {
                    current
                } else {
                    current.copy(card = current.card.copy(appointment = fresh.appointment))
                }
            }
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

/**
 * 카드를 고친다. 서버가 "이미 고친 카드"라고 막으면 최신 카드로 갈아타고 한 번만 다시
 * 보낸다(Backend#117).
 *
 * 옛 id를 들고 있을 길은 #219에서 막았다. 그래도 못 찾은 경로가 남아 있으면 여기서
 * 드러나고, 조용히 버전이 가지를 치는 대신 고친 값이 최신 카드에 얹힌다.
 *
 * 한 번만 따라간다. 두 번째도 막히면 그대로 실패로 알린다 — 계속 따라가면 어디서 멈출지
 * 알 수 없고, 그 사이에 다른 사람이 고치고 있는 것이라면 덮어쓰기를 반복하게 된다.
 */
private suspend fun CardRepository.patch(
    cardId: Long,
    axes: List<AxisEdit>,
    questions: List<String>,
    clinic: BriefCardHospital? = null,
): ApiResult<BriefCard> {
    val first = update(cardId, axes, questions, clinic)
    val latest = (first as? ApiResult.Rejected)?.latestCardId() ?: return first
    return update(latest, axes, questions, clinic)
}
