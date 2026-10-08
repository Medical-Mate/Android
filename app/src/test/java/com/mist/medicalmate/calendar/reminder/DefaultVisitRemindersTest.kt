package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.calendar.data.NewAppointment
import com.mist.medicalmate.core.model.VisitReminderSetting
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * 일정과 토글을 읽어 예약을 갈아끼우는 부분.
 *
 * 못 읽었을 때 손대지 않는지가 핵심이다. 연결이 끊긴 순간에 지우면 오프라인에서 연 앱이
 * 알림을 모두 날린다.
 */
class DefaultVisitRemindersTest {
    private val scheduler = RecordingScheduler()

    @Test
    fun `다가오는 일정으로 갈아끼운다`() = runTest {
        reminders(upcoming = ApiResult.Success(listOf(appointment(1), appointment(2)))).sync()

        assertEquals(listOf(1L, 2L), scheduler.replaced?.map { it.appointmentId })
    }

    @Test
    fun `일정이 없으면 비운다`() = runTest {
        // 마지막 일정을 지운 경우다. 걸려 있던 하나가 남으면 지운 일정의 알림이 온다.
        reminders(upcoming = ApiResult.Success(emptyList())).sync()

        assertEquals(emptyList<VisitReminder>(), scheduler.replaced)
    }

    @Test
    fun `토글이 꺼져 있으면 모두 지운다`() = runTest {
        reminders(enabled = ApiResult.Success(false)).sync()

        assertEquals(1, scheduler.cancels)
        assertNull(scheduler.replaced)
    }

    @Test
    fun `일정을 못 읽으면 걸린 것을 그대로 둔다`() = runTest {
        reminders(upcoming = OFFLINE).sync()

        assertNull(scheduler.replaced)
        assertEquals(0, scheduler.cancels)
    }

    @Test
    fun `토글을 못 읽으면 걸린 것을 그대로 둔다`() = runTest {
        // 끈 것인지 모르는 채로 지우지 않는다.
        reminders(enabled = OFFLINE).sync()

        assertNull(scheduler.replaced)
        assertEquals(0, scheduler.cancels)
    }

    @Test
    fun `지금 시각을 기준으로 지난 알림을 뺀다`() = runTest {
        // 15일 일정의 알림은 14일 20시다. 그 뒤에 맞추면 걸 것이 없다.
        val clock = Clock.fixed(LocalDateTime.of(2026, 10, 14, 21, 0).atZone(ZONE).toInstant(), ZONE)

        reminders(upcoming = ApiResult.Success(listOf(appointment(1))), clock = clock).sync()

        assertEquals(emptyList<VisitReminder>(), scheduler.replaced)
    }

    private fun reminders(
        enabled: ApiResult<Boolean> = ApiResult.Success(true),
        upcoming: ApiResult<List<Appointment>> = ApiResult.Success(emptyList()),
        clock: Clock = Clock.fixed(LocalDateTime.of(2026, 10, 8, 12, 0).atZone(ZONE).toInstant(), ZONE),
    ) = DefaultVisitReminders(
        appointments = { UpcomingOnly(upcoming) },
        setting = VisitReminderSetting { enabled },
        scheduler = scheduler,
        clock = clock,
    )

    private fun appointment(id: Long) = Appointment(
        id = id,
        title = "서울OO병원",
        on = LocalDate.of(2026, 10, 15),
        status = AppointmentStatus.SCHEDULED,
    )

    private class RecordingScheduler : ReminderScheduler {
        var replaced: List<VisitReminder>? = null
        var cancels = 0

        override fun replaceAll(reminders: List<VisitReminder>) {
            replaced = reminders
        }

        override fun cancelAll() {
            cancels++
        }
    }

    /** 다가오는 일정만 읽는다. 예약은 다른 호출을 부르지 않는다. */
    private class UpcomingOnly(private val upcoming: ApiResult<List<Appointment>>) : AppointmentRepository {
        override suspend fun upcoming() = upcoming

        override suspend fun month(month: YearMonth) = error("부르지 않는다")

        override suspend fun day(date: LocalDate) = error("부르지 않는다")

        override suspend fun create(appointment: NewAppointment) = error("부르지 않는다")

        override suspend fun update(id: Long, edit: AppointmentEdit) = error("부르지 않는다")

        override suspend fun delete(id: Long) = error("부르지 않는다")
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
