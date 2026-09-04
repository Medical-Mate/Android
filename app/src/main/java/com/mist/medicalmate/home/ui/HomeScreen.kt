package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import java.time.LocalDate

/**
 * 와이어프레임 1n. 색상과 간격은 디자인 시스템이 들어오면 맞춘다.
 * 지금은 구성과 상태 분기만 세운다.
 *
 * 화면을 이루는 조각들은 [HomeComponents.kt]에 있다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartIntakeClick: () -> Unit,
    onSavedCardClick: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onFamilyShareClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> LoadingContent(modifier)
        HomeUiState.Failed -> FailedContent(onRetryClick = onRetryClick, modifier = modifier)
        is HomeUiState.Content ->
            HomeContent(
                content = state,
                onStartIntakeClick = onStartIntakeClick,
                onSavedCardClick = onSavedCardClick,
                onCalendarClick = onCalendarClick,
                onFamilyShareClick = onFamilyShareClick,
                modifier = modifier,
            )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FailedContent(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.home_failed))
        Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) {
            Text(text = stringResource(R.string.home_retry))
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onStartIntakeClick: () -> Unit,
    onSavedCardClick: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onFamilyShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Greeting(userName = content.userName) }

        content.notice?.let { notice ->
            item { NoticeBanner(notice = notice) }
        }

        item { StartIntakeCard(onClick = onStartIntakeClick) }

        item {
            Text(
                text = stringResource(R.string.home_saved_cards),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        if (content.savedCards.isEmpty()) {
            item { Text(text = stringResource(R.string.home_saved_cards_empty)) }
        } else {
            items(items = content.savedCards, key = { it.id }) { card ->
                SavedCardRow(card = card, onClick = { onSavedCardClick(card.id) })
            }
        }

        item {
            BottomActions(
                onCalendarClick = onCalendarClick,
                onFamilyShareClick = onFamilyShareClick,
            )
        }
    }
}

private val previewContent =
    HomeUiState.Content(
        userName = "서연",
        notice = HomeNotice(LocalDate.of(2026, 9, 3), HomeNotice.Kind.TEST_RESULT),
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

@Composable
private fun HomeScreenPreview(state: HomeUiState) {
    MedicalMateTheme {
        HomeScreen(
            state = state,
            onStartIntakeClick = {},
            onSavedCardClick = {},
            onCalendarClick = {},
            onFamilyShareClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenContentPreview() {
    HomeScreenPreview(previewContent)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    HomeScreenPreview(
        HomeUiState.Content(userName = "서연", notice = null, savedCards = emptyList()),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenFailedPreview() {
    HomeScreenPreview(HomeUiState.Failed)
}
