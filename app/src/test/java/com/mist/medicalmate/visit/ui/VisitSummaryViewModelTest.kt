package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VisitSummaryViewModelTest {
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
        assertEquals(VisitSummaryUiState.Loading, VisitSummaryViewModel(FakeVisitRepository()).uiState.value)
    }

    @Test
    fun `숫자가 아닌 id는 서버를 부르지 않고 실패다`() {
        val repository = FakeVisitRepository()

        val viewModel = VisitSummaryViewModel(repository)
        viewModel.load("visit-없음")

        assertEquals(VisitSummaryUiState.Failed, viewModel.uiState.value)
        assertNull(repository.requestedId)
    }

    @Test
    fun `상세를 못 읽으면 실패다`() {
        // 병원도 진료 내용도 그 응답에서 나온다. 목록만으로는 그릴 것이 없다.
        val viewModel = VisitSummaryViewModel(FakeVisitRepository(detail = FakeVisitRepository.OFFLINE))

        viewModel.load("77")

        assertEquals(VisitSummaryUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `이번 진료와 직전 진료가 나란히 온다`() {
        val content = content(repository())
        val compare = requireNotNull(content.compare)

        assertEquals(LocalDate.of(2026, 9, 12), compare.current.visitedOn)
        assertEquals("복부통 · 3주", compare.current.title)
        assertEquals(LocalDate.of(2026, 8, 21), compare.previous.visitedOn)
        assertEquals("두통 · 어지러움", compare.previous.title)
    }

    @Test
    fun `내용은 처방 결과 한 것 차례로 둘까지만 잇는다`() {
        // 카드 두 장이 한 줄에 나란히 놓여 폭이 화면 절반이다. 셋을 다 이으면 견주기 어려워진다.
        val compare = requireNotNull(content(repository()).compare)

        assertEquals("2주분 · 위염 초기", compare.current.detail)
        assertEquals("진통제 · 경과 관찰", compare.previous.detail)
    }

    @Test
    fun `적은 것이 없으면 내용 줄이 없다`() {
        val repository = repository(current = CURRENT.copy(prescription = null, result = null, whatWasDone = null))

        val compare = requireNotNull(content(repository).compare)

        assertNull(compare.current.detail)
    }

    @Test
    fun `첫 진료에는 견줄 것이 없다`() {
        val repository = FakeVisitRepository(
            list = ApiResult.Success(listOf(item(id = "77", day = 12, title = "복부통 · 3주"))),
            detail = ApiResult.Success(CURRENT),
        )

        val content = content(repository)

        assertNull(content.compare)
        assertEquals("서울OO병원 내과", content.hospital?.name)
    }

    @Test
    fun `목록을 못 읽으면 비교만 빠지고 병원은 남는다`() {
        // 견줄 것이 없어도 방금 남긴 기록은 보여줄 수 있다.
        val repository = FakeVisitRepository(list = FakeVisitRepository.OFFLINE, detail = ApiResult.Success(CURRENT))

        val content = content(repository)

        assertNull(content.compare)
        assertEquals("서울OO병원 내과", content.hospital?.name)
        assertEquals(LocalDate.of(2026, 9, 12), content.hospital?.visitedOn)
    }

    @Test
    fun `병원 이름이 없으면 병원 카드를 그리지 않는다`() {
        val repository = FakeVisitRepository(detail = ApiResult.Success(CURRENT.copy(clinic = null)))

        assertNull(content(repository).hospital)
    }

    @Test
    fun `주소와 재방문 날짜는 서버에 자리가 없어 비어 있다`() {
        val hospital = requireNotNull(content(repository()).hospital)

        assertNull(hospital.address)
        assertNull(hospital.revisitOn)
    }

    @Test
    fun `이번 것과 직전 것 두 번만 읽는다`() {
        val repository = repository()

        VisitSummaryViewModel(repository).load("77")

        assertEquals(listOf(77L, 55L), repository.requestedIds)
    }

    @Test
    fun `다시 시도하면 처음부터 읽는다`() {
        val repository = repository()
        val viewModel = VisitSummaryViewModel(repository)

        viewModel.load("77")
        viewModel.load("77")

        assertEquals(listOf(77L, 55L, 77L, 55L), repository.requestedIds)
        assertTrue(viewModel.uiState.value is VisitSummaryUiState.Content)
    }

    private fun content(repository: FakeVisitRepository): VisitSummaryUiState.Content {
        val viewModel = VisitSummaryViewModel(repository)
        viewModel.load("77")
        return viewModel.uiState.value as VisitSummaryUiState.Content
    }

    private fun repository(current: Visit = CURRENT) = FakeVisitRepository(
        // 서버가 최근 진료일 순으로 준다. 이번 것 바로 다음 줄이 직전 진료다.
        list =
        ApiResult.Success(
            listOf(
                item(id = "77", day = 12, title = "복부통 · 3주"),
                item(id = "55", day = 21, month = 8, title = "두통 · 어지러움"),
            ),
        ),
        details = mapOf(77L to ApiResult.Success(current), 55L to ApiResult.Success(PREVIOUS)),
    )

    private fun item(id: String, day: Int, title: String, month: Int = 9) = VisitListItem(
        id = id,
        cardId = 1L,
        cardTitle = title,
        clinic = "서울OO병원 내과",
        visitedOn = LocalDate.of(2026, month, day),
    )

    private companion object {
        val CURRENT =
            Visit(
                id = "77",
                cardId = 1L,
                clinic = "서울OO병원 내과",
                visitedOn = LocalDate.of(2026, 9, 12),
                whatWasDone = "혈액검사",
                result = "위염 초기",
                prescription = "2주분",
                rawNote = "위염 초기라고 하셨고",
            )

        val PREVIOUS =
            Visit(
                id = "55",
                cardId = 2L,
                clinic = "서울OO의원",
                visitedOn = LocalDate.of(2026, 8, 21),
                whatWasDone = null,
                result = "경과 관찰",
                prescription = "진통제",
                rawNote = null,
            )
    }
}
