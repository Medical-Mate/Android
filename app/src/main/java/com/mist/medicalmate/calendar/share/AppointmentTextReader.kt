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
 */
internal fun pickSchedule(mentions: List<DateTimeMention>, today: LocalDate): Pair<LocalDate?, LocalTime?> {
    val ahead = mentions.filter { !it.at.toLocalDate().isBefore(today) }.sortedBy { it.start }
    val dated = ahead.firstOrNull() ?: return null to null
    val time =
        if (dated.hasTime) {
            dated.at.toLocalTime()
        } else {
            ahead.firstOrNull { it.hasTime && it.start >= dated.end && it.start - dated.end <= MAX_GAP }
                ?.at
                ?.toLocalTime()
        }
    return dated.at.toLocalDate() to time
}

/** 날짜와 시각 사이에 끼어도 되는 글자 수. `(목) `가 다섯 자다. */
private const val MAX_GAP = 6
