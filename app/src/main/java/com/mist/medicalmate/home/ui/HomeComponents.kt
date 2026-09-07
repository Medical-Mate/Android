package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import java.time.format.DateTimeFormatter

/**
 * 홈 화면을 이루는 조각들. [HomeScreen]에서만 쓴다.
 *
 * 안내 배너는 다른 화면에서도 쓰일 후보라 나중에 `core/designsystem`으로 옮길 수 있다.
 * 그때까지는 홈 안에 둔다. 옮길 때 DESIGN.md 8.4의 `Notice`로 맞춘다.
 */

@Composable
internal fun Greeting(userName: String) {
    Column {
        Text(
            text = stringResource(R.string.home_greeting, userName),
            style = MedicalMateTheme.typography.headingS,
        )
        Text(
            text = stringResource(R.string.home_greeting_sub),
            style = MedicalMateTheme.typography.headingM,
        )
    }
}

/**
 * DESIGN.md 8.4의 `Notice` Info 톤에 대응한다. 화면에 남는 인라인 안내다.
 * 제목과 본문을 나누는 구조는 문답 화면에서 Notice를 실제로 만들 때 맞춘다.
 */
@Composable
internal fun NoticeBanner(notice: HomeNotice) {
    val formatted = notice.date.format(bannerDate)
    val text =
        when (notice.kind) {
            HomeNotice.Kind.TEST_RESULT -> stringResource(R.string.home_notice_test_result, formatted)
            HomeNotice.Kind.REVISIT -> stringResource(R.string.home_notice_revisit, formatted)
        }
    Card(
        shape = MedicalMateRadius.md,
        colors = CardDefaults.cardColors(containerColor = MedicalMateTheme.colors.bgInfo),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.padding(MedicalMateSpace.s16),
        )
    }
}

/**
 * 와이어프레임에서 단독 강조되는 주요 행동.
 *
 * DESIGN.md 8.3의 Card는 Brand 강조를 `bg/primary-faint`로 두고 큰 유색 면은 환자
 * 콘텐츠에만 쓰라고 한다. 지금은 `bg/primary`로 채워져 있는데 강조 수준을 낮추는 것은
 * 디자인 판단이라 색은 그대로 두고 토큰만 붙였다.
 */
@Composable
internal fun StartIntakeCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MedicalMateRadius.lg,
        colors =
        CardDefaults.cardColors(
            containerColor = MedicalMateTheme.colors.bgPrimary,
            contentColor = MedicalMateTheme.colors.fgOnPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MedicalMateSpace.s20),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
        ) {
            Text(
                text = stringResource(R.string.home_start_intake_title),
                style = MedicalMateTheme.typography.headingS,
            )
            Text(
                text = stringResource(R.string.home_start_intake_subtitle),
                style = MedicalMateTheme.typography.bodyS,
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
    Card(
        onClick = onClick,
        shape = MedicalMateRadius.lg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(MedicalMateSpace.s16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = card.title, style = MedicalMateTheme.typography.bodyL)
            Text(
                text = badge,
                style = MedicalMateTheme.typography.labelM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
    }
}

/**
 * 캘린더(1r)는 MVP 범위가 미확정이고 가족 공유함(1k)은 다음 단계다.
 * 와이어프레임 구성을 유지하려고 자리만 두고 동작은 호출자가 준다.
 *
 * 규격은 DESIGN.md 8.1의 M Button이다. 높이 48, radius 14다.
 */
@Composable
internal fun BottomActions(onCalendarClick: () -> Unit, onFamilyShareClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
        OutlinedButton(
            onClick = onCalendarClick,
            shape = MedicalMateRadius.buttonM,
            modifier =
            Modifier
                .weight(1f)
                .height(MedicalMateSize.controlMd),
        ) {
            Text(
                text = stringResource(R.string.home_calendar),
                style = MedicalMateTheme.typography.labelL,
            )
        }
        OutlinedButton(
            onClick = onFamilyShareClick,
            shape = MedicalMateRadius.buttonM,
            modifier =
            Modifier
                .weight(1f)
                .height(MedicalMateSize.controlMd),
        ) {
            Text(
                text = stringResource(R.string.home_family_share),
                style = MedicalMateTheme.typography.labelL,
            )
        }
    }
}

/** 배너용 "9/3" 형식. */
private val bannerDate = DateTimeFormatter.ofPattern("M/d")

/** 카드 뱃지용 "06.20" 형식. */
private val cardDate = DateTimeFormatter.ofPattern("MM.dd")
