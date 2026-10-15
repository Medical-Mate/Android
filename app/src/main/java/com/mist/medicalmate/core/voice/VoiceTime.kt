package com.mist.medicalmate.core.voice

import java.time.LocalTime

/**
 * 말로 한 시각(#320). "오후 8시", "저녁 7시 반", "여덟 시 삼십 분", "20시", "8시 10분".
 *
 * 알림 시간(1s-3)이 쓴다. 일정 추가의 시각 읽기는 예약 문자 규칙(`calendar/share`)과 함께 있어 도메인 밖에서
 * 쓸 수 없다. 짧은 말이라 여기서 따로 읽는다.
 *
 * 오전·오후를 말하지 않았으면 [afternoonUpTo]시까지를 오후로 본다. 알림은 저녁에 받는 것이라 11시까지를
 * 오후로 보는 식이다. "아침", "새벽"은 오전이다.
 */
fun parseVoiceClock(text: String, afternoonUpTo: Int = NOON - 1): LocalTime? {
    val heard = nativeHours(VoiceWords.compact(text))
    val match = CLOCK.find(heard) ?: return null
    fun group(name: String): String = match.groups[name]?.value.orEmpty()
    val hour = group("hour").toInt()
    val minute = when {
        group("half").isNotEmpty() -> HALF_HOUR
        group("minute").isNotEmpty() -> group("minute").toInt()
        else -> 0
    }
    val meridiem = group("meridiem")
    val afternoon = meridiem in PM || (meridiem.isEmpty() && hour in 1..afternoonUpTo)
    val hour24 = if (afternoon && hour < NOON) hour + NOON else hour
    return if (hour24 in 0..LAST_HOUR && minute in 0..LAST_MINUTE) LocalTime.of(hour24, minute) else null
}

/** "여덟시" → "8시", "삼십분" → "30분". 시 · 분 바로 앞의 우리말 수만 바꾼다. */
private fun nativeHours(heard: String): String {
    val hours = NATIVE_HOUR.replace(heard.replace("정오", "${NOON}시")) { "${HOURS.indexOf(it.groupValues[1]) + 1}시" }
    return SINO_MINUTE.replace(hours) { "${minuteOf(it.groupValues[1])}분" }
}

private fun minuteOf(word: String): Int {
    val tens = word.substringBefore("십", "")
    val ones = word.substringAfter("십", word)
    val ten = if (word.contains("십")) (SINO.indexOf(tens) + 1).coerceAtLeast(1) * TEN else 0
    return ten + SINO.indexOf(ones) + 1
}

private val CLOCK =
    Regex("""(?<meridiem>오전|오후|아침|새벽|낮|저녁|밤)?(?<hour>\d{1,2})시(?:(?<half>반)|(?<minute>\d{1,2})분)?""")

private val PM = setOf("오후", "낮", "저녁", "밤")

/** 한 시부터 열두 시까지. 자리 + 1이 그 시다. */
private val HOURS = listOf("한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열", "열한", "열두")

/** 긴 것을 먼저 둔다. "열한"이 "열"과 "한"으로 갈라지지 않는다. */
private val NATIVE_HOUR = Regex("""(열한|열두|다섯|여섯|일곱|여덟|아홉|한|두|세|네|열)시""")

/** 일부터 구까지. 자리 + 1이 그 수다. 없으면 자리가 -1이라 0이 된다. */
private val SINO = listOf("일", "이", "삼", "사", "오", "육", "칠", "팔", "구")

private val SINO_MINUTE = Regex("""([일이삼사오육칠팔구]?십[일이삼사오육칠팔구]?|[일이삼사오육칠팔구])분""")

private const val NOON = 12
private const val HALF_HOUR = 30
private const val TEN = 10
private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59
