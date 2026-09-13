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
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateEditingKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuoteBlock
import com.mist.medicalmate.core.designsystem.component.MedicalMateRowDelete
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
        Head(clinicLine = state.record.clinicLine)
        MedicalMateDivider()
        state.items.forEachIndexed { index, item ->
            if (state.editing) {
                MedicalMateEditingKvRow(
                    key = item.key,
                    value = item.value,
                    onValueChange = { value -> callbacks.edit.onItemValueChange(index, value) },
                    delete =
                    MedicalMateRowDelete(
                        contentDescription = stringResource(R.string.visit_record_item_delete, item.key),
                        onClick = { callbacks.edit.onItemDeleteClick(index) },
                    ),
                )
            } else {
                MedicalMateKvRow(
                    key = item.key,
                    value = item.value,
                    type = rowType(item = item, editing = false),
                )
            }
        }
        MedicalMateDivider()
        Memo(memo = state.record.memo)
        state.record.classifiedCount?.takeIf { !state.editing }?.let { Caption(it) }
    }
}

/**
 * 카드 머리.
 *
 * 편집 진입은 여기 없다. 카드 안 연필을 Nav 우측 `편집`으로 옮겼다. 문서의 CRUD 규칙이
 * 편집 상태를 Nav 한 자리에서 바꾸기로 정했고, 카드 안에 연필을 남기면 진입이 두 곳이 된다.
 */
@Composable
private fun Head(clinicLine: String) {
    Text(
        text = stringResource(R.string.visit_record_card_title),
        style = MedicalMateTheme.typography.headingS,
        color = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.fillMaxWidth(),
    )
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
private fun Caption(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.visit_record_caption, count),
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
