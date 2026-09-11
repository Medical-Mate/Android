package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 온보딩 그림 셋. Figma `1157:4247`~`1157:4249`의 `Illustration`.
 *
 * 그림을 통째로 내보내지 않는다. 안에 든 글자가 이미지로 굳으면 번역도 글꼴 배율도 따라가지
 * 못한다. 몸 그림만 벡터로 내보내 쓰고(`img_onboarding_body`) 말풍선·카드·타임라인은 여기서
 * 그린다. 원래 시안에서도 그것들은 이 앱의 화면을 작게 그린 것이라 같은 토큰으로 만들어진다.
 *
 * 세 그림이 같은 판 위에 놓인다. 높이 366에 반경 20, 옅은 브랜드 면이다.
 */
@Composable
internal fun OnboardingIllustration(page: OnboardingPage, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.lg),
    ) {
        when (page) {
            OnboardingPage.INTRO -> Unit
            OnboardingPage.BODY -> BodyIllustration()
            OnboardingPage.CARD -> CardIllustration()
            OnboardingPage.TIMELINE -> TimelineIllustration()
        }
    }
}

/**
 * 부위 선택. 몸 그림 위에 짚은 자리를 겹친다.
 *
 * 동심원 셋이 짚은 자리다. 바깥은 옅게 번지고 가운데가 진하다. 정지 화면이라 번짐을 크기로만
 * 나타낸다.
 */
@Composable
private fun BoxScope.BodyIllustration() {
    val colors = MedicalMateTheme.colors
    // Image로 그린다. Icon은 단색 tint를 씌우는 컴포저블이라 그라디언트가 한 색으로 눌린다.
    Image(
        painter = painterResource(R.drawable.img_onboarding_body),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
    Box(
        modifier = Modifier.align(Alignment.Center).padding(start = AnchorOffsetX, bottom = AnchorOffsetY),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RippleSize)
                .background(color = colors.bgPrimarySubtle, shape = MedicalMateRadius.full),
        )
        Box(
            modifier = Modifier
                .size(RingSize)
                .background(color = colors.bgPrimary, shape = MedicalMateRadius.full),
        )
        Box(
            modifier = Modifier
                .size(CoreSize)
                .background(color = colors.bgSurface, shape = MedicalMateRadius.full),
        )
    }
    Chip(
        text = stringResource(R.string.onboarding_body_anchor),
        dot = true,
        modifier = Modifier.align(Alignment.Center).padding(start = LabelOffsetX, top = LabelOffsetY),
    )
    Chip(
        text = stringResource(R.string.onboarding_body_hint),
        dot = false,
        brand = true,
        modifier = Modifier.align(Alignment.BottomStart).padding(HintPadding),
    )
}

/** 문답이 카드 한 장이 되는 그림. 왼쪽은 주고받은 말, 오른쪽은 그 결과다. */
@Composable
private fun BoxScope.CardIllustration() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(PanelPadding),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            ) {
                Ask(R.string.onboarding_card_ask1)
                Answer(R.string.onboarding_card_answer1)
                Ask(R.string.onboarding_card_ask2)
                Answer(R.string.onboarding_card_answer2)
            }
            MiniCard(modifier = Modifier.weight(1f))
        }
        // 카드 바로 아래에 붙인다. 판 바닥에 두면 카드와 사이가 벌어진다.
        Chip(
            text = stringResource(R.string.onboarding_card_editable),
            dot = false,
            brand = true,
            check = true,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** 진료 전·후·다음이 한 줄로 이어지는 그림. 기록 상세(1j-3)의 타임라인을 작게 그린 것이다. */
@Composable
private fun BoxScope.TimelineIllustration() {
    Row(modifier = Modifier.fillMaxSize().padding(PanelPadding)) {
        Box(modifier = Modifier.width(TimelineRailWidth)) {
            // 점 셋을 잇는 선. 첫 점과 마지막 점 사이만 지난다.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = TimelineLineInset, top = TimelineWhenHeight / 2)
                    .width(TimelineLineWidth)
                    .height(TimelineWhenHeight * 2 + TimelineGap * 2)
                    .background(color = MedicalMateTheme.colors.bgPrimary, shape = MedicalMateRadius.full),
            )
            Column(verticalArrangement = Arrangement.spacedBy(TimelineGap)) {
                TimelineWhen(R.string.onboarding_timeline_when1)
                TimelineWhen(R.string.onboarding_timeline_when2)
                TimelineWhen(R.string.onboarding_timeline_when3)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TimelineGap),
        ) {
            TimelineCard(R.string.onboarding_timeline_title1, R.string.onboarding_timeline_detail1)
            TimelineCard(R.string.onboarding_timeline_title2, R.string.onboarding_timeline_detail2)
            TimelineCard(R.string.onboarding_timeline_title3, R.string.onboarding_timeline_detail3)
        }
    }
}

/** 판 위에 얹는 작은 알림. 점이나 체크가 앞에 붙는다. */
@Composable
private fun Chip(
    text: String,
    dot: Boolean,
    modifier: Modifier = Modifier,
    brand: Boolean = false,
    check: Boolean = false,
) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier =
        modifier
            .background(color = colors.bgSurface, shape = MedicalMateRadius.sm)
            .padding(horizontal = MedicalMateSpace.s10, vertical = MedicalMateSpace.s6),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(
                modifier = Modifier
                    .size(ChipDotSize)
                    .background(color = colors.bgPrimary, shape = MedicalMateRadius.full),
            )
        }
        if (check) {
            Icon(
                painter = painterResource(MedicalMateIcons.Check),
                contentDescription = null,
                tint = colors.fgPrimary,
                modifier = Modifier.size(MedicalMateSize.iconSm),
            )
        }
        Text(
            text = text,
            style = MedicalMateTheme.typography.labelS,
            color = if (brand) colors.fgPrimary else colors.fgDefault,
        )
    }
}

private val PanelHeight = 366.dp

private val PanelPadding = 16.dp

/** 짚은 자리의 동심원. 시안의 62 · 42 · 18이다. */
private val RippleSize = 62.dp

private val RingSize = 42.dp

private val CoreSize = 18.dp

/** 몸 그림의 팔 위치. 판 가운데에서 옮긴 값이다. */
private val AnchorOffsetX = 96.dp

private val AnchorOffsetY = 24.dp

private val LabelOffsetX = 150.dp

private val LabelOffsetY = 56.dp

private val HintPadding = 20.dp

internal val ChipDotSize = 6.dp

private val TimelineRailWidth = 76.dp

private val TimelineGap = 10.dp

/** 점 가운데를 지나는 선. 점이 6이라 그 절반에서 선 두께의 절반을 뺀다. */
private val TimelineLineInset = 2.dp

private val TimelineLineWidth = 2.dp

internal val TimelineCardBorder = 1.dp
