package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.model.VisitReminders
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.ondevice.OnDeviceModel
import com.mist.medicalmate.core.speech.CleanMicSetting
import com.mist.medicalmate.core.speech.VoiceWake
import com.mist.medicalmate.core.speech.WakeWordModelState
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthProfile
import com.mist.medicalmate.profile.data.HealthProfileRepository
import com.mist.medicalmate.profile.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 내 정보 상태 보유자.
 *
 * 건강 요약 세 줄이 `GET /api/me/health-profile`에서 온다. 건강 정보 수정(1s-2)이 저장하고
 * 돌아오는 자리라 화면이 다시 보일 때마다 읽는다. 한 번만 읽으면 방금 고친 값 대신 들어올
 * 때 읽은 값이 남는다.
 *
 * 프로필 줄(이름·생년·성별)도 같은 응답에서 온다. 이름은 서버가 마스킹해 줄 수 있어서
 * ("김OO") 아바타 글자를 여기서 자른다. 자르는 규칙이 화면에 있으면 마스킹 형식이 바뀔 때
 * 고칠 곳이 여럿이 된다. 홈 헤더의 아바타도 같은 규칙이다.
 *
 * 로그인 수단은 읽지 않는다. 서버의 사용자 식별자가 `kakaoId` 단독이라 카카오 하나뿐이고,
 * 그 줄은 문자열 리소스에 고정으로 있다.
 *
 * **알림 토글 둘은 계정에 붙는다**(#187 · #316). 진료 전 알림과 진료 후 기록 알림은 받을지 말지가 기기
 * 취향이 아니라 그 사람의 선택이라 `GET`·`PATCH /api/me/settings`에 있다. 기기를 바꾸거나 앱을 다시 깔면
 * "안 받겠다"고 한 사람에게 알림이 다시 가기 때문이다. 시간 미정 일정의 알림 시각도 같은 응답에서 읽어
 * `알림 시간` 줄과 토글 설명에 적는다. 고치는 것은 알림 시간 화면(1s-3)이다.
 *
 * **알림을 예약하는 것은 여전히 앱이다.** 그 값은 예약할지 말지를 정한다. 저장되면 예약을
 * 다시 맞춘다 — 끄면 걸린 것이 지워지고 켜면 다가오는 일정으로 다시 걸린다(#268).
 *
 * 토글을 누르면 화면을 먼저 바꾸고 저장을 보낸다. 왕복을 기다리면 누른 뒤에 잠깐 안 바뀐
 * 것처럼 보인다. 실패하면 되돌린다.
 *
 * **음성 호출(#273)은 마이크 권한을 받은 뒤에 켠다.** 화면이 권한을 묻고 받으면 켜기를 보낸다. 켤 때의
 * 확인 창은 걷어냈다(#316, 사용자 결정). 토글의 값은 [VoiceWake.enabled]를
 * 따라간다 — 받다가 실패하면 그쪽이 내리고 화면도 따라 내려간다.
 */
@HiltViewModel
class MyProfileViewModel
@Inject
internal constructor(
    private val repository: HealthProfileRepository,
    private val settings: SettingsRepository,
    private val reminders: VisitReminders,
    private val voiceWake: VoiceWake,
    onDevice: OnDeviceModel,
    private val cleanMic: CleanMicSetting,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = mutableUiState.asStateFlow()

    /** "휴대폰에서 기록 정리"(#289). 화면의 확인 창·지우기 버튼이 바로 부른다. */
    val onDeviceToggle = OnDeviceToggle(viewModelScope, onDevice, mutableUiState)

    init {
        mutableUiState.update { it.copy(cleanMicAvailable = cleanMic.available) }
        viewModelScope.launch {
            cleanMic.enabled.collect { enabled ->
                mutableUiState.update { it.copy(settings = it.settings + (AppSetting.CLEAN_MIC to enabled)) }
            }
        }
        viewModelScope.launch {
            voiceWake.enabled.collect { enabled ->
                mutableUiState.update { it.copy(settings = it.settings + (AppSetting.VOICE_WAKE to enabled)) }
            }
        }
        viewModelScope.launch {
            voiceWake.model.collect { model ->
                updateVoiceWake {
                    it.copy(status = model.toStatus(it.status), modelReady = model == WakeWordModelState.Ready)
                }
            }
        }
    }

    /**
     * 건강 요약을 읽는다.
     *
     * 읽지 못하면 화면을 비우지 않고 그대로 둔다. 돌아올 때마다 부르는 호출이라, 잠깐
     * 끊긴 것으로 적어 둔 값이 사라지면 지워진 것으로 보인다.
     */
    fun load() {
        viewModelScope.launch {
            val supported = voiceWake.supported()
            updateVoiceWake { it.copy(supported = supported) }
            onDeviceToggle.loadSupported()
            loadSettings()
            val profile = (repository.profile() as? ApiResult.Success)?.value ?: return@launch
            mutableUiState.update {
                it.copy(
                    profile = profile.toProfile(),
                    health =
                    HealthSummary(
                        medications = profile.health.medications,
                        conditions = profile.health.conditions,
                        allergies = profile.health.allergies,
                    ),
                )
            }
        }
    }

    /**
     * 알림 설정을 계정에서 읽는다. 못 읽으면 지금 값으로 둔다 — 화면 전체를 막을 값이 아니다.
     *
     * 알림 시간 화면(1s-3)에서 고치고 돌아오면 이 화면이 다시 보이며 새 시각을 읽는다.
     */
    private suspend fun loadSettings() {
        val reminders = (settings.reminders() as? ApiResult.Success)?.value ?: return
        mutableUiState.update { state ->
            state.copy(
                settings =
                state.settings +
                    mapOf(
                        AppSetting.VISIT_REMINDER to reminders.beforeVisit,
                        AppSetting.RECORD_REMINDER to reminders.afterVisit,
                    ),
                untimedReminderTime = reminders.beforeVisitTime,
            )
        }
    }

    /**
     * 토글을 눌렀다.
     *
     * 화면을 먼저 바꾸고 저장을 보낸다. 알림은 계정이라 실패하면 되돌린다 — 안 받겠다고 한
     * 것이 서버에 안 남았는데 화면만 꺼져 있으면 알림이 계속 온다.
     */
    fun onSettingChange(setting: AppSetting, enabled: Boolean) {
        when (setting) {
            AppSetting.VOICE_WAKE -> onVoiceWakeChange(enabled)
            AppSetting.ON_DEVICE -> onDeviceToggle.change(enabled)
            // 기기 값이다. 다음 받아쓰기부터 그 방식으로 듣는다(#328).
            AppSetting.CLEAN_MIC -> viewModelScope.launch { cleanMic.set(enabled) }
            AppSetting.VISIT_REMINDER, AppSetting.RECORD_REMINDER -> {
                mutableUiState.update { it.copy(settings = it.settings + (setting to enabled)) }
                viewModelScope.launch { saveReminder(setting, enabled) }
            }
        }
    }

    /**
     * 켜면 모델을 받기 시작하고 끄면 바로 끈다. 받아 둔 모델은 남긴다(#280). 켜기는 화면이 마이크 권한을 받은
     * 뒤에만 보낸다.
     */
    private fun onVoiceWakeChange(enabled: Boolean) {
        if (enabled) {
            updateVoiceWake { it.copy(status = VoiceWakeStatus.Idle) }
            viewModelScope.launch { voiceWake.enable() }
            return
        }
        mutableUiState.update { it.copy(settings = it.settings + (AppSetting.VOICE_WAKE to false)) }
        viewModelScope.launch { voiceWake.disable() }
    }

    /** "음성 모델 지우기"를 눌렀다. 꺼 둔 상태에서만 버튼이 있다. */
    fun onVoiceWakeModelDelete() {
        viewModelScope.launch { voiceWake.deleteModel() }
    }

    /** 켜려 했는데 마이크 권한을 거부했다. 듣지 못하는 호출어를 켜 둘 수 없다. */
    fun onVoiceWakeDenied() {
        updateVoiceWake { it.copy(status = VoiceWakeStatus.Denied) }
    }

    private fun updateVoiceWake(change: (VoiceWakeUi) -> VoiceWakeUi) {
        mutableUiState.update { it.copy(voiceWake = change(it.voiceWake)) }
    }

    /** 바꾼 알림 하나만 보낸다. 서버가 보낸 필드만 바꾼다(Backend#176). */
    private suspend fun saveReminder(setting: AppSetting, enabled: Boolean) {
        val result =
            when (setting) {
                AppSetting.VISIT_REMINDER -> settings.update(beforeVisit = enabled)
                AppSetting.RECORD_REMINDER -> settings.update(afterVisit = enabled)
                AppSetting.VOICE_WAKE, AppSetting.ON_DEVICE, AppSetting.CLEAN_MIC -> return
            }
        if (result is ApiResult.Success) {
            reminders.refresh()
            return
        }
        mutableUiState.update { it.copy(settings = it.settings + (setting to !enabled)) }
    }
}

/**
 * 모델 상태를 토글 아래 한 줄로.
 *
 * 다 받았거나 지웠으면 그 줄을 비운다. 다만 권한을 거부해 켜지 못한 것은 모델과 상관없는
 * 사정이라 남긴다.
 */
private fun WakeWordModelState.toStatus(current: VoiceWakeStatus): VoiceWakeStatus = when (this) {
    is WakeWordModelState.Downloading -> VoiceWakeStatus.Downloading((progress * PERCENT).toInt())
    WakeWordModelState.Failed -> VoiceWakeStatus.Failed
    WakeWordModelState.Ready, WakeWordModelState.Absent ->
        if (current == VoiceWakeStatus.Denied) current else VoiceWakeStatus.Idle
}

private const val PERCENT = 100

/**
 * 응답을 프로필 줄로.
 *
 * 이름이 없으면 아바타 글자도 없다. 카카오 동의를 거부한 계정이 그렇고, 빈 동그라미가
 * 그려진다. 아무 글자나 채우면 그것이 이름의 첫 자로 읽힌다.
 */
private fun HealthProfile.toProfile() = MyProfile(
    initial = name?.take(1).orEmpty(),
    name = name,
    birthYear = birthYear,
    sex = sex.toProfileSex(),
)

/** 서버의 `UNSPECIFIED`와 모르는 값은 null이다. 적을 자리가 시안에 없다. */
private fun String?.toProfileSex(): ProfileSex? = when (this) {
    "FEMALE" -> ProfileSex.FEMALE
    "MALE" -> ProfileSex.MALE
    else -> null
}

/**
 * Figma 1s-1(407:2375)의 내용. Preview에만 쓴다.
 *
 * 프로필과 건강 요약은 서버에서 온다. 설정만 아직 이 값이 화면에 그대로 나온다.
 */
internal val previewMyProfile =
    MyProfileUiState(
        profile =
        MyProfile(initial = "김", name = "김OO", birthYear = 1994, sex = ProfileSex.FEMALE),
        health =
        HealthSummary(
            medications = HealthField(listOf("혈압약", "진통제(증상 시)")),
            conditions = HealthField(listOf("고혈압")),
            allergies = HealthField(listOf("페니실린")),
        ),
    )
