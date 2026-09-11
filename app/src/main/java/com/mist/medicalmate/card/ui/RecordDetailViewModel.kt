package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기록 상세 상태 보유자.
 *
 * 내용이 픽스처다([recordDetailFixtures]). `GET /api/visits/{visitId}`가 붙으면 [load]가 그
 * 응답을 단계 목록으로 바꾼다. 카드와 진료 후 기록이 서로 다른 표현으로 오므로 한
 * 타임라인으로 세우는 일이 필요하고, 그 일이 화면이 아니라 여기 있어야 JVM에서 확인할 수
 * 있다.
 *
 * 모르는 id는 [RecordDetailUiState.Failed]다. 목록에서 들어오는 경로만 있어서 지금은 나지
 * 않지만, 서버 연동에서 삭제된 기록의 링크로 들어오는 경우가 이 상태가 된다.
 */
@HiltViewModel
class RecordDetailViewModel
@Inject
constructor() : ViewModel() {
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

    fun load(recordId: String) {
        val detail = recordDetailFixtures[recordId]
        mutableUiState.value =
            if (detail == null) RecordDetailUiState.Failed else RecordDetailUiState.Content(detail)
        mutableExpanded.value = emptySet()
    }

    fun onExpandToggle(index: Int) {
        mutableExpanded.value =
            mutableExpanded.value.let { if (index in it) it - index else it + index }
    }
}
