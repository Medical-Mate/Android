package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.core.model.FakeVisitReminders
import com.mist.medicalmate.core.model.ReminderSettings
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.ondevice.FakeOnDeviceModel
import com.mist.medicalmate.core.ondevice.OnDeviceModelState
import com.mist.medicalmate.core.speech.FakeCleanMicSetting
import com.mist.medicalmate.core.speech.FakeVoiceWake
import com.mist.medicalmate.core.speech.WakeWordModelState
import com.mist.medicalmate.profile.data.FakeHealthProfileRepository
import com.mist.medicalmate.profile.data.FakeSettingsRepository
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    private val reminders = FakeVisitReminders()

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
    fun `알림 둘은 처음에 켜져 있고 미정 시각은 오후 7시다`() {
        // 서버의 기본값과 같다(Backend#176).
        val state = viewModel().uiState.value

        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.RECORD_REMINDER))
        assertEquals(LocalTime.of(19, 0), state.untimedReminderTime)
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

        viewModel.onSettingChange(AppSetting.RECORD_REMINDER, false)

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.RECORD_REMINDER))
        assertTrue(state.isOn(AppSetting.VISIT_REMINDER))
    }

    private fun viewModel(
        repository: FakeHealthProfileRepository = FakeHealthProfileRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        voiceWake: FakeVoiceWake = FakeVoiceWake(),
        onDevice: FakeOnDeviceModel = FakeOnDeviceModel(),
        cleanMic: FakeCleanMicSetting = FakeCleanMicSetting(),
    ) = MyProfileViewModel(repository, settings, reminders, voiceWake, onDevice, cleanMic)

    @Test
    fun `휴대폰 정리는 NPU가 없는 기기에서 줄이 없다`() {
        // CPU로 돌면 열 배 가까이 느리고 발열로 무너진다. 눌러 봐야 쓸 수 없는 토글을 두지 않는다.
        val viewModel = viewModel(onDevice = FakeOnDeviceModel(usable = false))

        viewModel.load()

        assertFalse(AppSetting.ON_DEVICE in viewModel.uiState.value.mateSettings)
    }

    @Test
    fun `휴대폰 정리를 켜면 확인 창 없이 바로 받기 시작한다`() {
        // 확인 창은 걷어냈다(#316, 사용자 결정).
        val onDevice = FakeOnDeviceModel(usable = true)
        val viewModel = viewModel(onDevice = onDevice)
        viewModel.load()

        viewModel.onSettingChange(AppSetting.ON_DEVICE, true)

        val state = viewModel.uiState.value
        assertTrue(AppSetting.ON_DEVICE in state.mateSettings)
        assertTrue(state.isOn(AppSetting.ON_DEVICE))
        assertEquals(1, onDevice.enableCalls)
    }

    @Test
    fun `켜면 받는 진행률을 적는다`() {
        val onDevice = FakeOnDeviceModel(usable = true)
        val viewModel = viewModel(onDevice = onDevice)

        viewModel.onSettingChange(AppSetting.ON_DEVICE, true)
        onDevice.model.value = OnDeviceModelState.Downloading(0.37f)

        val state = viewModel.uiState.value
        assertTrue(state.isOn(AppSetting.ON_DEVICE))
        assertEquals(OnDeviceStatus.Downloading(37), state.onDevice.status)
    }

    @Test
    fun `정리 모델을 받다가 실패하면 토글이 내려가고 그 사실을 적는다`() {
        // 실제로는 OnDeviceModel이 꺼진 값을 저장한다. 화면은 그것을 따라간다.
        val onDevice = FakeOnDeviceModel(usable = true, enabled = true)
        val viewModel = viewModel(onDevice = onDevice)

        onDevice.model.value = OnDeviceModelState.Failed
        onDevice.enabled.value = false

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.ON_DEVICE))
        assertEquals(OnDeviceStatus.Failed, state.onDevice.status)
    }

    @Test
    fun `휴대폰 정리를 끄면 모델은 남기고 꺼 둔 동안 지우기 버튼이 있다`() {
        // 껐다 켤 때마다 1GB를 다시 받을 수 없다. 공간은 버튼으로 돌려받는다.
        val onDevice = FakeOnDeviceModel(usable = true, enabled = true, model = OnDeviceModelState.Ready)
        val viewModel = viewModel(onDevice = onDevice)
        viewModel.load()
        assertFalse(viewModel.uiState.value.canDeleteOnDeviceModel)

        viewModel.onSettingChange(AppSetting.ON_DEVICE, false)

        assertEquals(1, onDevice.disableCalls)
        assertEquals(0, onDevice.deleteCalls)
        assertTrue(viewModel.uiState.value.canDeleteOnDeviceModel)

        viewModel.onDeviceToggle.deleteModel()

        assertEquals(1, onDevice.deleteCalls)
        assertFalse(viewModel.uiState.value.canDeleteOnDeviceModel)
    }

    @Test
    fun `마이크 소리 다듬기는 release 빌드에 줄이 없다`() {
        // 받아쓰기 방식을 비교하는 시험용 줄이라 debug 빌드에서만 그린다(#328).
        val viewModel = viewModel(cleanMic = FakeCleanMicSetting(available = false))

        assertFalse(AppSetting.CLEAN_MIC in viewModel.uiState.value.mateSettings)
    }

    @Test
    fun `마이크 소리 다듬기를 켜면 기기 값으로 남는다`() {
        // 다음 받아쓰기부터 그 방식으로 듣는다. 계정 값이 아니라 서버에 보내지 않는다.
        val cleanMic = FakeCleanMicSetting(available = true)
        val viewModel = viewModel(cleanMic = cleanMic)

        viewModel.onSettingChange(AppSetting.CLEAN_MIC, true)

        assertTrue(cleanMic.enabled.value)
        assertTrue(AppSetting.CLEAN_MIC in viewModel.uiState.value.mateSettings)
        assertTrue(viewModel.uiState.value.isOn(AppSetting.CLEAN_MIC))
    }

    @Test
    fun `음성 호출은 켤 수 없는 기기에서 줄이 없다`() {
        // 메모리가 모자라거나 받아쓰기를 못 쓰는 기기다. 눌러 봐야 안 되는 토글을 두지 않는다.
        val viewModel = viewModel(voiceWake = FakeVoiceWake(usable = false))

        viewModel.load()

        assertFalse(AppSetting.VOICE_WAKE in viewModel.uiState.value.mateSettings)
    }

    @Test
    fun `음성 호출은 켤 수 있는 기기에서 줄이 있다`() {
        val viewModel = viewModel(voiceWake = FakeVoiceWake(usable = true))

        viewModel.load()

        assertTrue(AppSetting.VOICE_WAKE in viewModel.uiState.value.mateSettings)
    }

    @Test
    fun `음성 호출을 켜면 확인 창 없이 켜고 받는 중을 적는다`() {
        // 확인 창은 걷어냈다(#316, 사용자 결정). 마이크 권한은 화면이 먼저 받고 켜기를 보낸다.
        val voiceWake = FakeVoiceWake(usable = true)
        val viewModel = viewModel(voiceWake = voiceWake)

        viewModel.onSettingChange(AppSetting.VOICE_WAKE, true)

        val state = viewModel.uiState.value
        assertEquals(1, voiceWake.enableCalls)
        assertTrue(state.isOn(AppSetting.VOICE_WAKE))
        assertEquals(VoiceWakeStatus.Downloading(0), state.voiceWake.status)
    }

    @Test
    fun `받는 중의 진행률을 적고 다 받으면 지운다`() {
        val voiceWake = FakeVoiceWake(usable = true, enabled = true)
        val viewModel = viewModel(voiceWake = voiceWake)

        voiceWake.model.value = WakeWordModelState.Downloading(0.42f)
        assertEquals(VoiceWakeStatus.Downloading(42), viewModel.uiState.value.voiceWake.status)

        voiceWake.model.value = WakeWordModelState.Ready
        assertEquals(VoiceWakeStatus.Idle, viewModel.uiState.value.voiceWake.status)
    }

    @Test
    fun `받다가 실패하면 토글이 내려가고 그 사실을 적는다`() {
        // 실제로는 VoiceWake가 꺼진 값을 저장한다. 화면은 그것을 따라간다.
        val voiceWake = FakeVoiceWake(usable = true, enabled = true)
        val viewModel = viewModel(voiceWake = voiceWake)

        voiceWake.model.value = WakeWordModelState.Failed
        voiceWake.enabled.value = false

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.VOICE_WAKE))
        assertEquals(VoiceWakeStatus.Failed, state.voiceWake.status)
    }

    @Test
    fun `마이크 권한을 거부하면 켜지 않고 그 사실을 적는다`() {
        val voiceWake = FakeVoiceWake(usable = true)
        val viewModel = viewModel(voiceWake = voiceWake)

        // 권한은 켜기 전에 묻는다. 거부하면 켜기를 보내지 않는다.
        viewModel.onVoiceWakeDenied()

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.VOICE_WAKE))
        assertEquals(VoiceWakeStatus.Denied, state.voiceWake.status)
        assertEquals(0, voiceWake.enableCalls)
    }

    @Test
    fun `음성 호출을 끄면 바로 끄고 받아 둔 모델은 남긴다`() {
        // 끄는 데는 확인을 거치지 않는다. 모델은 남겨 다시 켤 때 41MB를 또 받지 않게 한다(#280).
        val voiceWake = FakeVoiceWake(usable = true, enabled = true, model = WakeWordModelState.Ready)
        val viewModel = viewModel(voiceWake = voiceWake)

        viewModel.onSettingChange(AppSetting.VOICE_WAKE, false)

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.VOICE_WAKE))
        assertEquals(1, voiceWake.disableCalls)
        assertEquals(0, voiceWake.deleteCalls)
        assertTrue(state.voiceWake.modelReady)
    }

    @Test
    fun `꺼 둔 채 모델이 남아 있으면 지우기 버튼이 있다`() {
        val viewModel =
            viewModel(voiceWake = FakeVoiceWake(usable = true, enabled = false, model = WakeWordModelState.Ready))

        viewModel.load()

        assertTrue(viewModel.uiState.value.canDeleteVoiceModel)
    }

    @Test
    fun `켜져 있으면 지우기 버튼이 없다`() {
        // 듣던 모델을 지우면 호출이 끊긴다.
        val viewModel =
            viewModel(voiceWake = FakeVoiceWake(usable = true, enabled = true, model = WakeWordModelState.Ready))

        viewModel.load()

        assertFalse(viewModel.uiState.value.canDeleteVoiceModel)
    }

    @Test
    fun `받아 둔 모델이 없으면 지우기 버튼이 없다`() {
        val viewModel = viewModel(voiceWake = FakeVoiceWake(usable = true, enabled = false))

        viewModel.load()

        assertFalse(viewModel.uiState.value.canDeleteVoiceModel)
    }

    @Test
    fun `지우기를 누르면 모델을 지우고 버튼이 사라진다`() {
        val voiceWake = FakeVoiceWake(usable = true, enabled = false, model = WakeWordModelState.Ready)
        val viewModel = viewModel(voiceWake = voiceWake)
        viewModel.load()

        viewModel.onVoiceWakeModelDelete()

        assertEquals(1, voiceWake.deleteCalls)
        assertFalse(viewModel.uiState.value.voiceWake.modelReady)
        assertFalse(viewModel.uiState.value.canDeleteVoiceModel)
    }

    @Test
    fun `모델이 남아 있으면 다시 켤 때 받지 않는다`() {
        val voiceWake = FakeVoiceWake(usable = true, enabled = false, model = WakeWordModelState.Ready)
        val viewModel = viewModel(voiceWake = voiceWake)
        assertTrue(viewModel.uiState.value.voiceWake.modelReady)

        viewModel.onSettingChange(AppSetting.VOICE_WAKE, true)

        val state = viewModel.uiState.value
        assertTrue(state.isOn(AppSetting.VOICE_WAKE))
        assertEquals(VoiceWakeStatus.Idle, state.voiceWake.status)
        assertEquals(WakeWordModelState.Ready, voiceWake.model.value)
    }

    private fun profile(
        name: String? = FakeHealthProfileRepository.PROFILE.name,
        sex: String? = FakeHealthProfileRepository.PROFILE.sex,
    ) = FakeHealthProfileRepository(
        read = ApiResult.Success(FakeHealthProfileRepository.PROFILE.copy(name = name, sex = sex)),
    )

    @Test
    fun `알림은 계정에서 읽는다`() {
        val read = ReminderSettings(beforeVisit = false, afterVisit = true, beforeVisitTime = LocalTime.of(20, 30))
        val viewModel = viewModel(settings = FakeSettingsRepository(read = ApiResult.Success(read)))

        viewModel.load()

        val state = viewModel.uiState.value
        assertFalse(state.isOn(AppSetting.VISIT_REMINDER))
        assertTrue(state.isOn(AppSetting.RECORD_REMINDER))
        assertEquals(LocalTime.of(20, 30), state.untimedReminderTime)
    }

    @Test
    fun `알림을 못 읽으면 지금 값으로 둔다`() {
        // 토글 하나 때문에 화면을 막지 않는다.
        val viewModel = viewModel(settings = FakeSettingsRepository(read = FakeSettingsRepository.OFFLINE))

        viewModel.load()

        assertTrue(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))
    }

    @Test
    fun `알림을 끄면 그 하나만 서버로 나간다`() {
        // 서버가 보낸 필드만 바꾼다. 다른 알림을 함께 보내면 다른 기기에서 고친 값을 덮는다.
        val settings = FakeSettingsRepository()
        val viewModel = viewModel(settings = settings)

        viewModel.onSettingChange(AppSetting.RECORD_REMINDER, false)

        assertEquals(false, settings.savedAfterVisit)
        assertNull(settings.savedBeforeVisit)
    }

    @Test
    fun `알림 저장이 실패하면 토글을 되돌린다`() {
        // 안 받겠다고 한 것이 서버에 안 남았는데 화면만 꺼져 있으면 알림이 계속 온다.
        val viewModel = viewModel(settings = FakeSettingsRepository(writeFails = true))

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, false)

        assertTrue(viewModel.uiState.value.isOn(AppSetting.VISIT_REMINDER))
    }

    @Test
    fun `알림 토글이 저장되면 예약을 다시 맞춘다`() {
        // 끄면 걸린 것이 지워지고 켜면 다가오는 일정으로 다시 걸린다.
        val viewModel = viewModel()

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, false)

        assertEquals(1, reminders.refreshes)
    }

    @Test
    fun `알림 저장이 실패하면 예약을 건드리지 않는다`() {
        val viewModel = viewModel(settings = FakeSettingsRepository(writeFails = true))

        viewModel.onSettingChange(AppSetting.VISIT_REMINDER, false)

        assertEquals(0, reminders.refreshes)
    }

    @Test
    fun `설정 목록에 빠진 항목이 없다`() {
        val state = viewModel().uiState.value

        AppSetting.entries.forEach { setting ->
            assertTrue(setting.name, setting in state.settings)
        }
    }
}
