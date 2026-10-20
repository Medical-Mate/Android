package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText

/**
 * 일자 화면의 할 일을 말로 체크하고 푼다(#336). 손으로도 편집 없이 누르는 일이다.
 *
 * **체크와 해제만 한다**(사용자 결정). 더하기 · 고치기 · 지우기는 일자 화면의 편집이라 말로 하지 않는다(#296).
 * 일정 추가와 같은 말을 받는다 — "약 챙기기 할 일 했어", "첫 번째 할 일 완료", "할 일 완료 취소". 할 일이 아니면 null이다.
 */
internal fun CalendarDayUiState.todoVoice(text: String, toggle: (id: String, done: Boolean) -> Unit): VoiceReply? =
    when (val voice = parseScheduleAddVoice(text)) {
        is ScheduleAddVoice.TodoCheck -> if (editing) VoiceReply.Done(R.string.voice_by_hand) else check(voice, toggle)
        is ScheduleAddVoice.TodoAdd, is ScheduleAddVoice.TodoDelete, is ScheduleAddVoice.TodoEdit ->
            VoiceReply.Done(R.string.voice_by_hand)

        else -> null
    }

private fun CalendarDayUiState.check(
    voice: ScheduleAddVoice.TodoCheck,
    toggle: (id: String, done: Boolean) -> Unit,
): VoiceReply {
    val matches = todos.matchItemsByVoice(voice.ordinal, voice.words) { it.label }
    val todo = matches.singleOrNull()
    return when {
        todo != null -> {
            toggle(todo.id, voice.done)
            val message = if (voice.done) R.string.voice_schedule_todo_done else R.string.voice_schedule_todo_undone
            VoiceReply.Done(message, listOf(todo.label))
        }

        matches.isEmpty() -> VoiceReply.Done(R.string.voice_schedule_todo_missing)
        else -> VoiceReply.Done(
            R.string.voice_schedule_todo_which,
            listOf(matches.size.toString()),
            question = VoiceText(
                R.string.voice_schedule_todo_ask_ordinal,
                listOf(
                    matches.joinToString("\n") {
                        "· ${it.label}"
                    },
                ),
            ),
        )
    }
}

/**
 * 다음 일정의 "시간 정하고 확정하기"(#336). 그 버튼은 시간이 아직 없을 때만 선다(1r-2-A). 다음 일정이 없거나
 * 이미 시간이 있으면 할 일이 없다고 한다.
 */
internal fun CalendarDayUiState.confirmNext(onConfirm: () -> Unit): VoiceReply {
    val next = nextEvent
    return when {
        next == null -> VoiceReply.Done(R.string.voice_day_no_next)
        next.at != null -> VoiceReply.Done(R.string.voice_day_next_timed)
        else -> {
            onConfirm()
            VoiceReply.Done(R.string.voice_day_next_confirm)
        }
    }
}
