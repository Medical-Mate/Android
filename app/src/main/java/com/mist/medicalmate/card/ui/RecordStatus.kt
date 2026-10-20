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
 *
 * **색은 `Badge` 마스터(`311:834`)의 변이 이름을 따른다**(#338). 마스터가 Brand를 "진료 전", Neutral을 "작성중",
 * Success를 "완료", Warning을 "임시 저장"으로 그려 둔다. 시안 1j-4는 작성 중을 노랑으로 그렸지만 그때는 임시 저장이
 * 없었다 — 둘이 함께 서면 마스터대로 갈라야 구별된다.
 */
@Composable
internal fun recordStatusLabel(status: RecordItem.Status): String = stringResource(
    when (status) {
        RecordItem.Status.IN_PROGRESS -> R.string.record_status_in_progress
        RecordItem.Status.TEMP_SAVED -> R.string.record_status_temp_saved
        RecordItem.Status.BEFORE_VISIT -> R.string.record_status_before_visit
        RecordItem.Status.CONFIRMED -> R.string.record_status_confirmed
    },
)

internal fun recordStatusTone(status: RecordItem.Status): MedicalMateBadgeTone = when (status) {
    RecordItem.Status.IN_PROGRESS -> MedicalMateBadgeTone.NEUTRAL
    RecordItem.Status.TEMP_SAVED -> MedicalMateBadgeTone.WARNING
    RecordItem.Status.BEFORE_VISIT -> MedicalMateBadgeTone.BRAND
    RecordItem.Status.CONFIRMED -> MedicalMateBadgeTone.SUCCESS
}
