package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateFooter
import com.mist.medicalmate.core.designsystem.component.MedicalMateHintRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingValueRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTimeWheel
import com.mist.medicalmate.core.voice.rememberVoiceScrollState
import com.mist.medicalmate.core.voice.voicePanelSpace
import java.time.LocalTime

/**
 * 알림 시간. 10-15 시안 1s-3 `3663:15830`(#316). 내 정보의 `알림 시간` 줄에서 들어온다.
 *
 * 위 카드는 시각을 정한 일정의 규칙을 읽기만 하게 보인다(진료 24시간 전 · 2시간 뒤). 아래 카드의 휠이 시간 미정
 * 일정의 알림 시각이고, 전날 진료 전 알림과 당일 진료 후 기록 알림에 함께 걸린다(사용자 결정). 하단의
 * `Hint Row`가 고른 시각으로 예를 든다.
 *
 * 휠은 고르기만 하고 `확인`을 눌러야 저장된다. 저장이 안 되면 버튼 위에 이유를 적는다 — 시안에 없는 줄이다.
 */
@Composable
fun ReminderTimeScreen(
    state: ReminderTimeUiState,
    onBackClick: () -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MedicalMateTheme.colors.bgSurface)) {
        MedicalMateNavBar(
            title = stringResource(R.string.reminder_time_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberVoiceScrollState()).voicePanelSpace()
                .padding(
                    start = MedicalMateSize.gutter,
                    end = MedicalMateSize.gutter,
                    top = MedicalMateSpace.s8,
                    bottom = MedicalMateSpace.s16,
                ),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        ) {
            MedicalMateSectionHeader(title = stringResource(R.string.reminder_time_timed))
            MedicalMateSettingCard {
                MedicalMateSettingValueRow(
                    title = stringResource(R.string.my_profile_setting_visit_reminder),
                    value = stringResource(R.string.reminder_time_before_value),
                )
                MedicalMateSettingValueRow(
                    title = stringResource(R.string.my_profile_setting_record_reminder),
                    value = stringResource(R.string.reminder_time_after_value),
                )
            }
            MedicalMateSectionHeader(title = stringResource(R.string.reminder_time_untimed))
            MedicalMateSettingCard {
                // 서버 값을 읽기 전에 그리면 기본값에서 시작했다가 읽은 값으로 튄다.
                if (state.loaded) {
                    MedicalMateTimeWheel(
                        time = state.time,
                        onTimeChange = onTimeChange,
                        meridiemLabels = meridiemLabels(),
                    )
                }
            }
        }
        ReminderTimeFooter(state = state, onConfirmClick = onConfirmClick)
    }
}

/** 하단. 고른 시각으로 예를 드는 `Hint Row`(bell)와 `확인`이다. 저장이 안 되면 그 사이에 이유를 적는다. */
@Composable
private fun ReminderTimeFooter(state: ReminderTimeUiState, onConfirmClick: () -> Unit) {
    MedicalMateFooter {
        MedicalMateHintRow(
            icon = MedicalMateIcons.Bell,
            label = stringResource(R.string.reminder_time_hint, reminderTimeValue(state.time)),
        )
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.reminder_time_save_failed),
                style = MedicalMateTheme.typography.bodyS,
                color = MedicalMateTheme.colors.fgDanger,
            )
        }
        MedicalMateButton(
            onClick = onConfirmClick,
            label = stringResource(R.string.reminder_time_confirm),
            enabled = state.loaded && !state.saving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun ReminderTimeScreenPreview() {
    MedicalMateTheme {
        ReminderTimeScreen(
            state = ReminderTimeUiState(loaded = true),
            onBackClick = {},
            onTimeChange = {},
            onConfirmClick = {},
        )
    }
}
