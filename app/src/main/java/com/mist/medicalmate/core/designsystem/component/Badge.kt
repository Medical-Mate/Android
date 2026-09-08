package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.3 `Badge`의 `Tone` variant. */
enum class MedicalMateBadgeTone {
    BRAND,
    NEUTRAL,
    SUCCESS,
    WARNING,
    DANGER,
}

/**
 * DESIGN.md 8.3 `Badge`.
 *
 * 읽는 상태나 속성을 붙인다. 누를 수 없다. 누르는 선택지는 [MedicalMateChip]이다.
 *
 * pill을 쓰지 않고 radius 8을 쓴다. 문서가 Chip과 구분하려고 정한 것이라서, 둥글게
 * 만들면 눌러도 되는 것처럼 보인다.
 */
@Composable
fun MedicalMateBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: MedicalMateBadgeTone = MedicalMateBadgeTone.NEUTRAL,
) {
    val colors = MedicalMateTheme.colors
    val container: Color
    val content: Color
    when (tone) {
        MedicalMateBadgeTone.BRAND -> {
            container = colors.bgPrimarySubtle
            content = colors.fgPrimary
        }

        MedicalMateBadgeTone.NEUTRAL -> {
            container = colors.bgSubtle
            content = colors.fgSubtle
        }

        MedicalMateBadgeTone.SUCCESS -> {
            container = colors.bgSuccess
            content = colors.fgSuccess
        }

        MedicalMateBadgeTone.WARNING -> {
            container = colors.bgWarning
            content = colors.fgWarning
        }

        MedicalMateBadgeTone.DANGER -> {
            container = colors.bgDanger
            content = colors.fgDanger
        }
    }

    Surface(
        shape = MedicalMateRadius.xs,
        color = container,
        contentColor = content,
        modifier = modifier.heightIn(min = BadgeHeight),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = MedicalMateSpace.s8),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = MedicalMateTheme.typography.labelM)
        }
    }
}

/** 문서 8.3이 지정한 높이. Scale에 없는 값이다. */
/** 문서 8.3의 높이. 큰 글꼴에서는 글자에 맞춰 늘어난다. */
private val BadgeHeight = 26.dp
