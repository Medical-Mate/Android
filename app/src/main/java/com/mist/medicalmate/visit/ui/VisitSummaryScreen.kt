package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateNoticeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1k. Figma `405:2322`.
 *
 * 저장이 끝났다는 것과, 이번 진료가 지난 진료와 어떻게 다른지를 알린다. 기록이 한 건씩
 * 쌓이는 것만으로는 나아지고 있는지 알 수 없어서 두 건을 나란히 둔다.
 *
 * 병원은 다음에 다시 갈 곳이라 주소와 다음 방문일을 함께 남긴다.
 */
@Composable
fun VisitSummaryScreen(
    state: VisitSummaryUiState,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_summary_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        SummaryContent(state)
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.visit_summary_home),
                onClick = onHomeClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.SummaryContent(state: VisitSummaryUiState) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
    ) {
        MedicalMateNotice(
            title = stringResource(R.string.visit_summary_notice_title),
            body = stringResource(R.string.visit_summary_notice_body),
            tone = MedicalMateNoticeTone.SUCCESS,
        )
        state.previous?.let { previous ->
            MedicalMateSectionHeader(title = stringResource(R.string.visit_summary_compare))
            Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10)) {
                CompareCard(card = previous, emphasis = MedicalMateCardEmphasis.QUIET)
                CompareCard(card = state.current, emphasis = MedicalMateCardEmphasis.BRAND)
            }
        }
        MedicalMateSectionHeader(title = stringResource(R.string.visit_summary_hospital))
        HospitalCard(state.hospital)
    }
}

/**
 * 비교 카드.
 *
 * 지난 진료는 조용한 면, 이번 진료는 브랜드 면이다. 나란히 두었을 때 어느 쪽이 지금
 * 이야기인지 색으로 먼저 읽힌다.
 */
@Composable
private fun RowScope.CompareCard(card: VisitCompareCard, emphasis: MedicalMateCardEmphasis) {
    MedicalMateCard(emphasis = emphasis, modifier = Modifier.weight(1f)) {
        Text(
            text = card.label,
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Text(
            text = card.title,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = card.detail,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 병원 카드. Figma `640:3914`.
 *
 * 시안의 위쪽은 병원 사진이다. 서버에서 올 이미지라 자리만 잡고 아이콘을 둔다. 사진이
 * 없는 병원도 있어서 이 자리는 어차피 대체 표시가 필요하다. 자리 색은 시안이 사진 위에
 * 깔아 둔 `bg/primary-faint`를 그대로 쓴다.
 *
 * `Card` 컴포넌트를 쓰지 않는다. 사진이 카드 끝까지 닿아야 해서 안쪽 여백을 줄 수 없다.
 * 층은 같은 `Elevation/Card`로 준다.
 */
@Composable
private fun HospitalCard(hospital: HospitalSummary) {
    Surface(
        shape = MedicalMateRadius.lg,
        color = MedicalMateTheme.colors.bgSurface,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier =
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HospitalImageHeight)
                    .background(MedicalMateTheme.colors.bgPrimaryFaint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Hospital),
                    contentDescription = null,
                    tint = MedicalMateTheme.colors.fgMuted,
                    modifier = Modifier.size(HospitalIconSize),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MedicalMateSpace.s16),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                Text(
                    text = hospital.name,
                    style = MedicalMateTheme.typography.headingS,
                    color = MedicalMateTheme.colors.fgDefault,
                )
                Text(
                    text = hospital.address,
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
                Text(
                    text = hospital.visitLine,
                    style = MedicalMateTheme.typography.labelM,
                    color = MedicalMateTheme.colors.fgDefault,
                )
            }
        }
    }
}

/** 시안의 사진 자리. 콘텐츠 폭 320에 높이 121이다. */
private val HospitalImageHeight = 121.dp

private val HospitalIconSize = 40.dp

@MedicalMateScreenPreviews
@Composable
private fun VisitSummaryScreenPreview() {
    MedicalMateTheme {
        VisitSummaryScreen(
            state = previewVisitSummary,
            onHomeClick = {},
            onBackClick = {},
        )
    }
}
