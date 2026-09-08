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
 * 수정은 원본을 두고 초안을 따로 들고 있다가 저장할 때 옮긴다. 취소하면 초안만 버린다.
 */
@HiltViewModel
class BriefCardViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardUiState>(BriefCardUiState.Loading)
    val uiState: StateFlow<BriefCardUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = BriefCardUiState.Content(card = fixture)
    }

    /** 수정 아이콘. 카드 안의 모든 값을 한 번에 연다. */
    fun onEditClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) {
                state
            } else {
                state.copy(editing = true, drafts = state.card.items.map { it.value })
            }
        }
    }

    fun onDraftChange(index: Int, value: String) {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content || index !in state.drafts.indices) {
                state
            } else {
                state.copy(drafts = state.drafts.toMutableList().also { it[index] = value })
            }
        }
    }

    /**
     * 저장. 초안을 카드에 옮기고 수정 모드를 닫는다.
     *
     * 서버 저장은 아직 없다. `PATCH /api/cards/{cardId}`를 붙이면 여기서 호출하고 실패
     * 상태를 하나 더 둔다.
     */
    fun onSaveClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content || !state.editing) {
                state
            } else {
                val items =
                    state.card.items.mapIndexed { index, item ->
                        item.copy(value = state.draftAt(index))
                    }
                state.copy(card = state.card.copy(items = items), editing = false, drafts = emptyList())
            }
        }
    }

    /** 취소. 초안을 버리고 원래 값으로 돌아간다. */
    fun onCancelClick() {
        mutableUiState.update { state ->
            if (state !is BriefCardUiState.Content) state else state.copy(editing = false, drafts = emptyList())
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
            )
    }
}
