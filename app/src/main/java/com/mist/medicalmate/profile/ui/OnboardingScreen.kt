package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateOnboardingProgress

/**
 * 와이어프레임 온보딩 v2. Figma `V2-00`~`V2-03`.
 *
 * 로그인 뒤 한 번 지나간다. 상단 바에 Nav Bar를 두지 않는다. 뒤로 갈 곳이 로그인이라
 * 돌아가면 로그아웃처럼 읽힌다. 진행 표시만 두고 앞으로만 간다. 시안도 그렇다.
 *
 * 글이 그림보다 위다. v1은 그림을 먼저 놓았는데 시안이 순서를 바꿨다.
 */
@Composable
fun OnboardingScreen(page: OnboardingPage, onNextClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        // 상단 바 56 안에서 진행 표시가 y=26에 놓인다. 위아래가 대칭이 아니라 값을 그대로 쓴다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopBarHeight)
                .padding(top = ProgressTop),
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
 * 글과 그림.
 *
 * 시안은 그림을 y=336에 고정해 두고 글 아래 여백으로 높이 차이를 받는다. 첫 장만 제목이
 * 두 줄이라 그 여백이 82, 나머지가 122다. 그래서 가중치 있는 빈 칸을 둔다.
 *
 * 글꼴 배율이 커지면 이 빈 칸이 먼저 줄고, 그다음 그림 아래 여백 80이 줄어든다. 그림이
 * 잘리기 전에 160 넘게 흡수한다.
 */
@Composable
private fun ColumnScope.PageContent(page: OnboardingPage) {
    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s16),
        ) {
            Text(
                text = stringResource(page.title),
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
            )
            Text(
                text = stringResource(page.description),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
                modifier = Modifier.padding(top = MedicalMateSpace.s24),
            )
        }
        Spacer(Modifier.weight(1f))
        Illustration(page = page)
        Spacer(Modifier.height(IllustrationBottom))
    }
}

/**
 * 장마다 하나씩인 선화.
 *
 * 시안의 그림은 352 폭이라 콘텐츠 320을 넘어 좌우 거터로 16씩 빠져나간다. 그래서 이 블록만
 * 거터를 받지 않고 화면 폭을 쓴다. 360 기준 남는 4씩을 빼고 비율로 그려서 좁은 화면에서는
 * 같은 비율로 함께 줄어든다.
 *
 * 그림 안에 글자가 없어서 통째로 내보냈다. 원본 색은 그대로 두고 tint를 씌우지 않는다.
 * 그라디언트가 한 색으로 눌리고, 선이 세 가지 색으로 나뉘어 있다.
 */
@Composable
private fun Illustration(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IllustrationHeight)
            .padding(horizontal = ArtSideInset),
    ) {
        Image(
            painter = painterResource(page.illustration),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ArtTop)
                .fillMaxWidth()
                .aspectRatio(ART_ASPECT),
        )
    }
}

/** 시안의 Top Bar 56과 그 안의 진행 표시 y=26. */
private val TopBarHeight = 56.dp

private val ProgressTop = 26.dp

/** 그림 칸 304와 그 아래 여백 80. Content 664에서 글과 빈 칸이 나머지를 쓴다. */
private val IllustrationHeight = 304.dp

private val IllustrationBottom = 80.dp

/** 그림은 352x290.4이고 칸 안에서 y=6.8에 놓인다. 360에서 좌우로 4씩 남는다. */
private val ArtSideInset = 4.dp

private val ArtTop = 6.8.dp

private const val ART_ASPECT = 352f / 290.4f

@MedicalMateScreenPreviews
@Composable
private fun OnboardingPreparePreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.PREPARE, onNextClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingPointPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.POINT, onNextClick = {})
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
private fun OnboardingFollowPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.FOLLOW, onNextClick = {})
    }
}
