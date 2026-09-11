package com.mist.medicalmate.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateOnboardingProgress

/**
 * 와이어프레임 A2 온보딩. Figma `1157:4246`~`1157:4249`.
 *
 * 로그인 뒤 한 번 지나간다. 전에는 한 장짜리 인트로(1a-2)였는데 시안이 네 장으로 바꿨고
 * 그 프레임은 삭제됐다.
 *
 * 상단 바에 Nav Bar를 두지 않는다. 뒤로 갈 곳이 로그인이라 돌아가면 로그아웃처럼 읽힌다.
 * 진행 표시만 두고 앞으로만 간다. 시안도 그렇다.
 */
@Composable
fun OnboardingScreen(page: OnboardingPage, onNextClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MedicalMateSpace.s20, bottom = MedicalMateSpace.s4),
            horizontalArrangement = Arrangement.Center,
        ) {
            MedicalMateOnboardingProgress(current = page.step, total = OnboardingPage.total)
        }
        PageContent(page = page)
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(page.ctaLabel),
                onClick = onNextClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 장의 본문.
 *
 * 첫 장만 구조가 다르다. 그림 없이 글과 단계 셋이고, 나머지 셋은 그림 아래에 같은 자리로
 * 글이 온다. 시안도 그 장만 다르게 그렸다.
 */
@Composable
private fun ColumnScope.PageContent(page: OnboardingPage) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
    ) {
        if (page != OnboardingPage.INTRO) {
            OnboardingIllustration(page = page)
            Eyebrow(page.eyebrow, top = MedicalMateSpace.s20)
        } else {
            Eyebrow(page.eyebrow, top = MedicalMateSpace.s8)
        }
        Text(
            text = stringResource(page.title),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.padding(top = MedicalMateSpace.s12),
        )
        Text(
            text = stringResource(page.description),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
            modifier = Modifier.padding(top = MedicalMateSpace.s12),
        )
        if (page == OnboardingPage.INTRO) {
            IntroSteps()
        }
    }
}

/** 제목 위의 작은 말. 무엇에 대한 장인지를 한 마디로 말한다. */
@Composable
private fun Eyebrow(@StringRes textRes: Int, top: androidx.compose.ui.unit.Dp) {
    Text(
        text = stringResource(textRes),
        style = MedicalMateTheme.typography.labelS,
        color = MedicalMateTheme.colors.fgPrimary,
        modifier = Modifier.padding(top = top),
    )
}

/** 첫 장의 단계 셋. 번호와 제목, 설명 한 줄이 구분선으로 나뉜다. */
@Composable
private fun IntroSteps() {
    Column(modifier = Modifier.padding(top = MedicalMateSpace.s24)) {
        IntroStep(1, R.string.onboarding_intro_step1, R.string.onboarding_intro_step1_detail)
        MedicalMateDivider()
        IntroStep(2, R.string.onboarding_intro_step2, R.string.onboarding_intro_step2_detail)
        MedicalMateDivider()
        IntroStep(3, R.string.onboarding_intro_step3, R.string.onboarding_intro_step3_detail)
    }
}

@Composable
private fun IntroStep(number: Int, @StringRes titleRes: Int, @StringRes detailRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MedicalMateSpace.s14),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "%02d".format(number),
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgMuted,
        )
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
            Text(
                text = stringResource(titleRes),
                style = MedicalMateTheme.typography.bodyMStrong,
                color = MedicalMateTheme.colors.fgDefault,
            )
            Text(
                text = stringResource(detailRes),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingIntroPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.INTRO, onNextClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingBodyPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.BODY, onNextClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingCardPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.CARD, onNextClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingTimelinePreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.TIMELINE, onNextClick = {})
    }
}
