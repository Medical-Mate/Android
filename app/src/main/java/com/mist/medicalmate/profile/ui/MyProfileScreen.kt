package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateAvatar
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateKvRowType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateToggle
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthStatus

/**
 * 와이어프레임 1s-1. Figma `407:2375`.
 *
 * 홈 헤더의 아바타에서 들어온다. 하단 탭에 두지 않은 이유는 세 탭이 기록·홈·캘린더로
 * 확정됐기 때문이다(Tab Bar v2).
 *
 * 건강 정보는 요약만 보여주고 고치는 것은 1s-2로 넘긴다. 진료 때 보여줄 값이라 실수로
 * 바뀌면 안 되고, 이 화면은 확인하는 자리다.
 */
@Composable
fun MyProfileScreen(
    state: MyProfileUiState,
    accountActions: AccountActionCallbacks,
    onBackClick: () -> Unit,
    onHealthEditClick: () -> Unit,
    onSettingChange: (AppSetting, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.my_profile_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        ProfileContent(
            state = state,
            accountActions = accountActions,
            onHealthEditClick = onHealthEditClick,
            onSettingChange = onSettingChange,
        )
    }
}

@Composable
private fun ColumnScope.ProfileContent(
    state: MyProfileUiState,
    accountActions: AccountActionCallbacks,
    onHealthEditClick: () -> Unit,
    onSettingChange: (AppSetting, Boolean) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s8,
                bottom = MedicalMateSpace.s32,
            ),
    ) {
        ProfileCard(state.profile)
        MedicalMateSectionHeader(
            title = stringResource(R.string.my_profile_health),
            actionLabel = stringResource(R.string.my_profile_health_edit),
            onActionClick = onHealthEditClick,
        )
        HealthCard(state.health)
        MedicalMateSectionHeader(title = stringResource(R.string.my_profile_settings))
        SettingsCard(state = state, onSettingChange = onSettingChange)
        AccountActions(
            accountActions = accountActions,
            modifier = Modifier.padding(top = MedicalMateSpace.s24),
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
    MedicalMateCard {
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
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
                Text(
                    text = profile.name,
                    style = MedicalMateTheme.typography.headingM,
                    color = MedicalMateTheme.colors.fgDefault,
                )
                Text(
                    text = profile.meta,
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
                Text(
                    text = profile.login,
                    style = MedicalMateTheme.typography.bodyS,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
        }
    }
}

/** 알러지만 브랜드색으로 세운다. 진료 때 먼저 전해야 하는 값이다. */
@Composable
private fun HealthCard(health: HealthSummary) {
    RowCard {
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

/**
 * 설정.
 *
 * 아직 저장되지 않는다. 화면 안에서 켜고 끄는 것까지가 지금 범위이고, 기기에 남기려면
 * `DataStore`가, 계정에 남기려면 API가 필요하다(#85).
 */
@Composable
private fun SettingsCard(state: MyProfileUiState, onSettingChange: (AppSetting, Boolean) -> Unit) {
    RowCard {
        AppSetting.entries.forEach { setting ->
            MedicalMateToggle(
                checked = state.isOn(setting),
                onCheckedChange = { checked -> onSettingChange(setting, checked) },
                label = stringResource(setting.labelRes),
            )
        }
    }
}

/**
 * 행을 담는 카드.
 *
 * `Card` 컴포넌트를 쓰지 않는다. 그쪽은 안쪽 여백 20에 자식 사이 간격 6인데, 이 두 카드는
 * 행 자체가 높이 54~56에 여백을 갖고 있어서 카드가 위아래로 4만 남긴다. 그대로 `Card`에
 * 넣으면 시안보다 44 높아진다. 층은 같은 `Elevation/Card`로 준다.
 */
@Composable
private fun RowCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .background(MedicalMateTheme.colors.bgSurface, MedicalMateRadius.lg)
            .padding(horizontal = MedicalMateSpace.s20, vertical = RowCardPadding),
        content = content,
    )
}

/** 시안의 아바타는 이 화면에서만 56이다. 홈 헤더는 44다. */
private val AvatarSize = 56.dp

/** 시안의 Health Card는 위아래 4, Settings는 6이다. 행 높이가 여백을 이미 갖고 있다. */
private val RowCardPadding = 4.dp

@MedicalMateScreenPreviews
@Composable
private fun MyProfileScreenPreview() {
    MedicalMateTheme {
        MyProfileScreen(
            state = previewMyProfile,
            accountActions = AccountActionCallbacks(),
            onBackClick = {},
            onHealthEditClick = {},
            onSettingChange = { _, _ -> },
        )
    }
}
