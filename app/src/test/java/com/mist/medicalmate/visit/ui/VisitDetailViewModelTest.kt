package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitFollowUp
import com.mist.medicalmate.visit.data.VisitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VisitDetailViewModelTest {
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
        assertEquals(VisitRecordUiState.Loading, VisitDetailViewModel(FakeVisitRepository()).uiState.value)
    }

    @Test
    fun `저장한 기록을 id로 읽어 카드로 그린다`() {
        val repository = FakeVisitRepository(detail = ApiResult.Success(savedVisit))
        val viewModel = VisitDetailViewModel(repository).apply { load("42") }

        val content = viewModel.uiState.value as VisitRecordUiState.Content
        assertEquals(42L, repository.requestedId)
        assertEquals("서울OO병원 내과 · 2026.09.12", content.record.clinicLine)
        assertEquals(listOf("소견", "약", "재방문"), content.record.items.map { it.key })
        assertEquals(RAW_NOTE, content.record.memo)
        // 읽기 전용이다. 사본이 없어 편집 줄이 열리지 않는다.
        assertFalse(content.editing)
    }

    @Test
    fun `재방문 줄만 링크 톤이다`() {
        val viewModel = VisitDetailViewModel(FakeVisitRepository(detail = ApiResult.Success(savedVisit)))
        viewModel.load("42")

        val tones = (viewModel.uiState.value as VisitRecordUiState.Content).record.items.map { it.tone }
        assertEquals(
            listOf(VisitRecordItem.Tone.DEFAULT, VisitRecordItem.Tone.DEFAULT, VisitRecordItem.Tone.LINK),
            tones,
        )
    }

    @Test
    fun `나눈 개수 캡션은 붙이지 않는다`() {
        // "AI가 4가지로 나눴어요"는 나눈 그 자리(1q-1)의 말이다. 다시 보는 자리에서는 항목이 곧 결과다.
        val viewModel = VisitDetailViewModel(FakeVisitRepository(detail = ApiResult.Success(savedVisit)))
        viewModel.load("42")

        assertNull((viewModel.uiState.value as VisitRecordUiState.Content).record.classifiedCount)
    }

    @Test
    fun `읽지 못하면 실패다`() {
        val viewModel = VisitDetailViewModel(FakeVisitRepository(detail = FakeVisitRepository.OFFLINE))
        viewModel.load("42")

        assertEquals(VisitRecordUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `숫자가 아닌 id는 부르지 않고 실패다`() {
        val repository = FakeVisitRepository()
        VisitDetailViewModel(repository).load("card-1")

        assertNull(repository.requestedId)
    }

    private companion object {
        const val RAW_NOTE = "위염 초기라고 하셨고, 2주 약 먹고 다시 오라고 했어요."

        val savedVisit =
            Visit(
                id = "42",
                cardId = 3,
                clinic = "서울OO병원 내과",
                visitedOn = LocalDate.of(2026, 9, 12),
                items =
                listOf(
                    VisitItem(axis = "findings", label = "소견", value = "위염 초기"),
                    VisitItem(axis = "medication", label = "약", value = "2주분 처방"),
                    VisitItem(axis = "follow_up", label = "재방문", value = "2주 뒤 재방문 (9월 26일 전후)"),
                ),
                followUp = VisitFollowUp(date = LocalDate.of(2026, 9, 26), approximate = true),
                patientNotes = emptyList(),
                rawNote = RAW_NOTE,
            )
    }
}
