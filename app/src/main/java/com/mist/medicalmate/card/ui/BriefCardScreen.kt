package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1e-1과 1e-1-E. Figma `404:1679`, `597:4804`.
 *
 * 문답을 마치면 AI가 정리한 카드가 여기 나온다. 읽기와 전체 수정이 같은 화면의 두 모드다.
 *
 * 수정 중에는 하단이 저장하기와 취소 둘이 된다. 읽을 때는 저장하기와 진료실에서
 * 보여주기다.
 *
 * **"진료실에서 보여주기"는 시안에 없다.** 1e-1의 하단에는 저장하기만 있는데 플로우는
 * 1e-1에서 진료실 화면(1f-1)으로 이어진다. 그대로 두면 그 화면에 갈 방법이 없어서
 * 보조 버튼으로 뒀고 디자인 트랙에 남겼다(#72).
 */
@Composable
fun BriefCardScreen(state: BriefCardUiState, callbacks: BriefCardCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.brief_card_title),
            onLeadingClick = callbacks.onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        when (state) {
            BriefCardUiState.Loading ->
                MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))

            BriefCardUiState.Failed ->
                FailedContent(
                    onRetryClick = callbacks.onRetryClick,
                    modifier = Modifier.weight(1f),
                )

            is BriefCardUiState.Content -> CardContent(state = state, callbacks = callbacks)
        }
    }
}

/**
 * 카드 화면에서 나가는 길들.
 *
 * 수정은 화면 안에서 모드만 바뀌므로 목적지가 아니다. 저장과 진료실 화면이 나가는 길이다.
 */
data class BriefCardCallbacks(
    val onBackClick: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onDraftChange: (Int, String) -> Unit = { _, _ -> },
    val onSaveClick: () -> Unit = {},
    val onCancelClick: () -> Unit = {},
    val onHandoffClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

@Composable
private fun ColumnScope.CardContent(state: BriefCardUiState.Content, callbacks: BriefCardCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        BriefCardBlock(
            card = state.card,
            // 수정 중에는 카드 전체를 브랜드 테두리로 감싼다. 값마다 밑줄이 생기지만
            // 밑줄만으로는 몇 줄이 열렸는지가 보이지 않는다. Figma 1e-1-E가 카드째로
            // 테두리를 두른 이유다.
            modifier =
            if (state.editing) {
                Modifier.border(
                    width = EditingBorderWidth,
                    color = MedicalMateTheme.colors.borderFocus,
                    shape = MedicalMateRadius.lg,
                )
            } else {
                Modifier
            },
            editing = state.editing,
            draftAt = state::draftAt,
            onDraftChange = callbacks.onDraftChange,
            onEditClick = if (state.editing) null else callbacks.onEditClick,
        )
        AllergyNotice(state.card.allergies)
        QuestionsCallout(state.card.questions)
    }
    Footer(editing = state.editing, callbacks = callbacks)
}

@Composable
private fun Footer(editing: Boolean, callbacks: BriefCardCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s8,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        MedicalMateButton(
            onClick = callbacks.onSaveClick,
            label = stringResource(R.string.brief_card_save),
            modifier = Modifier.fillMaxWidth(),
        )
        MedicalMateButton(
            onClick = if (editing) callbacks.onCancelClick else callbacks.onHandoffClick,
            label =
            stringResource(
                if (editing) R.string.brief_card_cancel else R.string.brief_card_handoff,
            ),
            type = MedicalMateButtonType.OUTLINE,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FailedContent(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.brief_card_failed_title),
            description = stringResource(R.string.brief_card_failed_description),
            actionLabel = stringResource(R.string.brief_card_retry),
            onActionClick = onRetryClick,
        )
    }
}

/** 문서 8.2가 포커스 테두리에 쓰는 두께. Figma 1e-1-E의 카드 테두리와 같다. */
private val EditingBorderWidth = 2.dp

/** Preview용 카드. AI가 넘겨줄 모양을 Figma 1e-1의 내용으로 채운 것이다. */
internal val previewBriefCard =
    BriefCard(
        id = "card-1",
        title = "복부 통증 · 3주",
        status = BriefCard.Status.BEFORE_VISIT,
        patientLine = "김OO · 32세 여 · 2026.09.04 작성",
        items =
        listOf(
            BriefCardItem(key = "부위", value = "복부 (명치 아래 · 배꼽 위)"),
            BriefCardItem(key = "기간", value = "3주 전 시작 · 최근 악화", emphasized = true),
            BriefCardItem(key = "양상", value = "식후 30분 뒤 쓰림 · 밤에 심해짐"),
            BriefCardItem(key = "복용약", value = "혈압약 · 진통제(증상 시)"),
            BriefCardItem(key = "기저질환", value = "고혈압"),
        ),
        severity = MedicalMateSeverity.LEVEL_3,
        allergies = listOf("페니실린"),
        questions =
        listOf(
            "검사를 받아야 하나요?",
            "지금 진통제 계속 먹어도 되나요?",
            "어떤 증상이면 바로 다시 와야 하나요?",
        ),
    )

@MedicalMateScreenPreviews
@Composable
private fun BriefCardScreenPreview() {
    MedicalMateTheme {
        BriefCardScreen(
            state = BriefCardUiState.Content(card = previewBriefCard),
            callbacks = BriefCardCallbacks(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun BriefCardEditingPreview() {
    MedicalMateTheme {
        BriefCardScreen(
            state = BriefCardUiState.Content(card = previewBriefCard, editing = true),
            callbacks = BriefCardCallbacks(),
        )
    }
}
