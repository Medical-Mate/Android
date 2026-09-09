package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
 * DESIGN.md 8.2 `Checkbox`.
 *
 * 동의와 다중 선택에 쓴다. **행 전체가 hit area다.** 24 상자만 누를 수 있으면 접근성
 * 기준 48에 못 미치고, 손이 떨리는 환자가 맞추기 어렵다.
 *
 * 미선택 테두리는 `border/strong` 1.5다. 문서 9절이 비텍스트 경계에 3:1 대비를 요구하고,
 * `border/default`는 흰 배경에서 그 기준에 못 미친다.
 *
 * [Role.Checkbox]를 주면 스크린 리더가 선택 여부를 함께 읽는다.
 */
@Composable
fun MedicalMateCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = SelectionRowHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = MedicalMateSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier
                .size(BoxSize)
                .background(
                    color = if (checked && enabled) colors.bgPrimary else colors.bgSurface,
                    shape = MedicalMateRadius.xs,
                )
                .then(
                    if (checked && enabled) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = UncheckedBorderWidth,
                            color = if (enabled) colors.borderStrong else colors.borderSubtle,
                            shape = MedicalMateRadius.xs,
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Check),
                    contentDescription = null,
                    tint = if (enabled) colors.fgOnPrimary else colors.fgDisabled,
                    modifier = Modifier.size(MedicalMateSize.iconSm),
                )
            }
        }
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyL,
            color = if (enabled) colors.fgDefault else colors.fgDisabled,
        )
    }
}

/**
 * DESIGN.md 8.2 `Radio`.
 *
 * 단일 선택이다. 항목이 5개를 넘으면 세로 카드 목록을 검토한다(문서 8.2).
 *
 * 선택 표시는 7 ring이다. 행 전체가 hit area인 것은 [MedicalMateCheckbox]와 같다.
 */
@Composable
fun MedicalMateRadio(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = SelectionRowHeight)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = MedicalMateSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier
                .size(BoxSize)
                .border(
                    width = if (selected && enabled) RadioRingWidth else UncheckedBorderWidth,
                    color =
                    when {
                        !enabled -> colors.borderSubtle
                        selected -> colors.borderPrimary
                        else -> colors.borderStrong
                    },
                    shape = MedicalMateRadius.full,
                ),
        )
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyL,
            color = if (enabled) colors.fgDefault else colors.fgDisabled,
        )
    }
}

/**
 * DESIGN.md 8.2 `Toggle`.
 *
 * 누르는 즉시 적용되는 설정에만 쓴다. 저장 버튼이 있는 폼에서는 쓰지 않는다. 되돌릴
 * 방법 없이 값이 바뀌면 사용자가 실수를 알아채기 어렵다.
 *
 * 라벨은 스위치 밖에 두고 행 전체를 hit area로 처리한다(문서 8.2). 52x32 스위치만
 * 누르게 하면 접근성 기준 48에 못 미친다.
 *
 * Material3 `Switch`를 쓴다. 크기 52x32가 M3 기본과 같고, 끌기와 접근성 처리가 이미
 * 들어 있다.
 */
@Composable
fun MedicalMateToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = SelectionRowHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = MedicalMateSpace.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyL,
            color = if (enabled) colors.fgDefault else colors.fgDisabled,
        )
        Switch(
            checked = checked,
            // 행이 toggleable을 갖고 있어 스위치 자체는 눌리지 않게 둔다.
            // 둘 다 반응하면 한 번 눌렀는데 두 번 바뀔 수 있다.
            onCheckedChange = null,
            enabled = enabled,
            colors =
            SwitchDefaults.colors(
                checkedThumbColor = colors.fgOnPrimary,
                checkedTrackColor = colors.bgPrimary,
                // 꺼진 트랙은 `border/strong`을 면으로 쓴다. Figma의 Toggle 마스터가 쓰는
                // 토큰이 흰색·`border/strong`·`bg/primary` 셋뿐이다. 옅은 면에 테두리를
                // 두르면 꺼진 것과 비활성이 구분되지 않는다.
                uncheckedThumbColor = colors.bgSurface,
                uncheckedTrackColor = colors.borderStrong,
                uncheckedBorderColor = colors.borderStrong,
            ),
        )
    }
}

/** 문서 8.2의 Checkbox·Radio 행 높이 54. */
private val SelectionRowHeight = 54.dp

/** 문서 8.2의 box 24. Radio도 같은 지름을 쓴다. */
private val BoxSize = 24.dp

private val UncheckedBorderWidth = 1.5.dp

/** 문서 8.2의 "선택은 7px ring". */
private val RadioRingWidth = 7.dp
