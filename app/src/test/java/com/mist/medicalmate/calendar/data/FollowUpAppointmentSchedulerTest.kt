package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.calendar.ui.RecordingAppointmentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FollowUpAppointmentSchedulerTest {
    private val on = LocalDate.of(2026, 9, 22)

    @Test
    fun `재방문 날짜로 그 카드가 걸린 일정을 만든다`() = runTest {
        val repository = RecordingAppointmentRepository()

        FollowUpAppointmentScheduler(repository).schedule("서울OO병원 내과", on, cardId = 3)

        assertEquals(on, repository.createdOn)
        assertEquals("서울OO병원 내과", repository.createdClinic)
        assertEquals(listOf(3L), repository.createdCardIds)
        // 홈과 일자 화면이 이 값으로 "재진"을 적는다.
        assertEquals(AppointmentOrigin.VISIT_FOLLOW_UP, repository.createdOrigin)
        // 시각은 비운다. 예약하고 나서 일자 화면에서 채운다.
        assertNull(repository.createdTime)
    }

    @Test
    fun `같은 카드로 그 날 일정이 이미 있으면 만들지 않는다`() = runTest {
        // 기록을 다시 저장하면 같은 일정이 둘이 된다.
        val repository = RecordingAppointmentRepository()
        repository.dayAppointments = listOf(booked(cardId = 3))

        FollowUpAppointmentScheduler(repository).schedule("서울OO병원 내과", on, cardId = 3)

        assertEquals(0, repository.createCount)
    }

    @Test
    fun `다른 카드의 일정은 막지 않는다`() = runTest {
        val repository = RecordingAppointmentRepository()
        repository.dayAppointments = listOf(booked(cardId = 9))

        FollowUpAppointmentScheduler(repository).schedule("서울OO병원 내과", on, cardId = 3)

        assertEquals(1, repository.createCount)
    }

    @Test
    fun `취소한 일정은 있는 것으로 치지 않는다`() = runTest {
        // 취소는 안 간 것이라 그 자리에 다시 잡을 수 있어야 한다.
        val repository = RecordingAppointmentRepository()
        repository.dayAppointments = listOf(booked(cardId = 3).copy(status = AppointmentStatus.CANCELED))

        FollowUpAppointmentScheduler(repository).schedule("서울OO병원 내과", on, cardId = 3)

        assertEquals(1, repository.createCount)
    }

    @Test
    fun `병원이 없으면 만들지 않는다`() = runTest {
        // 서버가 일정에 병원을 요구한다.
        val repository = RecordingAppointmentRepository()

        FollowUpAppointmentScheduler(repository).schedule(null, on, cardId = 3)

        assertEquals(0, repository.createCount)
    }

    private fun booked(cardId: Long) = Appointment(
        id = 7,
        title = "서울OO병원 내과",
        on = on,
        status = AppointmentStatus.SCHEDULED,
        cards = listOf(AppointmentCard(id = cardId, title = null)),
    )
}
