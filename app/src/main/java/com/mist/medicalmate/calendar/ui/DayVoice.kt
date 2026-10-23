package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

    /** 다음 일정의 "시간 정하고 확정하기"(#336). "시간 정할래", "다음 일정 확정할래". */
    CONFIRM_NEXT,

    /** 이 날로 일정을 추가한다(#336). "일정 추가해 줘". 전역 명령으로 가면 이 날이 빠졌다. */
    SCHEDULE_ADD,
}

/** 일자 화면의 말을 읽는다. 아니면 null이다. 보는 말이 있으면 남기는 말이 아니다. */
internal fun parseDayVoice(text: String): DayVoice? {
    val heard = VoiceWords.compact(text)
    // 알림 설정은 내 정보의 일이다. "진료 후 기록 알림 꺼 줘"가 기록을 시작하면 안 된다(#320).
    if (heard.contains("알림")) return null
    val looking = LOOK.any { heard.contains(it) }
    return when {
        CONFIRM_NEXT.any { heard.contains(it) } -> DayVoice.CONFIRM_NEXT
        SCHEDULE_ADD.any { heard.contains(it) } -> DayVoice.SCHEDULE_ADD
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
    more: CalendarDayVoiceRoutes = CalendarDayVoiceRoutes(),
) {
    // 되물은 할 일(#346). 다음 말이 번호면 그 후보 안에서 센다.
    val candidates = remember { VoiceCandidates() }
    VoiceTargetEffect(CALENDAR_DAY_SUGGESTIONS) { text ->
        val loaded = uiState.value
        val todo = loaded?.todoVoice(text, candidates, more.onTodoToggle)
        val voice = parseDayVoice(text)
        val state = voice?.let { withTimeoutOrNull(DAY_WAIT_MS) { uiState.first { it != null } } }
        val spokenDay = state?.let { (parseVoiceDate(text, LocalDate.now()) as? VoiceDate.Day)?.date }
        when {
            // 할 일 체크·해제(#336). 다른 말보다 먼저 본다 — "할 일" 낱말이 있을 때만 걸린다.
            todo != null -> todo
            state == null -> VoiceReply.NotMine
            // 다른 날을 말했으면 이 화면의 일이 아니다(#320). 전역 명령이 그 날을 연다.
            spokenDay != null && spokenDay != state.date -> VoiceReply.NotMine
            voice == DayVoice.RECORD_ADD -> state.startRecord(onRecordAdd)
            voice == DayVoice.CONFIRM_NEXT -> state.confirmNext(more.onNextConfirm)
            voice == DayVoice.SCHEDULE_ADD -> {
                more.onScheduleAdd(state.date)
                VoiceReply.Done(R.string.voice_day_schedule_add)
            }

            else -> state.open(voice, VoiceWords.ordinal(text), onRecordOpen, onCardOpen)
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
private fun CalendarDayUiState.open(
    voice: DayVoice?,
    ordinal: Int?,
    onRecordOpen: (String) -> Unit,
    onCardOpen: (String) -> Unit,
) = when (voice) {
    DayVoice.CARD -> card?.let {
        onCardOpen(it.id)
        VoiceReply.Done(R.string.voice_card_opened, listOf(it.title))
    } ?: VoiceReply.Done(R.string.voice_day_no_card_to_open)

    // 하루에 기록이 둘이면 "두 번째 기록 보여 줘"로 고른다(#336). 전에는 첫 기록만 열렸다.
    else -> records.getOrNull(if (ordinal == VoiceWords.LAST) records.lastIndex else ordinal ?: 0)?.let {
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

/**
 * 일자 화면의 말 가운데 화면을 옮기거나 상태를 바꾸는 것(#336). 할 일 체크 · 다음 일정 확정 · 이 날로 일정 추가다.
 */
internal class CalendarDayVoiceRoutes(
    val onTodoToggle: (id: String, done: Boolean) -> Unit = { _, _ -> },
    val onNextConfirm: () -> Unit = {},
    val onScheduleAdd: (LocalDate) -> Unit = {},
)

/** 그 날을 읽기를 기다리는 한도. */
private const val DAY_WAIT_MS = 5_000L

private val LOOK = listOf("보여", "열어", "볼래", "봐", "확인")

/** "시간 정할래", "시간 정하고 확정할래", "다음 일정 확정해 줘". */
private val CONFIRM_NEXT = listOf("시간정", "확정", "시간잡")

/** "일정 추가해 줘", "이 날 일정 잡아 줘". 확정하자는 말은 먼저 본다. */
private val SCHEDULE_ADD = listOf("일정추가", "일정잡", "일정만들", "일정넣", "일정등록")

/** 기록을 남기자는 말. 전역 명령과 같은 말을 받는다 — 전역 예시 "진료 끝났어"가 이 화면에 넘어온다(#320). */
private val RECORD_ADD =
    listOf(
        "기록할", "기록하기", "기록남", "기록쓸", "기록써", "기록적", "기록해", "진료후기록", "기록시작", "진료끝", "진료다녀",
        "진료받고왔", "진료받았", "진료봤", "병원다녀왔", "들은말", "들은거",
    )
