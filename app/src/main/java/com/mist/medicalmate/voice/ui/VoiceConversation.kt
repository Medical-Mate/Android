package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.speech.VoiceWakeTrace
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceScroll
import com.mist.medicalmate.core.voice.VoiceTargets
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceScroll

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
 * @param suggest 지금 화면의 추천 말을 막대 위에 띄운다(#292). 못 알아들었을 때 부른다.
 * @param shown 지금 막대에 떠 있는 것. 못 알아들었을 때 물음을 남기고, "다시 보여 줘"에 그대로 다시
 *   띄운다(#284).
 */
internal class VoiceConversation(
    private val targets: VoiceTargets,
    private val trace: VoiceWakeTrace,
    private val say: (message: VoiceMessage, confirming: Boolean, question: VoiceMessage?) -> Unit,
    private val command: (VoiceCommandRequest) -> Unit,
    private val shown: () -> VoicePanel? = { null },
    private val suggest: () -> Unit = {},
) {
    /** 아직 "네"를 기다리는 일. */
    private var pending: VoiceReply.Confirm? = null

    /** 말 하나에 답한다. 다음 말을 계속 들을지 돌려준다. */
    suspend fun respond(text: String): Boolean {
        val asking = pending
        val panel = shown()
        val scroll = parseVoiceScroll(text)
        return when {
            VoiceWords.isStop(text) -> {
                trace.log { "대화: \"$text\" → 그만" }
                false
            }

            // "아래로", "맨 위로". 어느 화면이든 같은 말로 굴린다. 문답에서 이 말이 답으로 나가지 않는다.
            scroll != null -> scroll(text, scroll, panel)

            // 막대에 띄운 목록을 미처 못 봤다. 화면에 넘기지 않고 그대로 다시 띄운다. 문답에서는 이
            // 말이 답으로 나가면 안 된다.
            panel?.question != null && VoiceWords.isRepeat(text) -> {
                trace.log { "대화: \"$text\" → 다시 보여 주기" }
                say(panel.message, panel.confirming, panel.question)
                true
            }

            asking != null -> answer(text, asking)
            else -> reply(text)
        }
    }

    /**
     * 지금 화면을 굴린다. 막대에 띄운 물음은 그대로 둔다 — 굴린 것은 무엇을 할지 바꾸지 않는다. "네"를
     * 기다리는 중이면 그 물음도 남긴다.
     */
    private fun scroll(text: String, direction: VoiceScroll, panel: VoicePanel?): Boolean {
        val scroller = targets.currentScroller
        val moved = scroller?.scroll(direction) == true
        trace.log { "대화: \"$text\" → 굴리기 $direction ${if (moved) "함" else "못 함"}" }
        val message =
            when {
                scroller == null -> R.string.voice_scroll_none
                !moved && (direction == VoiceScroll.DOWN || direction == VoiceScroll.BOTTOM) ->
                    R.string.voice_scroll_end_down

                !moved -> R.string.voice_scroll_end_up
                direction == VoiceScroll.DOWN -> R.string.voice_scroll_down
                direction == VoiceScroll.UP -> R.string.voice_scroll_up
                direction == VoiceScroll.BOTTOM -> R.string.voice_scroll_bottom
                else -> R.string.voice_scroll_top
            }
        val confirming = panel?.confirming == true
        say(if (confirming && panel != null) panel.message else VoiceMessage(message), confirming, panel?.question)
        return true
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
            // 묻던 것을 그대로 다시 세운다. "이대로 할까요?"로 바꾸면 무엇을 하려는지가 사라진다.
            say(VoiceMessage(asking.prompt, asking.args), true, VoiceMessage(R.string.voice_command_confirm_again))
            true
        }
    }

    /** 전역 명령. 화면을 옮기고 대화는 이어 간다. 옮겨 간 화면이 다음 말을 받는다. */
    private fun global(text: String) {
        val parsed = parseVoiceCommand(text)
        trace.log { "대화: 전역 명령 ${parsed ?: "모르는 말"}" }
        if (parsed == null) {
            // 묻던 것이 있으면 남긴다. 목록을 보며 답하다 잘못 들렸는데 목록이 사라지면 다시 볼 수 없다.
            say(
                VoiceMessage(R.string.voice_command_missed, listOf(text)),
                false,
                shown()?.question ?: VoiceMessage(R.string.voice_command_hint),
            )
            // 무엇을 말하면 되는지 모를 때다. 이 화면에서 알아듣는 말을 보여 준다.
            suggest()
        } else if (parsed == VoiceCommand.BACK && !targets.canGoBack) {
            // 첫 화면이다(#298). 옮기지 않으므로 "돌아갔어요"라고 하면 틀린 말이다.
            say(VoiceMessage(R.string.voice_moved_back_none), false, null)
        } else {
            command(VoiceCommandRequest(parsed, text))
            say(VoiceMessage(parsed.movedMessage()), false, null)
        }
    }
}

/** 옮긴 뒤 막대에 적는 말. 어디로 갔는지를 적는다. "뒤로"에 "열었어요"라고 하면 틀린 말이다. */
private fun VoiceCommand.movedMessage(): Int = when (this) {
    VoiceCommand.HOME -> R.string.voice_moved_home
    VoiceCommand.CALENDAR -> R.string.voice_moved_calendar
    VoiceCommand.RECORD -> R.string.voice_moved_record
    VoiceCommand.PROFILE -> R.string.voice_moved_profile
    VoiceCommand.BACK -> R.string.voice_moved_back
    VoiceCommand.INTAKE -> R.string.voice_moved_intake
    VoiceCommand.VISIT_RECORD -> R.string.voice_moved_visit_record
    VoiceCommand.BRIEF_CARDS -> R.string.voice_moved_brief_cards
    VoiceCommand.SCHEDULE_ADD -> R.string.voice_moved_schedule_add
    // 홈이 이어서 답한다. 옮긴 곳만 적는다.
    VoiceCommand.RESUME, VoiceCommand.NEXT_VISIT -> R.string.voice_moved_home
}
