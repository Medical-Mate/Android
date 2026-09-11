package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기록 탭 상태 보유자.
 *
 * 목록이 픽스처다. Figma 1j-1의 내용을 그대로 옮겼다. `GET /api/cards`가 붙으면 [load]가
 * 그 응답을 월별로 묶는다. 묶는 일이 화면이 아니라 여기 있어야 JVM에서 확인할 수 있다.
 */
@HiltViewModel
class RecordViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<RecordUiState>(RecordUiState.Loading)
    val uiState: StateFlow<RecordUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = RecordUiState.Content(groups = previewRecordGroups)
    }

    /** 편집으로 들어간다. 아무것도 고르지 않은 채로 시작한다. */
    fun onEditStart() {
        updateContent { it.copy(selectedIds = emptySet()) }
    }

    /** 편집에서 나온다. 고른 것은 버린다. 취소가 실행 취소를 대신한다. */
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
     * 고른 기록을 지운다.
     *
     * 서버에 나가지 않는다. `DELETE /api/cards`가 붙으면 여기서 부른다. 지운 뒤에는 편집을
     * 빠져나온다. 고른 것이 사라졌는데 편집 상태로 남으면 무엇을 더 하라는 것인지 알 수 없다.
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

    private fun updateContent(change: (RecordUiState.Content) -> RecordUiState.Content) {
        val content = mutableUiState.value as? RecordUiState.Content ?: return
        mutableUiState.value = change(content)
    }
}

/**
 * Figma 1j-1(406:2569)의 목록. Preview와 픽스처가 함께 쓴다.
 *
 * 서버 연동 시 삭제한다. 상태 셋이 모두 들어 있어서 배지와 아래 줄이 상태마다 다르게
 * 나오는 것을 한 화면에서 볼 수 있다.
 */
internal val previewRecordGroups =
    listOf(
        RecordGroup(
            monthLabel = "2026년 9월",
            items =
            listOf(
                RecordItem(
                    id = "card-3",
                    title = "무릎 통증",
                    status = RecordItem.Status.DRAFT,
                    meta = "오늘 · 증상 문답 4단계 중 2단계",
                    resumeLabel = "이어서 정리하기",
                ),
                RecordItem(
                    id = "card-2",
                    title = "두통 · 잦은 어지러움",
                    status = RecordItem.Status.BEFORE_VISIT,
                    meta = "08.21 작성 · 병원 미정",
                    detail = "묻고 싶은 것 3개 · 통증 2단계",
                ),
                RecordItem(
                    id = "card-1",
                    title = "복부 통증 · 3주",
                    status = RecordItem.Status.CONFIRMED,
                    meta = "09.12 진료 · 서울OO병원 내과",
                    detail = "위염 초기 · 2주 약 · 09.26 재방문",
                ),
            ),
        ),
        RecordGroup(
            monthLabel = "2026년 7월",
            items =
            listOf(
                RecordItem(
                    id = "card-0",
                    title = "목 통증 · 삼킬 때 아픔",
                    status = RecordItem.Status.CONFIRMED,
                    meta = "07.18 진료 · OO이비인후과",
                    detail = "인후염 · 5일 약 · 재방문 없음",
                ),
            ),
        ),
    )
