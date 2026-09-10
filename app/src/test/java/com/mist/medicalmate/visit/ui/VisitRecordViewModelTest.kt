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
    fun `편집을 켜면 기록이 사본으로 들어온다`() {
        val viewModel = editing()

        val content = content(viewModel)
        assertTrue(content.editing)
        assertEquals(VisitRecordDraft.of(content.record), content.draft)
    }

    @Test
    fun `열자마자는 바뀐 것이 없다`() {
        // Nav 우측이 `취소`로 남아야 한다.
        assertFalse(content(editing()).changed)
    }

    @Test
    fun `값을 고치면 바뀐 것이 있고 원본은 그대로다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(0, "위염 확진")

        val content = content(viewModel)
        assertTrue(content.changed)
        assertEquals("위염 확진", content.items[0].value)
        assertEquals("위염 초기 소견", content.record.items[0].value)
    }

    @Test
    fun `확인하면 사본이 원본으로 옮겨지고 읽기로 돌아온다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(0, "위염 확진")
        viewModel.onEditDoneClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("위염 확진", content.record.items[0].value)
    }

    @Test
    fun `취소하면 고친 것이 사라진다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(1, "혈액검사 안 함")
        viewModel.onCancelClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("혈액검사 시행\n결과는 다음 방문 때 확인", content.record.items[1].value)
    }

    @Test
    fun `항목을 지우면 사본에서만 빠진다`() {
        val viewModel = editing()
        val before = content(viewModel).record.items

        viewModel.editActions.onItemDeleteClick(1)

        val content = content(viewModel)
        assertEquals(before.size - 1, content.items.size)
        assertEquals(before.size, content.record.items.size)
        assertFalse(content.items.any { it.key == before[1].key })
    }

    @Test
    fun `지운 뒤 확인하면 원본에서도 빠진다`() {
        val viewModel = editing()
        val before = content(viewModel).record.items

        viewModel.editActions.onItemDeleteClick(1)
        viewModel.onEditDoneClick()

        assertEquals(before.size - 1, content(viewModel).record.items.size)
    }

    @Test
    fun `원문 메모는 편집해도 남는다`() {
        // 문서가 지울 수 없는 것 목록에 넣었다. AI 정리는 고치되 환자가 적은 말은 남는다.
        val viewModel = editing()

        viewModel.editActions.onItemDeleteClick(0)
        viewModel.onEditDoneClick()

        assertEquals(PREVIEW_VISIT_NOTE, content(viewModel).record.memo)
    }

    @Test
    fun `범위를 벗어난 조작은 무시한다`() {
        val viewModel = editing()
        val before = viewModel.uiState.value

        viewModel.editActions.onItemValueChange(9, "없는 행")
        viewModel.editActions.onItemDeleteClick(9)

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `편집 모드가 아니면 사본 조작이 아무것도 바꾸지 않는다`() {
        val viewModel = viewModel()
        val before = viewModel.uiState.value

        viewModel.editActions.onItemValueChange(0, "위염 확진")

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `삭제는 확인을 먼저 묻고 취소하면 편집 모드가 남는다`() {
        val viewModel = editing()

        viewModel.onDeleteClick()
        assertTrue(content(viewModel).deleteRequested)

        viewModel.onDeleteDismiss()
        val content = content(viewModel)
        assertFalse(content.deleteRequested)
        assertTrue(content.editing)
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
        viewModel.onDeleteClick()

        assertEquals(VisitRecordUiState.Loading, viewModel.uiState.value)
    }

    private fun viewModel() = VisitRecordViewModel().apply { load() }

    private fun editing() = viewModel().apply { onEditClick() }

    private fun loaded() = content(viewModel())

    private fun content(viewModel: VisitRecordViewModel) = viewModel.uiState.value as VisitRecordUiState.Content
}
