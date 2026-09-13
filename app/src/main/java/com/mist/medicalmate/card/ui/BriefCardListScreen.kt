package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialog
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar

/**
 * 와이어프레임 1j-4. Figma `1122:4830`.
 *
 * 홈의 "최근 브리핑 카드 · 전체 보기"에서 들어온다. 탭이 아니라 밀려 들어오는 화면이라
 * 상단 왼쪽에 뒤로가기가 있고 하단 탭바를 두지 않는다. 시안도 그 상태의 탭바를 감췄다.
 *
 * 편집은 기록 목록(1j-1)과 같은 CRUD 규칙이다. 다른 점이 둘 있다. 세는 단위가 "장"이고,
 * 고른 수는 묶음 머리와 하단 버튼이 알린다. 목록 위에 `Select Bar`를 두지 않는다 — 시안
 * `1j-4-D2`에 그 줄이 없고, 같은 수를 세 곳에서 말하게 된다.
 * 기록 쪽은 하단 버튼 글자에만 적는다. 두 화면이 같은 조작을 다르게 알리는 셈이라 디자인
 * 트랙에 확인을 넘겼고, 여기서는 각자의 프레임을 따랐다.
 */
@Composable
fun BriefCardListScreen(
    state: BriefCardListUiState,
    callbacks: BriefCardListCallbacks,
    modifier: Modifier = Modifier,
) {
    val content = state as? BriefCardListUiState.Content
    val editing = content?.editing == true

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.brief_card_list_title),
            onLeadingClick = callbacks.onBackClick,
            actionLabel = navActionLabel(content),
            onActionClick = if (editing) callbacks.onEditCancel else callbacks.onEditStart,
        )
        when (state) {
            BriefCardListUiState.Loading -> MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))
            BriefCardListUiState.Failed -> FailedContent(onRetryClick = callbacks.onRetryClick)
            is BriefCardListUiState.Content ->
                if (state.groups.isEmpty()) {
                    EmptyContent(onStartIntakeClick = callbacks.onStartIntakeClick)
                } else {
                    CardGroups(state = state, callbacks = callbacks)
                }
        }
        if (editing) {
            DeleteBar(count = content?.selectedCount ?: 0, onDeleteClick = callbacks.onDeleteClick)
        }
    }

    if (content?.deleteRequested == true) {
        MedicalMateDialog(
            title = stringResource(R.string.brief_card_list_delete_title, content.selectedCount),
            message = stringResource(R.string.brief_card_list_delete_body),
            confirmLabel = stringResource(R.string.brief_card_list_delete_confirm),
            onConfirm = callbacks.onDeleteConfirm,
            dismissLabel = stringResource(R.string.brief_card_list_edit_cancel),
            onDismissRequest = callbacks.onDeleteDismiss,
        )
    }
}

/** 브리핑 카드 전체에서 나가는 길과 화면 안의 조작. */
data class BriefCardListCallbacks(
    val onBackClick: () -> Unit = {},
    val onCardClick: (RecordItem) -> Unit = {},
    val onStartIntakeClick: () -> Unit = {},
    val onEditStart: () -> Unit = {},
    val onEditCancel: () -> Unit = {},
    val onSelectChange: (String, Boolean) -> Unit = { _, _ -> },
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

/** 목록이 비어 있으면 편집을 두지 않는다. 지울 것이 없는데 들어갈 수 있으면 안 된다. */
@Composable
private fun navActionLabel(content: BriefCardListUiState.Content?): String? = when {
    content == null || content.groups.isEmpty() -> null
    content.editing -> stringResource(R.string.brief_card_list_edit_cancel)
    else -> stringResource(R.string.brief_card_list_edit)
}

/** 목록. 편집 중에는 줄마다 체크가 붙는다. */
@Composable
private fun ColumnScope.CardGroups(state: BriefCardListUiState.Content, callbacks: BriefCardListCallbacks) {
    GroupList(
        groups = state.groups,
        countRes = R.string.brief_card_list_count,
        selectedRes = R.string.brief_card_list_selected,
        onItemClick = callbacks.onCardClick,
        selectedIds = state.selectedIds,
        onSelectChange = callbacks.onSelectChange,
    )
}

/** 편집 중 하단. 기록 목록과 같은 자리에 같은 모양이다. */
@Composable
private fun DeleteBar(count: Int, onDeleteClick: () -> Unit) {
    MedicalMateBottomCtaBar {
        MedicalMateButton(
            label =
            if (count == 0) {
                stringResource(R.string.brief_card_list_delete)
            } else {
                stringResource(R.string.brief_card_list_delete_count, count)
            },
            onClick = onDeleteClick,
            type = MedicalMateButtonType.DANGER,
            enabled = count > 0,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 불러오지 못했을 때. 빈 목록과 다르게 다시 시도를 준다. */
@Composable
private fun ColumnScope.FailedContent(onRetryClick: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = MedicalMateSize.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.brief_card_list_failed_title),
            description = stringResource(R.string.brief_card_list_failed_description),
            actionLabel = stringResource(R.string.brief_card_list_retry),
            onActionClick = onRetryClick,
        )
    }
}

/** 카드가 하나도 없을 때. 1j-2와 같은 자리에 같은 짜임이다. */
@Composable
private fun ColumnScope.EmptyContent(onStartIntakeClick: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = MedicalMateSize.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RECORD,
            title = stringResource(R.string.brief_card_list_empty_title),
            description = stringResource(R.string.brief_card_list_empty_description),
            actionLabel = stringResource(R.string.brief_card_list_empty_action),
            onActionClick = onStartIntakeClick,
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun BriefCardListScreenPreview() {
    MedicalMateTheme {
        BriefCardListScreen(
            state = BriefCardListUiState.Content(groups = previewBriefCardGroups),
            callbacks = BriefCardListCallbacks(),
        )
    }
}

/** 1j-4-D2. 두 장을 고른 편집 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun BriefCardListEditingPreview() {
    MedicalMateTheme {
        BriefCardListScreen(
            state =
            BriefCardListUiState.Content(
                groups = previewBriefCardGroups,
                selectedIds = setOf("card-1", "card-2"),
            ),
            callbacks = BriefCardListCallbacks(),
        )
    }
}
