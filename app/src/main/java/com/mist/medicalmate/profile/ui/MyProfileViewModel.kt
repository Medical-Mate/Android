package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthProfile
import com.mist.medicalmate.profile.data.HealthProfileRepository
import com.mist.medicalmate.profile.data.LocalSettingsStore
import com.mist.medicalmate.profile.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 * **설정 토글 셋 중 하나만 계정에 붙는다**(#187). 진료 하루 전 알림은 받을지 말지가 기기
 * 취향이 아니라 그 사람의 선택이라 `GET`·`PATCH /api/me/settings`에 있다. 기기를 바꾸거나
 * 앱을 다시 깔면 "안 받겠다"고 한 사람에게 알림이 다시 가기 때문이다. 나머지 둘은 이 기기에서
 * 어떻게 보일지의 문제라 `DataStore`에 둔다.
 *
 * **알림을 예약하는 것은 여전히 앱이다.** 그 값은 예약할지 말지를 정한다.
 *
 * 토글을 누르면 화면을 먼저 바꾸고 저장을 보낸다. 왕복을 기다리면 누른 뒤에 잠깐 안 바뀐
 * 것처럼 보인다. 실패하면 되돌린다.
 */
@HiltViewModel
class MyProfileViewModel
@Inject
internal constructor(
    private val repository: HealthProfileRepository,
    private val settings: SettingsRepository,
    private val localSettings: LocalSettingsStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = mutableUiState.asStateFlow()

    /**
     * 건강 요약을 읽는다.
     *
     * 읽지 못하면 화면을 비우지 않고 그대로 둔다. 돌아올 때마다 부르는 호출이라, 잠깐
     * 끊긴 것으로 적어 둔 값이 사라지면 지워진 것으로 보인다.
     */
    fun load() {
        viewModelScope.launch {
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
     * 토글 셋을 각자의 자리에서 읽는다.
     *
     * 알림은 계정, 나머지 둘은 이 기기다. 알림을 못 읽으면 그 토글만 지금 값으로 둔다 —
     * 화면 전체를 막을 값이 아니다.
     */
    private suspend fun loadSettings() {
        val reminder = (settings.visitReminder() as? ApiResult.Success)?.value
        val cardAutoSave = localSettings.cardAutoSave.first()
        val brightness = localSettings.handoffBrightness.first()
        mutableUiState.update { state ->
            state.copy(
                settings =
                state.settings +
                    listOfNotNull(
                        reminder?.let { AppSetting.VISIT_REMINDER to it },
                        AppSetting.CARD_AUTO_SAVE to cardAutoSave,
                        AppSetting.HANDOFF_BRIGHTNESS to brightness,
                    ),
            )
        }
    }

    /**
     * 토글을 눌렀다.
     *
     * 화면을 먼저 바꾸고 저장을 보낸다. 알림은 계정이라 실패하면 되돌린다 — 안 받겠다고 한
     * 것이 서버에 안 남았는데 화면만 꺼져 있으면 알림이 계속 온다. 나머지 둘은 이 기기에만
     * 쓰는 값이라 되돌릴 실패가 없다.
     */
    fun onSettingChange(setting: AppSetting, enabled: Boolean) {
        mutableUiState.update { it.copy(settings = it.settings + (setting to enabled)) }
        viewModelScope.launch {
            when (setting) {
                AppSetting.VISIT_REMINDER -> saveReminder(enabled)
                AppSetting.CARD_AUTO_SAVE -> localSettings.setCardAutoSave(enabled)
                AppSetting.HANDOFF_BRIGHTNESS -> localSettings.setHandoffBrightness(enabled)
            }
        }
    }

    private suspend fun saveReminder(enabled: Boolean) {
        if (settings.setVisitReminder(enabled) is ApiResult.Success) return
        mutableUiState.update { it.copy(settings = it.settings + (AppSetting.VISIT_REMINDER to !enabled)) }
    }
}

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
