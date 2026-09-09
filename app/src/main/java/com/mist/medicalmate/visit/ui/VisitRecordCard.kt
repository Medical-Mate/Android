package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuoteBlock
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltip

/**
 * 분류 결과 카드. Figma `587:3134`.
 *
 * 나눈 항목과 원문 메모를 한 장에 담는다. 파일을 나눈 이유는 화면 쪽의 함수 수다.
 */
@Composable
internal fun VisitRecordCard(state: VisitRecordUiState.Content, callbacks: VisitRecordCallbacks) {
    MedicalMateCard(
        // 수정 중에는 카드 전체를 브랜드 테두리로 감싼다. 값마다 밑줄이 생기지만 밑줄만으로는
        // 몇 줄이 열렸는지가 보이지 않는다. 브리핑 카드의 전체 수정(1e-1-E)과 같은 처리다.
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
    ) {
        Head(clinicLine = state.record.clinicLine, onEditClick = callbacks.onEditClick)
        MedicalMateDivider()
        state.record.items.forEachIndexed { index, item ->
            MedicalMateKvRow(
                key = item.key,
                value = state.drafts.getOrNull(index) ?: item.value,
                type = rowType(item = item, editing = state.editing),
                onValueChange =
                if (state.editing) {
                    { value -> callbacks.onDraftChange(index, value) }
                } else {
                    null
                },
            )
        }
        MedicalMateDivider()
        Memo(memo = state.record.memo)
        if (!state.editing) Caption(state.record.caption)
    }
}

@Composable
private fun Head(clinicLine: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.visit_record_card_title),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        MedicalMateIconButton(
            onClick = onEditClick,
            icon = MedicalMateIcons.Edit,
            contentDescription = stringResource(R.string.visit_record_edit),
            size = MedicalMateIconButtonSize.L,
        )
    }
    Text(
        text = clinicLine,
        style = MedicalMateTheme.typography.bodyS,
        color = MedicalMateTheme.colors.fgSubtle,
    )
}

/** 원문 메모. 나눈 값 아래에 그대로 남긴다. */
@Composable
private fun Memo(memo: String) {
    Text(
        text = stringResource(R.string.visit_record_memo),
        style = MedicalMateTheme.typography.labelS,
        color = MedicalMateTheme.colors.fgSubtle,
    )
    MedicalMateQuoteBlock(
        label = stringResource(R.string.visit_record_memo_quote),
        text = memo,
    )
}

/** AI가 몇 가지로 나눴는지. 옆의 툴팁이 무엇을 기준으로 나눴는지 알린다. */
@Composable
private fun Caption(caption: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = caption,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        MedicalMateTooltip(
            text = stringResource(R.string.visit_record_caption_tooltip),
            contentDescription = stringResource(R.string.visit_record_caption_tooltip_label),
        )
    }
}

/** 수정 중에는 모든 행이 입력이 된다. 재방문만 브랜드색으로 세운다. */
private fun rowType(item: VisitRecordItem, editing: Boolean): MedicalMateKvRowType = when {
    editing -> MedicalMateKvRowType.EDITING
    item.tone == VisitRecordItem.Tone.LINK -> MedicalMateKvRowType.LINK
    else -> MedicalMateKvRowType.DEFAULT
}

/** 시안 1q-1-E의 테두리 두께. */
private val EditingBorderWidth = 2.dp
