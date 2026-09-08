package com.mist.medicalmate.home.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 일정 D-day 계산.
 *
 * 화면에서 계산하면 확인할 방법이 없어 [daysUntil]을 컴포저블 밖에 뒀다.
 */
class HomeScheduleTest {
    private val today = LocalDate.of(2026, 9, 8)

    @Test
    fun `오늘 일정은 D-0이다`() {
        assertEquals(0L, daysUntil(today, today))
    }

    @Test
    fun `나흘 뒤 일정은 D-4다`() {
        assertEquals(4L, daysUntil(today, LocalDate.of(2026, 9, 12)))
    }

    @Test
    fun `달을 넘겨도 실제 일수로 센다`() {
        assertEquals(23L, daysUntil(today, LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `지난 일정은 음수가 된다`() {
        assertEquals(-1L, daysUntil(today, LocalDate.of(2026, 9, 7)))
    }
}
