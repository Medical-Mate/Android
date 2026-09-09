package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 병원 찾기 상태 보유자.
 *
 * 검색을 여기서 한다. 서버 연동에서는 [onQueryChange]가 `GET /api/hospitals?query=`를 부르고
 * 응답을 [HospitalPickUiState.results]에 넣는다. 좁히는 규칙이 화면이 아니라 여기 있어야
 * JVM에서 확인할 수 있다.
 *
 * 이름과 주소를 함께 본다. "관악"처럼 지역으로 찾는 사람이 있고, 그때 이름만 보면 결과가
 * 비어 버린다.
 */
@HiltViewModel
class HospitalPickViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(HospitalPickUiState())
    val uiState: StateFlow<HospitalPickUiState> = mutableUiState.asStateFlow()

    fun load() {
        mutableUiState.value = HospitalPickUiState(results = previewHospitals)
    }

    /**
     * 검색어가 바뀌면 결과를 다시 좁힌다.
     *
     * 고른 병원이 결과에서 빠지면 선택을 지운다. 보이지 않는 것이 골라져 있으면 완료를
     * 눌렀을 때 무엇이 저장되는지 알 수 없다.
     */
    fun onQueryChange(query: String) {
        val results = previewHospitals.filter { it.matches(query) }
        mutableUiState.update { state ->
            state.copy(
                query = query,
                results = results,
                selectedId = state.selectedId?.takeIf { id -> results.any { it.id == id } },
            )
        }
    }

    fun onHospitalClick(id: String) {
        mutableUiState.update { it.copy(selectedId = id) }
    }
}

private fun Hospital.matches(query: String): Boolean {
    val keyword = query.trim()
    return keyword.isEmpty() || name.contains(keyword) || address.contains(keyword)
}
