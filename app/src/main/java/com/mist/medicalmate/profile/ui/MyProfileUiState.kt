package com.mist.medicalmate.profile.ui

import androidx.annotation.StringRes
import com.mist.medicalmate.R
import com.mist.medicalmate.profile.data.HealthField

/**
 * 내 정보 화면의 상태. Figma 1s-1 `407:2375`.
 *
 * 프로필, 건강 정보 요약, 설정 세 덩이다. 서버 연동에서는
 * `GET /api/me/health-profile` 한 번으로 앞의 둘이 채워진다.
 */
data class MyProfileUiState(
    val profile: MyProfile = MyProfile(),
    val health: HealthSummary = HealthSummary(),
    val settings: Map<AppSetting, Boolean> = DefaultSettings,
) {
    fun isOn(setting: AppSetting): Boolean = settings[setting] == true
}

/**
 * 설정 세 개의 초기값. 시안 1s-1이 그린 대로다.
 *
 * 읽어 오기 전까지만 쓰인다. 알림은 계정에서, 나머지 둘은 이 기기에서 읽어 덮는다(#187).
 * 서버의 기본값도 켜짐이라 알림은 값이 같다.
 */
private val DefaultSettings =
    mapOf(
        AppSetting.VISIT_REMINDER to true,
        AppSetting.CARD_AUTO_SAVE to true,
        AppSetting.HANDOFF_BRIGHTNESS to false,
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
 */
enum class AppSetting(@StringRes val labelRes: Int) {
    VISIT_REMINDER(R.string.my_profile_setting_visit_reminder),
    CARD_AUTO_SAVE(R.string.my_profile_setting_card_auto_save),
    HANDOFF_BRIGHTNESS(R.string.my_profile_setting_handoff_brightness),
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
