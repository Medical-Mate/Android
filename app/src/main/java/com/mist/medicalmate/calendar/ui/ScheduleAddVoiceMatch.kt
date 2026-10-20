package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 말에 맞는 카드들. 몇 번째로 말했으면 그 한 장, 아니면 제목의 낱말이 가장 많이 겹친 카드들이다.
 *
 * 제목 낱말은 두 글자 이상만 본다. "복부 통증 · 3주"의 "3주"나 "·"로 엉뚱한 카드를 고르지 않는다.
 * **여러 장이 똑같이 겹치면 모두 돌려준다.** 하나를 멋대로 고르지 않고 어느 것인지 되묻는다.
 */
@JvmName("matchCardsVoice")
internal fun List<ScheduleAddCard>.matchVoice(ordinal: Int?, words: String): List<ScheduleAddCard> =
    matchByVoice(ordinal, words) { it.title }

/** 말에 맞는 할 일들. 카드와 같은 규칙으로 할 일의 글에서 낱말을 찾는다. */
@JvmName("matchTodosVoice")
internal fun List<ScheduleAddTodo>.matchVoice(ordinal: Int?, words: String): List<ScheduleAddTodo> =
    matchByVoice(ordinal, words) { it.label }

/** 말로 고른 카드. 한 장으로 좁혀질 때만이다. */
internal fun List<ScheduleAddCard>.byVoice(ordinal: Int?, words: String): ScheduleAddCard? =
    matchVoice(ordinal, words).singleOrNull()

/** 말로 고른 할 일. 하나로 좁혀질 때만이다. */
internal fun List<ScheduleAddTodo>.byVoice(ordinal: Int?, words: String): ScheduleAddTodo? =
    matchVoice(ordinal, words).singleOrNull()

/** 몇 번째나 낱말로 고른 것들. 일자 화면의 할 일(#336)도 같은 규칙으로 고른다. */
internal fun <T> List<T>.matchItemsByVoice(ordinal: Int?, words: String, label: (T) -> String): List<T> =
    matchByVoice(ordinal, words, label)

private fun <T> List<T>.matchByVoice(ordinal: Int?, words: String, label: (T) -> String): List<T> {
    if (ordinal != null) return listOfNotNull(if (ordinal == VoiceWords.LAST) lastOrNull() else getOrNull(ordinal))
    val heard = VoiceWords.compact(words)
    val scored = map { item -> item to label(item).meaningfulWords().count { it in heard } }
    val best = scored.maxOfOrNull { (_, hits) -> hits } ?: 0
    return if (best == 0) emptyList() else scored.filter { (_, hits) -> hits == best }.map { it.first }
}

private fun String.meaningfulWords(): List<String> =
    split(' ', '·', ',', '/').map(VoiceWords::compact).filter { it.length >= 2 && !DIGIT_IN_WORD.containsMatchIn(it) }

private val DIGIT_IN_WORD = Regex("""\d""")
