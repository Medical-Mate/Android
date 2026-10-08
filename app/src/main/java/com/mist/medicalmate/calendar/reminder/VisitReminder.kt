package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentStatus
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
)

/**
 * 일정에서 걸 알림을 고른다.
 *
 * **진료 전날 저녁 8시에 뜬다.** 진료 시각을 기준으로 24시간 전에 걸면 시간 미정 일정은
 * 걸 자리가 없고, 오전 9시 진료면 전날 아침 9시라 하루를 다 보내고 나서 잊는다. 저녁에
 * 받으면 브리핑 카드를 챙기고 다음 날 나갈 준비를 할 수 있다.
 *
 * 이미 지난 시각은 뺀다. 저녁 8시가 지나서 내일 일정을 잡았으면 그 일정은 알림이 없다 —
 * 방금 직접 적은 것을 바로 알려 줄 이유가 없다.
 *
 * 예정인 것만 건다. 다녀왔거나 취소한 일정에 "내일 진료"가 오면 안 된다.
 */
internal fun planVisitReminders(appointments: List<Appointment>, now: LocalDateTime): List<VisitReminder> = appointments
    .filter { it.status == AppointmentStatus.SCHEDULED }
    .map { appointment ->
        VisitReminder(
            appointmentId = appointment.id,
            title = appointment.title,
            on = appointment.on,
            time = appointment.time,
            fireAt = appointment.on.minusDays(1).atTime(REMINDER_TIME),
        )
    }.filter { it.fireAt.isAfter(now) }

private const val REMINDER_HOUR = 20

internal val REMINDER_TIME: LocalTime = LocalTime.of(REMINDER_HOUR, 0)
