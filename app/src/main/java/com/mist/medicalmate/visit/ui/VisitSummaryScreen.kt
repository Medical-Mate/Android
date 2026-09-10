package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateHospitalCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateHospitalChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateHospitalChipTone
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
        MedicalMateHospitalCard(
            name = state.hospital.name,
            address = state.hospital.address,
            chips = visitChips(state.hospital),
        )
    }
}

/**
 * 카드 아래 날짜 칩.
 *
 * 진료일은 지난 것, 재방문일은 예정된 것이라 색이 다르다. 색만으로 나누지 않도록 글자도
 * "진료"와 "재방문"으로 다르게 붙인다. 다시 갈 날을 정하지 않았으면 칩이 하나다.
 */
@Composable
private fun visitChips(hospital: HospitalSummary): List<MedicalMateHospitalChip> = buildList {
    add(
        MedicalMateHospitalChip(
            label = stringResource(R.string.visit_summary_chip_visit, hospital.visitDate),
            tone = MedicalMateHospitalChipTone.PAST,
        ),
    )
    hospital.revisitDate?.let { date ->
        add(
            MedicalMateHospitalChip(
                label = stringResource(R.string.visit_summary_chip_revisit, date),
                tone = MedicalMateHospitalChipTone.PLANNED,
            ),
        )
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
