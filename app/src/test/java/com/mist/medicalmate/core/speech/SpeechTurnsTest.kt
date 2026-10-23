package com.mist.medicalmate.core.speech

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 받아쓰기 차례(#348). 인식기는 하나라 한 번에 한쪽만 쓰고, 화면이 먼저다. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechTurnsTest {
    @Test
    fun `막대가 쓰는 중에 화면이 열려 하면 막대를 멈추게 하고 놓은 뒤에 연다`() = runTest {
        var interrupted = 0
        val turns = SpeechTurns(interrupt = { interrupted++ })
        turns.take(SpeechOwner.PANEL)

        val screen = launch { turns.take(SpeechOwner.SCREEN) }
        runCurrent()

        assertEquals(1, interrupted)
        assertTrue(turns.owns(SpeechOwner.PANEL))
        assertTrue(screen.isActive)

        turns.release()
        runCurrent()

        assertTrue(turns.owns(SpeechOwner.SCREEN))
        assertTrue(turns.active.value)
    }

    @Test
    fun `화면이 쓰는 동안 막대는 기다린다`() = runTest {
        var interrupted = 0
        val turns = SpeechTurns(interrupt = { interrupted++ })
        turns.take(SpeechOwner.SCREEN)

        val panel = launch { turns.take(SpeechOwner.PANEL) }
        runCurrent()

        // 막대는 화면을 끊지 않는다.
        assertEquals(0, interrupted)
        assertTrue(panel.isActive)
        assertTrue(turns.owns(SpeechOwner.SCREEN))

        turns.release()
        runCurrent()

        assertTrue(turns.owns(SpeechOwner.PANEL))
    }

    @Test
    fun `쓰는 쪽만 멈출 수 있다`() = runTest {
        val turns = SpeechTurns(interrupt = {})
        turns.take(SpeechOwner.PANEL)

        assertTrue(turns.owns(SpeechOwner.PANEL))
        assertFalse(turns.owns(SpeechOwner.SCREEN))

        turns.release()

        assertFalse(turns.owns(SpeechOwner.PANEL))
        assertFalse(turns.active.value)
    }
}
