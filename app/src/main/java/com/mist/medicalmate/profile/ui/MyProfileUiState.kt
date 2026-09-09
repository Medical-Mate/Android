package com.mist.medicalmate.profile.ui

import androidx.annotation.StringRes
import com.mist.medicalmate.R

/**
 * 내 정보 화면의 상태. Figma 1s-1 `407:2375`.
 *
 * 프로필, 건강 정보 요약, 설정 세 덩이다. 서버 연동에서는
 * `GET /api/me/health-profile` 한 번으로 앞의 둘이 채워진다.
 */
data class MyProfileUiState(
    val profile: MyProfile,
    val health: HealthSummary,
    val settings: Map<AppSetting, Boolean> = emptyMap(),
) {
    fun isOn(setting: AppSetting): Boolean = settings[setting] == true
}

/**
 * 프로필.
 *
 * [initial]은 아바타에 들어가는 한 글자다. 이름에서 잘라내지 않고 따로 받는다. 서버가
 * 이름을 마스킹해서 줄 수 있고("김OO"), 그때 첫 글자를 자르는 규칙이 화면에 있으면
 * 마스킹 형식이 바뀔 때 화면이 깨진다.
 *
 * [meta]와 [login]을 나눠 둔 이유는 시안이 두 줄로 그렸기 때문이다. 한 문장으로 합치면
 * 좁은 화면에서 줄바꿈 위치를 고를 수 없다.
 */
data class MyProfile(val initial: String, val name: String, val meta: String, val login: String)

/** 건강 정보 요약 3줄. 값이 없으면 "없음"이 오고, 그 문구도 서버가 정한다. */
data class HealthSummary(val medications: String, val conditions: String, val allergies: String)

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
) {
    fun chosenIn(step: ProfileSetupStep): Set<String> = chosen[step].orEmpty()

    fun extrasIn(step: ProfileSetupStep): List<String> = extras[step].orEmpty()

    val canAddDraft: Boolean get() = draft.isNotBlank()
}
