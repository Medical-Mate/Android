package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 일자 화면(1r-2)에서 말로 하는 일(#288). */
internal enum class DayVoice {
    /** 진료 후 기록을 남긴다. "기록할래", "진료 후 기록 남길래". */
    RECORD_ADD,

    /** 남긴 기록을 본다. "기록 보여 줘". */
    RECORD_OPEN,

    /** 가져간 카드를 본다. "카드 보여 줘". */
    CARD,
}

/** 일자 화면의 말을 읽는다. 아니면 null이다. 보는 말이 있으면 남기는 말이 아니다. */
internal fun parseDayVoice(text: String): DayVoice? {
    val heard = VoiceWords.compact(text)
    val looking = LOOK.any { heard.contains(it) }
    return when {
        heard.contains("카드") -> DayVoice.CARD
        heard.contains("기록") && looking -> DayVoice.RECORD_OPEN
        RECORD_ADD.any { heard.contains(it) } -> DayVoice.RECORD_ADD
        else -> null
    }
}

/**
 * 일자 화면이 알아듣는 말(#288). 버튼과 같은 일을 한다.
 *
 * 진료 후 기록은 그 날 일정에 카드가 걸려 있을 때만 남긴다. 버튼도 그때만 선다 — 카드 없이 들어가면 끝에서
 * 저장할 곳이 없다. 이미 남긴 기록이 있으면 새로 쓰지 않고 그 기록을 보라고 한다.
 *
 * 다른 화면에서 "진료 후 기록할래"로 들어오면 아직 그 날을 읽기 전이라 읽을 때까지 기다린다.
 */
@Composable
internal fun CalendarDayVoice(
    uiState: StateFlow<CalendarDayUiState?>,
    onRecordAdd: (cardId: String, cardTitle: String, clinic: String?, address: String?) -> Unit,
    onRecordOpen: (String) -> Unit,
    onCardOpen: (String) -> Unit,
) {
    VoiceTargetEffect { text ->
        val voice = parseDayVoice(text)
        val state = voice?.let { withTimeoutOrNull(DAY_WAIT_MS) { uiState.first { it != null } } }
        val card = state?.card
        val record = state?.records?.firstOrNull()
        when {
            state == null -> VoiceReply.NotMine
            voice == DayVoice.CARD && card != null -> {
                onCardOpen(card.id)
                VoiceReply.Done(R.string.voice_card_opened, listOf(card.title))
            }

            voice == DayVoice.RECORD_OPEN && record != null -> {
                onRecordOpen(record.id)
                VoiceReply.Done(R.string.voice_day_record_opened)
            }

            record != null -> VoiceReply.Done(
                R.string.voice_day_recorded,
                question = VoiceText(R.string.voice_day_ask_open),
            )
            card == null -> VoiceReply.Done(R.string.voice_day_no_card)
            else -> {
                onRecordAdd(card.id, card.title, state.schedule?.title, card.clinicAddress)
                VoiceReply.Done(R.string.voice_day_record_start)
            }
        }
    }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) {
        val state = withTimeoutOrNull(DAY_WAIT_MS) { uiState.first { it != null } } ?: return@LaunchedEffect
        val canRecord = state.card != null && state.records.isEmpty()
        if (canRecord) prompt(VoicePrompt(question = VoiceText(R.string.voice_day_ask)))
    }
}

/** 그 날을 읽기를 기다리는 한도. */
private const val DAY_WAIT_MS = 5_000L

private val LOOK = listOf("보여", "열어", "볼래", "봐", "확인")

private val RECORD_ADD =
    listOf("기록할", "기록하기", "기록남", "기록쓸", "기록써", "기록적", "기록해", "진료후기록", "기록시작")
