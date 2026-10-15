package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import java.time.LocalTime

/*
 * 알림 시각을 적는 두 모양(#316). 시안 1s-1이 값 자리에는 "오후 7:00", 토글 설명에는 "전날 오후 7시"로 적는다.
 *
 * `DateTimeFormatter`의 `a`를 쓰지 않는다. 오전·오후가 기기 로케일에서 오고 리소스로 옮길 수 없다.
 */

/** "오후 7:00". 알림 시간 줄의 값과 1s-3의 안내에 쓴다. */
@Composable
internal fun reminderTimeValue(time: LocalTime): String =
    stringResource(R.string.reminder_time_value, meridiem(time), hour12(time), time.minute)

/** "오후 7시", 분이 있으면 "오후 7시 30분". 토글 설명에 쓴다. */
@Composable
internal fun reminderTimeWords(time: LocalTime): String = if (time.minute == 0) {
    stringResource(R.string.reminder_time_hour, meridiem(time), hour12(time))
} else {
    stringResource(R.string.reminder_time_hour_minute, meridiem(time), hour12(time), time.minute)
}

/** 휠의 오전·오후 두 낱말. */
@Composable
internal fun meridiemLabels(): List<String> =
    listOf(stringResource(R.string.reminder_time_am), stringResource(R.string.reminder_time_pm))

@Composable
private fun meridiem(time: LocalTime): String =
    stringResource(if (time.hour < NOON) R.string.reminder_time_am else R.string.reminder_time_pm)

/** 12시는 0이 아니라 12로 적는다. */
private fun hour12(time: LocalTime): Int = (time.hour % NOON).let { if (it == 0) NOON else it }

private const val NOON = 12
