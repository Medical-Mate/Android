package com.mist.medicalmate.calendar.reminder

import android.content.Intent
import java.time.LocalDate

/**
 * 진료 알림을 눌러 열린 일정(#268).
 *
 * 알림이 이 값을 인텐트에 싣고 `MainActivity`가 꺼내 그래프에 넘긴다. 그래프가 그 날 일자
 * 화면(1r-2)을 연다. 하루에 일정이 둘일 수 있어 날짜와 함께 어느 일정인지를 든다.
 */
internal data class VisitReminderLink(val date: LocalDate, val appointmentId: Long) {
    fun writeTo(intent: Intent): Intent = intent
        .putExtra(EXTRA_DATE, date.toString())
        .putExtra(EXTRA_APPOINTMENT_ID, appointmentId)

    companion object {
        private const val EXTRA_DATE = "com.mist.medicalmate.extra.VISIT_REMINDER_DATE"
        private const val EXTRA_APPOINTMENT_ID = "com.mist.medicalmate.extra.VISIT_REMINDER_APPOINTMENT_ID"

        /** 알림에서 온 인텐트가 아니거나 날짜를 못 읽으면 null이다. */
        fun from(intent: Intent?): VisitReminderLink? {
            if (intent == null || !intent.hasExtra(EXTRA_APPOINTMENT_ID)) return null
            val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            return date?.let { VisitReminderLink(it, intent.getLongExtra(EXTRA_APPOINTMENT_ID, 0L)) }
        }
    }
}
