package com.mist.medicalmate.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/** 남은 날 배지 글(#294). */
class DdayTest {
    @Test
    fun `오늘은 D-day이고 그 밖은 남은 날이다`() {
        assertEquals("D-day", ddayLabel(0))
        assertEquals("D-1", ddayLabel(1))
        assertEquals("D-14", ddayLabel(14))
    }
}
