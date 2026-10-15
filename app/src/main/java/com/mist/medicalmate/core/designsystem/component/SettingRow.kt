package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/*
 * DESIGN.md의 `Setting Row`(`3645:2327`, #316). 설정 카드 안 한 줄이고 1s-1 내 정보 · 1s-3 알림 시간에서 쓴다.
 *
 * Type이 셋이다. Toggle은 제목 · 설명 · 토글, Navigation은 제목 · 값 · 화살표(최소 56), Value는 제목 · 읽기 전용
 * 값(최소 56)이다. 위아래 12이고 값은 `Body/M` `fg/subtle`이다.
 */

/**
 * 설정 행을 담는 카드. 10-15 시안의 `Card`(반경 20 · `Elevation/Card` · 좌우 18 · 위아래 6)다. 건강 정보 카드만
 * 위아래가 4라 [verticalPadding]으로 받는다.
 */
@Composable
fun MedicalMateSettingCard(
    modifier: Modifier = Modifier,
    verticalPadding: Dp = MedicalMateSpace.s6,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MedicalMateRadius.lg
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .dropShadow(shape, MedicalMateEffects.cardWide)
            .dropShadow(shape, MedicalMateEffects.cardTight)
            .clip(shape)
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(horizontal = CardPaddingHorizontal, vertical = verticalPadding),
        content = content,
    )
}

/**
 * `Type=Toggle`. 줄 전체가 스위치다(`Toggle` 마스터가 라벨을 밖에 두고 행 전체를 터치 영역으로 잡으라고 한다).
 *
 * 제목은 시안 1s-1의 인스턴스가 모두 `Body/L Strong`으로 덮어 그렸다. 마스터의 `Body/L`이 아니라 그쪽을 따른다.
 * [description]이 없으면 제목만 선다.
 */
@Composable
fun MedicalMateSettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = MedicalMateSpace.s12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
            Text(
                text = title,
                style = MedicalMateTheme.typography.bodyLStrong,
                color = if (enabled) MedicalMateTheme.colors.fgDefault else MedicalMateTheme.colors.fgDisabled,
            )
            description?.let {
                Text(text = it, style = MedicalMateTheme.typography.bodyS, color = MedicalMateTheme.colors.fgSubtle)
            }
        }
        MedicalMateSwitch(checked = checked, enabled = enabled)
    }
}

/** `Type=Navigation`. 줄 전체가 버튼이다. [value]가 있으면 화살표 왼쪽 4에 선다(`Show Value`). */
@Composable
fun MedicalMateSettingNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.controlLg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = MedicalMateSpace.s12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MedicalMateTheme.typography.bodyL,
            color = if (enabled) MedicalMateTheme.colors.fgDefault else MedicalMateTheme.colors.fgDisabled,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            value?.let {
                Text(text = it, style = MedicalMateTheme.typography.bodyM, color = MedicalMateTheme.colors.fgSubtle)
            }
            Icon(
                painter = painterResource(MedicalMateIcons.ChevronRight),
                contentDescription = null,
                tint = MedicalMateTheme.colors.fgDefault,
                modifier = Modifier.size(MedicalMateSize.iconMd),
            )
        }
    }
}

/** `Type=Value`. 읽기만 하는 줄이다. 제목과 값 사이가 12다. */
@Composable
fun MedicalMateSettingValueRow(title: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.controlLg)
            .padding(vertical = MedicalMateSpace.s12),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MedicalMateTheme.typography.bodyL,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MedicalMateTheme.typography.bodyM, color = MedicalMateTheme.colors.fgSubtle)
    }
}

/** 시안 설정 카드의 좌우 18. 간격 토큰에 없는 값이고 다른 카드(`List Row` · `Card / Action`)도 그렇게 둔다. */
private val CardPaddingHorizontal = 18.dp
