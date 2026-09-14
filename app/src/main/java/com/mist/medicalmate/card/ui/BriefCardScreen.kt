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
import androidx.compose.material3.Text
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
import com.mist.medicalmate.core.designsystem.component.MedicalMateCalloutEdit
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialog
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateHospitalCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1e-1과 1e-1-E. Figma `404:1679`, `597:4804`.
 *
 * 문답을 마치면 AI가 정리한 카드가 여기 나온다. 읽기와 편집이 같은 화면의 두 모드다.
 *
 * **편집 상태는 Nav 우측 한 자리에서 이름만 바뀐다.** 평소 `편집`, 편집 중 `취소`, 무엇이든
 * 바꾸면 `확인`이다. 문서의 CRUD 규칙이고, 아무것도 안 건드렸는데 `확인`이 떠 있으면 뭘
 * 확인하라는 건지 알 수 없다는 이유가 붙어 있다.
 *
 * 하단도 모드에 따라 갈린다. 읽을 때는 저장하기, 편집 중에는 `브리핑 카드 삭제`다. 개체를
 * 통째로 지우는 것이라 문서가 하단 Danger CTA와 확인 대화상자를 함께 요구한다.
 *
 * **"진료실에서 보여주기"는 시안에 없다.** 1e-1의 하단에는 저장하기만 있는데 플로우는
 * 1e-1에서 진료실 화면(1f-1)으로 이어진다. 그대로 두면 그 화면에 갈 방법이 없어서
 * 보조 버튼으로 뒀고 디자인 트랙에 남겼다(#72). 시안에서는 그 프레임이 지워졌다.
 */
@Composable
fun BriefCardScreen(state: BriefCardUiState, callbacks: BriefCardCallbacks, modifier: Modifier = Modifier) {
    val content = state as? BriefCardUiState.Content
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
            actionLabel = content?.let { stringResource(navActionLabel(it)) },
            onActionClick = content?.let { { onNavAction(it, callbacks) } },
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
    val onEditDoneClick: () -> Unit = {},
    val onCancelClick: () -> Unit = {},
    val edit: BriefCardEditActions = BriefCardEditActions {},
    val onSaveClick: () -> Unit = {},
    val onHospitalChangeClick: () -> Unit = {},
    val onDeleteClick: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

/**
 * Nav 우측 버튼의 이름. 한 자리에서 셋으로 갈린다.
 *
 * 편집 중이 아니면 `편집`, 편집 중이고 바뀐 것이 없으면 `취소`, 바뀐 것이 있으면 `확인`이다.
 */
private fun navActionLabel(content: BriefCardUiState.Content): Int = when {
    !content.editing -> R.string.brief_card_edit
    content.changed -> R.string.brief_card_edit_done
    else -> R.string.brief_card_cancel
}

private fun onNavAction(content: BriefCardUiState.Content, callbacks: BriefCardCallbacks) {
    when {
        !content.editing -> callbacks.onEditClick()
        content.changed -> callbacks.onEditDoneClick()
        else -> callbacks.onCancelClick()
    }
}

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
            // 편집 중에는 카드 전체를 브랜드 테두리로 감싼다. 값마다 밑줄이 생기지만
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
            items = state.items,
            editing = state.editing,
            onItemValueChange = callbacks.edit::onItemValueChange,
            onItemDeleteClick = if (state.editing) callbacks.edit::onItemDeleteClick else null,
        )
        // 편집 중에는 알러지 경고를 감춘다. 지울 수 없는 항목이라 x가 붙지 않는데, x가 없는
        // 블록이 편집 화면에 남아 있으면 왜 이것만 못 고치는지가 설명되지 않는다. 문서가
        // 지울 수 없는 것 목록에 이 경고를 넣고 편집 모드에서 숨기라고 적었다.
        if (!state.editing) {
            AllergyNotice(state.card.allergies)
        }
        QuestionsCallout(
            questions = state.questions,
            edit = if (state.editing) questionEdit(callbacks) else null,
        )
        HospitalSection(
            hospital = state.card.hospital,
            onChangeClick = callbacks.onHospitalChangeClick,
        )
    }
    Footer(state = state, callbacks = callbacks)

    if (state.deleteRequested) {
        MedicalMateDialog(
            title = stringResource(R.string.brief_card_delete_title),
            message = stringResource(R.string.brief_card_delete_body),
            confirmLabel = stringResource(R.string.brief_card_delete_confirm),
            onConfirm = callbacks.onDeleteConfirm,
            dismissLabel = stringResource(R.string.brief_card_cancel),
            onDismissRequest = callbacks.onDeleteDismiss,
        )
    }
}

/** 질문 편집에 필요한 문구와 조작을 모은다. 문구는 화면이 들고 조작은 사본이 받는다. */
@Composable
private fun questionEdit(callbacks: BriefCardCallbacks): MedicalMateCalloutEdit {
    val deleteLabelFormat = stringResource(R.string.brief_card_question_delete, QUESTION_NUMBER_SLOT)
    return MedicalMateCalloutEdit(
        addLabel = stringResource(R.string.brief_card_question_add),
        deleteContentDescription = { number ->
            deleteLabelFormat.replace(QUESTION_NUMBER_SLOT.toString(), number.toString())
        },
        placeholder = stringResource(R.string.brief_card_question_placeholder),
        onQuestionChange = callbacks.edit::onQuestionChange,
        onQuestionDelete = callbacks.edit::onQuestionDeleteClick,
        onQuestionAdd = callbacks.edit::onQuestionAddClick,
    )
}

/**
 * 진료받을 병원. Figma 1e-1의 마지막 섹션이다.
 *
 * **안 정했어도 섹션을 둔다.** 전에는 비어 있으면 통째로 감췄는데, 카드가 병원을 들게
 * 되면서(Backend#101) 그러면 정할 길이 사라진다 — `변경`이 이 섹션 안에 있다. 1m-B에
 * 건너뛰기가 있어서 안 정한 카드가 정상 상태이고, 그 카드도 나중에 정할 수 있어야 한다.
 *
 * 비었을 때의 낱말은 목록과 맞춘다. 서버 문서도 "안 골랐으면 병원 미정으로 찍으세요"라고
 * 적었다. 그 자리의 액션이 `변경`이 맞는지는 시안에 없다 — 디자인 트랙 확인 항목이다.
 *
 * `변경`은 진료 전 병원 찾기(1m-B)를 다시 연다.
 */
@Composable
private fun HospitalSection(hospital: BriefCardHospital?, onChangeClick: () -> Unit) {
    MedicalMateSectionHeader(
        title = stringResource(R.string.brief_card_hospital_section),
        actionLabel = stringResource(R.string.brief_card_hospital_change),
        onActionClick = onChangeClick,
    )
    MedicalMateHospitalCard(
        name = hospital?.name ?: stringResource(R.string.brief_card_hospital_unset),
        address = hospital?.address,
    )
}

/**
 * 하단.
 *
 * **이미 저장한 카드에는 하단이 없다**(#229). 홈이나 기록에서 여는 카드는 다시 보는
 * 자리인데 저장하기가 서 있으면 아직 저장이 안 된 것으로 읽힌다.
 *
 * 아직 저장하지 않은 카드에는 남긴다. 저장이 곧 확정이고, 확정하지 않은 카드에는 진료 후
 * 기록을 붙일 수 없다(#220). 저장 자리를 통째로 없애면 그 카드를 되살릴 길이 사라진다.
 *
 * [saveFailed]면 버튼 위에 왜 안 됐는지를 적는다. 아무 말도 하지 않으면 버튼이 안 먹는
 * 것으로 읽고 계속 누르게 된다 — 기기에서 실제로 그랬다(#196·#198). 버튼은 살려 둔다.
 * 다시 눌러야 하는 자리다.
 */
@Composable
private fun Footer(state: BriefCardUiState.Content, callbacks: BriefCardCallbacks) {
    val editing = state.editing
    val saveFailed = state.saveFailed
    if (!editing && state.card.status == BriefCard.Status.CONFIRMED) return

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
        if (editing) {
            // 편집 중 하단은 삭제 하나다. 사본을 옮기는 것은 Nav 우측 `확인`이 하고, 되돌리는
            // 것은 `취소`가 한다. 저장하기를 함께 두면 확인과 저장이 같은 일을 두 번 한다.
            MedicalMateButton(
                onClick = callbacks.onDeleteClick,
                label = stringResource(R.string.brief_card_delete),
                type = MedicalMateButtonType.DANGER,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            if (saveFailed) {
                Text(
                    text = stringResource(R.string.brief_card_save_failed),
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgDanger,
                )
            }
            // **진료실에서 보여주기가 없다.** IA가 진료실 전달을 1e-1로 대체하면서 그 버튼도
            // 사라졌다(#167 · #194). 저장하기가 카드를 확정하고 홈으로 나간다.
            MedicalMateButton(
                onClick = callbacks.onSaveClick,
                label = stringResource(R.string.brief_card_save),
                modifier = Modifier.fillMaxWidth(),
            )
        }
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

/** 문서의 컴포넌트 규격이 포커스 테두리에 쓰는 두께. Figma 1e-1-E의 카드 테두리와 같다. */
private val EditingBorderWidth = 2.dp

/**
 * 접근성 이름의 질문 번호 자리.
 *
 * `stringResource`는 컴포저블 안에서만 부를 수 있어서 질문마다 부를 수 없다. 한 번 받아 두고
 * 이 자리를 실제 번호로 바꾼다.
 */
private const val QUESTION_NUMBER_SLOT = 0

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
        hospital =
        BriefCardHospital(name = "서울OO병원 내과"),
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
            state = BriefCardUiState.Content(card = previewBriefCard, draft = BriefCardDraft.of(previewBriefCard)),
            callbacks = BriefCardCallbacks(),
        )
    }
}
