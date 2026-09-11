package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.HealthField
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
 * 프로필 줄(이름·생년·로그인)은 아직 픽스처다. 같은 응답에 값이 있지만 "1994년생 · 여"를
 * 조립하는 자리가 정해져야 한다. 상태에 문장을 담지 않기로 해 둔 것과 엮인다.
 *
 * 설정 토글도 저장되지 않는다. 화면 안에서만 바뀌고, 기기에 남기려면 `DataStore`가
 * 계정에 남기려면 API가 필요하다.
 */
@HiltViewModel
class MyProfileViewModel
@Inject
internal constructor(private val repository: HealthProfileRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(previewMyProfile)
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
 * Figma 1s-1(407:2375)의 내용.
 *
 * 건강 요약은 서버 값이 들어오면 덮인다. 프로필 줄과 설정은 아직 이 값이 그대로 나온다.
 */
internal val previewMyProfile =
    MyProfileUiState(
        profile =
        MyProfile(
            initial = "김",
            name = "김OO",
            meta = "1994년생 · 여",
            login = "카카오로 로그인",
        ),
        health =
        HealthSummary(
            medications = HealthField(listOf("혈압약", "진통제(증상 시)")),
            conditions = HealthField(listOf("고혈압")),
            allergies = HealthField(listOf("페니실린")),
        ),
        settings =
        mapOf(
            AppSetting.VISIT_REMINDER to true,
            AppSetting.CARD_AUTO_SAVE to true,
            AppSetting.HANDOFF_BRIGHTNESS to false,
        ),
    )
