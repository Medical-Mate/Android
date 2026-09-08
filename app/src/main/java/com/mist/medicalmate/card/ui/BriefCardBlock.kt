package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateCallout
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateNoticeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateSeverityReadout
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltip

/**
 * 브리핑 카드 본체. Figma `404:1711`.
 *
 * 1e-1 읽기, 1e-1-E 수정, 1f-1 진료실 화면이 같은 블록을 쓴다. 세 화면이 각자 그리면
 * 의사가 보는 카드와 환자가 고치는 카드가 어긋난다.
 *
 * [onEditClick]이 없으면 수정 연필을 그리지 않는다. 진료실 화면에서 의사가 환자의 카드를
 * 고칠 일이 없다.
 *
 * [editing]이면 값들이 입력 상태로 열린다. 항목별이 아니라 카드 전체가 한 번에 열린다.
 */
@Composable
internal fun BriefCardBlock(
    card: BriefCard,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    draftAt: (Int) -> String = { card.items[it].value },
    onDraftChange: (Int, String) -> Unit = { _, _ -> },
    onEditClick: (() -> Unit)? = null,
    showAiCaption: Boolean = true,
) {
    MedicalMateCard(modifier = modifier) {
        CardHead(card = card, onEditClick = onEditClick)
        MedicalMateDivider()
        card.items.forEachIndexed { index, item ->
            MedicalMateKvRow(
                key = item.key,
                value = if (editing) draftAt(index) else item.value,
                type = kvRowType(item = item, editing = editing),
                onValueChange = { onDraftChange(index, it) },
            )
        }
        card.severity?.let { MedicalMateSeverityReadout(severity = it) }
        if (showAiCaption && !editing) {
            AiCaption()
        }
    }
}

/**
 * 값이 어떤 모양으로 그려질지.
 *
 * 수정 중에는 전부 입력 상태다. 읽을 때는 AI가 세운 한 항목만 강조로 둔다.
 */
private fun kvRowType(item: BriefCardItem, editing: Boolean): MedicalMateKvRowType = when {
    editing -> MedicalMateKvRowType.EDITING
    item.emphasized -> MedicalMateKvRowType.EMPHASIS
    else -> MedicalMateKvRowType.DEFAULT
}

/** 제목·상태 배지·환자 줄과 수정 연필. Figma `596:3160`. */
@Composable
private fun CardHead(card: BriefCard, onEditClick: (() -> Unit)?) {
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
                    text = card.title,
                    style = MedicalMateTheme.typography.headingM,
                    color = MedicalMateTheme.colors.fgDefault,
                    modifier = Modifier.weight(1f),
                )
                MedicalMateBadge(label = statusLabel(card.status), tone = statusTone(card.status))
            }
            Text(
                text = card.patientLine,
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        onEditClick?.let {
            MedicalMateIconButton(
                onClick = it,
                icon = MedicalMateIcons.Edit,
                contentDescription = stringResource(R.string.brief_card_edit),
            )
        }
    }
}

@Composable
private fun statusLabel(status: BriefCard.Status): String = stringResource(
    when (status) {
        BriefCard.Status.BEFORE_VISIT -> R.string.brief_card_status_before_visit
        BriefCard.Status.CONFIRMED -> R.string.brief_card_status_confirmed
    },
)

private fun statusTone(status: BriefCard.Status): MedicalMateBadgeTone = when (status) {
    BriefCard.Status.BEFORE_VISIT -> MedicalMateBadgeTone.NEUTRAL
    BriefCard.Status.CONFIRMED -> MedicalMateBadgeTone.SUCCESS
}

/**
 * 카드 맨 아래 한 줄. Figma `605:6276`.
 *
 * 이 카드가 AI가 정리한 것이라는 사실을 밝힌다. 툴팁으로 어떻게 정리했는지를 덧붙인다.
 * 수정 중에는 감춘다. 고치는 동안에는 누가 썼는지가 아니라 무엇을 고치는지가 화면의
 * 일이다.
 */
@Composable
private fun AiCaption() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.brief_card_ai_caption),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
            modifier = Modifier.weight(1f),
        )
        MedicalMateTooltip(
            text = stringResource(R.string.brief_card_ai_tooltip),
            contentDescription = stringResource(R.string.brief_card_ai_tooltip_open),
        )
    }
}

/**
 * 알러지 경고. Figma `404:1699`.
 *
 * 신상정보에서 고른 값이 그대로 올라온다. 카드 맨 위에 항상 표시되는 항목이라 카드 안이
 * 아니라 카드 밖 경고 면에 둔다. 처방 전에 확인해야 하는 것이고, 카드 안에 섞으면 훑어
 * 읽을 때 다른 항목과 같은 무게로 지나간다.
 */
@Composable
internal fun AllergyNotice(allergies: List<String>, modifier: Modifier = Modifier) {
    if (allergies.isEmpty()) return

    MedicalMateNotice(
        title = stringResource(R.string.brief_card_allergy, allergies.joinToString(" · ")),
        body = stringResource(R.string.brief_card_allergy_body),
        tone = MedicalMateNoticeTone.WARNING,
        modifier = modifier,
    )
}

/** 환자가 묻고 싶어 하는 것. Figma `293:657`. 적어둔 질문이 없으면 두지 않는다. */
@Composable
internal fun QuestionsCallout(questions: List<String>, modifier: Modifier = Modifier) {
    if (questions.isEmpty()) return

    MedicalMateCallout(
        title = stringResource(R.string.brief_card_questions),
        questions = questions,
        modifier = modifier,
    )
}

/**
 * 진료실 화면 맨 위 안내. Figma `404:1837`.
 *
 * 폰을 건네받은 의사가 처음 읽는 줄이다. 이 화면이 환자가 미리 정리한 것이라는 사실을
 * 밝힌다. 툴팁으로 무엇을 어떻게 정리했는지를 덧붙인다.
 */
@Composable
internal fun HandoffHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgSubtle, shape = MedicalMateRadius.md)
            .padding(start = MedicalMateSpace.s16, end = MedicalMateSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.handoff_header),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = MedicalMateSpace.s14),
        )
        MedicalMateTooltip(
            text = stringResource(R.string.handoff_tooltip),
            contentDescription = stringResource(R.string.handoff_tooltip_open),
        )
    }
}
