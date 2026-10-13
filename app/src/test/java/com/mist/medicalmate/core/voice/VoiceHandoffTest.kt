package com.mist.medicalmate.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 화면을 옮긴 말을 옮겨 간 화면에 넘긴다(#286). */
class VoiceHandoffTest {
    @Test
    fun `넘긴 말은 한 번만 준다`() {
        val targets = VoiceTargets()

        targets.handOff("10월 20일 일정 보여줘")

        assertEquals("10월 20일 일정 보여줘", targets.takeHandoff())
        assertNull(targets.takeHandoff())
    }
}
