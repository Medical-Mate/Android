package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.1 `Chip`.
 *
 * 짧은 증상과 부위를 고르는 자리에 쓴다. 6개를 넘으면 칩 대신 세로 목록을 검토한다.
 *
 * 선택 상태를 색만으로 알리지 않는다. tint 면, 1.5dp 브랜드 경계, check 아이콘 세 가지를
 * 함께 쓴다. 문서 1항 6번이 상태를 색만으로 전달하지 말라고 한다. 색각 이상이나 흑백
 * 출력에서 선택이 사라지면 안 된다.
 *
 * 높이 40에 pill 형태다. 문서 8.1의 반경 20은 높이의 절반이라 4.2의 `radius/full`과 같은
 * 결과가 된다.
 *
 * Figma의 `State=Pressed`는 파라미터로 받지 않는다. 대응하는 색 토큰이 없어 press
 * indication에 맡긴다.
 *
 * 선택 여부는 `semantics`로 따로 알린다. 시각 표시만으로는 스크린 리더가 읽지 못한다.
 */
@Composable
fun MedicalMateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors
    val container = if (selected && enabled) colors.bgPrimarySubtle else colors.bgSubtle
    val content =
        when {
            !enabled -> colors.fgDisabled
            selected -> colors.fgPrimary
            else -> colors.fgDefault
        }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MedicalMateRadius.full,
        color = container,
        contentColor = content,
        border = if (selected && enabled) BorderStroke(SelectedBorderWidth, colors.borderPrimary) else null,
        modifier =
        modifier
            .height(MedicalMateSize.controlSm)
            .semantics { this.selected = selected },
    ) {
        Row(
            // 선택되면 check 아이콘이 붙어 왼쪽 여백을 12로 줄인다(DESIGN.md 8.1).
            modifier =
            Modifier.padding(
                start = if (selected) MedicalMateSpace.s12 else MedicalMateSpace.s16,
                end = MedicalMateSpace.s16,
            ),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Check),
                    contentDescription = null,
                    modifier = Modifier.size(MedicalMateSize.iconSm),
                )
            }
            Text(text = label, style = MedicalMateTheme.typography.labelM)
        }
    }
}

/** 문서 8.1이 지정한 선택 경계 두께다. */
private val SelectedBorderWidth = 1.5.dp
