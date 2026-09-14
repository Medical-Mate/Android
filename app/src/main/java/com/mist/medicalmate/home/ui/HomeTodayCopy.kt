package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 오늘의 한 줄 문구.
 *
 * 갈래를 고르는 규칙은 `HomeRepository`에 있고 여기는 고른 갈래를 글로 옮기기만 한다. 둘을
 * 한 파일에 두면 규칙을 고칠 때마다 문구가 함께 흔들린다.
 */
internal fun HomeTodayLine.labelRes(): Int = when (this) {
    HomeTodayLine.FirstVisit -> R.string.home_today_first_label
    else -> R.string.home_today_label
}

@Composable
internal fun HomeTodayLine.title(): String = when (this) {
    HomeTodayLine.FirstVisit -> stringResource(R.string.home_today_first_title)
    is HomeTodayLine.TodayAhead -> stringResource(R.string.home_today_ahead_title)
    HomeTodayLine.TodayDone -> stringResource(R.string.home_today_done_title)
    HomeTodayLine.TodayRecorded -> stringResource(R.string.home_today_recorded_title)
    is HomeTodayLine.RecordMissing -> stringResource(R.string.home_today_missing_title, on.format(todayLineDate))
    is HomeTodayLine.NextTomorrow -> stringResource(R.string.home_today_tomorrow_title)
    is HomeTodayLine.NextInDays -> stringResource(R.string.home_today_next_title, days)
    HomeTodayLine.LastYesterday -> stringResource(R.string.home_today_yesterday_title)
    is HomeTodayLine.LastDaysAgo -> stringResource(R.string.home_today_since_title, days)
    HomeTodayLine.CardReady -> stringResource(R.string.home_today_card_title)
}

/**
 * 본문 두 줄.
 *
 * 첫 줄이 상황마다 다르다. 일정이 있는 갈래는 시각과 병원을 그대로 적고, 없으면 그 자리만
 * 비운다 — 둘 다 없으면 적을 것이 없어 줄 자체를 버린다.
 */
@Composable
internal fun HomeTodayLine.body(): List<String> = when (this) {
    HomeTodayLine.FirstVisit -> listOf(stringResource(R.string.home_today_first_body))

    is HomeTodayLine.TodayAhead ->
        listOfNotNull(atLine(time, clinic), stringResource(R.string.home_today_ahead_body))

    HomeTodayLine.TodayDone ->
        listOf(stringResource(R.string.home_today_done_lead), stringResource(R.string.home_today_done_body))

    HomeTodayLine.TodayRecorded ->
        listOf(stringResource(R.string.home_today_recorded_lead), stringResource(R.string.home_today_recorded_body))

    is HomeTodayLine.RecordMissing ->
        listOf(stringResource(R.string.home_today_missing_lead), stringResource(R.string.home_today_missing_body))

    is HomeTodayLine.NextTomorrow ->
        listOfNotNull(atLine(time, clinic), stringResource(R.string.home_today_tomorrow_body))

    is HomeTodayLine.NextInDays ->
        listOf(onLine(on, clinic), stringResource(R.string.home_today_next_body))

    HomeTodayLine.LastYesterday ->
        listOf(stringResource(R.string.home_today_yesterday_lead), stringResource(R.string.home_today_last_body))

    is HomeTodayLine.LastDaysAgo ->
        listOf(stringResource(R.string.home_today_since_lead), stringResource(R.string.home_today_last_body))

    HomeTodayLine.CardReady ->
        listOf(stringResource(R.string.home_today_card_lead), stringResource(R.string.home_today_card_body))
}

/** "오전 10:30 서울OO병원 내과예요." 시각이나 병원이 없으면 있는 것만 적는다. */
@Composable
private fun atLine(time: LocalTime?, clinic: String?): String? {
    val at = time?.format(todayLineTime)
    return when {
        at != null && clinic != null -> stringResource(R.string.home_today_at_clinic, at, clinic)
        at != null -> stringResource(R.string.home_today_at, at)
        clinic != null -> stringResource(R.string.home_today_at, clinic)
        else -> null
    }
}

/** "9월 16일 서울OO병원 내과예요." 병원이 없으면 "9월 16일 진료예요."다. */
@Composable
private fun onLine(on: LocalDate, clinic: String?): String {
    val date = on.format(todayLineDate)
    return if (clinic != null) {
        stringResource(R.string.home_today_on_clinic, date, clinic)
    } else {
        stringResource(R.string.home_today_on, date)
    }
}

/** 오늘의 한 줄이 쓰는 날짜. */
private val todayLineDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")

/** "오전 10:30". `a`를 기기 로케일로 두면 영어가 섞인다. */
private val todayLineTime: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
