package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.speech.VoiceWakeTrace
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargets
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 대화의 한 마디에 답한다(#282).
 *
 * 받아쓴 말은 먼저 지금 화면([VoiceTargets.current])에 간다. 화면이 모르는 말이면 전역 명령이다.
 * 저장처럼 묻는 일은 다음 말이 "네"일 때만 한다. [VoiceCommandViewModel]이 마이크와 패널을 들고,
 * 여기는 말에 무엇으로 답할지만 정한다.
 *
 * @param say 막대에 적는다. `confirming`이면 "네"를 기다리는 물음이다. `question`은 화면이 되묻는
 *   것이다. 비우면 지난 물음을 지운다.
 * @param command 전역 명령을 그래프에 넘긴다.
 */
internal class VoiceConversation(
    private val targets: VoiceTargets,
    private val trace: VoiceWakeTrace,
    private val say: (message: VoiceMessage, confirming: Boolean, question: VoiceMessage?) -> Unit,
    private val command: (VoiceCommandRequest) -> Unit,
) {
    /** 아직 "네"를 기다리는 일. */
    private var pending: VoiceReply.Confirm? = null

    /** 말 하나에 답한다. 다음 말을 계속 들을지 돌려준다. */
    suspend fun respond(text: String): Boolean {
        val asking = pending
        return when {
            VoiceWords.isStop(text) -> {
                trace.log { "대화: \"$text\" → 그만" }
                false
            }

            asking != null -> answer(text, asking)
            else -> reply(text)
        }
    }

    private suspend fun reply(text: String): Boolean {
        val reply = targets.current?.handle(text) ?: VoiceReply.NotMine
        trace.log { "대화: \"$text\" → 화면 ${reply::class.simpleName}" }
        when (reply) {
            is VoiceReply.Done -> say(VoiceMessage(reply.message, reply.args), false, reply.question)
            is VoiceReply.Confirm -> {
                pending = reply
                say(VoiceMessage(reply.prompt, reply.args), true, VoiceMessage(R.string.voice_command_yes_no))
            }

            VoiceReply.NotMine -> global(text)
        }
        return true
    }

    /** "할까요?"에 대한 답. 네면 하고 대화를 마친다. 아니면 하지 않고 계속 듣는다. */
    private fun answer(text: String, asking: VoiceReply.Confirm): Boolean = when {
        VoiceWords.isYes(text) -> {
            trace.log { "대화: \"$text\" → 확인" }
            pending = null
            asking.onConfirm()
            false
        }

        VoiceWords.isNo(text) -> {
            trace.log { "대화: \"$text\" → 취소" }
            pending = null
            say(VoiceMessage(R.string.voice_command_cancelled), false, null)
            true
        }

        // 네도 아니요도 아니면 다시 묻는다. 묻던 것을 놓으면 다음 "네"가 엉뚱한 데 간다.
        else -> {
            say(VoiceMessage(R.string.voice_command_confirm_again), true, VoiceMessage(R.string.voice_command_yes_no))
            true
        }
    }

    /** 전역 명령. 화면을 옮기고 대화는 이어 간다. 옮겨 간 화면이 다음 말을 받는다. */
    private fun global(text: String) {
        val parsed = parseVoiceCommand(text)
        trace.log { "대화: 전역 명령 ${parsed ?: "모르는 말"}" }
        if (parsed == null) {
            say(
                VoiceMessage(R.string.voice_command_missed, listOf(text)),
                false,
                VoiceMessage(R.string.voice_command_hint),
            )
        } else {
            command(VoiceCommandRequest(parsed, text))
            say(VoiceMessage(R.string.voice_command_moved), false, null)
        }
    }
}
