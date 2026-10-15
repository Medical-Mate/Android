package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.core.model.ReminderSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 알림 하나.
 *
 * @param time 진료 시각. 없으면 시간 미정이고 알림 본문에서 빠진다.
 * @param fireAt 알림이 뜰 때. 기기 시간대의 지역 시각이다.
 */
internal data class VisitReminder(
    val appointmentId: Long,
    val title: String,
    val on: LocalDate,
    val time: LocalTime?,
    val fireAt: LocalDateTime,
    val kind: VisitReminderKind = VisitReminderKind.BEFORE_VISIT,
)

/** 진료 전 알림과 진료 후 기록 알림(#316). */
internal enum class VisitReminderKind { BEFORE_VISIT, AFTER_VISIT }

/**
 * 일정에서 걸 알림을 고른다(#316, Backend#176).
 *
 * | | 시각 있는 일정 | 시간 미정 |
 * | -- | -- | -- |
 * | 진료 전 | 진료 24시간 전 | 전날 [ReminderSettings.beforeVisitTime] |
 * | 진료 후 기록 | 진료 2시간 뒤 | 당일 [ReminderSettings.afterVisitTime] |
 *
 * 전에는 진료 전날 저녁 8시 하나뿐이었다. 시각을 정한 일정은 그 시각에 맞춰 알리고, 미정이면 사용자가 정한 시각에
 * 알린다. 꺼 둔 쪽은 걸지 않는다.
 *
 * 이미 지난 시각은 뺀다. 방금 잡은 일정을 바로 알려 줄 이유가 없다. 예정인 것만 건다 — 다녀왔거나 취소한 일정에
 * 알림이 오면 안 된다. 일정 하나에 알림이 둘이라 같은 일정이 두 번 나온다.
 */
internal fun planVisitReminders(
    appointments: List<Appointment>,
    settings: ReminderSettings,
    now: LocalDateTime,
): List<VisitReminder> = appointments
    .filter { it.status == AppointmentStatus.SCHEDULED }
    .flatMap { appointment ->
        listOfNotNull(
            appointment.reminder(VisitReminderKind.BEFORE_VISIT, beforeVisitAt(appointment, settings))
                .takeIf { settings.beforeVisit },
            appointment.reminder(VisitReminderKind.AFTER_VISIT, afterVisitAt(appointment, settings))
                .takeIf { settings.afterVisit },
        )
    }.filter { it.fireAt.isAfter(now) }

private fun beforeVisitAt(appointment: Appointment, settings: ReminderSettings): LocalDateTime =
    appointment.time?.let { appointment.on.atTime(it).minusHours(BEFORE_VISIT_HOURS) }
        ?: appointment.on.minusDays(1).atTime(settings.beforeVisitTime)

private fun afterVisitAt(appointment: Appointment, settings: ReminderSettings): LocalDateTime =
    appointment.time?.let { appointment.on.atTime(it).plusHours(AFTER_VISIT_HOURS) }
        ?: appointment.on.atTime(settings.afterVisitTime)

private fun Appointment.reminder(kind: VisitReminderKind, fireAt: LocalDateTime) =
    VisitReminder(appointmentId = id, title = title, on = on, time = time, fireAt = fireAt, kind = kind)

/** 시각을 정한 일정의 진료 전 알림. 24시간 전이다. */
private const val BEFORE_VISIT_HOURS = 24L

/** 시각을 정한 일정의 진료 후 기록 알림. 2시간 뒤다. */
private const val AFTER_VISIT_HOURS = 2L
