package com.mist.medicalmate.card.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailViewModelTest {
    @Test
    fun `처음에는 불러오는 중이다`() {
        assertEquals(RecordDetailUiState.Loading, RecordDetailViewModel().uiState.value)
    }

    @Test
    fun `기록을 불러오면 타임라인이 시간순으로 나온다`() {
        val detail = content("card-1").detail

        assertEquals("복부 통증 · 3주", detail.title)
        // 최신 날짜가 위다
        assertEquals(
            listOf(
                "09.26 예정",
                "09.12 · 진료 후 기록",
                "09.04 작성 · 09.12 진료실에서 보여줌",
            ),
            detail.steps.map { it.at },
        )
    }

    @Test
    fun `모르는 id는 실패다`() {
        val viewModel = RecordDetailViewModel()

        viewModel.load("card-없음")

        assertEquals(RecordDetailUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `목록의 네 건이 모두 열린다`() {
        val ids = previewRecordGroups.flatMap { group -> group.items.map { it.id } }

        ids.forEach { id ->
            val viewModel = RecordDetailViewModel()
            viewModel.load(id)
            assertTrue(id, viewModel.uiState.value is RecordDetailUiState.Content)
        }
    }

    @Test
    fun `상세의 상태와 제목은 목록과 같다`() {
        val items = previewRecordGroups.flatMap { it.items }

        items.forEach { item ->
            val detail = content(item.id).detail
            assertEquals(item.id, item.title, detail.title)
            assertEquals(item.id, item.status, detail.status)
        }
    }

    @Test
    fun `카드가 있는 건에만 카드 열기가 붙는다`() {
        val withCard = content("card-1").detail.steps.filterIsInstance<RecordStep.Block>()
        val draft = content("card-3").detail.steps.filterIsInstance<RecordStep.Block>()

        assertEquals(
            listOf(RecordStepAction.Target.BRIEF_CARD),
            withCard.mapNotNull { it.action?.target },
        )
        assertTrue(draft.all { it.action == null })
    }

    @Test
    fun `진료 전 건은 첫 단계가 예정이다`() {
        val steps = content("card-2").detail.steps

        // 최신순이라 아직 오지 않은 일이 맨 위다
        assertTrue(steps.first() is RecordStep.Pending)
        assertEquals(1, steps.count { it is RecordStep.Pending })
    }

    @Test
    fun `타임라인에 증상 정리 단계를 넣지 않는다`() {
        val titles =
            previewRecordGroups
                .flatMap { it.items }
                .flatMap { content(it.id).detail.steps.filterIsInstance<RecordStep.Block>() }
                .map { it.title }

        assertEquals(emptyList<String>(), titles.filter { it == "내가 입력한 증상" })
        assertEquals(setOf("브리핑 카드", "진료 후 기록"), titles.toSet())
    }

    @Test
    fun `작성 중인 건은 예정 한 단계뿐이다`() {
        val steps = content("card-3").detail.steps

        assertEquals(1, steps.size)
        assertTrue(steps.single() is RecordStep.Pending)
    }

    @Test
    fun `재방문이 없는 건은 예정 단계가 없다`() {
        val steps = content("card-0").detail.steps

        assertNull(steps.firstOrNull { it is RecordStep.Pending })
    }

    @Test
    fun `알러지는 경고색으로 나온다`() {
        val items = content("card-1").detail.steps.filterIsInstance<RecordStep.Block>().flatMap { it.items }
        val allergy = items.first { it.key == "알러지" }

        assertEquals(RecordDetailItem.Tone.WARNING, allergy.tone)
    }

    @Test
    fun `다시 불러와도 같은 기록이 나온다`() {
        val viewModel = RecordDetailViewModel()

        viewModel.load("card-1")
        val first = viewModel.uiState.value
        viewModel.load("card-1")

        assertEquals(first, viewModel.uiState.value)
    }

    private fun content(recordId: String): RecordDetailUiState.Content {
        val viewModel = RecordDetailViewModel()
        viewModel.load(recordId)
        return viewModel.uiState.value as RecordDetailUiState.Content
    }
}
