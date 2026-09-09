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
 * 수정은 [VisitRecordUiState.Content.drafts]에 담는다. 원본을 바로 고치면 취소했을 때
 * 되돌릴 것이 없다. 저장하면 초안을 원본으로 옮긴다.
 */
@HiltViewModel
class VisitRecordViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<VisitRecordUiState>(VisitRecordUiState.Loading)
    val uiState: StateFlow<VisitRecordUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = VisitRecordUiState.Content(record = previewVisitRecord)
    }

    fun onEditClick() {
        update { it.copy(editing = true, drafts = it.record.items.map { item -> item.value }) }
    }

    fun onDraftChange(index: Int, value: String) {
        update { content ->
            val drafts = content.drafts.toMutableList()
            if (index !in drafts.indices) return@update content
            drafts[index] = value
            content.copy(drafts = drafts)
        }
    }

    fun onScheduleChange(checked: Boolean) {
        update { it.copy(scheduleRevisit = checked) }
    }

    /**
     * 초안을 원본으로 옮긴다.
     *
     * 읽는 중에 눌렸으면 옮길 것이 없다. 그때 저장하기는 화면을 나가는 조작이고, 나가는
     * 판단은 그래프가 한다.
     */
    fun onSaveClick() {
        update { content ->
            if (!content.editing) return@update content
            val items =
                content.record.items.mapIndexed { index, item ->
                    item.copy(value = content.drafts.getOrNull(index) ?: item.value)
                }
            content.copy(record = content.record.copy(items = items), editing = false, drafts = emptyList())
        }
    }

    fun onCancelClick() {
        update { it.copy(editing = false, drafts = emptyList()) }
    }

    private fun update(transform: (VisitRecordUiState.Content) -> VisitRecordUiState.Content) {
        val content = mutableUiState.value as? VisitRecordUiState.Content ?: return
        mutableUiState.value = transform(content)
    }
}
