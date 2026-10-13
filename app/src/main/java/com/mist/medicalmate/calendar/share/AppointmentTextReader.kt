package com.mist.medicalmate.calendar.share

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 예약 문자에서 일정 칸을 읽는다(#270).
 *
 * 일정 추가가 이 결과로 비어 있는 칸을 채운다. 사람이 확인하고 저장하므로 틀릴 수 있다는
 * 전제로 쓴다 — 못 읽은 칸은 비워 두고, 읽은 것도 덮어쓰지 않는다.
 */
internal interface AppointmentTextReader {
    suspend fun read(text: String): SharedAppointment
}

/** 문자에서 읽은 일정. 못 읽은 칸은 null이다. */
data class SharedAppointment(val hospital: String? = null, val date: LocalDate? = null, val time: LocalTime? = null)

/**
 * 문자 안의 날짜·시각 한 군데.
 *
 * [start]·[end]는 글 안의 자리다. 날짜 바로 뒤에 따로 붙은 시각을 같은 일정으로 묶을 때 쓴다.
 * [hasTime]이 거짓이면 [at]의 시각은 의미가 없다.
 */
internal data class DateTimeMention(val start: Int, val end: Int, val at: LocalDateTime, val hasTime: Boolean)

/**
 * 추출기가 돌려준 값 하나를 [DateTimeMention]으로.
 *
 * 하루까지 짚지 못한 것(연·월·주)은 버린다. "다음 주"는 날짜 칸에 넣을 수 없다.
 *
 * @param granularity 어디까지 짚었는지. [Granularity] 중 하나로 옮겨 받는다.
 */
internal fun dateTimeMention(
    start: Int,
    end: Int,
    timestampMillis: Long,
    granularity: Granularity,
    zone: ZoneId,
): DateTimeMention? {
    if (granularity == Granularity.COARSE) return null
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), zone).truncatedTo(ChronoUnit.MINUTES)
    return DateTimeMention(start, end, at, hasTime = granularity == Granularity.TIME)
}

/** 추출기가 짚은 정도. 하루보다 넓으면 [COARSE], 하루면 [DAY], 시각까지면 [TIME]이다. */
internal enum class Granularity {
    COARSE,
    DAY,
    TIME,
}

/**
 * 문자에 나온 날짜·시각 중 일정으로 삼을 것.
 *
 * **오늘보다 앞선 날은 버린다.** 예약 문자는 앞날을 알린다. 지난 날짜는 "9/30 접수분"처럼
 * 다른 이야기다.
 *
 * **처음 나온 날을 고른다.** 예약 문자는 진료 날을 먼저 적고 변경 기한 같은 날을 뒤에 적는다.
 *
 * **시각은 그 날짜와 붙어 있을 때만 받는다.** 추출기가 "10월 15일(목) 오후 2시"를 날짜와 시각
 * 두 군데로 나누어 줄 수 있어서, 날짜 바로 뒤([MAX_GAP]자 안)에 시작하는 시각은 같은 일정으로
 * 본다. 떨어진 시각("변경은 오후 6시까지")을 붙이면 엉뚱한 시각이 들어간다.
 *
 * **추출기가 시각을 못 읽으면 [text]에서 직접 읽는다.** `09월08일 10시00분`에서 날짜만 나오고
 * 시각이 빠진 일이 기기에서 있었다. 규칙은 [timeNear]에 있고 붙어 있어야 한다는 조건은 같다.
 */
internal fun pickSchedule(
    mentions: List<DateTimeMention>,
    today: LocalDate,
    text: String = "",
): Pair<LocalDate?, LocalTime?> {
    val ahead = mentions.filter { !it.at.toLocalDate().isBefore(today) }.sortedBy { it.start }
    val dated = ahead.firstOrNull() ?: return null to null
    val time =
        if (dated.hasTime) {
            dated.at.toLocalTime()
        } else {
            ahead.firstOrNull { it.hasTime && it.start >= dated.end && it.start - dated.end <= MAX_GAP }
                ?.at
                ?.toLocalTime()
                ?: timeNear(text, dated.start, dated.end)
        }
    return dated.at.toLocalDate() to time
}

/**
 * 날짜 자리 안이나 바로 뒤에 적힌 시각.
 *
 * 날짜 자리 안을 먼저 본다. 추출기가 `09월08일 10시00분`을 한 덩어리로 잡고도 하루까지만 짚어
 * 줄 수 있다. 다음으로 날짜 바로 뒤를 본다. 사이에는 공백·쉼표와 요일(`(목)`·`목요일`), 추출기가 남긴 `일`만 끼어도 된다.
 *
 * 읽는 꼴은 `10시`·`10시00분`·`10시 30분`·`2시 반`·`17:58`이고 앞에 `오전`·`오후`가 붙어도 된다.
 * **오전·오후 없이 1시~7시면 오후로 본다.** 의원 진료 시간에 새벽 진료가 없다.
 */
internal fun timeNear(text: String, start: Int, end: Int): LocalTime? {
    if (start < 0 || end > text.length || start >= end) return null
    val inside = TIME.find(text.substring(start, end))
    val after = TIME_AFTER.find(text.substring(end).take(TIME_WINDOW))
    return (inside ?: after)?.let(::toLocalTime)
}

/**
 * 글에서 처음 나온 시각. 날짜와 붙어 있는지 보지 않는다.
 *
 * 예약 문자에는 쓰지 않는다. "변경은 오후 6시까지"의 6시가 진료 시각이 된다. 말로 "오후 3시로 해
 * 줘"라고 시간만 고치는 자리(#282)에서 쓴다. 그때는 날짜가 따로 없다.
 */
internal fun timeIn(text: String): LocalTime? = TIME.find(text)?.let(::toLocalTime)

private fun toLocalTime(match: MatchResult): LocalTime? {
    fun group(name: String): String = match.groups[name]?.value.orEmpty()
    val meridiem = group("meridiem")
    val hour = group("hour").toInt()
    val minuteText = group("minute")
    val half = group("half")
    val colonMinute = group("colon")
    val minute =
        when {
            colonMinute.isNotEmpty() -> colonMinute.toInt()
            minuteText.isNotEmpty() -> minuteText.toInt()
            half.isNotEmpty() -> HALF_HOUR
            else -> 0
        }
    val afternoon = meridiem in PM_WORDS || (meridiem.isEmpty() && hour in 1..LAST_PM_GUESS)
    val hour24 = if (afternoon && hour < NOON) hour + NOON else hour
    return if (hour24 in 0..LAST_HOUR && minute in 0..LAST_MINUTE) LocalTime.of(hour24, minute) else null
}

private const val TIME_PATTERN =
    """(?<meridiem>오전|오후|낮|저녁|밤)?\s*(?<!\d)(?<hour>\d{1,2})\s*""" +
        """(?:시\s*(?:(?<minute>\d{1,2})\s*분|(?<half>반))?|:(?<colon>\d{2}))"""

private val TIME = Regex(TIME_PATTERN)

/**
 * 날짜 바로 뒤. 공백·쉼표와 요일(`(목)`·`목요일`)만 건너뛴다.
 *
 * **날짜 자리 끝에 남은 `일`도 건너뛴다.** 추출기가 `10월19일`을 `10월19`까지만 잡는 일이 기기에서
 * 있었다. 그 `일` 한 글자가 날짜와 시각 사이를 막아 시각을 놓쳤다.
 */
private val TIME_AFTER =
    Regex("""^[일日]?[\s,]*(?:\([^)]{1,3}\)[\s,]*)?(?:[월화수목금토일]요일[\s,]*)?$TIME_PATTERN""")

private val PM_WORDS = setOf("오후", "저녁", "밤")

/** 날짜 뒤에서 시각을 찾는 글자 수. `(목) 오후 12시 30분`이 들어간다. */
private const val TIME_WINDOW = 20
private const val HALF_HOUR = 30
private const val NOON = 12
private const val LAST_PM_GUESS = 7
private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59

/** 날짜와 시각 사이에 끼어도 되는 글자 수. `(목) `가 다섯 자다. */
private const val MAX_GAP = 6
