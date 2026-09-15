package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.AXIS_FOLLOW_UP
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
 * 저장한 진료 후 기록 하나를 읽는다. `GET /api/visits/{id}`.
 *
 * 1q-1과 같은 카드를 그리지만 상태 보유자는 따로다(#256). 1q-1의 [VisitRecordViewModel]은
 * 메모를 나눠 저장하는 흐름이라 편집·저장·삭제·재방문 일정까지 들고 있는데, 여기는 이미
 * 저장한 것을 다시 보는 자리라 그중 아무것도 하지 않는다. **저장한 기록은 고치지 않는다**(#235).
 * 그 상태 보유자를 재사용하면 편집으로 들어갈 길이 화면에 남는다.
 *
 * 화면 상태는 [VisitRecordUiState]를 같이 쓴다. 카드 컴포저블이 그 타입을 받고, 여기서는
 * `Content`에 사본이 없어 늘 읽기 모드다.
 */
@HiltViewModel
class VisitDetailViewModel
@Inject
internal constructor(private val repository: VisitRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<VisitRecordUiState>(VisitRecordUiState.Loading)
    val uiState: StateFlow<VisitRecordUiState> = mutableUiState.asStateFlow()

    fun load(visitId: String) {
        val id = visitId.toLongOrNull()
        if (id == null) {
            mutableUiState.value = VisitRecordUiState.Failed
            return
        }
        mutableUiState.value = VisitRecordUiState.Loading
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.visit(id)) {
                    is ApiResult.Success -> VisitRecordUiState.Content(record = result.value.toRecord())
                    else -> VisitRecordUiState.Failed
                }
        }
    }
}

/**
 * 서버의 기록을 카드가 그리는 모양으로.
 *
 * 머리줄은 1q-1과 같은 `병원 · 2026.09.12`다. [VisitRecord.classifiedCount]는 비운다 — "AI가
 * 메모를 4가지로 나눴어요"는 나눈 그 자리에서 하는 말이고, 저장한 뒤 다시 보는 자리에서는
 * 항목이 곧 결과다. 원문 메모가 없으면([Visit.rawNote]) 카드가 그 블록을 그리지 않는다.
 */
internal fun Visit.toRecord() = VisitRecord(
    id = id,
    clinic = clinic,
    clinicLine = listOfNotNull(clinic, visitedOn?.format(DETAIL_VISITED_ON)).joinToString(" · "),
    items =
    items.map { item ->
        VisitRecordItem(
            key = item.label,
            value = item.value,
            tone = if (item.axis == AXIS_FOLLOW_UP) VisitRecordItem.Tone.LINK else VisitRecordItem.Tone.DEFAULT,
            axis = item.axis,
        )
    },
    memo = rawNote.orEmpty(),
    patientNotes = patientNotes,
    followUp = followUp,
)

private val DETAIL_VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
