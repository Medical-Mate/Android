package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.core.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 어떤 일정에 언제 알림을 걸지(#316, Backend#176). */
class VisitReminderPlanTest {
    @Test
    fun `시각을 정한 일정은 24시간 전과 2시간 뒤다`() {
        val plan = planVisitReminders(listOf(appointment(time = LocalTime.of(10, 30))), ReminderSettings(), NOW)

        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 30), plan.fireAt(VisitReminderKind.BEFORE_VISIT))
        assertEquals(LocalDateTime.of(2026, 10, 15, 12, 30), plan.fireAt(VisitReminderKind.AFTER_VISIT))
    }

    @Test
    fun `시간 미정이면 전날과 당일 정한 시각이다`() {
        val settings =
            ReminderSettings(beforeVisitTime = LocalTime.of(20, 30), afterVisitTime = LocalTime.of(21, 0))

        val plan = planVisitReminders(listOf(appointment(time = null)), settings, NOW)

        assertEquals(LocalDateTime.of(2026, 10, 14, 20, 30), plan.fireAt(VisitReminderKind.BEFORE_VISIT))
        assertEquals(LocalDateTime.of(2026, 10, 15, 21, 0), plan.fireAt(VisitReminderKind.AFTER_VISIT))
        assertNull(plan.first().time)
    }

    @Test
    fun `시간 미정의 기본 시각은 오후 7시다`() {
        val plan = planVisitReminders(listOf(appointment(time = null)), ReminderSettings(), NOW)

        assertEquals(LocalDateTime.of(2026, 10, 14, 19, 0), plan.fireAt(VisitReminderKind.BEFORE_VISIT))
        assertEquals(LocalDateTime.of(2026, 10, 15, 19, 0), plan.fireAt(VisitReminderKind.AFTER_VISIT))
    }

    @Test
    fun `꺼 둔 쪽은 걸지 않는다`() {
        val beforeOnly = planVisitReminders(listOf(appointment()), ReminderSettings(afterVisit = false), NOW)
        val afterOnly = planVisitReminders(listOf(appointment()), ReminderSettings(beforeVisit = false), NOW)

        assertEquals(listOf(VisitReminderKind.BEFORE_VISIT), beforeOnly.map { it.kind })
        assertEquals(listOf(VisitReminderKind.AFTER_VISIT), afterOnly.map { it.kind })
    }

    @Test
    fun `이미 지난 알림은 빼고 남은 것만 건다`() {
        // 진료 당일 아침이면 진료 전 알림(전날)은 지났고 진료 후 기록 알림만 남는다.
        val now = LocalDateTime.of(2026, 10, 15, 7, 0)

        val plan = planVisitReminders(listOf(appointment(time = LocalTime.of(10, 0))), ReminderSettings(), now)

        assertEquals(listOf(VisitReminderKind.AFTER_VISIT), plan.map { it.kind })
    }

    @Test
    fun `둘 다 지났으면 걸지 않는다`() {
        val now = LocalDateTime.of(2026, 10, 15, 13, 0)

        val plan = planVisitReminders(listOf(appointment(time = LocalTime.of(10, 0))), ReminderSettings(), now)

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `다녀왔거나 취소한 일정에는 걸지 않는다`() {
        val plan =
            planVisitReminders(
                listOf(
                    appointment(id = 1, status = AppointmentStatus.DONE),
                    appointment(id = 2, status = AppointmentStatus.CANCELED),
                    appointment(id = 3, status = AppointmentStatus.SCHEDULED),
                ),
                ReminderSettings(),
                NOW,
            )

        assertEquals(listOf(3L, 3L), plan.map { it.appointmentId })
    }

    @Test
    fun `알림에 실을 이름 · 날짜 · 시각을 들고 간다`() {
        val plan =
            planVisitReminders(
                listOf(appointment(title = "서울OO병원 내과 재진", time = LocalTime.of(10, 30))),
                ReminderSettings(beforeVisit = true, afterVisit = false),
                NOW,
            )

        val reminder = plan.single()
        assertEquals("서울OO병원 내과 재진", reminder.title)
        assertEquals(LocalDate.of(2026, 10, 15), reminder.on)
        assertEquals(LocalTime.of(10, 30), reminder.time)
    }

    private fun List<VisitReminder>.fireAt(kind: VisitReminderKind): LocalDateTime = single { it.kind == kind }.fireAt

    private fun appointment(
        id: Long = 1,
        title: String = "서울OO병원",
        on: LocalDate = LocalDate.of(2026, 10, 15),
        time: LocalTime? = LocalTime.of(10, 0),
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    ) = Appointment(id = id, title = title, on = on, time = time, status = status)

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
