package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 8.1 Action 컴포넌트 Preview.
 *
 * 이 묶음은 렌더링만 하는 코드라 JVM 테스트로 검증할 수 없다. Preview가 유일한 확인
 * 수단이므로 variant를 빠뜨리지 않고 늘어놓는다.
 */
@Preview(showBackground = true, name = "Button - Type", widthDp = 390)
@Composable
private fun ButtonTypePreview() {
    PreviewSurface {
        MedicalMateButtonType.entries.forEach { type ->
            MedicalMateButton(
                onClick = {},
                label = type.name,
                type = type,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Button - Size", widthDp = 390)
@Composable
private fun ButtonSizePreview() {
    PreviewSurface {
        MedicalMateButtonSize.entries.forEach { size ->
            MedicalMateButton(
                onClick = {},
                label = "증상 정리 시작 ($size)",
                size = size,
                leadingIcon = MedicalMateIcons.Mic,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Button - Disabled", widthDp = 390)
@Composable
private fun ButtonDisabledPreview() {
    PreviewSurface {
        MedicalMateButtonType.entries.forEach { type ->
            MedicalMateButton(
                onClick = {},
                label = type.name,
                type = type,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Button - 두 개 병렬", widthDp = 390)
@Composable
private fun ButtonPairPreview() {
    // 문서 8.1: 왼쪽 Outline, 오른쪽 Primary.
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
            MedicalMateButton(
                onClick = {},
                label = "취소",
                type = MedicalMateButtonType.OUTLINE,
                modifier = Modifier.width(PairButtonWidth),
            )
            MedicalMateButton(
                onClick = {},
                label = "확인",
                modifier = Modifier.width(PairButtonWidth),
            )
        }
    }
}

@Preview(showBackground = true, name = "Icon Button", widthDp = 390)
@Composable
private fun IconButtonPreview() {
    PreviewSurface {
        MedicalMateIconButtonStyle.entries.forEach { style ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MedicalMateIconButtonSize.entries.forEach { size ->
                    MedicalMateIconButton(
                        onClick = {},
                        icon = MedicalMateIcons.ChevronLeft,
                        contentDescription = "뒤로",
                        style = style,
                        size = size,
                    )
                }
                Text(text = style.name, style = MedicalMateTheme.typography.labelS)
            }
        }
    }
}

@Preview(showBackground = true, name = "Chip", widthDp = 390)
@Composable
private fun ChipPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            MedicalMateChip(label = "두통", selected = false, onClick = {})
            MedicalMateChip(label = "복통", selected = true, onClick = {})
            MedicalMateChip(label = "발열", selected = false, onClick = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Bottom CTA Bar", widthDp = 390)
@Composable
private fun BottomCtaBarPreview() {
    MedicalMateTheme {
        Column {
            MedicalMateSurfaceStyle.entries.forEach { surface ->
                MedicalMateBottomCtaBar(surface = surface) {
                    MedicalMateButton(
                        onClick = {},
                        label = "다음 (${surface.name})",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSurface(content: @Composable ColumnScope.() -> Unit) {
    MedicalMateTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(MedicalMateTheme.colors.bgCanvas)
                .padding(MedicalMateSize.gutter),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            content = content,
        )
    }
}

private val PairButtonWidth = MedicalMateSize.contentWidth / 2 - MedicalMateSpace.s8
