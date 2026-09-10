package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1c-5. Figma `1041:3655`.
 *
 * 문답 네 단계를 마친 뒤 갈라지는 자리다. 바로 카드를 만들 수도 있고, 진료받을 병원을 먼저
 * 찾을 수도 있다.
 *
 * **문답과 카드 사이에 이 화면을 끼운 이유가 있다.** 전에는 마지막 단계에서 곧바로 카드로
 * 넘어갔다. 그러면 병원을 고르는 길이 없고, 네 단계를 답한 것이 끝났다는 것도 알려주지
 * 않는다. 진행 표시(`4 / 4`)는 마지막 답을 마치면 사라지므로 끝났다는 신호가 남지 않는다.
 *
 * 진행 표시를 두지 않는다. 문답이 아니라 문답이 끝난 뒤의 화면이라 `5 / 4`가 될 곳이 없다.
 *
 * 병원 찾기가 보조 버튼이다. 시안이 카드 만들기를 Primary로 뒀다. 병원은 진료 후에도 등록할
 * 수 있어서 여기서 반드시 정해야 하는 값이 아니다.
 */
@Composable
fun IntakeDoneScreen(
    onCardClick: () -> Unit,
    onHospitalClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.intake_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        DoneMessage(modifier = Modifier.weight(1f))
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.intake_done_card),
                onClick = onCardClick,
                modifier = Modifier.fillMaxWidth(),
            )
            MedicalMateButton(
                label = stringResource(R.string.intake_done_hospital),
                onClick = onHospitalClick,
                type = MedicalMateButtonType.OUTLINE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 아이콘 원과 두 줄. 화면 가운데에 놓인다. */
@Composable
private fun DoneMessage(modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MedicalMateSize.gutter),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
            Modifier
                .size(IconCircleSize)
                .background(color = colors.bgPrimaryFaint, shape = MedicalMateRadius.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(MedicalMateIcons.CheckCircle),
                contentDescription = null,
                tint = colors.fgPrimary,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = stringResource(R.string.intake_done_title),
            style = MedicalMateTheme.typography.headingL,
            color = colors.fgDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.intake_done_description),
            style = MedicalMateTheme.typography.bodyM,
            color = colors.fgSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 시안(`1041:3663`)의 아이콘 원 80. 안쪽 아이콘은 38이다. */
private val IconCircleSize = 80.dp

/** 38은 아이콘 크기 토큰에 없는 값이다. 이 화면에만 쓰여서 여기 둔다. */
private val IconSize = 38.dp

@MedicalMateScreenPreviews
@Composable
private fun IntakeDoneScreenPreview() {
    MedicalMateTheme {
        IntakeDoneScreen(onCardClick = {}, onHospitalClick = {}, onBackClick = {})
    }
}
