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
 * 서버 연동 전이라 [refresh]가 픽스처를 그대로 노출한다. Repository가 들어오면 이 메서드
 * 안이 실제 호출로 바뀌고 화면과 상태 모델은 그대로 쓴다.
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
        /** Figma 1n-1(399:1339)에 그려진 내용 그대로다. 서버 연동 시 삭제한다. */
        val fixture =
            HomeUiState.Content(
                userInitial = "김",
                hasUnreadNotification = true,
                todayLine =
                HomeTodayLine.SinceLastVisit(
                    daysSinceLastVisit = 12,
                    nextVisit = LocalDate.of(2026, 9, 12),
                ),
                resume =
                HomeResume(
                    intakeId = "intake-1",
                    symptomTitle = "복부 통증",
                    answeredSteps = 2,
                    totalSteps = 3,
                ),
                savedCards =
                listOf(
                    SavedCardSummary(
                        id = "card-1",
                        title = "복부 통증 · 3주",
                        status = SavedCardSummary.Status.CONFIRMED,
                        writtenOn = LocalDate.of(2026, 9, 4),
                        clinic = "서울OO병원 내과",
                    ),
                    SavedCardSummary(
                        id = "card-2",
                        title = "두통 · 잦은 어지러움",
                        status = SavedCardSummary.Status.DRAFT,
                        writtenOn = LocalDate.of(2026, 8, 21),
                        clinic = null,
                    ),
                ),
                upcoming =
                listOf(
                    HomeSchedule(
                        id = "visit-1",
                        title = "서울OO병원 내과 재진",
                        date = LocalDate.of(2026, 9, 12),
                        time = "오전 10:30",
                    ),
                ),
            )
    }
}
