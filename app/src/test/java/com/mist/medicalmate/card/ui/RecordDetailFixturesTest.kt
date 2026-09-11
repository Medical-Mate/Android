package com.mist.medicalmate.card.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preview가 쓰는 상세 픽스처의 모양.
 *
 * 서버 연동 뒤에도 남긴다. `GET /api/visits/{id}`가 주는 것은 진료 후 기록 한 단계뿐이라
 * 시안의 카드 펼침(1j-3-X)과 재방문 누적(1j-3-R)을 화면에서 볼 수 있는 자리가 Preview뿐이다.
 * 그 두 상태를 그리는 규칙이 픽스처에 들어 있어서 여기서 지킨다.
 */
class RecordDetailFixturesTest {
    @Test
    fun `브리핑 카드 단계만 펼 수 있다`() {
        val steps = detail("card-1").steps.filterIsInstance<RecordStep.Block>()

        assertEquals(listOf("브리핑 카드"), steps.filter { it.card != null }.map { it.title })
        assertTrue(steps.filter { it.title != "브리핑 카드" }.all { it.card == null })
    }

    @Test
    fun `접혀 있을 때는 앞 세 줄만 보인다`() {
        val card = detail("card-1").steps
            .filterIsInstance<RecordStep.Block>()
            .first { it.card != null }

        assertEquals(5, card.items.size)
        assertEquals(3, card.card?.collapsedItemCount)
        assertEquals(listOf("부위", "기간", "양상"), card.items.take(3).map { it.key })
    }

    @Test
    fun `알러지는 카드를 펼쳤을 때 경고 블록으로 나온다`() {
        val card = detail("card-1").steps
            .filterIsInstance<RecordStep.Block>()
            .first { it.card != null }
            .card

        // 시안 1j-3-X가 알러지를 KV 줄이 아니라 노란 경고 블록으로 그린다. 줄로 두면
        // 다른 값과 같은 무게가 되고, 처방 전에 꼭 봐야 하는 값이 묻힌다.
        assertEquals(listOf("페니실린"), card?.allergies)
        assertTrue(card?.severity != null)
    }

    @Test
    fun `재방문까지 간 건은 배지와 병원 줄이 다르다`() {
        val detail = detail("card-4")

        assertEquals("진료 2회", detail.badge)
        assertEquals("서울OO병원 내과 · 09.12 초진 · 09.26 재방문", detail.clinicLine)
        assertEquals(
            listOf("진료 후 기록", "진료 후 기록", "브리핑 카드"),
            detail.steps.filterIsInstance<RecordStep.Block>().map { it.title },
        )
    }

    @Test
    fun `재방문 건에는 예정 단계가 없다`() {
        assertTrue(detail("card-4").steps.none { it is RecordStep.Pending })
    }

    @Test
    fun `진료 전 건은 첫 단계가 예정이다`() {
        val steps = detail("card-2").steps

        // 최신순이라 아직 오지 않은 일이 맨 위다
        assertTrue(steps.first() is RecordStep.Pending)
        assertEquals(1, steps.count { it is RecordStep.Pending })
    }

    @Test
    fun `작성 중인 건은 예정 한 단계뿐이다`() {
        val steps = detail("card-3").steps

        assertEquals(1, steps.size)
        assertTrue(steps.single() is RecordStep.Pending)
    }

    @Test
    fun `재방문이 없는 건은 예정 단계가 없다`() {
        assertNull(detail("card-0").steps.firstOrNull { it is RecordStep.Pending })
    }

    @Test
    fun `타임라인에 증상 정리 단계를 넣지 않는다`() {
        val titles =
            previewRecordGroups
                .flatMap { it.items }
                .flatMap { detail(it.id).steps.filterIsInstance<RecordStep.Block>() }
                .map { it.title }

        assertEquals(emptyList<String>(), titles.filter { it == "내가 입력한 증상" })
        assertEquals(setOf("브리핑 카드", "진료 후 기록"), titles.toSet())
    }

    @Test
    fun `목록의 네 건이 모두 픽스처에 있다`() {
        val ids = previewRecordGroups.flatMap { group -> group.items.map { it.id } }

        ids.forEach { id -> assertTrue(id, recordDetailFixtures.containsKey(id)) }
    }

    @Test
    fun `상세의 상태와 제목은 목록과 같다`() {
        previewRecordGroups.flatMap { it.items }.forEach { item ->
            val detail = detail(item.id)
            assertEquals(item.id, item.title, detail.title)
            assertEquals(item.id, item.status, detail.status)
        }
    }

    @Test
    fun `타임라인은 최신 날짜가 위다`() {
        assertEquals(
            listOf(
                "09.26 예정",
                "09.12 · 진료 후 기록",
                "09.04 작성 · 09.12 진료실에서 보여줌",
            ),
            detail("card-1").steps.map { it.at },
        )
    }

    private fun detail(recordId: String) = recordDetailFixtures.getValue(recordId)
}
