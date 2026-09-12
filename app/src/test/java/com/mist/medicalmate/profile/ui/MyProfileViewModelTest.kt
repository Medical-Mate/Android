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
import org.junit.Assert.assertNull
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
    fun `읽기 전에는 프로필이 비어 있다`() {
        // 픽스처를 먼저 보여주면 남의 이름과 생년이 잠깐 자기 정보로 읽힌다.
        val state = viewModel().uiState.value

        assertEquals(MyProfile(), state.profile)
    }

    @Test
    fun `프로필 줄이 읽어 온 값으로 채워진다`() {
        val viewModel = viewModel()

        viewModel.load()

        val profile = viewModel.uiState.value.profile
        assertEquals("김OO", profile.name)
        assertEquals(1994, profile.birthYear)
        assertEquals(ProfileSex.FEMALE, profile.sex)
    }

    @Test
    fun `아바타 글자는 이름의 첫 자다`() {
        // 서버가 마스킹해 줄 수 있어서("김OO") 자르는 자리를 화면에 두지 않는다.
        val viewModel = viewModel()

        viewModel.load()

        assertEquals("김", viewModel.uiState.value.profile.initial)
    }

    @Test
    fun `이름이 없으면 아바타 글자도 없다`() {
        val viewModel = viewModel(profile(name = null))

        viewModel.load()

        val profile = viewModel.uiState.value.profile
        assertEquals("", profile.initial)
        assertNull(profile.name)
    }

    @Test
    fun `밝히지 않은 성별은 적지 않는다`() {
        // 서버의 UNSPECIFIED다. "밝히지 않음"을 적을 자리가 시안에 없다.
        val viewModel = viewModel(profile(sex = "UNSPECIFIED"))

        viewModel.load()

        assertNull(viewModel.uiState.value.profile.sex)
    }

    @Test
    fun `남성도 읽는다`() {
        val viewModel = viewModel(profile(sex = "MALE"))

        viewModel.load()

        assertEquals(ProfileSex.MALE, viewModel.uiState.value.profile.sex)
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

    private fun profile(
        name: String? = FakeHealthProfileRepository.PROFILE.name,
        sex: String? = FakeHealthProfileRepository.PROFILE.sex,
    ) = FakeHealthProfileRepository(
        read = ApiResult.Success(FakeHealthProfileRepository.PROFILE.copy(name = name, sex = sex)),
    )

    @Test
    fun `설정 목록에 빠진 항목이 없다`() {
        val state = viewModel().uiState.value

        AppSetting.entries.forEach { setting ->
            assertTrue(setting.name, setting in state.settings)
        }
    }
}
