package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import java.time.LocalDate

/**
 * 홈 화면 Preview.
 *
 * [HomeScreen]과 같은 파일에 두면 파일 하나에 함수가 11개가 되어 detekt `TooManyFunctions`에
 * 걸린다. Preview는 화면 코드가 아니라 확인 도구라서 이쪽으로 뺐다.
 *
 * [previewToday]를 고정한다. `LocalDate.now()`를 쓰면 D-day가 Preview를 여는 날마다 달라져
 * Figma와 비교할 수 없다.
 */
private val previewToday = LocalDate.of(2026, 9, 7)

private val previewContent =
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
            totalSteps = 4,
        ),
        savedCards =
        listOf(
            SavedCardSummary(
                id = "card-1",
                title = "복부 통증 · 3주",
                visited = true,
                writtenOn = LocalDate.of(2026, 9, 4),
                clinic = "서울OO병원 내과",
            ),
            SavedCardSummary(
                id = "card-2",
                title = "두통 · 잦은 어지러움",
                visited = false,
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

@Composable
private fun HomeScreenPreview(state: HomeUiState) {
    MedicalMateTheme {
        HomeScreen(state = state, today = previewToday, callbacks = HomeCallbacks())
    }
}

@MedicalMateScreenPreviews
@Composable
private fun HomeScreenContentPreview() {
    HomeScreenPreview(previewContent)
}

@Preview(showBackground = true, name = "1n-2 기록 없음", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenEmptyPreview() {
    HomeScreenPreview(
        previewContent.copy(
            todayLine = HomeTodayLine.FirstVisit,
            resume = null,
            savedCards = emptyList(),
            upcoming = emptyList(),
        ),
    )
}

@Preview(showBackground = true, name = "불러오지 못함", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenFailedPreview() {
    HomeScreenPreview(HomeUiState.Failed)
}

/** 시작 버튼만 따로 본다. 마이크 아이콘과 라벨 간격이 문서의 gap 6인지 확인한다. */
@Preview(showBackground = true, widthDp = 390)
@Composable
private fun StartIntakeButtonPreview() {
    MedicalMateTheme {
        Column(modifier = Modifier.padding(MedicalMateSize.gutter)) {
            StartIntakeButton(onClick = {})
            MedicalMateButton(
                onClick = {},
                label = stringResource(R.string.home_retry),
                size = MedicalMateButtonSize.M,
            )
        }
    }
}
