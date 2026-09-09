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

    fun load(recordId: String) {
        val detail = recordDetailFixtures[recordId]
        mutableUiState.value =
            if (detail == null) RecordDetailUiState.Failed else RecordDetailUiState.Content(detail)
    }
}
