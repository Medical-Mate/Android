package com.mist.medicalmate.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton

/**
 * 와이어프레임 1a-2. Figma `397:1233`.
 *
 * 로그인 직후 온보딩이 필요한 사용자에게 보여준다. 무엇을 하는 앱인지와 문답이 어떻게
 * 흘러가는지를 세 단계로 알린다.
 *
 * [onStartClick]의 목적지는 1b-1 신상정보다. 그 화면이 아직 없어서 호출자가 홈으로
 * 보낸다(#63).
 *
 * 본문에 스크롤을 준 이유는 Figma 캔버스(844)보다 짧은 화면이 있기 때문이다. 일러스트
 * 260에 단계 셋까지 고정 높이가 많아 작은 화면에서 잘린다.
 */
@Composable
fun OnboardingIntroScreen(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MedicalMateSize.gutter,
                    end = MedicalMateSize.gutter,
                    top = MedicalMateSpace.s40,
                    bottom = MedicalMateSpace.s16,
                ),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s24),
        ) {
            Illustration()
            Copy()
            Steps()
        }
        Footer(onStartClick = onStartClick)
    }
}

/**
 * 일러스트 자리. Figma `397:1240` 350x260.
 *
 * 그림이 아직 없어서 옅은 판 위에 로고 심볼만 놓는다. Figma도 같은 상태다. 삽화가 오면
 * 이 판 안을 바꾼다.
 */
@Composable
private fun Illustration() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(IllustrationHeight)
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.xl),
        contentAlignment = Alignment.Center,
    ) {
        SubtleSymbol(size = IllustrationSymbolSize)
    }
}

/**
 * 옅은 면 위의 로고 심볼. Figma `351:1281`의 Subtle 변형이다.
 *
 * 드로어블로 따로 두지 않는다. `ic_logo_symbol`은 `primary/600` 판에 흰 마크가 박혀 있고,
 * Subtle은 판이 `bg/primary-subtle`, 마크가 `fg/primary`인 같은 도형이다. 토큰 두 개로
 * 조립하면 같은 그림이 나오고 색이 디자인 시스템에 남는다.
 *
 * 판의 반경이 크기의 1/3이다. Figma에서 105.6에 35.2, 마스터 48에 16으로 같은 비율이다.
 */
@Composable
private fun SubtleSymbol(size: Dp) {
    Box(
        modifier =
        Modifier
            .size(size)
            .background(
                color = MedicalMateTheme.colors.bgPrimarySubtle,
                shape = RoundedCornerShape(size / SYMBOL_RADIUS_DIVISOR),
            ),
    ) {
        Image(
            painter = painterResource(MedicalMateLogo.Mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MedicalMateTheme.colors.fgPrimary),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 제목과 설명. Figma `397:1246`. 제목은 두 줄로 끊어 둔 문구다. */
@Composable
private fun Copy() {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = stringResource(R.string.onboarding_description),
            style = MedicalMateTheme.typography.bodyL,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 문답 흐름 세 단계. Figma `397:1249`. */
@Composable
private fun Steps() {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10)) {
        StepRow(number = 1, textRes = R.string.onboarding_step_1)
        StepRow(number = 2, textRes = R.string.onboarding_step_2)
        StepRow(number = 3, textRes = R.string.onboarding_step_3)
    }
}

/**
 * 단계 한 줄. 번호는 [MedicalMateBadge]의 Brand 톤이다.
 *
 * 번호를 배지로 두는 것은 Figma가 배지 인스턴스를 쓰기 때문이다. 읽는 표시라서 칩이
 * 아니라 배지가 맞다.
 *
 * 번호를 위에 맞춘다. 좁은 화면이나 큰 글꼴에서 문구가 두 줄이 되면 가운데 정렬은 번호를
 * 줄 사이로 내려보낸다. 몇 번째 단계인지가 첫 줄과 붙어 있어야 읽힌다.
 */
@Composable
private fun StepRow(number: Int, @StringRes textRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.Top,
    ) {
        MedicalMateBadge(label = number.toString(), tone = MedicalMateBadgeTone.BRAND)
        Text(
            text = stringResource(textRes),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 하단 버튼. Figma `397:1262`.
 *
 * Figma의 Footer 높이가 143인데 여기서는 내용 높이로 둔다. 그 프레임은 버튼을 아래로
 * 붙여 둬서 화면 밑에서 24 띄운 결과가 같고, 남는 공간은 본문 끝에 생긴다.
 */
@Composable
private fun Footer(onStartClick: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSize.safeBottom,
            ),
    ) {
        MedicalMateButton(
            onClick = onStartClick,
            label = stringResource(R.string.onboarding_start),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val IllustrationHeight = 260.dp

/** 마스터 48의 2.2배. Figma 인스턴스를 키운 값이다. */
private val IllustrationSymbolSize = 105.6.dp

/** 심볼 판의 반경은 크기의 1/3이다. */
private const val SYMBOL_RADIUS_DIVISOR = 3f

@MedicalMateScreenPreviews
@Composable
private fun OnboardingIntroScreenPreview() {
    MedicalMateTheme {
        OnboardingIntroScreen(onStartClick = {})
    }
}
