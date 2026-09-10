package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Chip`.
 *
 * 짧은 증상과 부위를 고르는 자리에 쓴다. 6개를 넘으면 칩 대신 세로 목록을 검토한다.
 *
 * 선택 상태를 색만으로 알리지 않는다. tint 면, 1.5dp 브랜드 경계, check 아이콘 세 가지를
 * 함께 쓴다. 문서의 D11이 상태를 색만으로 전달하지 말라고 한다. 색각 이상이나 흑백
 * 출력에서 선택이 사라지면 안 된다.
 *
 * 높이 40에 pill 형태다. 문서의 반경 20은 높이의 절반이라 `radius/full`과 같은
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
    val container =
        when {
            selected && enabled -> colors.bgPrimarySubtle
            enabled -> colors.bgSurface
            else -> colors.bgSubtle
        }
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
        border =
        when {
            selected && enabled -> BorderStroke(SelectedBorderWidth, colors.borderPrimary)
            enabled -> BorderStroke(BorderWidth, colors.borderDefault)
            else -> null
        },
        modifier =
        modifier
            .heightIn(min = MedicalMateSize.controlSm)
            .widthIn(min = MinWidth)
            .semantics { this.selected = selected },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MedicalMateSpace.s16),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MedicalMateTheme.typography.labelM)
        }
    }
}

/** 문서의 컴포넌트 규격이 지정한 선택 경계 두께다. */
private val SelectedBorderWidth = 1.5.dp

/**
 * 마스터의 `min-w-[64px]`.
 *
 * "전신"처럼 두 글자짜리 항목은 좌우 여백을 더해도 64에 못 미쳐서, 칩을 여러 개 늘어놓으면
 * 폭이 들쭉날쭉해진다.
 */
private val MinWidth = 64.dp

/**
 * 고르지 않은 칩의 테두리.
 *
 * 마스터(`311:823`)의 기본 상태는 흰 면에 이 테두리다. 회색 채움은 눌린 상태에 쓰는 두
 * 번째 칸이라, 기본을 회색 면으로 두면 여러 개를 늘어놓았을 때 고른 것과 아닌 것의 대비가
 * 약해진다. 비활성만 회색 면으로 남긴다.
 */
private val BorderWidth = 1.dp
