package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 브리핑 카드 상태 보유자.
 *
 * **카드 내용이 픽스처다.** 실제로는 문답에서 주고받은 말을 AI가 카드 모양으로 정리해서
 * 넘겨준다. 지금은 Figma 1e-1의 내용을 그 모양으로 채워 뒀고, 연동하면 [load]가 그
 * 응답을 받는다. 화면과 상태 모델은 그대로 쓴다.
 *
 * 알러지는 AI가 만드는 값이 아니다. 신상정보에 저장된 것이 올라온다. 지금은 픽스처에
 * 함께 들어 있고, 연동하면 프로필 조회에서 온다.
 *
 * 편집은 원본을 두고 사본을 따로 들고 있다가 확인할 때 옮긴다. 취소하면 사본만 버린다.
 * 사본을 고치는 조작은 [editActions]에 있다.
 */
@HiltViewModel
class BriefCardViewModel
@Inject
constructor() : ViewModel() {
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

    fun load() {
        mutableUiState.value = BriefCardUiState.Content(card = fixture)
    }

    /** Nav 우측 `편집`. 카드 안의 모든 값을 한 번에 연다. */
    fun onEditClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(draft = BriefCardDraft.of(state.card))
        }
    }

    /**
     * Nav 우측 `확인`. 사본을 카드에 옮기고 편집 모드를 닫는다.
     *
     * 서버 저장은 아직 없다. `PATCH /api/cards/{cardId}`를 붙이면 여기서 호출하고 실패
     * 상태를 하나 더 둔다.
     */
    fun onEditDoneClick() {
        mutableUiState.update { state ->
            val draft = (state as? BriefCardUiState.Content)?.draft ?: return@update state
            state.copy(
                card = state.card.copy(items = draft.items, questions = draft.questions),
                draft = null,
            )
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
    fun onDeleteConfirm() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(deleteRequested = false, draft = null)
        }
    }

    private companion object {
        /** Figma 1e-1(404:1679)의 내용. AI 응답이 들어오면 삭제한다. */
        val fixture =
            BriefCard(
                id = "card-1",
                title = "복부 통증 · 3주",
                status = BriefCard.Status.BEFORE_VISIT,
                patientLine = "김OO · 32세 여 · 2026.09.04 작성",
                items =
                listOf(
                    BriefCardItem(key = "부위", value = "복부 (명치 아래 · 배꼽 위)"),
                    BriefCardItem(key = "기간", value = "3주 전 시작 · 최근 악화", emphasized = true),
                    BriefCardItem(key = "양상", value = "식후 30분 뒤 쓰림 · 밤에 심해짐"),
                    BriefCardItem(key = "복용약", value = "혈압약 · 진통제(증상 시)"),
                    BriefCardItem(key = "기저질환", value = "고혈압"),
                ),
                severity = MedicalMateSeverity.LEVEL_3,
                allergies = listOf("페니실린"),
                questions =
                listOf(
                    "검사를 받아야 하나요?",
                    "지금 진통제 계속 먹어도 되나요?",
                    "어떤 증상이면 바로 다시 와야 하나요?",
                ),
                hospital =
                BriefCardHospital(
                    name = "서울OO병원 내과",
                    address = "서울 관악구 남부순환로 1820, 3층",
                ),
            )
    }
}
