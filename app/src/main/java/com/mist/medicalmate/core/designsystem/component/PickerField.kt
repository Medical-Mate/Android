package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Picker Field`(`1129:9196`) 320x56.
 *
 * 병원 · 날짜 · 시간처럼 **직접 적지 않고 골라 채우는** 필드다. 눌러서 시트나 검색 화면을
 * 열고, 그 결과가 여기 표시된다. 그래서 키보드를 띄우는 [MedicalMateTextField]와 다르다.
 *
 * 상태를 파라미터로 받지 않는다. [value]가 비었는지로 갈린다. 상태를 따로 받으면 실제
 * 값과 어긋날 수 있다.
 *
 * 테두리가 채움 여부에 따라 바뀐다. 채워지면 `fg/default`, 비어 있으면 `border/strong`이다.
 * 문서가 선택 완료된 필드의 테두리를 `fg/default`로 못박았다. 글자와 테두리가 함께 진해져야
 * 채워졌다는 것이 읽힌다. 마스터(`1129:9196`)는 채워진 상태만 그려 뒀다.
 *
 * 오른쪽 `chevron-right`는 장식이 아니라 "눌러서 고른다"는 표시다. 아이콘 20은 마스터
 * 값이고 `Body/L` 옆 인라인 아이콘 규칙과도 같다.
 */
@Composable
fun MedicalMatePickerField(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors
    val filled = !value.isNullOrBlank()
    val border =
        when {
            !enabled -> colors.borderSubtle
            filled -> colors.fgDefault
            else -> colors.borderStrong
        }
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.controlLg)
            .border(width = BorderWidth, color = border, shape = MedicalMateRadius.md)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = MedicalMateSpace.s20, end = MedicalMateSpace.s16),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value?.takeIf { filled } ?: placeholder,
            style = MedicalMateTheme.typography.bodyL,
            color =
            when {
                !enabled -> colors.fgDisabled
                filled -> colors.fgDefault
                // 비어 있을 때의 안내 문구. 문서가 플레이스홀더에 `fg/subtle` 이상을
                // 요구한다. `fg/muted`는 흰 배경에서 3.78:1이라 본문 대비에 못 미친다.
                else -> colors.fgSubtle
            },
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(MedicalMateIcons.ChevronRight),
            contentDescription = null,
            tint = if (enabled) colors.fgDefault else colors.fgDisabled,
            modifier = Modifier.size(MedicalMateSize.iconMd),
        )
    }
}

/** 마스터의 테두리 1. 채워진 상태도 굵어지지 않는다. */
private val BorderWidth = 1.dp
