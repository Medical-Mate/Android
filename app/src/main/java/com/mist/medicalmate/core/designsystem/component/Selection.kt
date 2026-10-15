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
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Checkbox`.
 *
 * 동의와 다중 선택에 쓴다. **행 전체가 hit area다.** 24 상자만 누를 수 있으면 접근성
 * 기준 48에 못 미치고, 손이 떨리는 환자가 맞추기 어렵다.
 *
 * 미선택 테두리는 `border/strong` 1.5다. 문서의 접근성 기준이 비텍스트 경계에 3:1 대비를 요구하고,
 * `border/default`는 흰 배경에서 그 기준에 못 미친다.
 *
 * [Role.Checkbox]를 주면 스크린 리더가 선택 여부를 함께 읽는다.
 *
 * [label]에 null을 주면 상자만 그리고 행이 폭을 차지하지 않는다. 글자 자리에 입력이 들어가는
 * 경우(할 일을 적는 중인 [MedicalMateTodoRow])에 쓴다. 그때는 행 전체를 hit area로 삼을 수
 * 없다. 글자를 눌렀을 때 커서가 아니라 체크가 바뀌면 적을 수 없다. 대신 호출자가 옆 입력의
 * 높이를 48 이상으로 맞춘다.
 */
@Composable
fun MedicalMateCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MedicalMateTheme.colors

    Row(
        modifier =
        modifier
            .then(if (label == null) Modifier else Modifier.fillMaxWidth())
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
        MedicalMateCheckboxBox(checked = checked, enabled = enabled)
        if (label != null) {
            Text(
                text = label,
                style = MedicalMateTheme.typography.bodyL,
                color = if (enabled) colors.fgDefault else colors.fgDisabled,
            )
        }
    }
}

/**
 * 체크박스의 상자만. 눌리는 영역은 부르는 쪽이 정한다.
 *
 * `Question Row`처럼 줄 전체가 체크를 바꾸는 컴포넌트가 이 상자를 안에 둔다. 그 줄에 [MedicalMateCheckbox]를
 * 넣으면 상자와 줄이 각자 눌려 한 번 누름에 두 번 바뀐다.
 *
 * 체크 표시는 16이다. 마스터(`311:851`)의 `Box` 안 `Check`가 그 크기다.
 */
@Composable
internal fun MedicalMateCheckboxBox(checked: Boolean, enabled: Boolean = true) {
    val colors = MedicalMateTheme.colors
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
                modifier = Modifier.size(CheckSize),
            )
        }
    }
}

/**
 * DESIGN.md의 `Radio`.
 *
 * 단일 선택이다. 항목이 5개를 넘으면 세로 카드 목록을 검토한다(문서의 컴포넌트 규격).
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
 * DESIGN.md의 `Toggle`.
 *
 * 누르는 즉시 적용되는 설정에만 쓴다. 저장 버튼이 있는 폼에서는 쓰지 않는다. 되돌릴
 * 방법 없이 값이 바뀌면 사용자가 실수를 알아채기 어렵다.
 *
 * 라벨은 스위치 밖에 두고 행 전체를 hit area로 처리한다(문서의 컴포넌트 규격). 52x32 스위치만
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
        MedicalMateSwitch(checked = checked, enabled = enabled)
    }
}

/**
 * `Toggle` 마스터(`334:1155`)의 스위치 하나. 52×32다. 마스터가 라벨을 컴포넌트 밖에 두라고 해서, 시안마다
 * 라벨 글자와 행 높이가 다른 자리(1e-1-X의 `Body/M` 줄)는 행을 직접 짜고 이것만 얹는다.
 *
 * 스위치 자체는 눌리지 않는다. 행이 `toggleable`을 갖는다 — 둘 다 반응하면 한 번 눌렀는데 두 번 바뀔 수 있다.
 */
@Composable
fun MedicalMateSwitch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MedicalMateTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        modifier = modifier,
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

/** 문서의 Checkbox·Radio 행 높이 54. */
private val SelectionRowHeight = 54.dp

/** 문서의 box 24. Radio도 같은 지름을 쓴다. */
private val BoxSize = 24.dp

private val UncheckedBorderWidth = 1.5.dp

/** 마스터 `Box` 안의 `Check` 16. 크기 토큰(18 · 20 · 24)에 없는 값이다. */
private val CheckSize = 16.dp

/** 문서의 "선택은 7px ring". */
private val RadioRingWidth = 7.dp
