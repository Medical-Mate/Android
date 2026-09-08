package com.mist.medicalmate.card.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BriefCardViewModelTest {
    @Test
    fun `처음은 Loading이고 불러오면 카드가 나온다`() {
        val viewModel = BriefCardViewModel()

        assertEquals(BriefCardUiState.Loading, viewModel.uiState.value)

        viewModel.load()

        assertTrue(viewModel.uiState.value is BriefCardUiState.Content)
    }

    @Test
    fun `AI가 세운 항목이 하나뿐이다`() {
        val card = loadedContent().card

        assertEquals(1, card.items.count { it.emphasized })
    }

    @Test
    fun `수정을 열면 모든 값이 초안으로 들어온다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()

        viewModel.onEditClick()

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertTrue(content.editing)
        assertEquals(content.card.items.map { it.value }, content.drafts)
    }

    @Test
    fun `저장하면 초안이 카드로 옮겨지고 수정이 닫힌다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        viewModel.onEditClick()

        viewModel.onDraftChange(0, "복부 (배꼽 아래)")
        viewModel.onSaveClick()

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertFalse(content.editing)
        assertEquals("복부 (배꼽 아래)", content.card.items.first().value)
        assertEquals(emptyList<String>(), content.drafts)
    }

    @Test
    fun `취소하면 고치던 값이 사라지고 원래 값이 남는다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        val before = loadedContent().card.items.first().value
        viewModel.onEditClick()

        viewModel.onDraftChange(0, "엉뚱한 값")
        viewModel.onCancelClick()

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertFalse(content.editing)
        assertEquals(before, content.card.items.first().value)
    }

    @Test
    fun `수정 중이 아니면 저장이 아무것도 바꾸지 않는다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        val before = viewModel.uiState.value

        viewModel.onSaveClick()

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `없는 자리를 고치려 해도 그대로다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        viewModel.onEditClick()
        val before = viewModel.uiState.value

        viewModel.onDraftChange(99, "값")

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `초안이 없는 자리는 원래 값을 읽는다`() {
        val content = loadedContent()

        assertEquals(content.card.items[2].value, content.draftAt(2))
    }

    private fun loadedContent(): BriefCardUiState.Content {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        return viewModel.uiState.value as BriefCardUiState.Content
    }
}
