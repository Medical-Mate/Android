package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 브리핑 카드 전체 상태 보유자.
 *
 * 목록이 픽스처다. Figma 1j-4의 내용을 그대로 옮겼다. `GET /api/cards`가 붙으면 [load]가
 * 그 응답을 월별로 묶는다.
 *
 * 편집 조작이 [RecordViewModel]과 같다. 두 화면이 같은 CRUD 규칙을 쓰기 때문이다. 그래도
 * 상태 타입이 달라 합치지 않았다. 서버가 붙으면 각자 다른 엔드포인트를 부른다.
 */
@HiltViewModel
class BriefCardListViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardListUiState>(BriefCardListUiState.Loading)
    val uiState: StateFlow<BriefCardListUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = BriefCardListUiState.Content(groups = previewBriefCardGroups)
    }

    fun onEditStart() {
        updateContent { it.copy(selectedIds = emptySet()) }
    }

    fun onEditCancel() {
        updateContent { it.copy(selectedIds = null, deleteRequested = false) }
    }

    fun onSelectChange(id: String, selected: Boolean) {
        updateContent { content ->
            val ids = content.selectedIds ?: return@updateContent content
            content.copy(selectedIds = if (selected) ids + id else ids - id)
        }
    }

    fun onDeleteClick() {
        updateContent { if (it.selectedCount == 0) it else it.copy(deleteRequested = true) }
    }

    fun onDeleteDismiss() {
        updateContent { it.copy(deleteRequested = false) }
    }

    /**
     * 고른 카드를 지운다.
     *
     * 서버에 나가지 않는다. `DELETE /api/cards`가 붙으면 여기서 부른다. 빈 묶음은 함께
     * 사라지고 편집에서 빠져나온다.
     */
    fun onDeleteConfirm() {
        updateContent { content ->
            val ids = content.selectedIds.orEmpty()
            val groups =
                content.groups
                    .map { group -> group.copy(items = group.items.filterNot { it.id in ids }) }
                    .filter { it.items.isNotEmpty() }
            content.copy(groups = groups, selectedIds = null, deleteRequested = false)
        }
    }

    private fun updateContent(change: (BriefCardListUiState.Content) -> BriefCardListUiState.Content) {
        val content = mutableUiState.value as? BriefCardListUiState.Content ?: return
        mutableUiState.value = change(content)
    }
}

/**
 * Figma 1j-4(`1122:4830`)의 목록. Preview와 픽스처가 함께 쓴다.
 *
 * 1j-1과 같은 카드인데 보조 줄이 다르다. 기록은 진료에서 들은 것을 적고 여기는 카드가 담은
 * 항목을 적는다. 서버 연동 시 삭제한다.
 */
internal val previewBriefCardGroups =
    listOf(
        RecordGroup(
            monthLabel = "2026년 9월",
            items =
            listOf(
                RecordItem(
                    id = "card-1",
                    title = "복부 통증 · 3주",
                    status = RecordItem.Status.CONFIRMED,
                    meta = "09.04 작성 · 서울OO병원 내과",
                    detail = "부위 · 기간 · 양상 · 복용약 · 기저질환",
                ),
                RecordItem(
                    id = "card-3",
                    title = "무릎 통증",
                    status = RecordItem.Status.DRAFT,
                    meta = "오늘 · 4단계 중 2단계",
                    resumeLabel = "이어서 정리하기",
                ),
            ),
        ),
        RecordGroup(
            monthLabel = "2026년 8월",
            items =
            listOf(
                RecordItem(
                    id = "card-2",
                    title = "두통 · 잦은 어지러움",
                    status = RecordItem.Status.BEFORE_VISIT,
                    meta = "08.21 작성 · 병원 미정",
                    detail = "부위 · 기간 · 양상 · 질문 3개",
                ),
            ),
        ),
    )
