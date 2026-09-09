package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone

/**
 * 기록 상태 배지의 문구와 색.
 *
 * 목록(1j-1)과 상세(1j-3)가 같은 배지를 쓴다. 상태별 색이 두 화면에서 갈리면 같은 건이
 * 다르게 읽히므로 한곳에 둔다.
 */
@Composable
internal fun recordStatusLabel(status: RecordItem.Status): String = stringResource(
    when (status) {
        RecordItem.Status.DRAFT -> R.string.record_status_draft
        RecordItem.Status.BEFORE_VISIT -> R.string.record_status_before_visit
        RecordItem.Status.CONFIRMED -> R.string.record_status_confirmed
    },
)

internal fun recordStatusTone(status: RecordItem.Status): MedicalMateBadgeTone = when (status) {
    RecordItem.Status.DRAFT -> MedicalMateBadgeTone.WARNING
    RecordItem.Status.BEFORE_VISIT -> MedicalMateBadgeTone.BRAND
    RecordItem.Status.CONFIRMED -> MedicalMateBadgeTone.SUCCESS
}
