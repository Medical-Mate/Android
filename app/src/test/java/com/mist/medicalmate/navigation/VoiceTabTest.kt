package com.mist.medicalmate.navigation

import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.voice.ui.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Test

/** 말로 연 화면이 손으로 들어갈 때와 같은 탭 위에 선다(#298). */
class VoiceTabTest {
    @Test
    fun `홈에서 들어가는 화면은 홈 위에 선다`() {
        assertEquals(MedicalMateTab.HOME, voiceTabOf(VoiceCommand.INTAKE))
        assertEquals(MedicalMateTab.HOME, voiceTabOf(VoiceCommand.BRIEF_CARDS))
        assertEquals(MedicalMateTab.HOME, voiceTabOf(VoiceCommand.PROFILE))
    }

    @Test
    fun `캘린더에서 들어가는 화면은 캘린더 위에 선다`() {
        assertEquals(MedicalMateTab.CALENDAR, voiceTabOf(VoiceCommand.SCHEDULE_ADD))
        assertEquals(MedicalMateTab.CALENDAR, voiceTabOf(VoiceCommand.VISIT_RECORD))
        assertEquals(MedicalMateTab.CALENDAR, voiceTabOf(VoiceCommand.CALENDAR))
        assertEquals(MedicalMateTab.RECORD, voiceTabOf(VoiceCommand.RECORD))
    }
}
