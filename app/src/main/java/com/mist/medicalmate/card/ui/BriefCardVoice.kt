package com.mist.medicalmate.card.ui

import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.core.voice.VoiceWords
import java.time.LocalDate
import java.time.YearMonth

/**
 * 말로 브리핑 카드를 찾는다(#286). "두 번째", "두통 카드", "9월 4일 카드", "고려튼튼 거".
 *
 * 단서가 넷이다. 몇 번째인지, 제목이나 병원에 든 낱말, 작성한 날이나 달이다. 함께 말하면 모두 맞는
 * 것만 남긴다("지난달 두통 카드"). 몇 번째는 남은 것 안에서 센다. 받는 목록이 화면에 보이는 차례 그대로다 —
 * 맨 위의 카드 없는 문답 줄도 한 줄로 든다(#344, `voiceRows`).
 *
 * 단서가 하나도 없으면 null이다. "카드 보여 줘"는 찾으라는 말이 아니다. 카드를 가리키지 않은 말의 낱말이
 * 아무 카드에도 없을 때도 null이다 — 전역 명령으로 넘긴다.
 */
internal fun List<CardListItem>.findByVoice(text: String, today: LocalDate): List<CardListItem>? =
    // 새로 만들자는 말은 찾는 말이 아니다(#320). 전역 명령이 증상 정리를 연다 — 빈 목록의 유일한 길이다.
    if (CREATE.containsMatchIn(VoiceWords.compact(text))) null else findCard(text, today)

/**
 * 카드를 지우라는 말인지(#333). 말로는 지우지 않는다 — 완성된 카드다(#296). 묻지 않으면 "두 번째 카드 지워 줘"가
 * 그 카드를 열었다. 기록 탭과 같은 낱말이다.
 */
internal fun isCardDeleteRequest(text: String): Boolean = DELETE.any { VoiceWords.compact(text).contains(it) }

private val DELETE = listOf("지워", "삭제", "없애", "빼줘")

/** "최근 카드", "방금 만든 거"는 맨 위 카드다(#320). 목록이 최근 것부터 선다. */
private fun cardOrdinal(text: String): Int? =
    VoiceWords.ordinal(text) ?: 0.takeIf { RECENT.any { VoiceWords.compact(text).contains(it) } }

private fun List<CardListItem>.findCard(text: String, today: LocalDate): List<CardListItem>? {
    val ordinal = cardOrdinal(text)
    val written = writtenOf(text, today)
    val words = cardWordsOf(text)
    if (ordinal == null && written == null && words.isEmpty()) return null
    val matched =
        filter { card -> written?.matches(card.writtenOn) != false }
            .filter { card -> words.all { VoiceWords.compact(card.title + (card.clinic ?: "")).contains(it) } }
    val index = ordinal?.let { if (it == VoiceWords.LAST) matched.lastIndex else it }
    // 카드를 가리킨 말이 아닌데 낱말이 아무 카드에도 없으면 이 화면의 말이 아니다. "홈으로 가"가 그렇다.
    val vague = ordinal == null && written == null && CARD_WORDS.none { VoiceWords.compact(text).contains(it) }
    return when {
        index != null -> listOfNotNull(matched.getOrNull(index))
        matched.isEmpty() && vague -> null
        else -> matched
    }
}

/** 막대에 띄우는 후보 줄. 제목 뒤에 작성한 날을 붙여 같은 제목끼리도 가린다. */
internal fun CardListItem.voiceLine(): String = "· $title (${writtenOn.monthValue}.${writtenOn.dayOfMonth})"

/** 작성한 날이나 달. 날을 말했으면 그 날, 달만 말했으면 그 달이다. */
private data class Written(val day: LocalDate? = null, val month: YearMonth? = null) {
    fun matches(date: LocalDate): Boolean = day?.let { it == date } ?: (month == YearMonth.from(date))
}

/**
 * 말 속의 작성일. "9월 4일", "4일"(이번 달), "9월", "오늘", "어제", "지난달", "이번 달".
 *
 * 해는 올해로 본다. 달이 오늘보다 뒤면 지난해다 — 카드는 이미 쓴 것이라 앞날이 없다.
 */
private fun writtenOf(text: String, today: LocalDate): Written? {
    val heard = VoiceWords.compact(text)
    val monthDay = MONTH_DAY.find(heard)
    val month = MONTH.find(heard)?.groupValues?.get(1)?.toInt()
    val day = DAY.find(heard)?.groupValues?.get(1)?.toInt()
    return when {
        heard.contains("오늘") -> Written(day = today)
        heard.contains("어제") -> Written(day = today.minusDays(1))
        heard.contains("그저께") || heard.contains("그제") -> Written(day = today.minusDays(2))
        LAST_MONTH.any { heard.contains(it) } -> Written(month = YearMonth.from(today).minusMonths(1))
        heard.contains("이번달") -> Written(month = YearMonth.from(today))
        monthDay != null -> dayOf(monthDay.groupValues[1].toInt(), monthDay.groupValues[2].toInt(), today)
        month != null && month in 1..MONTHS -> Written(month = pastMonth(month, today))
        day != null -> dayOf(today.monthValue, day, today)
        else -> null
    }
}

private fun dayOf(month: Int, day: Int, today: LocalDate): Written? {
    val ym = pastMonth(month, today)
    return if (month in 1..MONTHS && day in 1..ym.lengthOfMonth()) Written(day = ym.atDay(day)) else null
}

private fun pastMonth(month: Int, today: LocalDate): YearMonth {
    val thisYear = YearMonth.of(today.year, month)
    return if (thisYear.isAfter(YearMonth.from(today))) thisYear.minusYears(1) else thisYear
}

/**
 * 제목이나 병원에서 찾을 낱말. 몇 번째·날짜·"카드"·"열어 줘" 같은 말과 낱말 끝의 조사를 떼고 남은 것이다.
 */
private fun cardWordsOf(text: String): List<String> = VoiceWords.stripOrdinal(text)
    .replace(DATE_PHRASE, " ")
    .split(' ', ',', '.', '?', '!')
    .map { it.replace(PARTICLE_TAIL, "") }
    .map(VoiceWords::compact)
    .filter { word -> word.isNotEmpty() && word !in FILLER && FILLER_PREFIX.none { word.startsWith(it) } }

private val CARD_WORDS = listOf("카드", "브리핑")

private val MONTH_DAY = Regex("""(\d{1,2})월(\d{1,2})일""")

private val MONTH = Regex("""(\d{1,2})월(?!요일)""")

private val DAY = Regex("""(?<!\d)(\d{1,2})일(?!요일)""")

private val LAST_MONTH = listOf("지난달", "저번달", "전달")

private const val MONTHS = 12

/** 낱말을 뽑기 전에 지울 날짜 표현. 띄어쓰기가 있어도 지운다. */
private val DATE_PHRASE =
    Regex("""\d{1,2}\s*월(?:\s*\d{1,2}\s*일)?|\d{1,2}\s*일|오늘|어제|그저께|그제|지난\s*달|저번\s*달|이번\s*달|전\s*달""")

/** 낱말 끝의 조사와 군말. "두통이" → "두통", "어깨거" → "어깨". */
private val PARTICLE_TAIL = Regex("""(?:에서|으로|이랑|하고|이|가|을|를|은|는|의|에|로|요|거|것|꺼)+$""")

private val CREATE = Regex("""만들|작성할|작성해|새카드|새로운카드|새로""")

private val RECENT = listOf("최근", "방금", "최신", "제일늦게", "마지막에만든", "마지막에쓴")

/** 찾을 낱말이 아닌 말. */
private val FILLER =
    setOf(
        "브리핑", "카드", "브리핑카드", "줘", "주세요", "좀", "그", "이거", "저거", "그거", "하나", "중", "중에", "다시",
        "작성", "작성한", "쓴", "만든", "병원", "목록", "것", "거", "꺼", "에서", "내", "제", "최근", "최근에", "방금",
        "가장", "제일", "최신",
    )

/** 이 말로 시작하면 동사다. "열어 줘", "보여 줘", "찾아 줘". */
private val FILLER_PREFIX = listOf("열어", "열래", "보여", "볼래", "찾아", "띄워", "보고", "선택", "골라", "고를")
