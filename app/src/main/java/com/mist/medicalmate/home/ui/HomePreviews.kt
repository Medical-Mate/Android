package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.model.IntakeStep
import java.time.LocalDate
import java.time.LocalTime

/**
 * 홈 화면 Preview.
 *
 * [HomeScreen]과 같은 파일에 두면 파일 하나에 함수가 너무 많아 detekt `TooManyFunctions`에 걸린다. Preview는
 * 화면 코드가 아니라 확인 도구라서 이쪽으로 뺐다.
 *
 * [previewToday]를 고정한다. `LocalDate.now()`를 쓰면 D-day가 Preview를 여는 날마다 달라져 Figma와 비교할 수
 * 없다. 값은 시안 1n-1-4의 더미 데이터다.
 */
private val previewToday = LocalDate.of(2026, 9, 18)

private val previewContent =
    HomeUiState.Content(
        userInitial = "김",
        hasUnreadNotification = false,
        banners =
        listOf(
            HomeBanner.Tomorrow(time = LocalTime.of(10, 30), clinic = "서울OO병원 내과", cardId = "card-1"),
            HomeBanner.Message,
            HomeBanner.Schedule,
            HomeBanner.SaveCard("card-1"),
            HomeBanner.Mate,
        ),
        resume = HomeResume(intakeId = "intake-1", symptomTitle = "두통", step = IntakeStep.SYMPTOM_CHAT),
        savedCards =
        listOf(
            SavedCardSummary(
                id = "card-1",
                title = "복부 통증 · 3주",
                status = HomeCardStatus.VISITED,
                writtenOn = LocalDate.of(2026, 9, 18),
                clinic = "서울OO병원 내과",
            ),
            SavedCardSummary(
                id = "card-2",
                title = "두통 · 잦은 어지러움",
                status = HomeCardStatus.BEFORE_VISIT,
                writtenOn = LocalDate.of(2026, 8, 21),
                clinic = null,
            ),
        ),
        upcoming =
        HomeSchedule(
            id = "visit-1",
            date = LocalDate.of(2026, 9, 25),
            time = LocalTime.of(10, 30),
            clinic = "서울OO병원",
            department = "내과",
            followUp = true,
        ),
    )

@Composable
private fun HomeScreenPreview(state: HomeUiState) {
    MedicalMateTheme {
        HomeScreen(state = state, today = previewToday, callbacks = HomeCallbacks())
    }
}

/** 1n-1-4. 내일 진료가 있는 홈. */
@MedicalMateScreenPreviews
@Composable
private fun HomeScreenContentPreview() {
    HomeScreenPreview(previewContent)
}

/** 1n-2. 처음 온 사람. */
@Preview(showBackground = true, name = "1n-2 처음", widthDp = 360, heightDp = 812)
@Composable
private fun HomeScreenFirstPreview() {
    HomeScreenPreview(
        previewContent.copy(
            banners = listOf(HomeBanner.Welcome),
            resume = null,
            savedCards = emptyList(),
            upcoming = null,
        ),
    )
}

@Preview(showBackground = true, name = "불러오지 못함", widthDp = 360, heightDp = 812)
@Composable
private fun HomeScreenFailedPreview() {
    HomeScreenPreview(HomeUiState.Failed)
}
