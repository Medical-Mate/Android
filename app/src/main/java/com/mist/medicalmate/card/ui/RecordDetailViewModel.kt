package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.Visit
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
 * 기록 상세 상태 보유자.
 *
 * `GET /api/visits/{visitId}`를 단계 목록으로 바꾼다. 그 일이 화면이 아니라 여기 있어야
 * JVM에서 확인할 수 있다.
 *
 * 숫자가 아닌 id는 부르기 전에 [RecordDetailUiState.Failed]다. 목록에서 들어오는 경로만
 * 있어서 지금은 나지 않지만, 지워진 기록의 링크로 들어오는 경우가 이 상태가 된다.
 *
 * 시안의 여러 단계 타임라인(1j-3-X·1j-3-R)은 [recordDetailFixtures]에 남아 Preview가 그린다.
 */
@HiltViewModel
class RecordDetailViewModel
@Inject
internal constructor(private val repository: VisitRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RecordDetailUiState>(RecordDetailUiState.Loading)
    val uiState: StateFlow<RecordDetailUiState> = mutableUiState.asStateFlow()

    private val mutableExpanded = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * 펼친 단계의 자리 번호.
     *
     * 화면 상태라 ViewModel이 든다. 단계 안에 두면 목록이 다시 만들어질 때마다 접힌다.
     * 단계에 id가 없어서 자리 번호로 가리킨다. 목록이 서버에서 바뀌면 번호가 어긋날 수
     * 있는데, 그때는 화면을 다시 여는 것과 같아서 접힌 채로 시작하는 편이 맞는다.
     */
    val expandedSteps: StateFlow<Set<Int>> = mutableExpanded.asStateFlow()

    /**
     * 기록 하나를 읽는다.
     *
     * **타임라인이 한 단계다.** 시안의 상세는 증상 정리 → 카드 → 진료 → 다음으로 이어지는데,
     * `GET /api/visits/{id}`가 주는 것은 진료에서 들은 것뿐이다. 카드 단계를 채우려면 그
     * 카드를 따로 읽어야 하고 그것은 다음 묶음이다(#148).
     */
    fun load(recordId: String) {
        val visitId = recordId.toLongOrNull()
        if (visitId == null) {
            mutableUiState.value = RecordDetailUiState.Failed
            return
        }
        mutableUiState.value = RecordDetailUiState.Loading
        mutableExpanded.value = emptySet()
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.visit(visitId)) {
                    is ApiResult.Success -> RecordDetailUiState.Content(result.value.toDetail())
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> RecordDetailUiState.Failed
                }
        }
    }

    fun onExpandToggle(index: Int) {
        mutableExpanded.value =
            mutableExpanded.value.let { if (index in it) it - index else it + index }
    }
}

/**
 * 기록을 상세 타임라인으로.
 *
 * 서버가 한 것·결과·처방 셋으로 고정해 준다. 값이 없는 줄은 만들지 않는다. 환자가 적지 않은
 * 것을 빈 줄로 남기면 무엇을 안 적었는지가 아니라 무엇이 비었는지로 읽힌다.
 *
 * 원문은 정리된 항목 아래에 그대로 남긴다. AI가 나눈 것과 환자가 말한 것을 가르는 자리다.
 */
private fun Visit.toDetail(): RecordDetail {
    val items =
        listOfNotNull(
            whatWasDone?.takeIf { it.isNotBlank() }?.let { RecordDetailItem(key = "한 것", value = it) },
            result?.takeIf { it.isNotBlank() }?.let { RecordDetailItem(key = "결과", value = it) },
            prescription?.takeIf { it.isNotBlank() }?.let { RecordDetailItem(key = "처방", value = it) },
        )
    val day = visitedOn?.format(VISITED_ON).orEmpty()
    return RecordDetail(
        id = id,
        title = clinic.orEmpty(),
        status = RecordItem.Status.CONFIRMED,
        clinicLine = listOfNotNull(visitedOn?.format(CLINIC_LINE), clinic).joinToString(" · "),
        steps =
        listOf(
            RecordStep.Block(
                at = "$day · 진료 후 기록",
                title = "진료에서 들은 것",
                items = items,
                quote = rawNote?.takeIf { it.isNotBlank() }?.let { RecordQuote(label = "내가 적은 그대로", text = it) },
            ),
        ),
    )
}

private val VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)

private val CLINIC_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
