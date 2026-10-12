package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.speech.FakeVoiceWake
import com.mist.medicalmate.core.speech.FakeWakeWordDetector
import com.mist.medicalmate.core.speech.SpeechChunk
import com.mist.medicalmate.core.speech.WakeWordModelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCommandViewModelTest {
    private val detector = FakeWakeWordDetector()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `켜고 모델을 받았고 앱이 화면에 있으면 듣는다`() {
        viewModel().ready()

        assertEquals(1, detector.listeners)
    }

    @Test
    fun `모델을 다 받기 전에는 듣지 않는다`() {
        val wake = FakeVoiceWake(enabled = true, model = WakeWordModelState.Downloading(0.4f))
        viewModel(wake = wake).ready()

        assertEquals(0, detector.listeners)

        wake.model.value = WakeWordModelState.Ready

        assertEquals(1, detector.listeners)
    }

    @Test
    fun `앱이 뒤로 가면 마이크를 놓는다`() {
        // 뒤에서 마이크를 잡는 앱이 되지 않는다. 모델도 내려 480MB를 돌려준다.
        val viewModel = viewModel().ready()

        viewModel.setForeground(false)

        assertEquals(0, detector.listeners)
    }

    @Test
    fun `로그인 화면이나 권한이 없으면 듣지 않는다`() {
        val viewModel = viewModel()
        viewModel.setForeground(true)

        assertEquals(0, detector.listeners)
    }

    @Test
    fun `설정에서 끄면 듣지 않는다`() {
        val wake = FakeVoiceWake(enabled = true, model = WakeWordModelState.Ready)
        viewModel(wake = wake).ready()

        wake.enabled.value = false

        assertEquals(0, detector.listeners)
    }

    @Test
    fun `다른 화면이 받아쓰는 동안에는 마이크를 내준다`() {
        // 증상 문답에서 마이크를 누르면 두 쪽이 한 마이크를 같이 잡게 된다.
        val speech = FakeSpeechToText(usable = true)
        viewModel(speech = speech).ready()

        speech.active.value = true
        assertEquals(0, detector.listeners)

        speech.active.value = false
        assertEquals(1, detector.listeners)
    }

    @Test
    fun `부르면 감지기를 거두고 패널을 연다`() = runTest {
        val speech = FakeSpeechToText(usable = true, keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        assertNotNull(viewModel.uiState.value.panel)
        assertEquals(0, detector.listeners)
    }

    @Test
    fun `명령을 알아들으면 패널을 닫고 명령을 넘긴다`() = runTest {
        val speech =
            FakeSpeechToText(
                usable = true,
                chunks = listOf(SpeechChunk.Partial("캘린더"), SpeechChunk.Final("캘린더 열어줘")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        val state = viewModel.uiState.value
        assertNull(state.panel)
        assertEquals(VoiceCommand.CALENDAR, state.command)
        assertTrue(speech.stopped)
    }

    @Test
    fun `명령을 넘기면 비운다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("홈")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        detector.wake()

        viewModel.onCommandHandled()

        assertNull(viewModel.uiState.value.command)
    }

    @Test
    fun `명령을 마치면 다시 듣는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("홈")))
        viewModel(speech = speech).ready()

        detector.wake()

        assertEquals(1, detector.listeners)
    }

    @Test
    fun `못 알아들은 말이면 그 말을 남기고 계속 듣는다`() = runTest {
        val speech =
            FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("오늘 날씨 어때")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        val panel = viewModel.uiState.value.panel
        assertEquals(VoicePanel(voice = MedicalMateVoiceState.IDLE, heard = "오늘 날씨 어때", missed = true), panel)
        assertNull(viewModel.uiState.value.command)
    }

    @Test
    fun `말하는 중에는 들은 말을 패널에 적는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Partial("캘린")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        assertEquals(VoicePanel(voice = MedicalMateVoiceState.LISTENING, heard = "캘린"), viewModel.uiState.value.panel)
    }

    @Test
    fun `받아쓰기가 실패하면 패널을 닫는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Failed), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        assertNull(viewModel.uiState.value.panel)
        assertNull(viewModel.uiState.value.command)
    }

    @Test
    fun `패널을 닫으면 받아쓰기를 멈추고 다시 호출어를 듣는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        detector.wake()

        viewModel.dismiss()

        assertNull(viewModel.uiState.value.panel)
        assertTrue(speech.stopped)
        assertEquals(1, detector.listeners)
    }

    @Test
    fun `듣는 중에 앱이 뒤로 가면 패널을 닫는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        detector.wake()

        viewModel.setForeground(false)

        assertNull(viewModel.uiState.value.panel)
        assertEquals(0, detector.listeners)
    }

    @Test
    fun `로그아웃하면 패널을 닫는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        detector.wake()

        viewModel.setAllowed(false)

        assertNull(viewModel.uiState.value.panel)
    }

    private fun viewModel(
        wake: FakeVoiceWake = FakeVoiceWake(usable = true, enabled = true, model = WakeWordModelState.Ready),
        speech: FakeSpeechToText = FakeSpeechToText(usable = true),
    ) = VoiceCommandViewModel(wake, detector, speech)

    /** 앱이 화면에 있고 홈에 갈 수 있는 세션에 마이크 권한도 있다. */
    private fun VoiceCommandViewModel.ready() = apply {
        setAllowed(true)
        setForeground(true)
    }
}
