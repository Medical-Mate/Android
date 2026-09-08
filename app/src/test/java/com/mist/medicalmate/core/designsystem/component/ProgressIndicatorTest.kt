package com.mist.medicalmate.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Progress Indicator의 분기 규칙을 고정한다.
 *
 * 컴포저블 자체는 JVM에서 그릴 수 없지만, 칸을 나눌지 연속 막대로 바꿀지 정하는 기준과
 * 단계 범위 검사는 순수 계산이라 확인할 수 있다.
 *
 * `total`이 6을 넘으면 칸이 잘게 쪼개져 진행이 보이지 않는다는 것이 문서 8.4의 근거이고,
 * 그 경계가 조용히 바뀌면 8단계 문답에서 칸이 8개로 그려진다.
 */
class ProgressIndicatorTest {
    @Test
    fun `기본 단계 수는 문서의 4다`() {
        assertEquals(4, progressDefaultSteps())
    }

    @Test
    fun `6단계까지는 칸으로 나눈다`() {
        for (total in 1..6) {
            assertFalse("total=$total", shouldUseContinuousBar(total))
        }
    }

    @Test
    fun `6단계를 넘으면 연속 막대로 바꾼다`() {
        assertTrue(shouldUseContinuousBar(7))
        assertTrue(shouldUseContinuousBar(8))
    }

    @Test
    fun `현재 단계는 1부터 total까지만 받는다`() {
        assertTrue(isValidStep(current = 1, total = 4))
        assertTrue(isValidStep(current = 4, total = 4))
        // 0단계나 total 초과는 화면에 그릴 수 없는 상태다.
        assertFalse(isValidStep(current = 0, total = 4))
        assertFalse(isValidStep(current = 5, total = 4))
    }
}
