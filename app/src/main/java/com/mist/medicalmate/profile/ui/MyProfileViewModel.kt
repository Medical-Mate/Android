package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 내 정보 상태 보유자.
 *
 * 내용이 픽스처다. `GET /api/me/health-profile`이 붙으면 프로필과 건강 요약이 그 응답으로
 * 채워진다.
 *
 * 설정 토글은 아직 저장되지 않는다. 화면 안에서만 바뀌고, 기기에 남기려면 `DataStore`가
 * 계정에 남기려면 API가 필요하다.
 */
@HiltViewModel
class MyProfileViewModel
@Inject
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(previewMyProfile)
    val uiState: StateFlow<MyProfileUiState> = mutableUiState.asStateFlow()

    fun onSettingChange(setting: AppSetting, enabled: Boolean) {
        mutableUiState.update { it.copy(settings = it.settings + (setting to enabled)) }
    }
}

/** Figma 1s-1(407:2375)의 내용. Preview와 픽스처가 함께 쓴다. 서버 연동 시 삭제한다. */
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
            medications = "혈압약 · 진통제(증상 시)",
            conditions = "고혈압",
            allergies = "페니실린",
        ),
        settings =
        mapOf(
            AppSetting.VISIT_REMINDER to true,
            AppSetting.CARD_AUTO_SAVE to true,
            AppSetting.HANDOFF_BRIGHTNESS to false,
        ),
    )
