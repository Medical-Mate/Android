package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.speech.SpeechChunk
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

/** 말이 1초 끊기면 보낸다(#318). 전에는 다 말하고 패널을 한 번 더 눌러야 보내졌다. */
@OptIn(ExperimentalCoroutinesApi::class)
class IntakeVoiceSilenceTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `말이 1초 끊기면 누르지 않아도 보낸다`() {
        val repository = FakeSessionRepository()
        val viewModel = listening(repository, SpeechChunk.Final("배가 아파요"))

        dispatcher.scheduler.advanceTimeBy(SILENCE + 1)

        assertEquals("배가 아파요", repository.sentText)
        assertTrue(repository.sentByVoice)
    }

    @Test
    fun `1초가 지나기 전에는 보내지 않는다`() {
        val repository = FakeSessionRepository()
        listening(repository, SpeechChunk.Final("배가 아파요"))

        dispatcher.scheduler.advanceTimeBy(SILENCE - 100)

        assertNull(repository.sentText)
    }

    @Test
    fun `말하는 중에는 새 말이 올 때마다 다시 센다`() {
        // 부분 결과가 이어 들어오는 동안은 말하는 중이다. 처음 들린 때부터 세면 말하는 도중에 보낸다.
        val repository = FakeSessionRepository()
        listening(repository, SpeechChunk.Partial("배가"))

        dispatcher.scheduler.advanceTimeBy(SILENCE - 100)

        assertNull(repository.sentText)
    }

    @Test
    fun `글로 바꾸면 보내지 않는다`() {
        val repository = FakeSessionRepository()
        val viewModel = listening(repository, SpeechChunk.Final("배가 아파요"))

        viewModel.onInputModeChange(IntakeInputMode.TEXT)
        dispatcher.scheduler.advanceTimeBy(SILENCE + 1)

        assertNull(repository.sentText)
    }

    @Test
    fun `문답이 끝난 뒤에는 다음 말고는 보내지 않는다`() {
        // 그 자리에서는 "다음"만 듣는다. 다른 말은 적히기만 한다.
        val repository = FakeSessionRepository(ends = true)
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Final("배가 아파요")), keepOpen = true)
        val viewModel = intakeViewModel(repository = repository, speech = speech)
        viewModel.openChatStep()
        viewModel.voice.onVoiceMode()
        dispatcher.scheduler.advanceTimeBy(SILENCE + 1)
        val sentBefore = repository.sentCount

        dispatcher.scheduler.advanceTimeBy(SILENCE + 1)

        assertEquals(sentBefore, repository.sentCount)
        assertFalse(viewModel.uiState.value.completed)
    }

    private fun listening(repository: FakeSessionRepository, chunk: SpeechChunk): IntakeViewModel {
        val speech = FakeSpeechToText(chunks = listOf(chunk), keepOpen = true)
        val viewModel = intakeViewModel(repository = repository, speech = speech)
        viewModel.openChatStep()
        viewModel.voice.onVoiceMode()
        return viewModel
    }

    private companion object {
        const val SILENCE = 1_000L
    }
}
