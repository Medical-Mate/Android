package com.mist.medicalmate.profile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
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
 * 돌아가면 로그아웃처럼 읽힌다. 앞으로 가는 길과 건너뛰는 길만 둔다. 시안도 그렇다.
 *
 * **장이 밀려서 바뀐다**(#227). 전에는 글과 그림이 그 자리에서 즉시 갈렸다. 넘긴 것인지
 * 화면이 바뀐 것인지 구별되지 않고, 되짚어 온 것인지도 알 수 없다. 앞으로 갈 때는 오른쪽에서
 * 들어오고 되짚을 때는 왼쪽에서 들어온다 — 화면 전환(`MedicalMateNavTransitions`)과 같은
 * 방향이다.
 *
 * 상단 바와 하단 버튼은 그대로 둔다. 진행 표시가 함께 밀리면 몇 번째 장인지가 눈에서
 * 사라지고, 버튼은 자리가 고정이라 손가락이 따라다니지 않아도 된다.
 *
 * 글이 그림보다 위다. v1은 그림을 먼저 놓았는데 시안이 순서를 바꿨다.
 */
@Composable
fun OnboardingScreen(
    page: OnboardingPage,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    forward: Boolean = true,
    onPreviousClick: () -> Unit = {},
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        TopBar(page = page, onSkipClick = onSkipClick)
        PageContent(
            page = page,
            forward = forward,
            onNext = onNextClick,
            onPrevious = onPreviousClick,
        )
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
 * 진행 표시와 건너뛰기.
 *
 * 시안이 건너뛰기 폭과 같은 빈 칸을 왼쪽에 둬서 진행 표시를 가운데에 세운다. 그 빈 칸을
 * 59로 박지 않고 양쪽에 같은 가중치를 준다. 결과는 시안과 같고, 글꼴 배율이 커져 건너뛰기가
 * 넓어져도 진행 표시가 한쪽으로 밀리지 않는다.
 *
 * 건너뛰기는 `Button`이 아니다. Ghost 버튼의 라벨이 15·13인데 시안은 `Body/L Strong` 17이다.
 * 대신 48 터치 영역과 버튼 역할을 얹는다 — 글자 높이가 26뿐이라 그냥 두면 접근성 기준에
 * 못 미친다.
 *
 * 마지막 장에도 둔다. 시안 네 장이 모두 그렇다.
 */
@Composable
private fun TopBar(page: OnboardingPage, onSkipClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .padding(horizontal = MedicalMateSize.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        MedicalMateOnboardingProgress(current = page.step, total = OnboardingPage.total)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = MedicalMateSize.touchMin)
                    .clip(MedicalMateRadius.sm)
                    .clickable(role = Role.Button, onClick = onSkipClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MedicalMateTheme.typography.bodyLStrong,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
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
private fun ColumnScope.PageContent(
    page: OnboardingPage,
    forward: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val direction = if (forward) 1 else -1
            val spec = tween<IntOffset>(PAGE_DURATION, easing = FastOutSlowInEasing)
            val fade = tween<Float>(PAGE_DURATION, easing = FastOutSlowInEasing)
            (
                slideInHorizontally(spec) { width -> direction * width } + fadeIn(fade)
                ) togetherWith (
                slideOutHorizontally(spec) { width -> -direction * width / PAGE_PARALLAX } + fadeOut(fade)
                )
        },
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            // 손가락으로 넘기고 되짚는다(#239). 끌리는 동안 화면이 따라오지는 않는다 —
            // `AnimatedContent`가 장을 통째로 갈아 끼우는 구조라 중간 상태가 없고,
            // 손을 떼는 순간 같은 전환이 돈다.
            .pointerInput(page) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        when {
                            dragged <= -threshold && !page.isLast -> onNext()
                            dragged >= threshold -> onPrevious()
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    dragged += amount
                }
            },
        label = "onboarding",
    ) { shown ->
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s16),
            ) {
                Text(
                    text = stringResource(shown.title),
                    style = MedicalMateTheme.typography.headingL,
                    color = MedicalMateTheme.colors.fgDefault,
                )
                Text(
                    text = stringResource(shown.description),
                    style = MedicalMateTheme.typography.bodyM,
                    color = MedicalMateTheme.colors.fgSubtle,
                    modifier = Modifier.padding(top = MedicalMateSpace.s24),
                )
            }
            Spacer(Modifier.weight(1f))
            Illustration(page = shown)
            Spacer(Modifier.height(IllustrationBottom))
        }
    }
}

/** 화면 전환과 같은 값이다. 한 장 넘기는 데 320이면 충분하다. */
private const val PAGE_DURATION = 320

/** 물러나는 장이 움직이는 몫. 들어오는 장이 네 배 더 움직인다. */
private const val PAGE_PARALLAX = 4

/**
 * 장마다 하나씩인 선화.
 *
 * 시안의 그림은 352 폭이라 콘텐츠 320을 넘어 좌우 거터로 16씩 빠져나간다. 그래서 이 블록만
 * 거터를 받지 않고 화면 폭을 쓴다.
 *
 * 폭을 352에서 멈춘다. 화면 폭을 그대로 채우게 두면 시안보다 넓은 기기에서 그림이 함께
 * 커진다. SM-S928N(411dp)에서 재 보니 1.15배가 되어 304 칸을 29 넘겼다. 352보다 좁은
 * 기기에서만 비율대로 줄어든다.
 *
 * 그림 안에 글자가 없어서 통째로 내보냈다. 원본 색은 그대로 두고 tint를 씌우지 않는다.
 * 그라디언트가 한 색으로 눌리고, 선이 세 가지 색으로 나뉘어 있다.
 */
@Composable
private fun Illustration(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IllustrationHeight),
    ) {
        Image(
            painter = painterResource(page.illustration),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ArtTop)
                // widthIn이 fillMaxWidth보다 앞이어야 한다. 뒤에 두면 fillMaxWidth가 폭을
                // 고정해 버려서 상한이 최소값에 밀려 무시된다.
                .widthIn(max = ArtMaxWidth)
                .fillMaxWidth()
                .aspectRatio(ART_ASPECT),
        )
    }
}

/** 시안의 Top Bar 56. 그 안에서 진행 표시와 건너뛰기가 세로 가운데에 선다. */
private val TopBarHeight = 56.dp

/** 그림 칸 304와 그 아래 여백 80. Content 664에서 글과 빈 칸이 나머지를 쓴다. */
private val IllustrationHeight = 304.dp

/** 장이 넘어가는 최소 거리. 이보다 짧으면 넘기려던 것이 아니라 스쳤다고 본다. */
private val SwipeThreshold = 56.dp

private val IllustrationBottom = 80.dp

/** 그림은 352x290.4이고 칸 안에서 y=6.8에 놓인다. 360 화면에서 좌우로 4씩 남는다. */
private val ArtMaxWidth = 352.dp

private val ArtTop = 6.8.dp

private const val ART_ASPECT = 352f / 290.4f

@MedicalMateScreenPreviews
@Composable
private fun OnboardingPreparePreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.PREPARE, onNextClick = {}, onSkipClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingPointPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.POINT, onNextClick = {}, onSkipClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingCardPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.CARD, onNextClick = {}, onSkipClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun OnboardingFollowPreview() {
    MedicalMateTheme {
        OnboardingScreen(page = OnboardingPage.FOLLOW, onNextClick = {}, onSkipClick = {})
    }
}
