package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 브리핑 카드 전체 상태 보유자.
 *
 * `GET /api/me/cards`가 최근 작성 순으로 준다. 월별로 묶는 것은 화면이 할 일이라 여기서
 * 한다. 서버가 묶어 주면 그 기준을 바꿀 때마다 배포해야 한다.
 *
 * 편집 조작이 [RecordViewModel]과 같다. 두 화면이 같은 CRUD 규칙을 쓰기 때문이다. 그래도
 * 상태 타입이 달라 합치지 않았다. 각자 다른 엔드포인트를 부른다.
 */
@HiltViewModel
class BriefCardListViewModel
@Inject
internal constructor(private val repository: CardRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardListUiState>(BriefCardListUiState.Loading)
    val uiState: StateFlow<BriefCardListUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = BriefCardListUiState.Loading
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.cards()) {
                    is ApiResult.Success -> BriefCardListUiState.Content(groups = result.value.toGroups())
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> BriefCardListUiState.Failed
                }
        }
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
        val ids = (mutableUiState.value as? BriefCardListUiState.Content)?.selectedIds.orEmpty()
        if (ids.isEmpty()) return
        updateContent { it.copy(deleteRequested = false) }
        viewModelScope.launch {
            val gone = repository.deleteAll(ids)
            updateContent { content -> content.without(gone).copy(selectedIds = null) }
        }
    }

    private fun updateContent(change: (BriefCardListUiState.Content) -> BriefCardListUiState.Content) {
        val content = mutableUiState.value as? BriefCardListUiState.Content ?: return
        mutableUiState.value = change(content)
    }
}

/**
 * 응답을 월별 묶음으로.
 *
 * 서버가 최근 작성 순으로 주므로 순서를 다시 세우지 않는다. 같은 달끼리 이어 붙이기만 한다.
 */
private fun List<CardListItem>.toGroups(): List<RecordGroup> = groupBy { it.writtenOn.format(MONTH_LABEL) }
    .map { (label, items) -> RecordGroup(monthLabel = label, items = items.map { it.toRow() }) }

/**
 * 목록의 한 줄.
 *
 * 상태가 세 갈래다. 확정 전이면 `DRAFT`, 확정했지만 아직 진료를 안 갔으면 `BEFORE_VISIT`,
 * 진료를 마쳤으면 `CONFIRMED`다. 문서가 "진료 완료" 뱃지는 `visited`로 판단하라고 적었다.
 * 병원명은 선택 입력이라 진료를 마쳤어도 비어 있을 수 있다.
 *
 * 보조 줄([RecordItem.detail])은 비워 둔다. 목록 응답에 본문이 담기지 않아서 카드가 무엇을
 * 담았는지 알 수 없다. 상세를 열어야 나온다.
 */
private fun CardListItem.toRow() = RecordItem(
    id = id,
    title = title,
    status =
    when {
        visited -> RecordItem.Status.CONFIRMED
        confirmed -> RecordItem.Status.BEFORE_VISIT
        else -> RecordItem.Status.DRAFT
    },
    meta = listOfNotNull(writtenOn.format(WRITTEN_ON) + " 작성", clinic).joinToString(" · "),
)

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)

private val WRITTEN_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)

/**
 * 지워진 것만 목록에서 뺀다.
 *
 * 여러 장을 한 번에 지울 때 일부만 실패할 수 있다. 실패한 것을 함께 빼면 지워지지 않은
 * 카드가 지워진 것처럼 보이고, 다시 열었을 때 되살아난 것으로 읽힌다.
 */
private fun BriefCardListUiState.Content.without(ids: Set<String>): BriefCardListUiState.Content = copy(
    groups =
    groups
        .map { group -> group.copy(items = group.items.filterNot { it.id in ids }) }
        .filter { it.items.isNotEmpty() },
)

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
