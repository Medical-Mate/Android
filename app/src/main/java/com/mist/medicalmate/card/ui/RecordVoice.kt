package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSuggestion
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.YearMonth

/**
 * 기록 탭(1j-1)에서 말로 기록을 찾는다(#324). 전에는 이 탭에 알아듣는 말이 없어 "첫 번째 기록 열어 줘"가 기록
 * 탭으로 옮기기만 하고 아무것도 열지 않았다.
 *
 * 단서는 몇 번째 · 진료한 날이나 달 · 제목이나 병원의 낱말 · "최근"이다. 함께 말하면 모두 맞는 것만 남긴다.
 * 단서가 없으면 null이다 — 이 화면의 말이 아니라 전역 명령으로 넘긴다.
 */
internal fun List<RecordItem>.findRecordByVoice(text: String, today: LocalDate): List<RecordItem>? {
    val heard = VoiceWords.compact(text)
    val ordinal = VoiceWords.ordinal(text) ?: 0.takeIf { RECENT.any { heard.contains(it) } }
    val date = parseVoiceDate(text, today)
    val words = recordWordsOf(text)
    if (ordinal == null && date == null && words.isEmpty()) return null
    val matched = filter { item -> item.matches(date, words) }
    val index = ordinal?.let { if (it == VoiceWords.LAST) matched.lastIndex else it }
    val vague = ordinal == null && date == null && !heard.contains(RECORD_WORD)
    return when {
        pointsElsewhere(heard) -> null
        index != null -> listOfNotNull(matched.getOrNull(index))
        // 기록을 가리킨 말이 아닌데 낱말이 아무 기록에도 없으면 이 화면의 말이 아니다(#333). "캘린더 보여 줘",
        // "증상 정리할래"가 "그런 기록을 찾지 못했어요"가 돼 전역 명령으로 가지 못했다. 카드 목록과 같은 규칙이다.
        matched.isEmpty() && vague -> null
        else -> matched
    }
}

/** 말한 날이나 달에 다녀왔고, 남은 낱말이 모두 제목이나 병원에 있는지. */
private fun RecordItem.matches(date: VoiceDate?, words: List<String>): Boolean {
    val onDate =
        when (date) {
            is VoiceDate.Day -> visitedOn == date.date
            is VoiceDate.Month -> visitedOn?.let(YearMonth::from) == date.month
            null -> true
        }
    return onDate && words.all { VoiceWords.compact(title + (clinic ?: "")).contains(it) }
}

/** 다른 화면의 것을 가리키는지(#333). "10월 20일 일정", "두 번째 카드"는 날짜나 몇 번째가 있어도 넘긴다. */
private fun pointsElsewhere(heard: String): Boolean =
    !heard.contains(RECORD_WORD) && OTHER_SCREENS.any { heard.contains(it) }

/** 기록을 가리키는 낱말. "정형외과 기록", "진료 기록 열어 줘". */
private const val RECORD_WORD = "기록"

/** 다른 화면의 것. 기록 탭에서 말해도 그 화면이 받는다. */
private val OTHER_SCREENS = listOf("일정", "캘린더", "달력", "카드", "브리핑")

/** 기록 탭이 받는 말의 갈래. */
internal enum class RecordVoiceIntent {
    /** "기록 몇 개야". */
    COUNT,

    /** "기록 지워 줘". 말로는 지우지 않는다. */
    DELETE,
}

internal fun parseRecordIntent(text: String): RecordVoiceIntent? {
    val heard = VoiceWords.compact(text)
    return when {
        DELETE.any { heard.contains(it) } -> RecordVoiceIntent.DELETE
        COUNT.any { heard.contains(it) } -> RecordVoiceIntent.COUNT
        else -> null
    }
}

/**
 * 기록 탭의 말을 버튼과 같은 일로. 하나로 좁혀지면 그 기록을 열고, 여럿이면 후보를 띄워 몇 번째인지 묻는다. 그 다음
 * "두 번째"는 후보 안에서 센다. 기록이 하나도 없으면 없다고 한다. 편집 중에는 받지 않는다.
 */
@Composable
internal fun RecordVoiceTarget(uiState: StateFlow<RecordUiState>, onOpen: (RecordItem) -> Unit) {
    val today = remember { LocalDate.now() }
    val candidates = remember { mutableListOf<RecordItem>() }
    VoiceTargetEffect(RECORD_SUGGESTIONS) { text ->
        // 다른 화면에서 넘겨받은 말은 목록을 읽기 전에 올 수 있다. 읽을 때까지 기다린다.
        val content =
            withTimeoutOrNull(RECORD_WAIT_MS) {
                uiState.first { it !is RecordUiState.Loading }
            } as? RecordUiState.Content
        val items = content?.groups.orEmpty().flatMap { it.items }
        val intent = parseRecordIntent(text)
        val pool = candidates.toList().ifEmpty { items }
        val found = pool.findRecordByVoice(text, today)
        when {
            content == null || content.selectedIds != null -> VoiceReply.NotMine
            intent == RecordVoiceIntent.DELETE -> VoiceReply.Done(R.string.voice_by_hand)
            items.isEmpty() && (intent != null || found != null) -> VoiceReply.Done(R.string.voice_record_none)
            intent == RecordVoiceIntent.COUNT ->
                VoiceReply.Done(R.string.voice_record_count, listOf(items.size.toString()))

            found == null -> VoiceReply.NotMine
            found.isEmpty() -> VoiceReply.Done(R.string.voice_record_not_found, question = askRecord())
            found.size == 1 -> {
                candidates.clear()
                onOpen(found.single())
                VoiceReply.Done(R.string.voice_card_opened, listOf(found.single().title))
            }

            else -> {
                candidates.clear()
                candidates += found
                VoiceReply.Done(
                    R.string.voice_record_which,
                    listOf(found.size.toString()),
                    question = VoiceText(
                        R.string.voice_card_ask_which,
                        listOf(
                            found.joinToString("\n") {
                                it.voiceLine()
                            },
                        ),
                    ),
                )
            }
        }
    }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) {
        val content =
            withTimeoutOrNull(RECORD_WAIT_MS) {
                uiState.first { it !is RecordUiState.Loading }
            } as? RecordUiState.Content
        if (content?.groups.orEmpty().any { it.items.isNotEmpty() }) prompt(VoicePrompt(question = askRecord()))
    }
}

/** 목록을 읽기를 기다리는 한도. */
private const val RECORD_WAIT_MS = 5_000L

private fun askRecord() = VoiceText(R.string.voice_records_ask)

/** 막대에 띄우는 후보 줄. 진료한 날을 붙여 같은 제목끼리도 가린다. */
private fun RecordItem.voiceLine(): String =
    "· $title" + (visitedOn?.let { " (${it.monthValue}.${it.dayOfMonth})" } ?: "")

/** 찾을 낱말. 기록을 가리키는 말 · 동사 · 날짜 · 몇 번째를 뺀다. */
private fun recordWordsOf(text: String): List<String> = VoiceWords.stripOrdinal(text)
    .replace(DATE_WORDS, " ")
    .split(' ', ',', '.', '?', '!')
    .map { VoiceWords.compact(it).replace(PARTICLE_TAIL, "") }
    .filter { word -> word.length >= 2 && word !in FILLER && FILLER_PREFIX.none { word.startsWith(it) } }

internal val RECORD_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_record_recent,
            R.string.voice_suggest_record_recent_do,
            MedicalMateIcons.Note,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_record_ordinal,
            R.string.voice_suggest_record_ordinal_do,
            MedicalMateIcons.Note,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_record_date,
            R.string.voice_suggest_record_date_do,
            MedicalMateIcons.Calendar,
        ),
    )

private val RECENT = listOf("최근", "방금", "최신", "마지막에다녀온", "지난번", "저번에다녀온", "제일늦게")

private val COUNT = listOf("몇개", "몇건", "몇번", "얼마나있", "몇번이나")

private val DELETE = listOf("지워", "삭제", "없애", "빼줘")

private val DATE_WORDS =
    Regex(
        """\d{1,2}\s*월(?:\s*\d{1,2}\s*일)?|\d{1,2}\s*일|오늘|어제|그저께|그제|지난\s*달|저번\s*달|이번\s*달|전\s*달|""" +
            """이전\s*달|지난\s*주|이번\s*주""",
    )

private val PARTICLE_TAIL = Regex("""(?:에서|으로|이랑|하고|이|가|을|를|은|는|의|에|로|요|거|것|꺼|꺼야)+$""")

/** 찾을 낱말이 아닌 말. */
private val FILLER =
    setOf(
        "기록", "진료기록", "진료", "병원", "다녀온", "다녀왔던", "갔던", "받은", "내역", "목록", "최근", "방금", "최신",
        "지난번", "제일", "가장", "마지막", "줘", "주세요", "좀", "그", "거", "것", "꺼", "내", "제",
    )

/** 이 말로 시작하면 동사다. "열어 줘", "보여 줘", "찾아 줘". */
private val FILLER_PREFIX = listOf("열어", "열래", "보여", "볼래", "찾아", "띄워", "보고", "선택", "골라", "고를", "봐")
