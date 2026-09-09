package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.designsystem.component.MedicalMateTabBar

/**
 * 와이어프레임 1j-1과 1j-2. Figma `406:2569`, `406:2646`.
 *
 * 하단 탭의 기록이다. 카드와 진료 기록을 월별로 묶어 보여준다.
 *
 * 1Depth 화면이라 하단 탭을 함께 그린다. 문답이나 카드처럼 흐름 안에 들어간 화면에서는
 * 감춘다(문서 8.5).
 */
@Composable
fun RecordScreen(
    state: RecordUiState,
    onItemClick: (String) -> Unit,
    onStartIntakeClick: () -> Unit,
    onTabSelect: (MedicalMateTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgCanvas),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.record_title),
            leading = MedicalMateNavLeading.NONE,
        )
        when (state) {
            RecordUiState.Loading -> MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))
            is RecordUiState.Content ->
                if (state.groups.isEmpty()) {
                    EmptyContent(onStartIntakeClick = onStartIntakeClick)
                } else {
                    GroupList(groups = state.groups, onItemClick = onItemClick)
                }
        }
        MedicalMateTabBar(selected = MedicalMateTab.RECORD, onSelect = onTabSelect)
    }
}

/**
 * 월별 묶음 목록.
 *
 * 묶음 머리에 개수를 함께 둔다. 몇 건인지가 먼저 보이면 그 달에 무슨 일이 있었는지
 * 가늠된다.
 */
@Composable
private fun ColumnScope.GroupList(groups: List<RecordGroup>, onItemClick: (String) -> Unit) {
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
        groups.forEach { group ->
            item(key = group.monthLabel) { GroupHeader(group) }
            items(items = group.items, key = { it.id }) { item ->
                RecordRow(item = item, onClick = { onItemClick(item.id) })
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
 */
@Composable
private fun RecordRow(item: RecordItem, onClick: () -> Unit) {
    MedicalMateCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.title,
                        style = MedicalMateTheme.typography.bodyLStrong,
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
                Text(
                    text = item.detail,
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
                item.resumeLabel?.let {
                    Text(
                        text = it,
                        style = MedicalMateTheme.typography.bodySStrong,
                        color = MedicalMateTheme.colors.fgWarning,
                    )
                }
            }
            Icon(
                painter = painterResource(MedicalMateIcons.ChevronRight),
                contentDescription = null,
                tint = MedicalMateTheme.colors.fgMuted,
            )
        }
    }
}

/** 1j-2. 빈 상태를 화면 위쪽에 둔다. Figma가 가운데가 아니라 1/4 지점에 놓았다. */
@Composable
private fun ColumnScope.EmptyContent(onStartIntakeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s40),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RECORD,
            title = stringResource(R.string.record_empty_title),
            description = stringResource(R.string.record_empty_description),
            actionLabel = stringResource(R.string.record_empty_action),
            onActionClick = onStartIntakeClick,
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun RecordScreenPreview() {
    MedicalMateTheme {
        RecordScreen(
            state = RecordUiState.Content(groups = previewRecordGroups),
            onItemClick = {},
            onStartIntakeClick = {},
            onTabSelect = {},
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun RecordScreenEmptyPreview() {
    MedicalMateTheme {
        RecordScreen(
            state = RecordUiState.Content(groups = emptyList()),
            onItemClick = {},
            onStartIntakeClick = {},
            onTabSelect = {},
        )
    }
}
