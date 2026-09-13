package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.VisitListItem
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 기록 탭 상태 보유자.
 *
 * `GET /api/me/visits`를 월별로 묶는다. 묶는 일이 화면이 아니라 여기 있어야 JVM에서 확인할
 * 수 있다.
 *
 * 삭제는 `DELETE /api/visits/{id}`다(#178). 카드는 남는다.
 */
@HiltViewModel
class RecordViewModel
@Inject
internal constructor(private val repository: VisitRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RecordUiState>(RecordUiState.Loading)
    val uiState: StateFlow<RecordUiState> = mutableUiState.asStateFlow()

    /**
     * 진료 기록을 읽는다.
     *
     * 서버가 최근 진료일 순으로 준다. 월별 묶음은 화면이 만든다. 서버가 묶어 주면 그 기준을
     * 바꿀 때마다 배포해야 한다.
     *
     * 브리핑 카드는 여기 섞지 않는다. 서버가 둘을 다른 경로로 주고, 카드 목록은 1j-4가 따로
     * 보여준다.
     */
    fun load() {
        mutableUiState.value = RecordUiState.Loading
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.visits()) {
                    is ApiResult.Success -> RecordUiState.Content(groups = result.value.toGroups())
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> RecordUiState.Failed
                }
        }
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
     * **지우는 것은 기록이다.** `DELETE /api/visits/{visitId}`이고 카드는 남는다. 전에는 이
     * 자리가 없어 카드 삭제로 나갔고, 기록 한 건을 지우려던 사람이 카드와 문답까지 잃었다.
     *
     * 지운 뒤에는 편집을 빠져나온다. 고른 것이 사라졌는데 편집 상태로 남으면 무엇을 더 하라는
     * 것인지 알 수 없다.
     */
    fun onDeleteConfirm() {
        val ids = (mutableUiState.value as? RecordUiState.Content)?.selectedIds.orEmpty()
        if (ids.isEmpty()) return
        updateContent { it.copy(deleteRequested = false) }
        viewModelScope.launch {
            val gone = repository.deleteAll(ids)
            updateContent { state -> state.without(gone).copy(selectedIds = null) }
        }
    }

    private fun updateContent(change: (RecordUiState.Content) -> RecordUiState.Content) {
        val content = mutableUiState.value as? RecordUiState.Content ?: return
        mutableUiState.value = change(content)
    }
}

/** 응답을 월별 묶음으로. 서버가 최근 순으로 주므로 순서를 다시 세우지 않는다. */
/**
 * 지워진 것만 목록에서 뺀다.
 *
 * 여러 건을 한 번에 지울 때 일부만 실패할 수 있다. 실패한 것을 함께 빼면 지워지지 않은
 * 기록이 지워진 것처럼 보이고, 다시 열었을 때 되살아난 것으로 읽힌다.
 */
private fun RecordUiState.Content.without(ids: Set<String>): RecordUiState.Content = copy(
    groups =
    groups
        .map { group -> group.copy(items = group.items.filterNot { it.id in ids }) }
        .filter { it.items.isNotEmpty() },
)

private fun List<VisitListItem>.toGroups(): List<RecordGroup> = groupBy { it.visitedOn.format(MONTH_LABEL) }
    .map { (label, items) -> RecordGroup(monthLabel = label, items = items.map { it.toRow() }) }

/**
 * 목록의 한 줄.
 *
 * 기록이 있다는 것은 진료를 다녀왔다는 뜻이라 상태가 하나다. 보조 줄은 비운다. 목록 응답에
 * 원문이 담기지 않아 무엇을 들었는지 알 수 없고, 상세를 열어야 나온다.
 */
private fun VisitListItem.toRow() = RecordItem(
    id = id,
    cardId = cardId?.toString(),
    title = cardTitle,
    status = RecordItem.Status.CONFIRMED,
    meta = listOfNotNull(visitedOn.format(VISITED_ON) + " 진료", clinic).joinToString(" · "),
)

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)

private val VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)

/**
 * Figma 1j-1(406:2569)의 목록. Preview가 쓴다.
 *
 * 서버 응답으로는 진료를 다녀온 건만 와서 상태가 하나다. 작성 중·진료 전까지 섞인 목록을
 * 볼 수 있는 자리가 Preview뿐이라 남긴다.
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
