package com.mist.medicalmate.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Segmented Control의 칸 수 제한을 고정한다.
 *
 * 문서 8.2가 모바일 최대 4칸으로 정했다. 390 폭에서 5칸을 넣으면 각 칸의 글자가 잘리고,
 * 잘린 라벨은 어느 뷰로 가는지 알 수 없게 만든다.
 *
 * 칸이 하나면 전환할 곳이 없다. 그래서 최소도 2다.
 */
class SegmentedControlTest {
    @Test
    fun `모바일 최대 칸 수는 문서의 4다`() {
        assertEquals(4, maxSegments())
    }

    @Test
    fun `2에서 4칸까지 받는다`() {
        for (count in 2..4) {
            assertTrue("count=$count", isValidSegmentCount(count))
        }
    }

    @Test
    fun `1칸 이하와 5칸 이상은 거절한다`() {
        assertFalse(isValidSegmentCount(0))
        assertFalse(isValidSegmentCount(1))
        assertFalse(isValidSegmentCount(5))
    }
}
