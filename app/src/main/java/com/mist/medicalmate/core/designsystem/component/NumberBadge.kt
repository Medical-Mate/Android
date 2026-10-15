package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** `Number Badge`의 `Tone`. */
enum class MedicalMateNumberBadgeTone {
    /** 질문·단계 번호. */
    PRIMARY,

    /** 빈 칸 번호. 아직 아무것도 없는 자리다. */
    SUBTLE,
}

/**
 * DESIGN.md의 `Number Badge`(`3644:7400`). 22 원 안의 번호.
 *
 * `Question Row`와 `Step Row` 안에서 쓴다. 번호 글자는 `Label/S`다.
 */
@Composable
fun MedicalMateNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    tone: MedicalMateNumberBadgeTone = MedicalMateNumberBadgeTone.PRIMARY,
) {
    val colors = MedicalMateTheme.colors
    val primary = tone == MedicalMateNumberBadgeTone.PRIMARY
    Box(
        modifier =
        modifier
            .size(BadgeSize)
            .background(color = if (primary) colors.bgPrimary else colors.bgSubtle, shape = MedicalMateRadius.full),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MedicalMateTheme.typography.labelS,
            color = if (primary) colors.fgOnPrimary else colors.fgSubtle,
        )
    }
}

/** 마스터의 지름 22. 크기 토큰에 없는 값이다. */
private val BadgeSize = 22.dp
