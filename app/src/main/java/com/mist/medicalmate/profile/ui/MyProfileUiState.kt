package com.mist.medicalmate.profile.ui

import androidx.annotation.StringRes
import com.mist.medicalmate.R
import com.mist.medicalmate.core.model.DEFAULT_UNTIMED_REMINDER
import com.mist.medicalmate.profile.data.HealthField
import java.time.LocalTime

/**
 * 내 정보 화면의 상태. 10-15 시안 1s-1 `3663:15766`(#316).
 *
 * 프로필, 건강 정보 요약, 알림, 메이트 네 덩이다. `GET /api/me/health-profile` 한 번으로 앞의 둘이 채워지고,
 * 알림은 `GET /api/me/settings`에서 온다.
 *
 * [untimedReminderTime]은 시간 미정 일정의 알림 시각이다. 알림 카드의 `알림 시간` 줄과 두 토글의 설명이 이
 * 값을 적는다.
 */
data class MyProfileUiState(
    val profile: MyProfile = MyProfile(),
    val health: HealthSummary = HealthSummary(),
    val settings: Map<AppSetting, Boolean> = DefaultSettings,
    val untimedReminderTime: LocalTime = DEFAULT_UNTIMED_REMINDER,
    val voiceWake: VoiceWakeUi = VoiceWakeUi(),
    val onDevice: OnDeviceUi = OnDeviceUi(),
    /** 받아쓰기 방식 비교 토글을 그릴지(#328). debug 빌드에서만 참이다. */
    val cleanMicAvailable: Boolean = false,
) {
    fun isOn(setting: AppSetting): Boolean = settings[setting] == true

    /** 메이트 카드에 그릴 토글. 음성 호출과 휴대폰 정리는 이 기기에서 켤 수 있을 때만 있다. */
    val mateSettings: List<AppSetting>
        get() =
            listOfNotNull(
                AppSetting.VOICE_WAKE.takeIf { voiceWake.supported },
                AppSetting.ON_DEVICE.takeIf { onDevice.supported },
                AppSetting.CLEAN_MIC.takeIf { cleanMicAvailable },
            )

    /** "정리 모델 지우기"를 둘지. 꺼 둔 채 모델이 남아 있을 때만이다. */
    val canDeleteOnDeviceModel: Boolean
        get() = onDevice.supported && onDevice.modelReady && !isOn(AppSetting.ON_DEVICE)

    /** "음성 모델 지우기"를 둘지. 꺼 둔 채 모델이 남아 있을 때만이다. */
    val canDeleteVoiceModel: Boolean
        get() = voiceWake.supported && voiceWake.modelReady && !isOn(AppSetting.VOICE_WAKE)
}

/**
 * "헤이 메이트"로 부르기(#273)의 자리.
 *
 * 켜려면 마이크 권한을 받아야 한다. 확인 창은 걷어냈다(#316, 사용자 결정). 켜진 뒤에도 모델을 다 받기
 * 전까지는 듣지 않아서 그 사이를 적는다.
 *
 * 꺼도 받아 둔 모델은 남는다(#280). 그래서 꺼 둔 채 모델이 있으면 지우는 버튼을 둔다. 켜져
 * 있을 때 지우면 듣던 것이 끊기므로 그때는 두지 않는다.
 *
 * @param supported 이 기기에서 켤 수 있는지. 아니면 토글을 그리지 않는다.
 * @param modelReady 받아 둔 모델이 있다. 켤 때 다시 받지 않는다.
 */
data class VoiceWakeUi(
    val supported: Boolean = false,
    val status: VoiceWakeStatus = VoiceWakeStatus.Idle,
    val modelReady: Boolean = false,
)

/** 토글 아래 한 줄. 문구는 화면이 고른다. */
sealed interface VoiceWakeStatus {
    data object Idle : VoiceWakeStatus

    data class Downloading(val percent: Int) : VoiceWakeStatus

    data object Failed : VoiceWakeStatus

    /** 마이크 권한을 거부해 켜지 못했다. */
    data object Denied : VoiceWakeStatus
}

/**
 * 설정의 초기값. 읽어 오기 전까지만 쓰인다.
 *
 * 알림 둘은 서버의 기본값과 같은 켜짐이다. 음성 호출과 휴대폰 정리는 켜면 모델을 받으므로 꺼진 채 시작한다.
 */
private val DefaultSettings =
    mapOf(
        AppSetting.VISIT_REMINDER to true,
        AppSetting.RECORD_REMINDER to true,
        AppSetting.VOICE_WAKE to false,
        AppSetting.ON_DEVICE to false,
        AppSetting.CLEAN_MIC to false,
    )

/**
 * 프로필.
 *
 * "1994년생 · 여" 같은 완성된 문장을 담지 않는다. 앞말과 뒷말은 문자열 리소스에 있고 조립은
 * 화면이 한다. 시안이 이름 · 생년월일 · 로그인 수단을 세 줄로 그려서 줄을 나누는 것도
 * 화면의 일이다.
 *
 * [initial]은 아바타에 들어가는 한 글자다. 이름에서 잘라내는 규칙을 화면에 두지 않는다.
 * 서버가 이름을 마스킹해서 줄 수 있고("김OO"), 마스킹 형식이 바뀌면 고칠 곳이 여럿이 된다.
 * 자르는 자리는 [MyProfileViewModel]이고 홈 헤더의 아바타도 같은 규칙이다.
 *
 * 값이 없으면 그 줄을 그리지 않는다. 온보딩을 마치면 셋 다 차 있지만, 카카오 동의를 거부한
 * 계정은 이름이나 성별이 빈 채로 온다.
 */
data class MyProfile(
    val initial: String = "",
    val name: String? = null,
    val birthYear: Int? = null,
    val sex: ProfileSex? = null,
)

/**
 * 화면에 적을 성별.
 *
 * 서버의 `UNSPECIFIED`는 여기서 null이다. "밝히지 않음"을 적을 자리가 시안에 없고, 적어도
 * 환자에게 쓸모가 없다.
 */
enum class ProfileSex(@StringRes val labelRes: Int) {
    FEMALE(R.string.my_profile_sex_female),
    MALE(R.string.my_profile_sex_male),
}

/**
 * 건강 정보 요약 3줄.
 *
 * 문구가 아니라 값을 담는다. "없어요"와 "잘 모르겠어요"는 문자열 리소스에 있고 화면이
 * 고른다. 상태가 문구를 들면 지금이 어느 상태인지 코드가 알 수 없다.
 */
data class HealthSummary(
    val medications: HealthField = HealthField(),
    val conditions: HealthField = HealthField(),
    val allergies: HealthField = HealthField(),
)

/**
 * 설정 토글.
 *
 * 문구를 상태에 담지 않고 리소스 id로 둔다. 상태가 문자열을 들면 지금이 어느 설정인지
 * 코드가 알 수 없고, 문구를 다듬을 때마다 테스트가 깨진다(홈의 오늘 한 줄과 같은 이유).
 *
 * 앞의 둘이 계정 설정(#316)이고 뒤의 둘이 이 기기의 기능이다. 예전의 "브리핑 카드 자동 저장" ·
 * "진료실 화면 밝기 최대"는 시안에서 빠졌고 어디서도 값을 읽지 않아 걷어냈다(사용자 결정).
 */
enum class AppSetting(@StringRes val labelRes: Int) {
    VISIT_REMINDER(R.string.my_profile_setting_visit_reminder),
    RECORD_REMINDER(R.string.my_profile_setting_record_reminder),

    /** 메이트 카드의 첫 줄(#316). 이 기기에서 켤 수 있을 때만 그린다. */
    VOICE_WAKE(R.string.my_profile_setting_voice_wake),

    /**
     * 시안에 없는 줄이다(#289). 온디바이스를 확정할 때까지 시험용으로 메이트 카드 둘째 줄에 둔다(#316, 사용자
     * 결정). NPU가 잡히는 기기에서만 그린다.
     */
    ON_DEVICE(R.string.my_profile_setting_on_device),

    /**
     * 받아쓰기 전에 마이크 소리를 다듬을지(#328). 지금 방식과 비교하려고 둔 시험용 줄이다. 여러 곳에서 PC 없이
     * 바꿔야 해서 화면에 둔다(사용자 결정). 시안에 없고 debug 빌드에서만 그린다. 비교가 끝나면 걷어낸다.
     */
    CLEAN_MIC(R.string.my_profile_setting_clean_mic),
}

/**
 * 건강 정보 수정 화면의 상태. Figma 1s-2 `407:2650`.
 *
 * 세 갈래를 한 화면에서 고친다. 고를 수 있는 항목은 신상정보 입력(1b)과 같은 배열을 써서
 * [ProfileSetupStep.optionsRes]로 가져온다. 두 화면의 목록이 갈리면 1b에서 고른 것이
 * 여기에 없는 일이 생긴다.
 *
 * [adding]은 "직접 추가"를 누른 갈래다. 시안에 그 칩만 있고 뒤에 무엇이 나오는지는 정해져
 * 있지 않아, 1i의 추가 질문과 같은 방식(입력 칸 + 더하기)으로 뒀다.
 */
data class HealthEditUiState(
    val chosen: Map<ProfileSetupStep, Set<String>> = emptyMap(),
    val extras: Map<ProfileSetupStep, List<String>> = emptyMap(),
    val adding: ProfileSetupStep? = null,
    val draft: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    /** 저장이 안 된 채로 저장하기를 눌렀는지. 하단에 한 줄이 붙는다. 시안에 없는 문구다. */
    val saveFailed: Boolean = false,
) {
    fun chosenIn(step: ProfileSetupStep): Set<String> = chosen[step].orEmpty()

    fun extrasIn(step: ProfileSetupStep): List<String> = extras[step].orEmpty()

    val canAddDraft: Boolean get() = draft.isNotBlank()
}
