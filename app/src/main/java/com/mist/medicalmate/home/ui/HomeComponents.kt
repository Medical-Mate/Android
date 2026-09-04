package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import java.time.format.DateTimeFormatter

/**
 * 홈 화면을 이루는 조각들. [HomeScreen]에서만 쓴다.
 *
 * 안내 배너는 다른 화면에서도 쓰일 후보라 나중에 `core/designsystem`으로 옮길 수 있다.
 * 그때까지는 홈 안에 둔다.
 */

@Composable
internal fun Greeting(userName: String) {
    Column {
        Text(
            text = stringResource(R.string.home_greeting, userName),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.home_greeting_sub),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
internal fun NoticeBanner(notice: HomeNotice) {
    val formatted = notice.date.format(bannerDate)
    val text =
        when (notice.kind) {
            HomeNotice.Kind.TEST_RESULT -> stringResource(R.string.home_notice_test_result, formatted)
            HomeNotice.Kind.REVISIT -> stringResource(R.string.home_notice_revisit, formatted)
        }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, modifier = Modifier.padding(16.dp))
    }
}

/** 와이어프레임에서 단독 강조되는 주요 행동. */
@Composable
internal fun StartIntakeCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_start_intake_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_start_intake_subtitle),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun SavedCardRow(card: SavedCardSummary, onClick: () -> Unit) {
    val badge =
        when (card.status) {
            SavedCardSummary.Status.CONFIRMED ->
                stringResource(R.string.home_card_written_on, card.writtenOn.format(cardDate))

            SavedCardSummary.Status.DRAFT -> stringResource(R.string.home_card_continue)
        }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = card.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = badge, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * 캘린더(1r)는 MVP 범위가 미확정이고 가족 공유함(1k)은 다음 단계다.
 * 와이어프레임 구성을 유지하려고 자리만 두고 동작은 호출자가 준다.
 */
@Composable
internal fun BottomActions(onCalendarClick: () -> Unit, onFamilyShareClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCalendarClick, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.home_calendar))
        }
        OutlinedButton(onClick = onFamilyShareClick, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.home_family_share))
        }
    }
}

/** 배너용 "9/3" 형식. */
private val bannerDate = DateTimeFormatter.ofPattern("M/d")

/** 카드 뱃지용 "06.20" 형식. */
private val cardDate = DateTimeFormatter.ofPattern("MM.dd")
