package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateAvatar
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateNoticeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingNavigationRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingToggleRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.voice.rememberVoiceScrollState
import com.mist.medicalmate.core.voice.voicePanelSpace
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthStatus

/**
 * 와이어프레임 1s-1. 10-15 시안 `3663:15766`(#316).
 *
 * 홈 헤더의 아바타에서 들어온다. 프로필 카드 아래로 건강 정보 · 알림 · 메이트 · 계정 · 앱 정보가 차례로 선다.
 * 덩이 사이가 14이고 구역 머리는 `Section Header`, 줄은 `Setting Row`다.
 *
 * 건강 정보는 요약만 보여주고 고치는 것은 1s-2로 넘긴다. 진료 때 보여줄 값이라 실수로
 * 바뀌면 안 되고, 이 화면은 확인하는 자리다.
 *
 * **이용약관 · 개인정보 처리방침은 자리만 있다.** 출시 준비 때 문서를 붙인다(사용자 결정). 앱 버전은 빌드의
 * 버전 이름이다.
 */
@Composable
fun MyProfileScreen(
    state: MyProfileUiState,
    accountActions: AccountActionCallbacks,
    callbacks: MyProfileCallbacks,
    modifier: Modifier = Modifier,
    voiceWakeCallbacks: VoiceWakeCallbacks = VoiceWakeCallbacks(),
    onDeviceCallbacks: OnDeviceCallbacks = OnDeviceCallbacks(),
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.my_profile_title),
            onLeadingClick = callbacks.onBackClick,
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
            ProfileCard(state.profile)
            MedicalMateSectionHeader(
                title = stringResource(R.string.my_profile_health),
                actionLabel = stringResource(R.string.my_profile_health_edit),
                onActionClick = callbacks.onHealthEditClick,
            )
            HealthCard(state.health)
            ReminderSection(state = state, callbacks = callbacks)
            MateSection(
                state = state,
                onSettingChange = callbacks.onSettingChange,
                onDeleteVoiceModel = voiceWakeCallbacks.onDeleteModel,
                onDeleteOnDeviceModel = onDeviceCallbacks.onDeleteModel,
            )
            MedicalMateSectionHeader(title = stringResource(R.string.my_profile_account))
            AccountActions(accountActions = accountActions)
            AppInfoSection(appVersion = callbacks.appVersion)
        }
    }
}

/**
 * 내 정보에서 나가는 길과 화면 안의 조작.
 *
 * [notificationsBlocked]는 휴대폰 알림 권한이 꺼져 있는지다. 기기 설정이라 라우트가 읽어 넘긴다.
 */
data class MyProfileCallbacks(
    val onBackClick: () -> Unit = {},
    val onHealthEditClick: () -> Unit = {},
    val onReminderTimeClick: () -> Unit = {},
    val onSettingChange: (AppSetting, Boolean) -> Unit = { _, _ -> },
    val notificationsBlocked: Boolean = false,
    val appVersion: String = "",
)

/**
 * 알림. 진료 전 · 진료 후 기록 토글과 `알림 시간` 줄이다. 토글 설명과 줄의 값이 시간 미정 일정의 알림 시각을
 * 적는다.
 *
 * 휴대폰 알림이 꺼져 있으면 카드 아래 `Notice`(Warning)가 선다. 토글을 켜 둬도 그 상태로는 알림이 오지 않는다.
 */
@Composable
private fun ReminderSection(state: MyProfileUiState, callbacks: MyProfileCallbacks) {
    val untimed = reminderTimeWords(state.untimedReminderTime)
    MedicalMateSectionHeader(title = stringResource(R.string.my_profile_reminders))
    MedicalMateSettingCard {
        MedicalMateSettingToggleRow(
            title = stringResource(AppSetting.VISIT_REMINDER.labelRes),
            description = stringResource(R.string.my_profile_visit_reminder_description, untimed),
            checked = state.isOn(AppSetting.VISIT_REMINDER),
            onCheckedChange = { callbacks.onSettingChange(AppSetting.VISIT_REMINDER, it) },
        )
        MedicalMateSettingToggleRow(
            title = stringResource(AppSetting.RECORD_REMINDER.labelRes),
            description = stringResource(R.string.my_profile_record_reminder_description, untimed),
            checked = state.isOn(AppSetting.RECORD_REMINDER),
            onCheckedChange = { callbacks.onSettingChange(AppSetting.RECORD_REMINDER, it) },
        )
        MedicalMateSettingNavigationRow(
            title = stringResource(R.string.my_profile_reminder_time),
            value = reminderTimeValue(state.untimedReminderTime),
            onClick = callbacks.onReminderTimeClick,
        )
    }
    if (callbacks.notificationsBlocked) {
        MedicalMateNotice(
            title = stringResource(R.string.my_profile_notifications_off_title),
            body = stringResource(R.string.my_profile_notifications_off_body),
            tone = MedicalMateNoticeTone.WARNING,
        )
    }
}

/**
 * 메이트. 헤이 메이트로 부르기와, 시험용으로 남긴 휴대폰에서 기록 정리(사용자 결정)다. 둘 다 이 기기에서 켤 수
 * 있을 때만 줄이 서고, 둘 다 없으면 구역째 감춘다.
 *
 * 모델을 받는 중이거나 실패한 사정, 꺼 둔 채 남은 모델을 지우는 버튼은 시안에 없는 줄이라 카드 아래에 둔다.
 */
@Composable
private fun MateSection(
    state: MyProfileUiState,
    onSettingChange: (AppSetting, Boolean) -> Unit,
    onDeleteVoiceModel: () -> Unit,
    onDeleteOnDeviceModel: () -> Unit,
) {
    val settings = state.mateSettings
    if (settings.isEmpty()) return
    MedicalMateSectionHeader(title = stringResource(R.string.my_profile_mate))
    Column {
        MedicalMateSettingCard {
            settings.forEach { setting ->
                MedicalMateSettingToggleRow(
                    title = stringResource(setting.labelRes),
                    description =
                    when (setting) {
                        AppSetting.VOICE_WAKE -> stringResource(R.string.my_profile_voice_wake_description)
                        AppSetting.CLEAN_MIC -> stringResource(R.string.my_profile_clean_mic_description)
                        else -> null
                    },
                    checked = state.isOn(setting),
                    onCheckedChange = { onSettingChange(setting, it) },
                )
            }
        }
        VoiceWakeStatusLine(state.voiceWake.status)
        if (state.canDeleteVoiceModel) VoiceWakeModelDelete(onClick = onDeleteVoiceModel)
        OnDeviceStatusLine(state.onDevice.status)
        if (state.canDeleteOnDeviceModel) OnDeviceModelDelete(onClick = onDeleteOnDeviceModel)
    }
}

/** 앱 정보. 이용약관 · 개인정보 처리방침은 출시 준비 때 붙일 자리라 눌러도 아무 일이 없다(사용자 결정). */
@Composable
private fun AppInfoSection(appVersion: String) {
    MedicalMateSectionHeader(title = stringResource(R.string.my_profile_app_info))
    MedicalMateSettingCard {
        MedicalMateSettingNavigationRow(title = stringResource(R.string.my_profile_terms), onClick = {})
        MedicalMateSettingNavigationRow(title = stringResource(R.string.my_profile_privacy), onClick = {})
        MedicalMateSettingNavigationRow(
            title = stringResource(R.string.my_profile_app_version),
            value = appVersion,
            onClick = {},
        )
    }
}

/**
 * 프로필 카드.
 *
 * 시안에는 오른쪽에 chevron이 있는데 가리키는 화면이 없다. 이름·생년·성별은 카카오에서
 * 오는 값이고, 와이어프레임에 그것을 고치는 화면이 없다. 신상정보 입력(1b)은 온보딩
 * 흐름이라 끝나면 홈으로 나가므로 수정 진입으로 재사용할 수 없다.
 *
 * `List Row`의 규칙과 같은 이유로 비워 둔다 — 눌러도 아무 일이 없는데 chevron이 있으면
 * 사용자가 눌러본다. 수정 화면이 정해지면 chevron과 함께 붙인다.
 */
@Composable
private fun ProfileCard(profile: MyProfile) {
    // 시안 `Profile`. 사방 18 · 아바타와 글 사이 14 · 글 줄 사이 3이다.
    MedicalMateSettingCard(verticalPadding = ProfileCardPadding) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicalMateAvatar(
                initial = profile.initial,
                size = AvatarSize,
                contentDescription = null,
            )
            Column(verticalArrangement = Arrangement.spacedBy(ProfileLineGap)) {
                profile.name?.let { name ->
                    Text(
                        text = name,
                        style = MedicalMateTheme.typography.headingM,
                        color = MedicalMateTheme.colors.fgDefault,
                    )
                }
                metaLine(profile)?.let { meta ->
                    Text(
                        text = meta,
                        style = MedicalMateTheme.typography.bodyS,
                        color = MedicalMateTheme.colors.fgSubtle,
                    )
                }
                Text(
                    text = stringResource(R.string.my_profile_login_kakao),
                    style = MedicalMateTheme.typography.labelS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
        }
    }
}

/**
 * 생년과 성별을 한 줄로. "1994년생 · 여".
 *
 * 둘 다 없으면 null이고 그 줄을 그리지 않는다. 카카오 동의를 거부한 계정이 그렇다.
 */
@Composable
private fun metaLine(profile: MyProfile): String? = listOfNotNull(
    profile.birthYear?.let { stringResource(R.string.my_profile_birth_year, it) },
    profile.sex?.let { stringResource(it.labelRes) },
).joinToString(" · ").takeIf { it.isNotBlank() }

/** 알러지만 브랜드색으로 세운다. 진료 때 먼저 전해야 하는 값이다. 카드는 위아래 4다(시안 `Health Card`). */
@Composable
private fun HealthCard(health: HealthSummary) {
    MedicalMateSettingCard(verticalPadding = MedicalMateSpace.s4) {
        MedicalMateKvRow(
            key = stringResource(R.string.my_profile_health_medications),
            value = summaryText(health.medications),
        )
        MedicalMateKvRow(
            key = stringResource(R.string.my_profile_health_conditions),
            value = summaryText(health.conditions),
        )
        MedicalMateKvRow(
            key = stringResource(R.string.my_profile_health_allergies),
            value = summaryText(health.allergies),
            type = MedicalMateKvRowType.LINK,
        )
    }
}

/**
 * 요약 한 줄의 문구.
 *
 * 적어 둔 것이 없을 때 "없어요"와 "잘 모르겠어요"를 가른다. 알러지에서 둘은 처방이
 * 달라지는 값이라 같은 말로 뭉뚱그리면 안 된다. 지금 화면에는 "없어요"를 말할 자리가
 * 없어서 서버가 그렇게 들고 있을 때만 나온다(#150).
 */
@Composable
private fun summaryText(field: HealthField): String = when (field.status) {
    HealthStatus.KNOWN -> field.items.joinToString(" · ")
    HealthStatus.NONE -> stringResource(R.string.my_profile_health_none)
    HealthStatus.UNKNOWN -> stringResource(R.string.my_profile_health_unknown)
}

/** 시안의 아바타는 이 화면에서만 56이다. 홈 헤더는 44다. */
private val AvatarSize = 56.dp

/** 시안 `Profile` 카드의 사방 18과 글 줄 사이 3. 토큰에 없는 값이다. */
private val ProfileCardPadding = 18.dp

private val ProfileLineGap = 3.dp

@MedicalMateScreenPreviews
@Composable
private fun MyProfileScreenPreview() {
    MedicalMateTheme {
        MyProfileScreen(
            state = previewMyProfile,
            accountActions = AccountActionCallbacks(),
            callbacks = MyProfileCallbacks(appVersion = "1.0.0"),
        )
    }
}
