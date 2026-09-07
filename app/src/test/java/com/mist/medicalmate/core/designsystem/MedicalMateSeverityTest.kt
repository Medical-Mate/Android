package com.mist.medicalmate.core.designsystem

import com.mist.medicalmate.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 통증 5단계 정의를 고정한다.
 *
 * NRS 등가는 DESIGN.md에 없고 Figma의 Severity Readout 마스터(`333:1126`)에서 읽은
 * 값이다. 문서에 근거가 없는 값이라 코드에서만 지켜지므로 여기서 고정한다. 임상 척도가
 * 조용히 어긋나면 의사가 잘못된 강도를 읽는다.
 */
class MedicalMateSeverityTest {
    @Test
    fun `다섯 단계가 1부터 5까지다`() {
        assertEquals(listOf(1, 2, 3, 4, 5), MedicalMateSeverity.entries.map { it.level })
    }

    @Test
    fun `NRS 구간이 Figma 값과 같다`() {
        val actual = MedicalMateSeverity.entries.map { it.nrsFirst to it.nrsLast }
        val expected = listOf(1 to 2, 3 to 4, 5 to 6, 7 to 8, 9 to 10)
        assertEquals(expected, actual)
    }

    @Test
    fun `NRS 구간이 겹치지 않고 1부터 10을 빈틈없이 덮는다`() {
        val covered = MedicalMateSeverity.entries.flatMap { (it.nrsFirst..it.nrsLast).toList() }
        assertEquals((1..10).toList(), covered)
    }

    @Test
    fun `단계마다 낱말과 상황 설명이 따로 있다`() {
        val labels = MedicalMateSeverity.entries.map { it.labelRes }
        val descriptions = MedicalMateSeverity.entries.map { it.descriptionRes }
        // 같은 리소스를 두 자리에 쓰면 색 말고 단계를 알릴 수단이 하나로 줄어든다.
        assertEquals(5, labels.toSet().size)
        assertEquals(5, descriptions.toSet().size)
        assertEquals(emptySet<Int>(), labels.toSet() intersect descriptions.toSet())
    }

    @Test
    fun `문서 표의 리소스와 단계가 맞물린다`() {
        assertEquals(R.string.severity_1_label, MedicalMateSeverity.LEVEL_1.labelRes)
        assertEquals(R.string.severity_5_description, MedicalMateSeverity.LEVEL_5.descriptionRes)
    }

    @Test
    fun `ofLevel이 단계를 찾는다`() {
        assertEquals(MedicalMateSeverity.LEVEL_3, MedicalMateSeverity.ofLevel(3))
    }

    @Test
    fun `범위를 벗어난 단계는 예외다`() {
        // 저장된 값이 깨졌을 때 조용히 1단계로 떨어지면 통증이 축소되어 전달된다.
        assertThrows(IllegalStateException::class.java) { MedicalMateSeverity.ofLevel(0) }
        assertThrows(IllegalStateException::class.java) { MedicalMateSeverity.ofLevel(6) }
    }
}
