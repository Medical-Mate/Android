package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthProfile
import com.mist.medicalmate.profile.data.HealthProfileRepository
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
 * 설정 토글도 저장되지 않는다. 화면 안에서만 바뀌고, 기기에 남기려면 `DataStore`가
 * 계정에 남기려면 API가 필요하다.
 */
@HiltViewModel
class MyProfileViewModel
@Inject
internal constructor(private val repository: HealthProfileRepository) : ViewModel() {
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

    fun onSettingChange(setting: AppSetting, enabled: Boolean) {
        mutableUiState.update { it.copy(settings = it.settings + (setting to enabled)) }
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
