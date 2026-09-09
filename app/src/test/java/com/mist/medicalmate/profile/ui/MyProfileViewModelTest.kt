package com.mist.medicalmate.profile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyProfileViewModelTest {
    @Test
    fun `프로필과 건강 요약이 나온다`() {
        val state = MyProfileViewModel().uiState.value

        assertEquals("김", state.profile.initial)
        assertEquals("김OO", state.profile.name)
        assertEquals("페니실린", state.health.allergies)
    }

    @Test
    fun `설정 세 개의 초기값이 시안과 같다`() {
        val state = MyProfileViewModel().uiState.value

        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.CARD_AUTO_SAVE))
        assertFalse(state.isOn(AppSetting.HANDOFF_BRIGHTNESS))
    }

    @Test
    fun `토글을 끄고 켠다`() {
        val viewModel = MyProfileViewModel()

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, false)
        assertFalse(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, true)
        assertTrue(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))
    }

    @Test
    fun `하나를 바꿔도 다른 설정은 그대로다`() {
        val viewModel = MyProfileViewModel()

        viewModel.onSettingChange(AppSetting.HANDOFF_BRIGHTNESS, true)

        val state = viewModel.uiState.value
        assertTrue(state.isOn(AppSetting.HANDOFF_BRIGHTNESS))
        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.CARD_AUTO_SAVE))
    }

    @Test
    fun `설정 목록에 빠진 항목이 없다`() {
        val state = MyProfileViewModel().uiState.value

        AppSetting.entries.forEach { setting ->
            assertTrue(setting.name, setting in state.settings)
        }
    }
}
