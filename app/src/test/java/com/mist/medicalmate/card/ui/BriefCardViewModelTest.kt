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
    fun `편집을 열면 카드가 사본으로 들어온다`() {
        val viewModel = editing()

        val content = viewModel.content()
        assertTrue(content.editing)
        assertEquals(BriefCardDraft.of(content.card), content.draft)
    }

    @Test
    fun `열자마자는 바뀐 것이 없다`() {
        // Nav 우측이 `취소`로 남아야 한다. 아무것도 안 건드렸는데 `확인`이 뜨면 안 된다.
        assertFalse(editing().content().changed)
    }

    @Test
    fun `값을 고치면 바뀐 것이 있다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(0, "복부 (배꼽 아래)")

        assertTrue(viewModel.content().changed)
    }

    @Test
    fun `확인하면 사본이 카드로 옮겨지고 편집이 닫힌다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(0, "복부 (배꼽 아래)")
        viewModel.onEditDoneClick()

        val content = viewModel.content()
        assertFalse(content.editing)
        assertEquals("복부 (배꼽 아래)", content.card.items.first().value)
    }

    @Test
    fun `취소하면 고치던 값이 사라지고 원래 값이 남는다`() {
        val viewModel = editing()
        val before = viewModel.content().card.items.first().value

        viewModel.editActions.onItemValueChange(0, "엉뚱한 값")
        viewModel.onCancelClick()

        val content = viewModel.content()
        assertFalse(content.editing)
        assertEquals(before, content.card.items.first().value)
    }

    @Test
    fun `항목을 지우면 사본에서만 빠진다`() {
        val viewModel = editing()
        val before = viewModel.content().card.items

        viewModel.editActions.onItemDeleteClick(1)

        val content = viewModel.content()
        assertEquals(before.size - 1, content.items.size)
        assertEquals(before.size, content.card.items.size)
        assertFalse(content.items.any { it.key == before[1].key })
    }

    @Test
    fun `지운 뒤 확인하면 카드에서도 빠진다`() {
        val viewModel = editing()
        val before = viewModel.content().card.items

        viewModel.editActions.onItemDeleteClick(1)
        viewModel.onEditDoneClick()

        assertEquals(before.size - 1, viewModel.content().card.items.size)
    }

    @Test
    fun `질문을 더하면 빈 질문이 목록 끝에 붙는다`() {
        val viewModel = editing()
        val before = viewModel.content().questions.size

        viewModel.editActions.onQuestionAddClick()

        val questions = viewModel.content().questions
        assertEquals(before + 1, questions.size)
        assertEquals("", questions.last())
    }

    @Test
    fun `질문을 지우면 뒤 질문의 번호가 밀린다`() {
        val viewModel = editing()
        val third = viewModel.content().questions[2]

        viewModel.editActions.onQuestionDeleteClick(1)

        // 번호는 목록 순서가 정한다. 3번이던 질문이 2번이 된다.
        assertEquals(third, viewModel.content().questions[1])
    }

    @Test
    fun `편집 모드가 아니면 사본 조작이 아무것도 바꾸지 않는다`() {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        val before = viewModel.uiState.value

        viewModel.editActions.onItemValueChange(0, "값")
        viewModel.editActions.onQuestionAddClick()

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `없는 자리를 고치거나 지워도 그대로다`() {
        val viewModel = editing()
        val before = viewModel.uiState.value

        viewModel.editActions.onItemValueChange(99, "값")
        viewModel.editActions.onItemDeleteClick(99)
        viewModel.editActions.onQuestionChange(99, "값")
        viewModel.editActions.onQuestionDeleteClick(99)

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `삭제는 확인을 먼저 묻는다`() {
        val viewModel = editing()

        viewModel.onDeleteClick()

        assertTrue(viewModel.content().deleteRequested)
    }

    @Test
    fun `삭제를 취소하면 편집 모드가 남는다`() {
        val viewModel = editing()
        viewModel.onDeleteClick()

        viewModel.onDeleteDismiss()

        val content = viewModel.content()
        assertFalse(content.deleteRequested)
        assertTrue(content.editing)
    }

    @Test
    fun `읽기 상태에서도 카드에 진료받을 병원이 있다`() {
        assertEquals("서울OO병원 내과", loadedContent().card.hospital?.name)
    }

    private fun editing(): BriefCardViewModel {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        viewModel.onEditClick()
        return viewModel
    }

    private fun BriefCardViewModel.content(): BriefCardUiState.Content = uiState.value as BriefCardUiState.Content

    private fun loadedContent(): BriefCardUiState.Content {
        val viewModel = BriefCardViewModel()
        viewModel.load()
        return viewModel.uiState.value as BriefCardUiState.Content
    }
}
