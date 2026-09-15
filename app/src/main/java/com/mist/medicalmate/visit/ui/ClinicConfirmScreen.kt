package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 와이어프레임 1m-12. Figma `1576:8517`.
 *
 * 일정 상세의 "진료 후 기록하기"를 누르면 먼저 오는 화면이다(#246). 병원은 일정을 추가할 때
 * 이미 등록해 두고 진료 후 기록은 그 일정에서만 쓸 수 있는데, 전에는 그 병원을 아는 채로
 * 병원 찾기(1m)를 첫 화면으로 띄워 매번 다시 찾게 했다. 여기서 등록한 병원이 맞는지만 묻고,
 * 맞으면 바로 메모(1p)로, 다른 곳에서 진료받았으면 1m으로 간다.
 *
 * 병원 줄은 1m의 고른 결과 줄과 같은 모양이다 — 시안이 그 줄을 그대로 가져다 두었다. 눌리지
 * 않는다. 고르는 자리가 아니라 확인하는 자리라 CTA 둘이 답이다.
 *
 * "다른 병원이에요"는 Ghost 버튼이다. 시안이 왼쪽에 맞춰 두었고 채움 없는 글자라 CTA와
 * 무게가 다르다 — 보통은 맞는 병원이고 바꾸는 쪽이 예외다.
 */
@Composable
fun ClinicConfirmScreen(
    clinic: String,
    scheduledOn: LocalDate,
    onConfirmClick: () -> Unit,
    onOtherClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    address: String? = null,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_note_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        ConfirmContent(
            clinic = clinic,
            address = address,
            scheduledOn = scheduledOn,
            onOtherClick = onOtherClick,
        )
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.clinic_confirm_submit),
                onClick = onConfirmClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.ConfirmContent(
    clinic: String,
    address: String?,
    scheduledOn: LocalDate,
    onOtherClick: () -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20),
    ) {
        Column(
            modifier = Modifier.padding(top = MedicalMateSpace.s8),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        ) {
            Text(
                text = stringResource(R.string.clinic_confirm_question),
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
            )
            Text(
                text = stringResource(R.string.clinic_confirm_description, scheduledOn.format(ScheduledOn)),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        Column {
            ClinicRow(clinic = clinic, address = address)
            MedicalMateDivider()
        }
        // 시안은 글자가 콘텐츠 왼쪽 끝에 붙는다. 버튼의 좌우 여백 20을 그만큼 왼쪽으로 물려 글자를
        // 그 자리에 두고 터치 영역은 그대로 남긴다 — 버튼 컴포넌트의 여백을 건드리지 않는다.
        MedicalMateButton(
            label = stringResource(R.string.clinic_confirm_other),
            onClick = onOtherClick,
            type = MedicalMateButtonType.GHOST,
            size = MedicalMateButtonSize.M,
            modifier = Modifier.offset(x = -MedicalMateSpace.s20),
        )
    }
}

/**
 * 등록해 둔 병원 한 줄. 1m의 고른 결과 줄(`I1576:8525;335:1110`)이다.
 *
 * 주소는 같은 이름의 다른 지점을 구별하는 값이라 이름 아래에 붙인다. 일정 응답에는 주소가
 * 없어서 카드에 남은 것을 받고, 그것도 없으면 줄을 그리지 않는다 — 빈 줄이 남으면 주소가
 * 없는 병원이 아니라 주소가 빈 병원으로 읽힌다.
 */
@Composable
private fun ClinicRow(clinic: String, address: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        Text(
            text = clinic,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgPrimary,
        )
        address?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
    }
}

/** 시안의 "9월 12일 일정". */
private val ScheduledOn: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

@MedicalMateScreenPreviews
@Composable
private fun ClinicConfirmScreenPreview() {
    MedicalMateTheme {
        ClinicConfirmScreen(
            clinic = "서울OO병원 내과",
            address = "서울 관악구 남부순환로 1820, 3층",
            scheduledOn = LocalDate.of(2026, 9, 12),
            onConfirmClick = {},
            onOtherClick = {},
            onBackClick = {},
        )
    }
}
