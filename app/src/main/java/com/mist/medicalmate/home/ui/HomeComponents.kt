package com.mist.medicalmate.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateScheduleCard
import com.mist.medicalmate.core.designsystem.ddayLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * 홈(1n-1 · 1n-2)을 이루는 조각(#312). 화면 틀은 `HomeScreen.kt`, 맨 위 배너는 `HomeBanners.kt`에 있다.
 */

/**
 * 배너 아래 흰 시트. 배너 위로 28 겹쳐 올라오고(1n-1의 프레임 간격 −28) 위 모서리가 28이다. `Elevation/Sheet`를
 * 두 겹 그대로 건다. 안쪽 여백은 위 28 · 좌우 20 · 아래 20이고 블록 간격은 12다.
 */
@Composable
internal fun HomeSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MedicalMateRadius.sheetTop
    Column(
        modifier =
        modifier
            .overlapUp(SheetOverlap)
            .fillMaxWidth()
            .dropShadow(shape, SheetShadowWide)
            .dropShadow(shape, SheetShadowTight)
            .clip(shape)
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = SheetPaddingTop,
                bottom = MedicalMateSize.gutter,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        content = content,
    )
}

/**
 * 최근 브리핑 카드 묶음. 카드 한 장(반경 20 · `Elevation/Card`) 안에 줄을 담고 `Divider`로 가른다(1n-1). 줄의
 * 배지는 브리핑 카드 전체(1j-4)와 같은 규칙이다(사용자 결정).
 */
@Composable
internal fun BriefingList(cards: List<SavedCardSummary>, onCardClick: (String) -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .dropShadow(MedicalMateRadius.lg, CardShadow)
            .clip(MedicalMateRadius.lg)
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        cards.forEachIndexed { index, card ->
            if (index > 0) MedicalMateDivider()
            val written = card.writtenOn.format(cardDate)
            MedicalMateListRow(
                title = card.title,
                meta =
                card.clinic?.let { stringResource(R.string.home_card_meta, written, it) }
                    ?: stringResource(R.string.home_card_meta_draft, written),
                badge = stringResource(card.status.label()),
                badgeTone = card.status.tone(),
                type = MedicalMateListRowType.BADGE,
                onClick = { onCardClick(card.id) },
                floating = false,
            )
        }
    }
}

/** 다가오는 일정 한 장. Figma `Card / Schedule`. 제목은 "내과 재진", 메타는 "서울OO병원 · 금 오전 10:30"이다. */
@Composable
internal fun ScheduleCard(schedule: HomeSchedule, today: LocalDate, onClick: () -> Unit) {
    val visit =
        stringResource(if (schedule.followUp) R.string.home_schedule_follow_up else R.string.home_schedule_first)
    val time = schedule.time?.format(bannerTime) ?: stringResource(R.string.calendar_time_unset)
    val whenText = "${schedule.date.format(weekday)} $time"
    MedicalMateScheduleCard(
        month = stringResource(R.string.home_schedule_month, schedule.date.monthValue),
        day = schedule.date.dayOfMonth.toString(),
        meta = schedule.clinic?.let { stringResource(R.string.home_schedule_meta, it, whenText) } ?: whenText,
        title = schedule.department?.let { stringResource(R.string.home_schedule_title, it, visit) } ?: visit,
        badge = ddayLabel(daysUntil(today, schedule.date)),
        onClick = onClick,
    )
}

/** 남은 일수. 컴포저블 밖에 둬서 JVM 테스트로 확인한다. */
internal fun daysUntil(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

private fun HomeCardStatus.label(): Int = when (this) {
    HomeCardStatus.DRAFT -> R.string.record_status_draft
    HomeCardStatus.BEFORE_VISIT -> R.string.record_status_before_visit
    HomeCardStatus.VISITED -> R.string.record_status_confirmed
}

/** 브리핑 카드 전체(1j-4)와 같은 톤. 작성 중 Warning · 진료 전 Brand · 진료 완료 Success. */
private fun HomeCardStatus.tone(): MedicalMateBadgeTone = when (this) {
    HomeCardStatus.DRAFT -> MedicalMateBadgeTone.WARNING
    HomeCardStatus.BEFORE_VISIT -> MedicalMateBadgeTone.BRAND
    HomeCardStatus.VISITED -> MedicalMateBadgeTone.SUCCESS
}

/**
 * 위로 [by]만큼 겹쳐 올린다. 차지하는 높이도 그만큼 줄여 아래가 비지 않는다. 높이가 정해진 자리(처음 홈의
 * 남은 높이)에서는 그만큼 더 크게 그려 바닥까지 채운다.
 */
private fun Modifier.overlapUp(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val grown =
        if (constraints.hasBoundedHeight) {
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = constraints.minHeight + extra,
                maxHeight = constraints.maxHeight + extra,
            )
        } else {
            constraints
        }
    val placeable = measurable.measure(grown)
    layout(placeable.width, (placeable.height - extra).coerceAtLeast(0)) { placeable.place(0, -extra) }
}

private val SheetOverlap = 28.dp
private val SheetPaddingTop = 28.dp

/** `Elevation/Sheet` 두 겹. 위로 4 · blur 24 · 10%, 제자리 blur 2 · 5%. */
private val SheetShadowWide =
    Shadow(radius = 24.dp, color = ShadowTint.copy(alpha = 0.10f), offset = DpOffset(0.dp, (-4).dp))
private val SheetShadowTight = Shadow(radius = 2.dp, color = ShadowTint.copy(alpha = 0.05f))

/** 최근 브리핑 카드 묶음의 `Elevation/Card`. 앞의 겹(y3 blur10 8%)을 쓴다. */
private val CardShadow =
    Shadow(radius = 10.dp, color = ShadowTint.copy(alpha = 0.08f), offset = DpOffset(0.dp, MedicalMateElevation.card))

/** 카드 메타용 "2026.09.18". */
private val cardDate = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** "오전 10:30". 로케일을 고정한다 — 기기 언어가 한국어가 아니면 AM/PM이 섞인다. */
internal val bannerTime = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/** 일정 카드의 요일 "금". */
private val weekday = DateTimeFormatter.ofPattern("E", Locale.KOREAN)
