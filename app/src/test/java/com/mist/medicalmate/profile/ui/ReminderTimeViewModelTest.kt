package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.core.model.FakeVisitReminders
import com.mist.medicalmate.core.model.ReminderSettings
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.FakeSettingsRepository
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

/** 알림 시간(1s-3, #316). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderTimeViewModelTest {
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
    fun `저장된 시각으로 연다`() {
        val read = ReminderSettings(beforeVisitTime = LocalTime.of(20, 30))
        val viewModel = viewModel(FakeSettingsRepository(read = ApiResult.Success(read)))

        viewModel.load()

        val state = viewModel.uiState.value
        assertTrue(state.loaded)
        assertEquals(LocalTime.of(20, 30), state.time)
    }

    @Test
    fun `못 읽으면 오후 7시로 연다`() {
        val viewModel = viewModel(FakeSettingsRepository(read = FakeSettingsRepository.OFFLINE))

        viewModel.load()

        assertEquals(LocalTime.of(19, 0), viewModel.uiState.value.time)
    }

    @Test
    fun `휠을 굴리기만 하면 저장하지 않는다`() {
        val settings = FakeSettingsRepository()
        val viewModel = viewModel(settings).apply { load() }

        viewModel.onTimeChange(LocalTime.of(21, 0))

        assertNull(settings.savedTime)
    }

    @Test
    fun `확인하면 고른 시각을 저장하고 예약을 맞추고 나간다`() {
        // 휠 하나가 전날 · 당일 알림에 함께 걸린다(사용자 결정).
        val settings = FakeSettingsRepository()
        val viewModel = viewModel(settings).apply { load() }
        viewModel.onTimeChange(LocalTime.of(21, 0))
        var left = false

        viewModel.onConfirm { left = true }

        assertEquals(LocalTime.of(21, 0), settings.savedTime)
        assertEquals(1, reminders.refreshes)
        assertTrue(left)
    }

    @Test
    fun `저장이 안 되면 남아서 알린다`() {
        // 나가 버리면 안 바뀐 시각을 바뀐 것으로 안다.
        val viewModel = viewModel(FakeSettingsRepository(writeFails = true)).apply { load() }
        var left = false

        viewModel.onConfirm { left = true }

        assertFalse(left)
        assertTrue(viewModel.uiState.value.saveFailed)
        assertEquals(0, reminders.refreshes)
    }

    private fun viewModel(settings: FakeSettingsRepository) = ReminderTimeViewModel(settings, reminders)
}
