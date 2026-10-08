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

/**
 * 문자에 적힌 병원 이름.
 *
 * 예약 문자는 대개 머리에 `[서울OO병원]`처럼 보낸 곳을 적는다. 대괄호 안을 먼저 보고, 없으면
 * 본문에서 찾는다. `[Web발신]`·`[광고]`처럼 병원이 아닌 머리는 이름 꼴이 아니라 건너뛴다.
 *
 * **과 이름으로 끝나는 의원도 병원이다.** 동네 의원은 `튼튼정형외과`·`밝은안과`처럼 `의원`을
 * 떼고 부르는 일이 흔하다. 다만 과 이름만 홀로 선 것(`정형외과 진료 예약`)은 어느 병원인지가
 * 아니라 어느 과인지라 건너뛴다.
 *
 * 진료과가 바로 뒤에 붙어 있으면 함께 든다. 같은 병원의 다른 과는 다른 일정이다.
 */
internal fun hospitalIn(text: String): String? {
    val bracketed = BRACKET.findAll(text).map { it.groupValues[1] }
    return (bracketed + text).firstNotNullOfOrNull { part ->
        HOSPITAL.findAll(part)
            .firstOrNull { !BARE_DEPARTMENT.matches(it.groupValues[1]) }
            ?.value
            ?.trim()
    }
}

private val BRACKET = Regex("""[\[【]([^\]】]{2,40})[\]】]""")

/**
 * 진료과 이름. 병원 이름의 끝, 병원 뒤에 붙는 과, 홀로 선 과를 가려내는 데 함께 쓴다.
 *
 * `~의학과`를 `[가-힣]+의학과`로 묶지 않는다. 그러면 `튼튼재활의학과`가 통째로 과 이름이 되어
 * 홀로 선 과로 버려진다.
 */
private const val DEPARTMENT =
    "(?:정신건강의학과|재활의학과|가정의학과|영상의학과|마취통증의학과|비뇨의학과|응급의학과" +
        "|소아청소년과|이비인후과|산부인과|정형외과|신경외과|성형외과|흉부외과|비뇨기과" +
        "|소아과|청소년과|피부과|신경과|내과|외과|안과)"

/**
 * 병원 이름을 이루는 글자.
 *
 * 한글·영문·숫자에 전각 영문·숫자(`ＯＯ병원`)를 더한다. 가림 글자(`서울○○병원`·`서울**병원`)는
 * 이름 **안에서만** 받는다 — 첫 글자로 받으면 `*튼튼정형외과*`가 `*튼튼정형외과`로 잡힌다.
 * `&`·`·`·`-`는 받지 않는다. 이름 안보다 말과 말 사이에 더 자주 와서(`예약 안내·튼튼정형외과`)
 * 앞말까지 이름으로 붙는다.
 */
private const val NAME_HEAD = "[가-힣A-Za-z0-9Ａ-Ｚａ-ｚ０-９○◯]"
private const val NAME_BODY = "[가-힣A-Za-z0-9Ａ-Ｚａ-ｚ０-９○◯●◎*＊]"

private val HOSPITAL =
    Regex(
        "($NAME_HEAD$NAME_BODY*(?:병원|의원|치과|클리닉|의료원|보건소|메디컬센터|$DEPARTMENT))" +
            "(?:\\s[가-힣]{0,6}$DEPARTMENT)?",
    )

private val BARE_DEPARTMENT = Regex(DEPARTMENT)
