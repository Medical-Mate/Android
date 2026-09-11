package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.FakeHealthProfileRepository
import com.mist.medicalmate.profile.data.HealthEdit
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프로필 줄은 아직 픽스처다`() {
        val state = viewModel().uiState.value

        assertEquals("김", state.profile.initial)
        assertEquals("김OO", state.profile.name)
    }

    @Test
    fun `건강 요약은 읽어 온 값으로 바뀐다`() {
        val viewModel =
            viewModel(
                FakeHealthProfileRepository(
                    read =
                    ApiResult.Success(
                        FakeHealthProfileRepository.PROFILE.copy(
                            health =
                            HealthEdit(
                                medications = HealthField(listOf("혈압약", "진통제")),
                                conditions = HealthField(),
                                allergies = HealthField(emptyList(), HealthStatus.NONE),
                            ),
                        ),
                    ),
                ),
            )

        viewModel.load()

        val health = viewModel.uiState.value.health
        assertEquals(listOf("혈압약", "진통제"), health.medications.items)
        assertEquals(HealthStatus.UNKNOWN, health.conditions.status)
        // 서버가 "없다"로 들고 있으면 그대로 온다. 화면에는 그렇게 말할 자리가 아직 없다.
        assertEquals(HealthStatus.NONE, health.allergies.status)
    }

    @Test
    fun `읽지 못하면 보이던 값을 지우지 않는다`() {
        // 돌아올 때마다 부르는 호출이라, 잠깐 끊긴 것으로 값이 사라지면 지워진 것으로 보인다.
        val viewModel = viewModel(FakeHealthProfileRepository(read = FakeHealthProfileRepository.OFFLINE))
        val before = viewModel.uiState.value.health

        viewModel.load()

        assertEquals(before, viewModel.uiState.value.health)
    }

    @Test
    fun `설정 세 개의 초기값이 시안과 같다`() {
        val state = viewModel().uiState.value

        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.CARD_AUTO_SAVE))
        assertFalse(state.isOn(AppSetting.HANDOFF_BRIGHTNESS))
    }

    @Test
    fun `토글을 끄고 켠다`() {
        val viewModel = viewModel()

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, false)
        assertFalse(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, true)
        assertTrue(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))
    }

    @Test
    fun `하나를 바꿔도 다른 설정은 그대로다`() {
        val viewModel = viewModel()

        viewModel.onSettingChange(AppSetting.HANDOFF_BRIGHTNESS, true)

        val state = viewModel.uiState.value
        assertTrue(state.isOn(AppSetting.HANDOFF_BRIGHTNESS))
        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.CARD_AUTO_SAVE))
    }

    private fun viewModel(repository: FakeHealthProfileRepository = FakeHealthProfileRepository()) =
        MyProfileViewModel(repository)

    @Test
    fun `설정 목록에 빠진 항목이 없다`() {
        val state = viewModel().uiState.value

        AppSetting.entries.forEach { setting ->
            assertTrue(setting.name, setting in state.settings)
        }
    }
}
