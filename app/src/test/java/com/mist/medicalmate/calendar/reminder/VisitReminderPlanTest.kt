package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 어떤 일정에 언제 알림을 걸지. */
class VisitReminderPlanTest {
    @Test
    fun `진료 전날 저녁 8시에 뜬다`() {
        val plan = planVisitReminders(listOf(appointment(time = LocalTime.of(10, 30))), NOW)

        assertEquals(LocalDateTime.of(2026, 10, 14, 20, 0), plan.single().fireAt)
    }

    @Test
    fun `진료 시각과 상관없이 같은 때다`() {
        // 아침 진료도 전날 아침이 아니라 전날 저녁이다.
        val early = planVisitReminders(listOf(appointment(time = LocalTime.of(8, 0))), NOW)
        val late = planVisitReminders(listOf(appointment(time = LocalTime.of(17, 0))), NOW)

        assertEquals(early.single().fireAt, late.single().fireAt)
    }

    @Test
    fun `시간 미정 일정에도 건다`() {
        val plan = planVisitReminders(listOf(appointment(time = null)), NOW)

        assertEquals(LocalDateTime.of(2026, 10, 14, 20, 0), plan.single().fireAt)
        assertNull(plan.single().time)
    }

    @Test
    fun `전날 저녁 8시가 이미 지났으면 걸지 않는다`() {
        val now = LocalDateTime.of(2026, 10, 14, 21, 0)

        val plan = planVisitReminders(listOf(appointment(on = LocalDate.of(2026, 10, 15))), now)

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `오늘 일정에는 걸지 않는다`() {
        val now = LocalDateTime.of(2026, 10, 15, 7, 0)

        val plan = planVisitReminders(listOf(appointment(on = LocalDate.of(2026, 10, 15))), now)

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
                NOW,
            )

        assertEquals(listOf(3L), plan.map { it.appointmentId })
    }

    @Test
    fun `같은 날 일정이 둘이면 둘 다 건다`() {
        val plan = planVisitReminders(listOf(appointment(id = 1), appointment(id = 2)), NOW)

        assertEquals(listOf(1L, 2L), plan.map { it.appointmentId })
    }

    @Test
    fun `알림에 실을 이름과 시각을 들고 간다`() {
        val plan =
            planVisitReminders(listOf(appointment(title = "서울OO병원 내과 재진", time = LocalTime.of(10, 30))), NOW)

        assertEquals("서울OO병원 내과 재진", plan.single().title)
        assertEquals(LocalTime.of(10, 30), plan.single().time)
    }

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
