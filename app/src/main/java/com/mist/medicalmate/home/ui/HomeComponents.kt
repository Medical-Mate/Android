package com.mist.medicalmate.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateAvatar
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRowType
import com.mist.medicalmate.core.model.IntakeStep
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 홈 화면을 이루는 조각들. [HomeScreen]에서만 쓴다.
 *
 * 모두 디자인 시스템 컴포넌트를 조립한 것이다. 홈에서만 쓰는 배치라 여기 둔다.
 */

/**
 * Figma Header(`399:1342`) 350x52.
 *
 * 로고가 마스터의 0.8배다. 로그인 화면은 36으로 쓰고 홈은 28.8이다. Figma 인스턴스를
 * 축소한 값이라 반올림하지 않았다.
 *
 * 알림과 아바타는 hit area 48을 유지한다. 아바타의 시각 크기는 36이라 바깥 `Box`가
 * 터치 목표를 확보한다.
 *
 * 알림의 접근성 이름이 읽지 않은 알림 유무에 따라 바뀐다. 아이콘 위 점만으로는 스크린
 * 리더에 전달되지 않는다.
 */
@Composable
internal fun HomeHeader(
    userInitial: String,
    hasUnreadNotification: Boolean,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(MedicalMateLogo.Lockup),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(HeaderLockupHeight),
        )
        Box(modifier = Modifier.weight(1f))
        MedicalMateIconButton(
            onClick = onNotificationClick,
            icon = MedicalMateIcons.Bell,
            contentDescription =
            stringResource(
                if (hasUnreadNotification) {
                    R.string.home_notification_unread
                } else {
                    R.string.home_notification
                },
            ),
        )
        Box(
            modifier =
            Modifier.sizeIn(
                minWidth = MedicalMateSize.touchMin,
                minHeight = MedicalMateSize.touchMin,
            ),
            contentAlignment = Alignment.Center,
        ) {
            MedicalMateAvatar(
                initial = userInitial,
                contentDescription = stringResource(R.string.home_profile),
                size = HeaderAvatarSize,
                onClick = onProfileClick,
            )
        }
    }
}

/**
 * Figma의 "오늘의 한 줄" 카드(`399:1634`) 350x140.
 *
 * `Card`의 Brand 강조를 쓴다. 문서의 컴포넌트 규격이 큰 유색 면을 환자 콘텐츠에만 쓰라고 하는데,
 * 이 카드가 담는 것은 환자의 진료 흐름이다.
 *
 * 문구를 [HomeTodayLine] 갈래별로 조립한다. 첫 방문과 재방문의 문장이 아예 달라서
 * 한 문구에 값만 끼워 넣을 수 없다.
 */
@Composable
internal fun TodayLineCard(todayLine: HomeTodayLine) {
    MedicalMateCard(emphasis = MedicalMateCardEmphasis.BRAND) {
        Text(
            text = stringResource(todayLine.labelRes()),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Text(text = todayLine.title(), style = MedicalMateTheme.typography.headingS)
        Text(
            text = todayLine.body(),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

private fun HomeTodayLine.labelRes(): Int = when (this) {
    HomeTodayLine.FirstVisit -> R.string.home_today_first_label
    is HomeTodayLine.SinceLastVisit -> R.string.home_today_since_label
}

@Composable
private fun HomeTodayLine.title(): String = when (this) {
    HomeTodayLine.FirstVisit -> stringResource(R.string.home_today_first_title)
    is HomeTodayLine.SinceLastVisit ->
        stringResource(R.string.home_today_since_title, daysSinceLastVisit)
}

@Composable
private fun HomeTodayLine.body(): String = when (this) {
    HomeTodayLine.FirstVisit -> stringResource(R.string.home_today_first_body)
    is HomeTodayLine.SinceLastVisit ->
        nextVisit?.let {
            stringResource(R.string.home_today_since_body_next, it.format(nextVisitDate))
        } ?: stringResource(R.string.home_today_since_body)
}

/** Figma의 시작 버튼(`399:1638`) 350x56. 마이크 아이콘이 붙는다. */
@Composable
internal fun StartIntakeButton(onClick: () -> Unit) {
    MedicalMateButton(
        onClick = onClick,
        label = stringResource(R.string.home_start_intake),
        leadingIcon = MedicalMateIcons.Mic,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Figma의 "이어서 하기" 카드(`399:1642`) 350x116.
 *
 * 눌러서 이어 쓸 수 있으므로 카드에 [onClick]을 준다.
 */
@Composable
internal fun ResumeCard(resume: HomeResume, onClick: () -> Unit) {
    MedicalMateCard(onClick = onClick) {
        Text(
            text = stringResource(R.string.home_resume_label),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Text(
            text = stringResource(R.string.home_resume_title),
            style = MedicalMateTheme.typography.headingS,
        )
        Text(
            text =
            stringResource(
                R.string.home_resume_progress,
                resume.symptomTitle,
                IntakeStep.total,
                resume.step.number,
            ),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 저장된 브리핑 카드 한 줄.
 *
 * 진료를 다녀온 카드는 상태 배지와 병원·진료과를, 아직인 카드는 카드만 작성됐다는 메타를
 * 보여준다. Figma `1n-1`의 두 행이 각각 그 경우다.
 *
 * 배지는 카드의 확정 여부가 아니라 진료를 다녀왔는지로 붙인다. 확정만 하고 아직 안 간 카드에
 * "진료 완료"가 붙으면 안 된다.
 */
@Composable
internal fun SavedCardRow(card: SavedCardSummary, onClick: () -> Unit) {
    val visited = card.visited
    val written = card.writtenOn.format(cardDate)

    MedicalMateListRow(
        title = card.title,
        meta =
        card.clinic?.let { stringResource(R.string.home_card_meta, written, it) }
            ?: stringResource(R.string.home_card_meta_draft, written),
        badge = if (visited) stringResource(R.string.home_card_confirmed) else null,
        badgeTone = MedicalMateBadgeTone.SUCCESS,
        type =
        if (visited) MedicalMateListRowType.BADGE else MedicalMateListRowType.DEFAULT,
        onClick = onClick,
    )
}

/**
 * 다가오는 일정 한 줄.
 *
 * D-day를 [today]에서 계산한다. 화면이 열린 날이 기준이어야 하므로 호출자가 넘긴다.
 * 값을 미리 만들어 두면 날짜가 바뀐 뒤에도 옛 값이 남는다.
 */
@Composable
internal fun ScheduleRow(schedule: HomeSchedule, today: LocalDate, onClick: () -> Unit) {
    MedicalMateListRow(
        title = schedule.title,
        meta =
        stringResource(
            R.string.home_schedule_meta,
            schedule.date.format(scheduleDate),
            schedule.time ?: stringResource(R.string.calendar_time_unset),
        ),
        badge = stringResource(R.string.home_schedule_dday, daysUntil(today, schedule.date)),
        badgeTone = MedicalMateBadgeTone.BRAND,
        type = MedicalMateListRowType.BADGE,
        onClick = onClick,
    )
}

/** 남은 일수. 컴포저블 밖에 둬서 JVM 테스트로 확인한다. */
internal fun daysUntil(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

private val HeaderHeight = 52.dp

/** Figma 인스턴스가 마스터(139x36)의 0.8배다. */
private val HeaderLockupHeight = 28.8.dp

/** Figma 인스턴스가 마스터(44)보다 작다. hit area는 바깥에서 48을 확보한다. */
private val HeaderAvatarSize = 36.dp

/** 카드 메타용 "2026.09.04" 형식. */
private val cardDate = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** 일정용 "9월 12일 (금)" 형식. */
private val scheduleDate = DateTimeFormatter.ofPattern("M월 d일 (E)")

/** 오늘의 한 줄에 쓰는 "9월 12일" 형식. */
private val nextVisitDate = DateTimeFormatter.ofPattern("M월 d일")
