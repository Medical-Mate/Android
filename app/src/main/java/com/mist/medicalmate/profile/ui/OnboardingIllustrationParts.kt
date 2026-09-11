package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 온보딩 그림의 부품.
 *
 * `OnboardingIllustrations`에서 나눴다. 그림 셋이 각자 부품을 두세 개씩 쓰면서 한 파일에
 * 함수가 열하나가 됐고 detekt의 파일당 상한에 닿았다.
 */

/** 말풍선 왼쪽. AI가 물은 말이다. */
@Composable
internal fun Ask(resId: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
        Text(
            text = "AI",
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgMuted,
        )
        Text(
            text = stringResource(resId),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 말풍선 오른쪽. 환자가 답한 말이라 면이 있다. */
@Composable
internal fun Answer(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MedicalMateTheme.typography.labelS,
        color = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier
            .background(color = MedicalMateTheme.colors.bgPrimarySubtle, shape = MedicalMateRadius.sm)
            .padding(horizontal = MedicalMateSpace.s8, vertical = MedicalMateSpace.s6),
    )
}

/** 오른쪽의 작은 브리핑 카드. 실제 카드(1e-1)의 줄을 셋만 남기고 알러지 경고를 붙인다. */
@Composable
internal fun MiniCard(modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier = modifier
            .background(color = colors.bgSurface, shape = MedicalMateRadius.md)
            .padding(MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        Text(
            text = stringResource(R.string.onboarding_card_mini_title),
            style = MedicalMateTheme.typography.labelM,
            color = colors.fgDefault,
        )
        MiniKv(R.string.onboarding_card_mini_key1, R.string.onboarding_card_mini_value1)
        MiniKv(R.string.onboarding_card_mini_key2, R.string.onboarding_card_mini_value2)
        MiniKv(R.string.onboarding_card_mini_key3, R.string.onboarding_card_mini_value3)
        Text(
            text = stringResource(R.string.onboarding_card_mini_allergy),
            style = MedicalMateTheme.typography.labelS,
            color = colors.fgWarning,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.bgWarning, shape = MedicalMateRadius.sm)
                .padding(horizontal = MedicalMateSpace.s8, vertical = MedicalMateSpace.s6),
        )
    }
}

/** 작은 카드의 한 줄. 키 폭을 고정해 값이 세로로 맞는다. */
@Composable
private fun MiniKv(keyRes: Int, valueRes: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
        Text(
            text = stringResource(keyRes),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgMuted,
            modifier = Modifier.width(MiniKeyWidth),
        )
        Text(
            text = stringResource(valueRes),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgDefault,
        )
    }
}

private val MiniKeyWidth = 30.dp

/** 타임라인 왼쪽의 때. 점과 글자를 한 줄로 둔다. */
@Composable
internal fun TimelineWhen(resId: Int) {
    Row(
        modifier = Modifier.height(TimelineWhenHeight),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ChipDotSize)
                .background(color = MedicalMateTheme.colors.bgPrimary, shape = MedicalMateRadius.full),
        )
        Text(
            text = stringResource(resId),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgPrimary,
        )
    }
}

/** 타임라인 오른쪽의 카드 한 장. */
@Composable
internal fun TimelineCard(titleRes: Int, detailRes: Int) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TimelineWhenHeight)
            .background(color = colors.bgSurface, shape = MedicalMateRadius.md)
            .border(width = TimelineCardBorder, color = colors.borderSubtle, shape = MedicalMateRadius.md)
            .padding(horizontal = MedicalMateSpace.s12, vertical = MedicalMateSpace.s8),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MedicalMateTheme.typography.labelM,
            color = colors.fgDefault,
        )
        Text(
            text = stringResource(detailRes),
            style = MedicalMateTheme.typography.labelS,
            color = colors.fgSubtle,
        )
    }
}

internal val TimelineWhenHeight = 86.dp
