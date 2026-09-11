package com.mist.medicalmate.card.ui
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BriefCardListViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loaded(): BriefCardListViewModel =
        BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(testItems))).apply {
            load()
        }

    private fun BriefCardListViewModel.content(): BriefCardListUiState.Content =
        uiState.value as BriefCardListUiState.Content

    @Test
    fun `불러오기 전에는 편집으로 들어갈 수 없다`() {
        val viewModel = BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(testItems)))

        viewModel.onEditStart()

        assertEquals(BriefCardListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `처음에는 카드가 두 달로 묶여 있다`() {
        val content = loaded().content()

        assertEquals(listOf("2026년 9월", "2026년 8월"), content.groups.map { it.monthLabel })
        assertEquals(listOf(2, 1), content.groups.map { it.items.size })
    }

    @Test
    fun `처음에는 편집이 아니다`() {
        val content = loaded().content()

        assertFalse(content.editing)
        assertNull(content.selectedIds)
        assertEquals(0, content.selectedCount)
    }

    @Test
    fun `편집으로 들어가면 아무것도 고르지 않은 채 시작한다`() {
        val viewModel = loaded()

        viewModel.onEditStart()

        assertTrue(viewModel.content().editing)
        assertEquals(emptySet<String>(), viewModel.content().selectedIds)
    }

    @Test
    fun `고르고 취소하면 고른 것이 사라진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("card-1", true)

        viewModel.onEditCancel()

        assertFalse(viewModel.content().editing)
        assertNull(viewModel.content().selectedIds)
    }

    @Test
    fun `취소해도 목록은 그대로다`() {
        val viewModel = loaded()
        val before = viewModel.content().groups
        viewModel.onEditStart()
        viewModel.onSelectChange("card-1", true)

        viewModel.onEditCancel()

        assertEquals(before, viewModel.content().groups)
    }

    @Test
    fun `같은 줄을 두 번 고르면 켜고 꺼진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onSelectChange("card-1", true)
        assertEquals(setOf("card-1"), viewModel.content().selectedIds)

        viewModel.onSelectChange("card-1", false)
        assertEquals(emptySet<String>(), viewModel.content().selectedIds)
    }

    @Test
    fun `편집이 아닐 때 고르기는 아무 일도 하지 않는다`() {
        val viewModel = loaded()

        viewModel.onSelectChange("card-1", true)

        assertNull(viewModel.content().selectedIds)
    }

    @Test
    fun `하나도 고르지 않으면 확인을 묻지 않는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onDeleteClick()

        assertFalse(viewModel.content().deleteRequested)
    }

    @Test
    fun `고른 뒤 삭제를 누르면 확인을 묻는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("card-1", true)

        viewModel.onDeleteClick()

        assertTrue(viewModel.content().deleteRequested)
    }

    @Test
    fun `확인을 닫으면 고른 것은 남는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("card-1", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteDismiss()

        assertFalse(viewModel.content().deleteRequested)
        assertEquals(setOf("card-1"), viewModel.content().selectedIds)
    }

    @Test
    fun `지우면 그 줄만 사라지고 편집에서 나온다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("card-1", true)
        viewModel.onSelectChange("card-3", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("card-2"), ids)
        assertFalse(viewModel.content().editing)
        assertFalse(viewModel.content().deleteRequested)
    }

    @Test
    fun `묶음이 비면 그 달도 사라진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("card-2", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf("2026년 9월"), viewModel.content().groups.map { it.monthLabel })
    }

    @Test
    fun `다 지우면 빈 상태가 된다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.content().groups.flatMap { it.items }.forEach { viewModel.onSelectChange(it.id, true) }
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(emptyList<RecordGroup>(), viewModel.content().groups)
    }
}

private val testItems =
    listOf(
        CardListItem(
            id = "card-1",
            title = "복부 통증 · 3주",
            confirmed = true,
            visited = true,
            clinic = "서울OO병원 내과",
            writtenOn = LocalDate.of(2026, 9, 4),
        ),
        CardListItem(
            id = "card-3",
            title = "무릎 통증",
            confirmed = false,
            visited = false,
            clinic = null,
            writtenOn = LocalDate.of(2026, 9, 20),
        ),
        CardListItem(
            id = "card-2",
            title = "두통 · 잦은 어지러움",
            confirmed = true,
            visited = false,
            clinic = null,
            writtenOn = LocalDate.of(2026, 8, 21),
        ),
    )
