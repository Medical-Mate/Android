package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

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
    // 알림 설정은 내 정보의 일이다. "진료 후 기록 알림 꺼 줘"가 기록을 시작하면 안 된다(#320).
    if (heard.contains("알림")) return null
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
 * 저장할 곳이 없다. 이미 남긴 기록이 있으면 새로 쓰지 않고 그 기록을 보라고 한다. 아직 오지 않은 날이면
 * 진료가 끝난 뒤에 하라고 한다(#302) — 버튼도 그 날에는 서지 않는다.
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
    VoiceTargetEffect(CALENDAR_DAY_SUGGESTIONS) { text ->
        val voice = parseDayVoice(text)
        val state = voice?.let { withTimeoutOrNull(DAY_WAIT_MS) { uiState.first { it != null } } }
        val spokenDay = state?.let { (parseVoiceDate(text, LocalDate.now()) as? VoiceDate.Day)?.date }
        when {
            state == null -> VoiceReply.NotMine
            // 다른 날을 말했으면 이 화면의 일이 아니다(#320). 전역 명령이 그 날을 연다.
            spokenDay != null && spokenDay != state.date -> VoiceReply.NotMine
            voice == DayVoice.RECORD_ADD -> state.startRecord(onRecordAdd)
            else -> state.open(voice, onRecordOpen, onCardOpen)
        }
    }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) {
        val state = withTimeoutOrNull(DAY_WAIT_MS) { uiState.first { it != null } } ?: return@LaunchedEffect
        if (state.canRecord) prompt(VoicePrompt(question = VoiceText(R.string.voice_day_ask)))
    }
}

/**
 * 남긴 기록이나 가져간 카드를 연다. 보자고 한 것이 없으면 없다고만 한다(#320). 전에는 기록을 남기는 갈래로 흘러
 * "기록 보여 줘"가 새 기록을 시작했다.
 */
private fun CalendarDayUiState.open(voice: DayVoice?, onRecordOpen: (String) -> Unit, onCardOpen: (String) -> Unit) =
    when (voice) {
        DayVoice.CARD -> card?.let {
            onCardOpen(it.id)
            VoiceReply.Done(R.string.voice_card_opened, listOf(it.title))
        } ?: VoiceReply.Done(R.string.voice_day_no_card_to_open)

        else -> records.firstOrNull()?.let {
            onRecordOpen(it.id)
            VoiceReply.Done(R.string.voice_day_record_opened)
        } ?: VoiceReply.Done(
            R.string.voice_day_no_record,
            question = VoiceText(R.string.voice_day_ask).takeIf { canRecord },
        )
    }

/** 진료 후 기록을 시작한다. 버튼과 같은 조건이다. */
private fun CalendarDayUiState.startRecord(
    onRecordAdd: (cardId: String, cardTitle: String, clinic: String?, address: String?) -> Unit,
): VoiceReply {
    val card = card
    return when {
        records.isNotEmpty() -> VoiceReply.Done(
            R.string.voice_day_recorded,
            question = VoiceText(R.string.voice_day_ask_open),
        )
        card == null -> VoiceReply.Done(R.string.voice_day_no_card)
        upcoming -> VoiceReply.Done(R.string.voice_day_upcoming)
        else -> {
            onRecordAdd(card.id, card.title, schedule?.title, card.clinicAddress)
            VoiceReply.Done(R.string.voice_day_record_start)
        }
    }
}

/** 그 날을 읽기를 기다리는 한도. */
private const val DAY_WAIT_MS = 5_000L

private val LOOK = listOf("보여", "열어", "볼래", "봐", "확인")

/** 기록을 남기자는 말. 전역 명령과 같은 말을 받는다 — 전역 예시 "진료 끝났어"가 이 화면에 넘어온다(#320). */
private val RECORD_ADD =
    listOf(
        "기록할", "기록하기", "기록남", "기록쓸", "기록써", "기록적", "기록해", "진료후기록", "기록시작", "진료끝", "진료다녀",
        "진료받고왔", "진료받았", "진료봤", "병원다녀왔", "들은말", "들은거",
    )
