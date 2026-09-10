package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 자동 분류 결과 상태 보유자.
 *
 * 내용이 픽스처다. AI 연동에서는 [load]가 메모를 보내고 나눈 결과를 받는다.
 *
 * 편집은 [VisitRecordDraft]에 담는다. 원본을 바로 고치면 취소했을 때 되돌릴 것이 없다.
 * 확인하면 사본을 원본으로 옮긴다. 사본을 고치는 조작은 [editActions]에 있다.
 */
@HiltViewModel
class VisitRecordViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<VisitRecordUiState>(VisitRecordUiState.Loading)
    val uiState: StateFlow<VisitRecordUiState> = mutableUiState.asStateFlow()

    /** 편집 모드가 아니면 아무것도 하지 않는다. 사본이 없는데 고칠 수는 없다. */
    val editActions =
        VisitRecordEditActions { change ->
            update { content ->
                val draft = content.draft ?: return@update content
                content.copy(draft = change(draft))
            }
        }

    fun load() {
        mutableUiState.value = VisitRecordUiState.Content(record = previewVisitRecord)
    }

    /** Nav 우측 `편집`. 카드 안의 모든 값을 한 번에 연다. */
    fun onEditClick() {
        update { it.copy(draft = VisitRecordDraft.of(it.record)) }
    }

    /**
     * Nav 우측 `확인`. 사본을 원본으로 옮기고 편집 모드를 닫는다.
     *
     * 서버 저장은 아직 없다. AI 분류 결과를 저장하는 API를 붙이면 여기서 호출한다.
     */
    fun onEditDoneClick() {
        update { content ->
            val draft = content.draft ?: return@update content
            content.copy(record = content.record.copy(items = draft.items), draft = null)
        }
    }

    /** Nav 우측 `취소`. 사본을 버리고 원래 값으로 돌아간다. */
    fun onCancelClick() {
        update { it.copy(draft = null) }
    }

    fun onScheduleChange(checked: Boolean) {
        update { it.copy(scheduleRevisit = checked) }
    }

    /**
     * 읽는 중의 저장하기. 화면을 나가는 조작이고 나가는 판단은 그래프가 한다.
     *
     * 편집 중에는 하단에 이 버튼이 없다. 그 자리가 삭제이고 사본을 옮기는 것은 `확인`이 한다.
     */
    fun onSaveClick() = Unit

    /**
     * 하단 `진료 후 기록 삭제`. 대화상자를 띄우는 것까지만 한다.
     *
     * 개체를 통째로 지우는 것이라 문서가 확인을 필수로 둔다. 원문 메모까지 사라지는 자리다.
     */
    fun onDeleteClick() {
        update { it.copy(deleteRequested = true) }
    }

    fun onDeleteDismiss() {
        update { it.copy(deleteRequested = false) }
    }

    /**
     * 대화상자의 `삭제`. 화면을 떠나는 것은 호출자가 한다.
     *
     * 서버 삭제는 아직 없다. 지금은 대화상자를 닫고 편집 모드를 내린다.
     */
    fun onDeleteConfirm() {
        update { it.copy(deleteRequested = false, draft = null) }
    }

    private fun update(transform: (VisitRecordUiState.Content) -> VisitRecordUiState.Content) {
        val content = mutableUiState.value as? VisitRecordUiState.Content ?: return
        mutableUiState.value = transform(content)
    }
}
