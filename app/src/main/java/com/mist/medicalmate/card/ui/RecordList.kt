package com.mist.medicalmate.card.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCheckbox

/**
 * 기록 목록의 묶음 머리와 줄. Figma 1j-1 `406:2569`, 1j-1-D `1121:4527`.
 *
 * `RecordScreen`에서 나눴다. 편집 상태가 들어오면서 한 파일에 함수가 열하나가 됐고
 * detekt의 파일당 상한에 닿았다. 화면 뼈대와 줄 그리기를 나누는 편이 읽기도 낫다.
 */

/**
 * 월별 묶음 목록.
 *
 * 묶음 머리에 개수를 함께 둔다. 몇 건인지가 먼저 보이면 그 달에 무슨 일이 있었는지
 * 가늠된다.
 */
@Composable
internal fun ColumnScope.GroupList(state: RecordUiState.Content, callbacks: RecordCallbacks) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding =
        PaddingValues(
            start = MedicalMateSize.gutter,
            end = MedicalMateSize.gutter,
            top = MedicalMateSpace.s12,
            bottom = MedicalMateSpace.s16,
        ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
    ) {
        state.groups.forEach { group ->
            item(key = group.monthLabel) { GroupHeader(group) }
            items(items = group.items, key = { it.id }) { item ->
                val selected = state.selectedIds?.contains(item.id)
                RecordRow(
                    item = item,
                    selected = selected,
                    onClick = {
                        if (selected == null) {
                            callbacks.onItemClick(item.id)
                        } else {
                            callbacks.onSelectChange(item.id, !selected)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(group: RecordGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MedicalMateSpace.s10, bottom = MedicalMateSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.monthLabel,
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.record_count, group.items.size),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 기록 한 줄.
 *
 * `List Row`를 쓰지 않고 카드로 짠다. 그 컴포넌트는 제목과 메타 한 줄까지인데 이 화면은
 * 메타가 두 줄이고 작성 중일 때 이어서 하라는 줄이 하나 더 붙는다.
 *
 * [selected]가 null이 아니면 편집 중이다. 왼쪽에 체크가 붙고 아래 줄이 하나로 줄어든다.
 * 시안이 편집에서 줄 높이를 104에서 80으로 낮췄다. 고르는 동안에는 어느 기록인지만
 * 알아보면 되고, 목록이 짧아야 한 번에 더 많이 보인다.
 *
 * 고른 줄은 테두리로 알린다. 체크 표시 하나만으로는 목록을 훑을 때 눈에 들어오지 않는다.
 */
@Composable
private fun RecordRow(item: RecordItem, selected: Boolean?, onClick: () -> Unit) {
    val editing = selected != null
    MedicalMateCard(
        onClick = onClick,
        modifier =
        if (selected == true) {
            Modifier.border(
                width = SelectedBorderWidth,
                color = MedicalMateTheme.colors.borderFocus,
                shape = MedicalMateRadius.lg,
            )
        } else {
            Modifier
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing) {
                MedicalMateCheckbox(
                    checked = selected == true,
                    onCheckedChange = { onClick() },
                    label = null,
                    modifier = Modifier.padding(end = MedicalMateSpace.s4),
                )
            }
            RowText(item = item, editing = editing, modifier = Modifier.weight(1f))
            // 편집에서는 이동 표시를 빼고 체크만 둔다. 누르면 고르는 것이지 들어가는 것이
            // 아니다.
            if (!editing) {
                Icon(
                    painter = painterResource(MedicalMateIcons.ChevronRight),
                    contentDescription = null,
                    tint = MedicalMateTheme.colors.fgMuted,
                )
            }
        }
    }
}

/**
 * 줄의 글자 묶음.
 *
 * 편집에서는 마지막 줄을 감춘다. 이어서 하라는 안내도 들은 내용도 고르는 동안에는 할 일이
 * 아니다. 시안이 편집에서 줄 높이를 104에서 80으로 낮춘 것이 이 한 줄만큼이다.
 */
@Composable
private fun RowText(item: RecordItem, editing: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.title,
                style = MedicalMateTheme.typography.headingS,
                color = MedicalMateTheme.colors.fgDefault,
            )
            MedicalMateBadge(
                label = recordStatusLabel(item.status),
                tone = recordStatusTone(item.status),
            )
        }
        Text(
            text = item.meta,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        if (editing) return@Column
        item.detail?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        item.resumeLabel?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgWarning,
            )
        }
    }
}

/** 고른 줄의 테두리. 브리핑 카드 편집(1e-1-E)이 카드를 두르는 것과 같은 굵기다. */
private val SelectedBorderWidth = 1.5.dp
