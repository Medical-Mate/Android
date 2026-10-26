package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.speech.VoiceWakeTrace
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTarget
import com.mist.medicalmate.core.voice.VoiceTargets
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** "할까요?" 확인은 물은 화면에서만 받는다(#348). */
class VoiceConfirmTest {
    private val targets = VoiceTargets()

    private val commands = mutableListOf<VoiceCommandRequest>()

    private val said = mutableListOf<Int>()

    private val conversation =
        VoiceConversation(
            targets = targets,
            trace = VoiceWakeTrace {},
            say = { message, _, _ -> said += message.text },
            command = { commands += it },
        )

    @Test
    fun `물은 화면에서 네라고 하면 한다`() = runTest {
        var saved = 0
        targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) { saved++ } })

        conversation.respond("저장해 줘")
        conversation.respond("네")

        assertEquals(1, saved)
    }

    @Test
    fun `기다려야 하는 일은 네 뒤에 했다고 하지 않고 하는 중이라고 한다`() = runTest {
        // 저장 요청을 보내자마자 "했어요"라고 하면 실패해도 막대는 했다고 한다(#350). 결과는 화면이 받은 뒤에 알린다.
        targets.register(
            VoiceTarget {
                VoiceReply.Confirm(R.string.voice_record_confirm_save, confirmed = R.string.voice_saving) {}
            },
        )

        conversation.respond("저장해 줘")
        conversation.respond("네")

        assertEquals(R.string.voice_saving, said.last())
    }

    @Test
    fun `화면을 옮긴 뒤의 네는 떠난 화면의 일을 하지 않는다`() = runTest {
        // 전에는 떠난 화면의 저장이 실행되거나 일정 추가의 "나갈래"가 맨 위의 다른 화면을 닫았다.
        var saved = 0
        targets.register(VoiceTarget { VoiceReply.Confirm(R.string.voice_schedule_confirm_save) { saved++ } })
        conversation.respond("저장해 줘")

        var heard: String? = null
        targets.register(
            VoiceTarget { text ->
                heard = text
                VoiceReply.Done(R.string.voice_command_confirmed)
            },
        )
        conversation.respond("네")

        assertEquals(0, saved)
        // 그 말은 새 화면이 받는다.
        assertEquals("네", heard)
    }
}
