package com.mist.medicalmate.card.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.VisitListItem
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
class RecordViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `불러오기 전에는 편집으로 들어갈 수 없다`() {
        val viewModel = RecordViewModel(FakeVisitRepository())

        viewModel.onEditStart()

        assertEquals(RecordUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `응답을 달로 묶는다`() {
        // 서버가 최근 진료일 순으로 준다. 묶기만 하고 차례는 그대로 둔다.
        val content = loaded().content()

        assertEquals(listOf("2026년 9월", "2026년 7월"), content.groups.map { it.monthLabel })
        assertEquals(
            listOf(listOf("13", "12", "11"), listOf("10")),
            content.groups.map { group -> group.items.map { it.id } },
        )
    }

    @Test
    fun `줄에는 진료일과 병원이 적힌다`() {
        val rows = loaded().content().groups.flatMap { it.items }

        assertEquals("복부 통증 · 3주", rows[2].title)
        assertEquals("09.12 진료 · 서울OO병원 내과", rows[2].meta)
        assertEquals(RecordItem.Status.CONFIRMED, rows[2].status)
    }

    @Test
    fun `병원이 없으면 날짜만 적는다`() {
        val rows = loaded().content().groups.flatMap { it.items }

        assertEquals("09.20 진료", rows[0].meta)
    }

    @Test
    fun `읽지 못하면 실패다`() {
        val viewModel = RecordViewModel(FakeVisitRepository(list = FakeVisitRepository.OFFLINE))

        viewModel.load()

        assertEquals(RecordUiState.Failed, viewModel.uiState.value)
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
        viewModel.onSelectChange("11", true)

        viewModel.onEditCancel()

        assertFalse(viewModel.content().editing)
        assertNull(viewModel.content().selectedIds)
    }

    @Test
    fun `취소해도 목록은 그대로다`() {
        val viewModel = loaded()
        val before = viewModel.content().groups
        viewModel.onEditStart()
        viewModel.onSelectChange("11", true)

        viewModel.onEditCancel()

        assertEquals(before, viewModel.content().groups)
    }

    @Test
    fun `같은 줄을 두 번 고르면 켜고 꺼진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onSelectChange("11", true)
        assertEquals(setOf("11"), viewModel.content().selectedIds)

        viewModel.onSelectChange("11", false)
        assertEquals(emptySet<String>(), viewModel.content().selectedIds)
    }

    @Test
    fun `편집이 아닐 때 고르기는 아무 일도 하지 않는다`() {
        val viewModel = loaded()

        viewModel.onSelectChange("11", true)

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
        viewModel.onSelectChange("11", true)

        viewModel.onDeleteClick()

        assertTrue(viewModel.content().deleteRequested)
    }

    @Test
    fun `확인을 닫으면 고른 것은 남는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("11", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteDismiss()

        assertFalse(viewModel.content().deleteRequested)
        assertEquals(setOf("11"), viewModel.content().selectedIds)
    }

    @Test
    fun `지우면 그 줄만 사라지고 편집에서 나온다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("11", true)
        viewModel.onSelectChange("13", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("12", "10"), ids)
        assertFalse(viewModel.content().editing)
        assertFalse(viewModel.content().deleteRequested)
    }

    @Test
    fun `묶음이 비면 그 달도 사라진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("10", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf("2026년 9월"), viewModel.content().groups.map { it.monthLabel })
    }

    @Test
    fun `기록을 지우면 기록이 지워진다`() {
        // 카드가 아니라 기록이다(#178). 전에는 카드 삭제로 나가서 문답까지 사라졌다.
        val repository = FakeVisitRepository(list = ApiResult.Success(VISITS))
        val viewModel = RecordViewModel(repository).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("11", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf("11"), repository.deletedIds)
    }

    @Test
    fun `안 지워진 기록은 목록에 남는다`() {
        val repository = FakeVisitRepository(list = ApiResult.Success(VISITS)).apply { deleteFails = setOf("13") }
        val viewModel = RecordViewModel(repository).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("13", true)
        viewModel.onSelectChange("11", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("13", "12", "10"), ids)
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

    private fun loaded() = RecordViewModel(FakeVisitRepository(list = ApiResult.Success(VISITS))).apply { load() }

    private fun RecordViewModel.content(): RecordUiState.Content = uiState.value as RecordUiState.Content

    private companion object {
        val VISITS =
            listOf(
                VisitListItem(
                    id = "13",
                    cardId = 103,
                    cardTitle = "무릎 통증",
                    clinic = null,
                    visitedOn = LocalDate.of(2026, 9, 20),
                ),
                VisitListItem(
                    id = "12",
                    cardId = 102,
                    cardTitle = "두통 · 잦은 어지러움",
                    clinic = "OO내과",
                    visitedOn = LocalDate.of(2026, 9, 15),
                ),
                VisitListItem(
                    id = "11",
                    cardId = 101,
                    cardTitle = "복부 통증 · 3주",
                    clinic = "서울OO병원 내과",
                    visitedOn = LocalDate.of(2026, 9, 12),
                ),
                VisitListItem(
                    id = "10",
                    cardId = 100,
                    cardTitle = "목 통증 · 삼킬 때 아픔",
                    clinic = "OO이비인후과",
                    visitedOn = LocalDate.of(2026, 7, 18),
                ),
            )
    }
}
