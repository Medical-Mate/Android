package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.speech.FakeVoiceWake
import com.mist.medicalmate.core.speech.FakeWakeWordDetector
import com.mist.medicalmate.core.speech.SpeechChunk
import com.mist.medicalmate.core.speech.WakeWordModelState
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTarget
import com.mist.medicalmate.core.voice.VoiceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        // 뒤에서 마이크를 잡는 앱이 되지 않는다. 모델도 내려 메모리를 돌려준다.
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
    fun `로그인했어도 마이크 권한이 없으면 듣지 않는다`() {
        // 설정에서 권한을 거둘 수 있다. 화면으로 돌아올 때마다 다시 본다.
        val viewModel = viewModel()
        viewModel.setAllowed(signedIn = true, micGranted = false)
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
    fun `전역 명령을 알아들으면 명령을 넘기고 대화는 이어 간다`() = runTest {
        // 한 흐름을 말로 끝까지 하려면 화면을 옮긴 뒤에도 계속 들어야 한다(#282).
        val speech =
            FakeSpeechToText(
                usable = true,
                chunks = listOf(SpeechChunk.Partial("캘린더"), SpeechChunk.Final("캘린더 열어줘")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        val state = viewModel.uiState.value
        assertEquals(VoiceCommandRequest(VoiceCommand.CALENDAR, "캘린더 열어줘"), state.command)
        assertEquals(VoiceMessage(R.string.voice_command_moved), state.panel?.message)
        assertFalse(speech.stopped)
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
        assertEquals(VoiceMessage(R.string.voice_command_missed, listOf("오늘 날씨 어때")), panel?.message)
        assertNull(viewModel.uiState.value.command)
    }

    @Test
    fun `말하는 중에는 들은 말을 패널에 적는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Partial("캘린")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        val panel = viewModel.uiState.value.panel
        assertEquals(MedicalMateVoiceState.LISTENING, panel?.voice)
        assertEquals("캘린", panel?.heard)
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

        viewModel.setAllowed(signedIn = false, micGranted = true)

        assertNull(viewModel.uiState.value.panel)
    }

    @Test
    fun `말은 먼저 지금 화면에 간다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("10월 20일")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        val heard = mutableListOf<String>()
        viewModel.targets.register(
            VoiceTarget { text ->
                heard += text
                VoiceReply.Done(R.string.voice_schedule_filled, listOf("10월 20일"))
            },
        )

        detector.wake()

        assertEquals(listOf("10월 20일"), heard)
        assertEquals(
            VoiceMessage(R.string.voice_schedule_filled, listOf("10월 20일")),
            viewModel.uiState.value.panel?.message,
        )
        assertNull(viewModel.uiState.value.command)
    }

    @Test
    fun `화면이 모르는 말이면 전역 명령으로 넘어간다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("뒤로 가")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        viewModel.targets.register(VoiceTarget { VoiceReply.NotMine })

        detector.wake()

        assertEquals(VoiceCommand.BACK, viewModel.uiState.value.command?.command)
    }

    @Test
    fun `나중에 등록한 화면이 지금 화면이다`() = runTest {
        // 화면이 바뀌는 동안 나가는 화면과 들어오는 화면이 함께 조합된다.
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("첫 번째")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        val leaving = VoiceTarget { VoiceReply.Done(R.string.voice_schedule_missing) }
        val entering = VoiceTarget { VoiceReply.Done(R.string.voice_hospital_no_row) }
        viewModel.targets.register(leaving)
        viewModel.targets.register(entering)

        detector.wake()

        assertEquals(VoiceMessage(R.string.voice_hospital_no_row), viewModel.uiState.value.panel?.message)
    }

    @Test
    fun `저장처럼 묻는 일은 네라고 해야 하고 하고 나면 닫는다`() = runTest {
        val speech =
            FakeSpeechToText(
                usable = true,
                chunks = listOf(SpeechChunk.Final("저장해 줘"), SpeechChunk.Final("네")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech = speech).ready()
        var saved = 0
        viewModel.targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) { saved++ } })

        detector.wake()

        assertEquals(1, saved)
        assertNull(viewModel.uiState.value.panel)
        assertTrue(speech.stopped)
    }

    @Test
    fun `묻는 중에 아니요라고 하면 하지 않고 계속 듣는다`() = runTest {
        val speech =
            FakeSpeechToText(
                usable = true,
                chunks = listOf(SpeechChunk.Final("저장해 줘"), SpeechChunk.Final("아니요")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech = speech).ready()
        var saved = 0
        viewModel.targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) { saved++ } })

        detector.wake()

        assertEquals(0, saved)
        assertEquals(VoiceMessage(R.string.voice_command_cancelled), viewModel.uiState.value.panel?.message)
    }

    @Test
    fun `묻는 중에 다른 말을 하면 다시 묻는다`() = runTest {
        // 묻던 것을 놓으면 다음 "네"가 엉뚱한 데 간다.
        val speech =
            FakeSpeechToText(
                usable = true,
                chunks = listOf(SpeechChunk.Final("저장해 줘"), SpeechChunk.Final("음 글쎄"), SpeechChunk.Final("네")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech = speech).ready()
        var saved = 0
        viewModel.targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) { saved++ } })

        detector.wake()

        assertEquals(1, saved)
    }

    @Test
    fun `그만이라고 하면 닫는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("그만")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()

        detector.wake()

        assertNull(viewModel.uiState.value.panel)
        assertEquals(1, detector.listeners)
    }

    @Test
    fun `12초 동안 아무것도 들리지 않으면 닫는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        detector.wake()
        assertNotNull(viewModel.uiState.value.panel)

        advanceTimeBy(11_999)
        assertNotNull(viewModel.uiState.value.panel)
        advanceTimeBy(2)

        assertNull(viewModel.uiState.value.panel)
    }

    @Test
    fun `화면의 답에 붙은 물음을 막대에 적는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("20일")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        viewModel.targets.register(
            VoiceTarget {
                VoiceReply.Done(
                    R.string.voice_schedule_filled,
                    listOf("10월 20일"),
                    question = VoiceText(R.string.voice_ask_hospital),
                )
            },
        )

        detector.wake()

        assertEquals(VoiceText(R.string.voice_ask_hospital), viewModel.uiState.value.panel?.question)
    }

    @Test
    fun `화면이 먼저 건 말을 막대에 적는다`() = runTest {
        // 병원 찾기가 결과를 받아 "같은 이름이 2곳 있어요"라고 묻는 길이다.
        val viewModel = viewModel(speech = FakeSpeechToText(usable = true, keepOpen = true)).ready()
        detector.wake()

        viewModel.targets.ask(
            VoicePrompt(
                message = VoiceText(R.string.voice_hospital_found_same, listOf("2")),
                question = VoiceText(R.string.voice_hospital_ask_which),
            ),
        )

        val panel = viewModel.uiState.value.panel
        assertEquals(VoiceText(R.string.voice_hospital_found_same, listOf("2")), panel?.message)
        assertEquals(VoiceText(R.string.voice_hospital_ask_which), panel?.question)
    }

    @Test
    fun `화면이 말을 걸면 12초를 다시 센다`() = runTest {
        // 물어 놓고 바로 닫히면 답할 틈이 없다.
        val viewModel = viewModel(speech = FakeSpeechToText(usable = true, keepOpen = true)).ready()
        detector.wake()

        advanceTimeBy(10_000)
        viewModel.targets.ask(VoicePrompt(question = VoiceText(R.string.voice_ask_date)))
        advanceTimeBy(10_000)

        assertNotNull(viewModel.uiState.value.panel)
        advanceTimeBy(2_001)
        assertNull(viewModel.uiState.value.panel)
    }

    @Test
    fun `막대가 닫혀 있을 때 건 말은 버려진다`() = runTest {
        val viewModel = viewModel().ready()

        viewModel.targets.ask(VoicePrompt(question = VoiceText(R.string.voice_ask_date)))

        assertNull(viewModel.uiState.value.panel)
    }

    @Test
    fun `저장을 물을 때는 네나 아니요로 답하라고 적는다`() = runTest {
        val speech = FakeSpeechToText(usable = true, chunks = listOf(SpeechChunk.Final("저장해 줘")), keepOpen = true)
        val viewModel = viewModel(speech = speech).ready()
        viewModel.targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) {} })

        detector.wake()

        assertEquals(VoiceText(R.string.voice_command_yes_no), viewModel.uiState.value.panel?.question)
    }

    private fun viewModel(
        wake: FakeVoiceWake = FakeVoiceWake(usable = true, enabled = true, model = WakeWordModelState.Ready),
        speech: FakeSpeechToText = FakeSpeechToText(usable = true),
    ) = VoiceCommandViewModel(wake, detector, speech) {}

    /** 앱이 화면에 있고 홈에 갈 수 있는 세션에 마이크 권한도 있다. */
    private fun VoiceCommandViewModel.ready() = apply {
        setAllowed(signedIn = true, micGranted = true)
        setForeground(true)
    }
}
