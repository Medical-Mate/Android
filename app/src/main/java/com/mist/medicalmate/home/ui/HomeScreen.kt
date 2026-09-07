package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
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
    accountActions: AccountActionCallbacks = AccountActionCallbacks(),
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
                accountActions = accountActions,
                modifier = modifier,
            )
    }
}

/**
 * 계정 관련 임시 동작. 와이어프레임에 없는 개발용 진입점이라 한 덩어리로 묶어
 * 기본값을 준다. 설정 화면이 생기면 이 파라미터는 사라진다.
 *
 * 홈이 `auth` 도메인을 직접 참조하지 않도록 콜백만 받는다. 실제 수행은
 * `SessionViewModel`이 하고 `MainActivity`가 연결한다.
 */
data class AccountActionCallbacks(
    val enabled: Boolean = true,
    val onLogoutClick: () -> Unit = {},
    val onWithdrawClick: () -> Unit = {},
)

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
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_failed),
            style = MedicalMateTheme.typography.bodyL,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Button(
            onClick = onRetryClick,
            shape = MedicalMateRadius.buttonM,
            modifier =
            Modifier
                .padding(top = MedicalMateSpace.s12)
                .height(MedicalMateSize.controlMd),
        ) {
            Text(
                text = stringResource(R.string.home_retry),
                style = MedicalMateTheme.typography.labelL,
            )
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
    accountActions: AccountActionCallbacks,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        item { Greeting(userName = content.userName) }

        content.notice?.let { notice ->
            item { NoticeBanner(notice = notice) }
        }

        item { StartIntakeCard(onClick = onStartIntakeClick) }

        item {
            Text(
                text = stringResource(R.string.home_saved_cards),
                // DESIGN.md 3절이 카드·리스트 그룹 제목을 Heading/S로 규정한다.
                style = MedicalMateTheme.typography.headingS,
            )
        }

        if (content.savedCards.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_saved_cards_empty),
                    style = MedicalMateTheme.typography.bodyM,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
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

        item {
            AccountActions(
                enabled = accountActions.enabled,
                onLogoutClick = accountActions.onLogoutClick,
                onWithdrawClick = accountActions.onWithdrawClick,
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
