package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitListItem
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 1k 진료 정리 상태 보유자.
 *
 * 1q-1의 저장이 끝나고 서버가 매긴 기록 id를 들고 들어오는 자리다. 그 id로 방금 저장된
 * 것을 다시 읽는다. 저장할 때 보낸 값을 그대로 그리지 않는 이유는, 그러면 서버가 무엇을
 * 받았는지가 아니라 앱이 무엇을 보냈다고 생각하는지를 보여주기 때문이다.
 *
 * **견주는 대상은 직전 진료 전체다.** 같은 카드의 이전 기록이 아니다. 카드 하나에 기록
 * 하나라 그런 것이 있을 수 없다. 목록이 최근 진료일 순으로 오므로 이번 것 바로 다음 줄이
 * 직전 진료다.
 */
@HiltViewModel
class VisitSummaryViewModel
@Inject
internal constructor(private val repository: VisitRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<VisitSummaryUiState>(VisitSummaryUiState.Loading)
    val uiState: StateFlow<VisitSummaryUiState> = mutableUiState.asStateFlow()

    /**
     * 기록 하나와 그 앞의 것을 읽는다.
     *
     * 상세를 못 읽으면 실패다. 병원도 진료 내용도 그 응답에서 나온다.
     *
     * 목록만 못 읽으면 비교를 빼고 나머지를 그린다. 견줄 것이 없어도 저장된 내용은 보여줄
     * 수 있고, 여기까지 온 사람이 확인하려는 것은 자기가 방금 남긴 기록이다.
     */
    fun load(visitId: String) {
        val id = visitId.toLongOrNull()
        if (id == null) {
            mutableUiState.value = VisitSummaryUiState.Failed
            return
        }
        mutableUiState.value = VisitSummaryUiState.Loading
        viewModelScope.launch {
            val visit = (repository.visit(id) as? ApiResult.Success)?.value
            if (visit == null) {
                mutableUiState.value = VisitSummaryUiState.Failed
                return@launch
            }
            val list = (repository.visits() as? ApiResult.Success)?.value.orEmpty()
            mutableUiState.value = VisitSummaryUiState.Content(
                compare = comparison(id, visit, list),
                hospital = visit.clinic?.let { name ->
                    HospitalSummary(name = name, visitedOn = visit.visitedOn ?: list.dateOf(id))
                },
            )
        }
    }

    /**
     * 이번 것과 직전 것을 한 덩이로.
     *
     * 목록에 이번 기록이 없으면 견줄 자리를 셈할 수 없다. 지워졌거나 목록을 못 읽은
     * 경우이고, 둘 다 비교를 빼고 지나간다.
     */
    private suspend fun comparison(id: Long, visit: Visit, list: List<VisitListItem>): VisitComparison? {
        val index = list.indexOfFirst { it.id == id.toString() }
        val current = list.getOrNull(index)
        val previous = list.getOrNull(index + 1)
        if (current == null || previous == null) return null
        val previousDetail = (repository.visit(previous.id.toLong()) as? ApiResult.Success)?.value
        return VisitComparison(
            previous = previous.toCard(previousDetail),
            current = current.toCard(visit),
        )
    }

    private fun List<VisitListItem>.dateOf(id: Long) = firstOrNull { it.id == id.toString() }?.visitedOn
}

/**
 * 목록 한 줄과 그 상세를 카드 한 장으로.
 *
 * 제목은 목록에만 있고(`cardTitle`) 내용은 상세에만 있다. 둘을 따로 읽어 합친다.
 *
 * 상세를 못 읽었으면 제목만 있는 카드다. 이름과 날짜는 이미 있으니 카드를 통째로 빼는 것보다
 * 낫다.
 */
private fun VisitListItem.toCard(detail: Visit?) = VisitCompareCard(
    visitedOn = visitedOn,
    title = cardTitle.ifBlank { clinic.orEmpty() },
    detail = detail?.summaryLine(),
)

/**
 * 처방·결과·한 것 중 적힌 것을 잇는다.
 *
 * 두 장이 나란히 놓여 한 장의 폭이 화면 절반이다. 셋을 다 이으면 카드가 길어져 견주기
 * 어려워지므로 앞의 둘에서 멈춘다. 차례는 시안이 "진통제 처방 · 경과 관찰"로 처방을 먼저
 * 둔 것을 따랐다.
 */
private fun Visit.summaryLine(): String? = listOfNotNull(prescription, result, whatWasDone)
    .mapNotNull { it.takeIf(String::isNotBlank) }
    .take(SUMMARY_PARTS)
    .joinToString(" · ")
    .takeIf { it.isNotBlank() }

private const val SUMMARY_PARTS = 2
