package com.mist.medicalmate.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Onboarding Progress`(`1155:854`). 문서가 3.0에서 더한 컴포넌트다.
 *
 * 점 네 개 중 현재 것만 알약으로 늘어난다. 온보딩 **화면 넘김**을 알린다.
 *
 * [MedicalMateProgressIndicator]와 역할이 다르다. 그쪽은 작업 진행률이라 지나온 칸이 채워지고
 * `n/총` 숫자가 붙는다. 이쪽은 몇 번째 화면인지만 알리고 되돌아갈 수 있어서 **지나온 점도
 * 채우지 않는다.** 숫자도 없다. 읽고 넘기는 소개 화면에 진행률을 붙이면 남은 분량을
 * 재촉하는 것으로 읽힌다.
 *
 * 접근성 트리에서는 "2/4 단계"로 한 번만 읽는다. 점을 하나씩 읽으면 소리만 길어지고, 점
 * 자체는 조작할 수 없다.
 *
 * 늘어나는 것은 애니메이션으로 잇는다. 점이 알약으로 바뀌는 것이 화면 넘김과 같은 동작이라
 * 끊어지면 어느 점이 현재인지 눈으로 따라가지 못한다. 기기에서 애니메이션을 끄면 배율이 0이
 * 되어 즉시 바뀐다.
 */
@Composable
fun MedicalMateOnboardingProgress(current: Int, modifier: Modifier = Modifier, total: Int = DEFAULT_STEPS) {
    require(current in 1..total) { "현재 단계는 1..$total 범위여야 합니다. 받은 값: $current" }
    val spoken = stringResource(R.string.progress_step, current, total)

    Row(
        modifier =
        modifier
            .heightIn(min = TrackHeight)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index -> Dot(active = index + 1 == current) }
    }
}

@Composable
private fun Dot(active: Boolean) {
    val colors = MedicalMateTheme.colors
    val width by animateDpAsState(
        targetValue = if (active) ActiveDotWidth else DotSize,
        label = "onboardingDotWidth",
    )
    Box(
        modifier =
        Modifier
            .width(width)
            .height(DotSize)
            .background(
                color = if (active) colors.bgPrimary else colors.bgSubtle,
                shape = MedicalMateRadius.full,
            ),
    )
}

/** 온보딩은 네 화면이다. */
private const val DEFAULT_STEPS = 4

/** 문서의 점 6x6. */
private val DotSize = 6.dp

/** 문서의 현재 점 20x6. */
private val ActiveDotWidth = 20.dp

/** 문서의 전체 높이 24. 점은 6이고 나머지는 위아래 여백이다. */
private val TrackHeight = 24.dp
