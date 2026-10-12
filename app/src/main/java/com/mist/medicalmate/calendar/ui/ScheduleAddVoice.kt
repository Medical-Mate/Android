package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 일정 추가(1r-4)에서 알아듣는 말(#282).
 *
 * 화면의 칸과 버튼을 모두 말로 부른다. 날짜·시간·병원은 예약 문자 공유(#270)의 읽기를 그대로
 * 써서, "10월 20일 오후 3시 고려튼튼통증의학과"처럼 한 번에 말해도 칸이 채워진다.
 *
 * 몇 번째인지(`ordinal`)는 0부터 센다. [VoiceWords.LAST]면 마지막이다. 없으면 [ScheduleAddVoice.words]의
 * 낱말로 고른다.
 */
internal sealed interface ScheduleAddVoice {
    /** 저장. 확인을 받고 한다. */
    data object Save : ScheduleAddVoice

    /** 화면을 닫는다. 적은 것이 있으면 확인을 받는다. */
    data object Exit : ScheduleAddVoice

    /** 지금 병원 이름으로 병원 찾기를 연다. "병원 바꿀래". */
    data object OpenHospital : ScheduleAddVoice

    /** 날짜나 시간을 고르는 시트를 연다. "날짜 고를래". */
    data class OpenSheet(val sheet: ScheduleAddSheet) : ScheduleAddVoice

    /** 시간을 비운다. 시간 미정으로 저장된다. */
    data object ClearTime : ScheduleAddVoice

    /** 할 일 한 줄을 더한다. 글이 비면 무엇을 적을지 묻는다. */
    data class TodoAdd(val label: String) : ScheduleAddVoice

    data class TodoCheck(val ordinal: Int?, val words: String, val done: Boolean) : ScheduleAddVoice

    data class TodoDelete(val ordinal: Int?, val words: String) : ScheduleAddVoice

    data class TodoEdit(val ordinal: Int?, val label: String) : ScheduleAddVoice

    /** 가져갈 카드로 고른다. "두 번째 카드", "두통 카드". */
    data class CardPick(val ordinal: Int?, val words: String) : ScheduleAddVoice

    /** 가져갈 카드에서 뺀다. "두통 카드 빼 줘". */
    data class CardDrop(val ordinal: Int?, val words: String) : ScheduleAddVoice

    /** 새 브리핑 카드를 만들러 간다. */
    data object CardNew : ScheduleAddVoice

    /** 날짜·시간·병원일 수 있는 말. 읽어 봐서 없으면 이 화면의 말이 아니다. */
    data class Fill(val text: String) : ScheduleAddVoice
}

/**
 * 받아쓴 말을 일정 추가의 할 일로.
 *
 * 차례가 곧 우선순위다. 할 일과 카드는 그 낱말이 있을 때만이고 먼저 본다("할 일, 10시 전에
 * 금식"의 10시가 진료 시각이 되면 안 된다). 화면 조작(나가기·시트·병원 찾기·시간 비우기)은
 * 그 말로 시작할 때만이다. 저장은 날짜가 함께 없을 때만이다. "20일로 저장해 줘"는 날짜를 고치는
 * 말이 먼저다. 나머지는 날짜·시간·병원으로 읽어 본다.
 */
internal fun parseScheduleAddVoice(text: String): ScheduleAddVoice? = when {
    text.isBlank() -> null
    TODO_WORD.containsMatchIn(text) -> parseTodo(text)
    text.contains("카드") -> parseCard(text)
    else -> parseScreen(text)
}

/**
 * 할 일. 지우기·고치기·체크가 앞서고, 그 말이 없을 때 "할 일"로 시작하면 더하기다.
 *
 * "할 일 추가"처럼 글이 비면 [ScheduleAddVoice.TodoAdd]의 글이 빈 채로 간다. 무엇을 적을지 묻는다.
 */
private fun parseTodo(text: String): ScheduleAddVoice? {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    val add = TODO_ADD.find(text)?.groupValues?.get(1)?.trim { it in TRIM }
    val edit = EDIT.find(text)?.groupValues?.get(1)?.trim { it in TRIM }
    return when {
        DELETE.any { it in compact } -> ScheduleAddVoice.TodoDelete(ordinal, text)
        edit != null -> ScheduleAddVoice.TodoEdit(ordinal, edit)
        UNDONE.any { it in compact } -> ScheduleAddVoice.TodoCheck(ordinal, text, done = false)
        DONE.any { it in compact } -> ScheduleAddVoice.TodoCheck(ordinal, text, done = true)
        add != null -> ScheduleAddVoice.TodoAdd(add)
        else -> null
    }
}

private fun parseCard(text: String): ScheduleAddVoice {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    return when {
        CARD_NEW.any { it in compact } -> ScheduleAddVoice.CardNew
        CARD_DROP.any { it in compact } -> ScheduleAddVoice.CardDrop(ordinal, text)
        else -> ScheduleAddVoice.CardPick(ordinal, text)
    }
}

/** 화면 조작과 저장. 어느 것도 아니면 날짜·시간·병원으로 읽어 본다. */
private fun parseScreen(text: String): ScheduleAddVoice = when {
    EXIT.containsMatchIn(text) -> ScheduleAddVoice.Exit
    OPEN_HOSPITAL.containsMatchIn(text) -> ScheduleAddVoice.OpenHospital
    OPEN_DATE.containsMatchIn(text) -> ScheduleAddVoice.OpenSheet(ScheduleAddSheet.DATE)
    OPEN_TIME.containsMatchIn(text) -> ScheduleAddVoice.OpenSheet(ScheduleAddSheet.TIME)
    CLEAR_TIME.containsMatchIn(text) -> ScheduleAddVoice.ClearTime
    VoiceWords.isSave(text) && !DIGIT.containsMatchIn(text) -> ScheduleAddVoice.Save
    else -> ScheduleAddVoice.Fill(text)
}

/**
 * 말로 고른 카드. 몇 번째로 말했으면 그 차례, 아니면 제목의 낱말이 말에 든 카드다.
 *
 * 제목 낱말은 두 글자 이상만 본다. "복부 통증 · 3주"의 "3주"나 "·"로 엉뚱한 카드를 고르지 않는다.
 * 여러 장이 걸리면 가장 많이 겹친 카드다. 하나도 안 걸리면 null이다.
 */
internal fun List<ScheduleAddCard>.byVoice(ordinal: Int?, words: String): ScheduleAddCard? =
    pickByVoice(ordinal, words) { it.title }

/** 말로 고른 할 일. 카드와 같은 규칙으로 할 일의 글에서 낱말을 찾는다. */
internal fun List<ScheduleAddTodo>.byVoice(ordinal: Int?, words: String): ScheduleAddTodo? =
    pickByVoice(ordinal, words) { it.label }

private fun <T> List<T>.pickByVoice(ordinal: Int?, words: String, label: (T) -> String): T? {
    if (ordinal != null) return if (ordinal == VoiceWords.LAST) lastOrNull() else getOrNull(ordinal)
    val heard = VoiceWords.compact(words)
    return map { item -> item to label(item).meaningfulWords().count { it in heard } }
        .filter { (_, hits) -> hits > 0 }
        .maxByOrNull { (_, hits) -> hits }
        ?.first
}

private fun String.meaningfulWords(): List<String> =
    split(' ', '·', ',', '/').map(VoiceWords::compact).filter { it.length >= 2 && !DIGIT.containsMatchIn(it) }

private val TODO_WORD = Regex("""할\s*일""")

private val TODO_ADD = Regex("""^\s*할\s*일\s*(?:추가|더해|적어)?\s*(?:해\s*줘|해\s*주세요)?\s*[,:]?\s*(.*)$""")

/** "첫 번째 할 일 고쳐, 약 챙기기" → "약 챙기기". */
private val EDIT = Regex("""(?:고쳐|수정|바꿔)\s*(?:줘|주세요)?\s*[,:]?\s*(.+)$""")

private val DELETE = listOf("지워", "삭제", "빼줘", "없애")

private val UNDONE = listOf("완료취소", "체크해제", "안했", "취소")

private val DONE = listOf("완료", "했어", "했다", "체크", "끝냈")

private val CARD_NEW = listOf("새카드", "새로만들", "카드만들")

private val CARD_DROP = listOf("빼", "안가져", "해제", "취소")

private val EXIT = Regex("""^\s*(?:나가|나갈래|일정\s*추가\s*(?:취소|안\s*할래|그만))""")

private val OPEN_HOSPITAL = Regex("""^\s*(?:다른\s*)?병원\s*(?:찾기|찾아|바꿔|바꿀래|바꾸기|고를래|고르기|선택)""")

private val OPEN_DATE = Regex("""^\s*날짜\s*(?:고를래|고르기|선택|열어)""")

private val OPEN_TIME = Regex("""^\s*시간\s*(?:고를래|고르기|선택|열어)""")

private val CLEAR_TIME = Regex("""시간\s*(?:미정|지워|빼|없음|몰라|모름|비워)""")

private val DIGIT = Regex("""\d""")

private const val TRIM = " .,!?"
