package com.mist.medicalmate.card.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.Visit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 불러오는 중이다`() {
        assertEquals(RecordDetailUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `숫자가 아닌 id는 서버를 부르지 않고 실패다`() {
        // 목록에서만 들어오는 경로라 지금은 나지 않는다. 지워진 기록의 링크가 이 자리다.
        val repository = FakeVisitRepository()
        val viewModel = RecordDetailViewModel(repository)

        viewModel.load("card-없음")

        assertEquals(RecordDetailUiState.Failed, viewModel.uiState.value)
        assertNull(repository.requestedId)
    }

    @Test
    fun `불러오면 그 id로 읽는다`() {
        val repository = FakeVisitRepository()

        RecordDetailViewModel(repository).load("77")

        assertEquals(77L, repository.requestedId)
    }

    @Test
    fun `진료에서 들은 것이 한 단계로 나온다`() {
        val detail = content(FULL).detail

        assertEquals("서울OO병원 내과", detail.title)
        assertEquals("2026.09.12 · 서울OO병원 내과", detail.clinicLine)
        assertEquals(RecordItem.Status.CONFIRMED, detail.status)

        val step = detail.steps.single() as RecordStep.Block
        assertEquals("09.12 · 진료 후 기록", step.at)
        assertEquals("진료에서 들은 것", step.title)
        assertEquals(listOf("한 것", "결과", "처방"), step.items.map { it.key })
        assertEquals(listOf("혈액검사", "위염 초기", "2주분"), step.items.map { it.value })
    }

    @Test
    fun `원문은 인용으로 따로 남는다`() {
        // AI가 나눈 것과 환자가 말한 것을 가르는 자리다.
        val step = content(FULL).detail.steps.single() as RecordStep.Block

        assertEquals("내가 적은 그대로", step.quote?.label)
        assertEquals("배가 아파서 갔더니 위염이래요", step.quote?.text)
    }

    @Test
    fun `값이 없는 줄은 만들지 않는다`() {
        val step = content(FULL.copy(result = null, prescription = "  ")).detail.steps.single() as RecordStep.Block

        assertEquals(listOf("한 것"), step.items.map { it.key })
    }

    @Test
    fun `원문이 없으면 인용을 넣지 않는다`() {
        val step = content(FULL.copy(rawNote = null)).detail.steps.single() as RecordStep.Block

        assertNull(step.quote)
    }

    @Test
    fun `날짜가 없으면 병원만 적는다`() {
        val detail = content(FULL.copy(visitedOn = null)).detail

        assertEquals("서울OO병원 내과", detail.clinicLine)
        assertEquals(" · 진료 후 기록", (detail.steps.single() as RecordStep.Block).at)
    }

    @Test
    fun `읽지 못하면 실패다`() {
        val viewModel = RecordDetailViewModel(FakeVisitRepository(detail = FakeVisitRepository.OFFLINE))

        viewModel.load("77")

        assertEquals(RecordDetailUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `펼침은 자리마다 따로 켜고 끈다`() {
        val viewModel = viewModel()
        viewModel.load("77")

        viewModel.onExpandToggle(2)
        assertEquals(setOf(2), viewModel.expandedSteps.value)

        viewModel.onExpandToggle(0)
        assertEquals(setOf(0, 2), viewModel.expandedSteps.value)

        viewModel.onExpandToggle(2)
        assertEquals(setOf(0), viewModel.expandedSteps.value)
    }

    @Test
    fun `다른 건을 열면 펼친 것이 접힌다`() {
        val viewModel = viewModel()
        viewModel.load("77")
        viewModel.onExpandToggle(2)

        viewModel.load("78")

        assertEquals(emptySet<Int>(), viewModel.expandedSteps.value)
    }

    @Test
    fun `다시 불러와도 같은 기록이 나온다`() {
        val viewModel = viewModel()

        viewModel.load("77")
        val first = viewModel.uiState.value
        viewModel.load("77")

        assertEquals(first, viewModel.uiState.value)
    }

    private fun viewModel(visit: Visit? = null) = RecordDetailViewModel(
        FakeVisitRepository(detail = visit?.let { ApiResult.Success(it) } ?: ApiResult.Success(FULL)),
    )

    private fun content(visit: Visit): RecordDetailUiState.Content {
        val viewModel = viewModel(visit)
        viewModel.load("77")
        return viewModel.uiState.value as RecordDetailUiState.Content
    }

    private companion object {
        val FULL =
            Visit(
                id = "77",
                cardId = 3,
                clinic = "서울OO병원 내과",
                visitedOn = LocalDate.of(2026, 9, 12),
                whatWasDone = "혈액검사",
                result = "위염 초기",
                prescription = "2주분",
                rawNote = "배가 아파서 갔더니 위염이래요",
            )
    }
}
