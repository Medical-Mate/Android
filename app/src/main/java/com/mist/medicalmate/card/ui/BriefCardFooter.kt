package com.mist.medicalmate.card.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateFooter
import java.time.LocalDate

/**
 * 하단. 10-15의 그림자 없는 `Footer`다(#314).
 *
 * 편집 중에는 `브리핑 카드 삭제` 하나다. 사본을 옮기는 것은 Nav 우측 `확인`이 하고 되돌리는 것은 `취소`가 한다.
 * 개체를 통째로 지우는 것이라 문서가 하단 Danger CTA와 확인 대화상자를 함께 요구한다.
 *
 * 읽는 중에는 시안 1e-1 네 장대로 그 카드의 저장 여부와 일정으로 갈린다([footerAction]). 저장하지 않았으면
 * `저장하기`, 일정이 없으면 `진료 일정 잡기`, 앞날 일정이면 Outline `진료 일정 보기`, 당일이거나 지났으면
 * Tonal `진료 후 기록하기`다.
 *
 * 저장에 실패하면 버튼 위에 왜 안 됐는지를 적는다. 아무 말도 하지 않으면 버튼이 안 먹는 것으로 읽고 계속 누르게
 * 된다 — 기기에서 실제로 그랬다(#196·#198). 버튼은 살려 둔다.
 */
@Composable
internal fun BriefCardFooter(state: BriefCardUiState.Content, today: LocalDate, callbacks: BriefCardCallbacks) {
    MedicalMateFooter {
        if (state.editing) {
            MedicalMateButton(
                onClick = callbacks.onDeleteClick,
                label = stringResource(R.string.brief_card_delete),
                type = MedicalMateButtonType.DANGER,
                modifier = Modifier.fillMaxWidth(),
            )
            return@MedicalMateFooter
        }
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.brief_card_save_failed),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgDanger,
            )
        }
        val appointment = state.card.appointment
        val (label, type, onClick) =
            when (state.card.footerAction(today)) {
                BriefCardFooterAction.SAVE ->
                    Triple(R.string.brief_card_save, MedicalMateButtonType.PRIMARY, callbacks.onSaveClick)
                BriefCardFooterAction.SCHEDULE ->
                    Triple(R.string.brief_card_schedule, MedicalMateButtonType.PRIMARY, callbacks.onScheduleClick)
                BriefCardFooterAction.VIEW_SCHEDULE ->
                    Triple(R.string.brief_card_view_schedule, MedicalMateButtonType.OUTLINE) {
                        if (appointment != null) callbacks.onViewScheduleClick(appointment)
                    }
                BriefCardFooterAction.RECORD_VISIT ->
                    Triple(R.string.brief_card_record_visit, MedicalMateButtonType.TONAL) {
                        if (appointment != null) callbacks.onRecordVisitClick(appointment)
                    }
            }
        MedicalMateButton(
            onClick = onClick,
            label = stringResource(label),
            type = type,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 저장 직후 일정 시트. Figma 1e-1-S `3681:17846`(#314).
 *
 * 카드를 처음 저장했고 그 카드로 잡은 일정이 없을 때 뜬다. 그래버 아래 14 · 제목 · 4 · 설명 · 20 · Primary
 * `진료 일정 잡기` · 8 · Ghost `아직 안 정했어요`다. 안 정했다고 하면 시트만 닫고, 하단이 `진료 일정 잡기`로
 * 남아 나중에 잡을 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BriefCardScheduleSheet(visible: Boolean, onScheduleClick: () -> Unit, onDismiss: () -> Unit) {
    if (!visible) return
    MedicalMateBottomSheet(onDismissRequest = onDismiss, grabberGap = MedicalMateSpace.s14) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHead(
                title = stringResource(R.string.brief_card_schedule_sheet_title),
                body = stringResource(R.string.brief_card_schedule_sheet_body),
            )
            Spacer(modifier = Modifier.height(MedicalMateSpace.s20))
            MedicalMateButton(
                onClick = onScheduleClick,
                label = stringResource(R.string.brief_card_schedule),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
            MedicalMateButton(
                onClick = onDismiss,
                label = stringResource(R.string.brief_card_schedule_sheet_later),
                type = MedicalMateButtonType.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
