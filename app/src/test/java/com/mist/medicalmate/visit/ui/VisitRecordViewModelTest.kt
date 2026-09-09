package com.mist.medicalmate.visit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitRecordViewModelTest {
    @Test
    fun `처음에는 불러오는 중이다`() {
        assertEquals(VisitRecordUiState.Loading, VisitRecordViewModel().uiState.value)
    }

    @Test
    fun `불러오면 네 항목과 원문 메모가 나온다`() {
        val content = loaded()

        assertEquals(listOf("소견", "검사", "약", "재방문"), content.record.items.map { it.key })
        assertEquals(PREVIEW_VISIT_NOTE, content.record.memo)
        assertFalse(content.editing)
    }

    @Test
    fun `재방문만 브랜드색으로 세운다`() {
        val links = loaded().record.items.filter { it.tone == VisitRecordItem.Tone.LINK }

        assertEquals(listOf("재방문"), links.map { it.key })
    }

    @Test
    fun `수정을 켜면 초안이 원본으로 채워진다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()

        val content = content(viewModel)
        assertTrue(content.editing)
        assertEquals(content.record.items.map { it.value }, content.drafts)
    }

    @Test
    fun `초안을 고쳐도 저장 전에는 원본이 그대로다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onDraftChange(0, "위염 확진")

        val content = content(viewModel)
        assertEquals("위염 확진", content.drafts[0])
        assertEquals("위염 초기 소견", content.record.items[0].value)
    }

    @Test
    fun `저장하면 초안이 원본으로 옮겨지고 읽기로 돌아온다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onDraftChange(0, "위염 확진")
        viewModel.onSaveClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("위염 확진", content.record.items[0].value)
        assertEquals(emptyList<String>(), content.drafts)
    }

    @Test
    fun `취소하면 고친 것이 사라진다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onDraftChange(1, "혈액검사 안 함")
        viewModel.onCancelClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("혈액검사 시행\n결과는 다음 방문 때 확인", content.record.items[1].value)
    }

    @Test
    fun `범위를 벗어난 초안은 무시한다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onDraftChange(9, "없는 행")

        assertEquals(content(viewModel).record.items.map { it.value }, content(viewModel).drafts)
    }

    @Test
    fun `읽는 중에 저장을 눌러도 내용이 바뀌지 않는다`() {
        val viewModel = viewModel()
        val before = content(viewModel)

        viewModel.onSaveClick()

        assertEquals(before, content(viewModel))
    }

    @Test
    fun `일정 등록을 켜고 끈다`() {
        val viewModel = viewModel()

        viewModel.onScheduleChange(true)
        assertTrue(content(viewModel).scheduleRevisit)

        viewModel.onScheduleChange(false)
        assertFalse(content(viewModel).scheduleRevisit)
    }

    @Test
    fun `불러오기 전에는 조작이 아무 일도 하지 않는다`() {
        val viewModel = VisitRecordViewModel()

        viewModel.onEditClick()
        viewModel.onScheduleChange(true)

        assertEquals(VisitRecordUiState.Loading, viewModel.uiState.value)
    }

    private fun viewModel() = VisitRecordViewModel().apply { load() }

    private fun loaded() = content(viewModel())

    private fun content(viewModel: VisitRecordViewModel) = viewModel.uiState.value as VisitRecordUiState.Content
}
