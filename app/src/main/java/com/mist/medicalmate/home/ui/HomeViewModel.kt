package com.mist.medicalmate.home.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * 홈 화면 상태 보유자.
 *
 * 서버 연동 전이라 [refresh]가 픽스처를 그대로 노출한다. Repository가 들어오면
 * 이 메서드 안이 실제 호출로 바뀌고 화면과 상태 모델은 그대로 쓴다.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    fun refresh() {
        mutableUiState.value = HomeUiState.Loading
        mutableUiState.value = fixture
    }

    private companion object {
        /** 와이어프레임 1n에 그려진 내용 그대로다. 서버 연동 시 삭제한다. */
        val fixture =
            HomeUiState.Content(
                userName = "서연",
                notice =
                HomeNotice(
                    date = LocalDate.of(2026, 9, 3),
                    kind = HomeNotice.Kind.TEST_RESULT,
                ),
                savedCards =
                listOf(
                    SavedCardSummary(
                        id = "card-1",
                        title = "손가락 경직·부종",
                        status = SavedCardSummary.Status.CONFIRMED,
                        writtenOn = LocalDate.of(2026, 6, 20),
                    ),
                    SavedCardSummary(
                        id = "card-2",
                        title = "재방문·검사 결과",
                        status = SavedCardSummary.Status.DRAFT,
                        writtenOn = LocalDate.of(2026, 9, 1),
                    ),
                ),
            )
    }
}
