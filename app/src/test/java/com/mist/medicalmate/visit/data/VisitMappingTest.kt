package com.mist.medicalmate.visit.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 응답을 기록 값으로 옮기는 부분.
 *
 * 항목이 가변이라 축을 펴는 규칙이 여기 모여 있다. 재방문 줄에 날짜를 덧붙이는 것도 같은
 * 자리다 — 1q-1과 기록 상세가 같은 줄을 그린다.
 */
class VisitMappingTest {
    @Test
    fun `재방문 줄에 날짜가 붙는다`() {
        val visit = response(followUp = FollowUpResponse(date = "2026-09-27", text = "2주 뒤")).toVisit()

        assertEquals("2주 뒤 (9월 27일)", visit.items.single { it.axis == AXIS_FOLLOW_UP }.value)
    }

    @Test
    fun `범위로 말한 날짜에는 전후를 붙인다`() {
        // "2주 뒤"는 날짜가 아니라 범위다. 정확한 날짜처럼 그리면 그날이 아니면 안 되는 것으로
        // 읽힌다.
        val visit =
            response(
                followUp = FollowUpResponse(date = "2026-09-27", text = "2주 뒤", approximate = true),
            ).toVisit()

        assertEquals("2주 뒤 (9월 27일 전후)", visit.items.single { it.axis == AXIS_FOLLOW_UP }.value)
    }

    @Test
    fun `이미 날짜가 붙어 있으면 다시 붙이지 않는다`() {
        // 붙인 값을 그대로 저장한 기록이 남아 있다. 읽을 때 또 붙이면 날짜가 두 번 선다(#245).
        val visit =
            response(
                followUp = FollowUpResponse(date = "2026-09-27", approximate = true),
                followUpValue = "2주 뒤 (9월 27일 전후)",
            ).toVisit()

        assertEquals("2주 뒤 (9월 27일 전후)", visit.items.single { it.axis == AXIS_FOLLOW_UP }.value)
    }

    @Test
    fun `저장할 값에서는 붙인 날짜를 뗀다`() {
        val followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 27), approximate = true)

        assertEquals("2주 뒤", "2주 뒤 (9월 27일 전후)".withoutRevisitNote(followUp))
        // 안 붙은 값은 그대로다. 날짜를 못 뽑았으면 뗄 것도 없다.
        assertEquals("2주 뒤", "2주 뒤".withoutRevisitNote(followUp))
        assertEquals("2주 뒤 (9월 27일 전후)", "2주 뒤 (9월 27일 전후)".withoutRevisitNote(null))
    }

    @Test
    fun `날짜를 못 뽑았으면 환자가 말한 그대로 둔다`() {
        val visit = response().toVisit()

        assertEquals("2주 뒤", visit.items.single { it.axis == AXIS_FOLLOW_UP }.value)
    }

    @Test
    fun `다른 줄에는 날짜를 붙이지 않는다`() {
        val visit = response(followUp = FollowUpResponse(date = "2026-09-27")).toVisit()

        assertEquals("위염 초기", visit.items.single { it.axis == AXIS_FINDINGS }.value)
    }

    @Test
    fun `나눈 결과에도 같은 규칙을 쓴다`() {
        // 1q-1이 보는 값이다. 저장한 뒤 기록 상세가 보는 값과 같은 줄이어야 한다.
        val classified =
            ClassifyMemoResponse(
                axes = mapOf(AXIS_FOLLOW_UP to VisitAxisResponse(value = "2주 뒤")),
                followUp = FollowUpResponse(date = "2026-09-27", approximate = true),
            ).toClassification()

        assertEquals("2주 뒤 (9월 27일 전후)", classified.items.single().value)
    }

    private fun response(followUp: FollowUpResponse? = null, followUpValue: String = "2주 뒤") = VisitResponse(
        visitId = 1,
        axes =
        mapOf(
            AXIS_FINDINGS to VisitAxisResponse(value = "위염 초기"),
            AXIS_FOLLOW_UP to VisitAxisResponse(value = followUpValue),
        ),
        followUp = followUp,
    )
}
