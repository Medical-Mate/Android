package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mist.medicalmate.BuildConfig
import com.mist.medicalmate.core.notification.rememberNotificationPermission
import com.mist.medicalmate.core.speech.rememberMicPermission
import com.mist.medicalmate.core.voice.LocalVoiceTargets
import kotlinx.serialization.Serializable

/** 와이어프레임 1s-1. 홈 헤더의 아바타에서 들어온다. */
@Serializable
internal data object MyProfileDestination

/** 와이어프레임 1s-2. */
@Serializable
internal data object HealthEditDestination

/** 와이어프레임 1s-3 알림 시간(#316). 내 정보의 `알림 시간` 줄에서 들어온다. */
@Serializable
internal data object ReminderTimeDestination

internal fun NavGraphBuilder.myProfileDestination(
    accountActions: AccountActionCallbacks,
    onHealthEdit: () -> Unit,
    onReminderTime: () -> Unit,
    onExit: () -> Unit,
) {
    composable<MyProfileDestination> {
        MyProfileRoute(
            accountActions = accountActions,
            onHealthEdit = onHealthEdit,
            onReminderTime = onReminderTime,
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.reminderTimeDestination(onExit: () -> Unit) {
    composable<ReminderTimeDestination> {
        ReminderTimeRoute(onExit = onExit)
    }
}

/** 알림 시간(1s-3). 저장되면 내 정보로 돌아가고, 그 화면이 다시 보이며 새 시각을 읽는다. */
@Composable
private fun ReminderTimeRoute(onExit: () -> Unit, viewModel: ReminderTimeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    ReminderTimeVoice(onTimeChange = viewModel::onTimeChange, onConfirm = { viewModel.onConfirm(onExit) })
    ReminderTimeScreen(
        state = state,
        onBackClick = onExit,
        onTimeChange = viewModel::onTimeChange,
        onConfirmClick = { viewModel.onConfirm(onExit) },
    )
}

internal fun NavGraphBuilder.healthEditDestination(onSaved: () -> Unit, onExit: () -> Unit) {
    composable<HealthEditDestination> {
        HealthEditRoute(onSaved = onSaved, onExit = onExit)
    }
}

@Composable
private fun MyProfileRoute(
    accountActions: AccountActionCallbacks,
    onHealthEdit: () -> Unit,
    onReminderTime: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestNotificationPermission = rememberNotificationPermission()
    // 음성 호출은 토글을 켤 때 마이크 권한을 묻고 받으면 켠다. 그때가 마이크가 왜 필요한지 보이는 자리다.
    val requestMicPermission =
        rememberMicPermission(
            onGranted = { viewModel.onSettingChange(AppSetting.VOICE_WAKE, true) },
            onDenied = viewModel::onVoiceWakeDenied,
        )

    val context = LocalContext.current
    // 휴대폰 알림이 꺼져 있는지(#316). 기기 설정에서 바꾸고 돌아올 수 있어 보일 때마다 다시 본다.
    var notificationsBlocked by remember { mutableStateOf(false) }

    // 1s-2 · 1s-3에서 고치고 돌아오는 자리라 들어올 때 한 번이 아니라 보일 때마다 읽는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
        notificationsBlocked = !NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val onSettingChange = { setting: AppSetting, enabled: Boolean ->
        // 켜는 순간이 알림이 왜 필요한지 보이는 자리다.
        if (setting.isReminder && enabled) requestNotificationPermission()
        if (setting == AppSetting.VOICE_WAKE && enabled) {
            requestMicPermission()
        } else {
            viewModel.onSettingChange(setting, enabled)
        }
    }
    // 말로 알림을 켜고 끄고 알림 시간 · 건강 정보를 연다(#320). "알림 8시로 바꿔"는 알림 시간이 그 말을 받는다.
    val targets = LocalVoiceTargets.current
    MyProfileVoice(
        ProfileVoiceRoutes(
            onSettingChange = onSettingChange,
            onReminderTime = { spoken ->
                targets?.handOff(spoken)
                onReminderTime()
            },
            onHealthEdit = onHealthEdit,
        ),
    )

    MyProfileScreen(
        state = state,
        accountActions = accountActions,
        callbacks =
        MyProfileCallbacks(
            onBackClick = onExit,
            onHealthEditClick = onHealthEdit,
            onReminderTimeClick = onReminderTime,
            onSettingChange = onSettingChange,
            notificationsBlocked = notificationsBlocked,
            appVersion = BuildConfig.VERSION_NAME,
        ),
        modifier = modifier,
        voiceWakeCallbacks =
        VoiceWakeCallbacks(onDeleteModel = viewModel::onVoiceWakeModelDelete),
        onDeviceCallbacks = OnDeviceCallbacks(onDeleteModel = viewModel.onDeviceToggle::deleteModel),
    )
}

/**
 * 상태 있는 진입점.
 *
 * 저장하면 화면을 나간다. 저장 자체는 아직 없고, 나가는 판단은 그래프가 한다.
 */
@Composable
private fun HealthEditRoute(
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    HealthEditScreen(
        state = state,
        callbacks =
        HealthEditCallbacks(
            onCloseClick = onExit,
            onOptionClick = viewModel::onOptionClick,
            onAddClick = viewModel::onAddClick,
            onDraftChange = viewModel::onDraftChange,
            onDraftSubmit = viewModel::onDraftSubmit,
            onSaveClick = { viewModel.onSaveClick(onSaved) },
        ),
        modifier = modifier,
    )
}

/** 알림 토글인지. 켤 때 휴대폰 알림 권한을 묻는다. */
private val AppSetting.isReminder: Boolean
    get() = this == AppSetting.VISIT_REMINDER || this == AppSetting.RECORD_REMINDER
