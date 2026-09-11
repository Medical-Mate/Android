package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.visit.data.FakeVisitRepository
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VisitRecordViewModelTest {
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
        assertEquals(VisitRecordUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `불러오면 네 자리가 비어 있고 원문이 그대로 있다`() {
        // 분류 엔진이 없다. 자리만 만들고 값은 환자가 채운다.
        val content = loaded()

        assertEquals(listOf("소견", "검사", "약", "재방문"), content.record.items.map { it.key })
        assertEquals(listOf("", "", "", ""), content.record.items.map { it.value })
        assertEquals(NOTE, content.record.memo)
        assertFalse(content.editing)
    }

    @Test
    fun `병원과 오늘 날짜가 머리줄이 된다`() {
        assertEquals("서울OO병원 내과 · 2026.09.12", loaded().record.clinicLine)
    }

    @Test
    fun `병원을 고르지 않았으면 날짜만 적는다`() {
        val viewModel = viewModel().apply { load(clinic = null, note = NOTE) }

        assertEquals("2026.09.12", content(viewModel).record.clinicLine)
    }

    @Test
    fun `나눈 가짓수를 알릴 것이 없으면 비어 있다`() {
        assertEquals("", loaded().record.caption)
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

        viewModel.editActions.onItemValueChange(0, "위염 초기 소견")

        val content = content(viewModel)
        assertTrue(content.changed)
        assertEquals("위염 초기 소견", content.items[0].value)
        assertEquals("", content.record.items[0].value)
    }

    @Test
    fun `확인하면 사본이 원본으로 옮겨지고 읽기로 돌아온다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(0, "위염 초기 소견")
        viewModel.onEditDoneClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("위염 초기 소견", content.record.items[0].value)
    }

    @Test
    fun `취소하면 고친 것이 사라진다`() {
        val viewModel = editing()

        viewModel.editActions.onItemValueChange(1, "혈액검사 시행")
        viewModel.onCancelClick()

        val content = content(viewModel)
        assertFalse(content.editing)
        assertEquals("", content.record.items[1].value)
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

        assertEquals(NOTE, content(viewModel).record.memo)
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
        val viewModel = viewModel().apply { load(CLINIC, NOTE) }
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
        val viewModel = viewModel().apply { load(CLINIC, NOTE) }

        viewModel.onScheduleChange(true)
        assertTrue(content(viewModel).scheduleRevisit)

        viewModel.onScheduleChange(false)
        assertFalse(content(viewModel).scheduleRevisit)
    }

    @Test
    fun `불러오기 전에는 조작이 아무 일도 하지 않는다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onScheduleChange(true)
        viewModel.onDeleteClick()

        assertEquals(VisitRecordUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `저장하면 채운 값이 서버의 세 필드로 간다`() {
        val repository = FakeVisitRepository()
        val viewModel = filled(repository)
        var saved: String? = null

        viewModel.onSaveClick(cardId = "3", onSaved = { saved = it })

        val request = repository.request
        assertEquals(3L, repository.cardId)
        assertEquals(CLINIC, request?.clinicName)
        assertEquals(TODAY, request?.visitedOn)
        assertEquals("혈액검사 시행", request?.whatWasDone)
        assertEquals("위염 초기 소견", request?.result)
        assertEquals("2주분 처방", request?.prescription)
        assertEquals(NOTE, request?.rawNote)
        assertEquals(FakeVisitRepository.SAVED_ID, saved)
    }

    @Test
    fun `비운 줄은 보내지 않는다`() {
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        val request = repository.request
        assertNull(request?.whatWasDone)
        assertNull(request?.result)
        assertNull(request?.prescription)
        assertEquals(NOTE, request?.rawNote)
    }

    @Test
    fun `재방문 줄은 보낼 자리가 없다`() {
        // POST /api/cards/{id}/visit에 재방문 날짜 필드가 없다. #148에 적어 뒀다.
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE) }
        viewModel.onEditClick()
        viewModel.editActions.onItemValueChange(3, "2주 뒤")
        viewModel.onEditDoneClick()

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        val request = repository.request
        assertFalse(listOfNotNull(request?.whatWasDone, request?.result, request?.prescription).contains("2주 뒤"))
    }

    @Test
    fun `카드가 없으면 저장하지 않는다`() {
        val repository = FakeVisitRepository()
        val viewModel = filled(repository)
        var saved: String? = null

        viewModel.onSaveClick(cardId = null, onSaved = { saved = it })

        assertEquals(0, repository.createCount)
        assertNull(saved)
    }

    @Test
    fun `불러오기 전에는 저장하지 않는다`() {
        val repository = FakeVisitRepository()

        viewModel(repository).onSaveClick(cardId = "3", onSaved = {})

        assertEquals(0, repository.createCount)
    }

    @Test
    fun `실패하면 화면에 남는다`() {
        // 나가 버리면 적은 것이 사라지고 다시 누를 수도 없다.
        val repository = FakeVisitRepository(saved = FakeVisitRepository.OFFLINE)
        val viewModel = filled(repository)
        var saved: String? = null

        viewModel.onSaveClick(cardId = "3", onSaved = { saved = it })

        assertNull(saved)
        assertTrue(viewModel.uiState.value is VisitRecordUiState.Content)
    }

    private fun viewModel(repository: FakeVisitRepository = FakeVisitRepository()) =
        VisitRecordViewModel(repository, Clock.fixed(Instant.parse("2026-09-12T01:00:00Z"), ZoneId.of("Asia/Seoul")))

    private fun filled(repository: FakeVisitRepository) = viewModel(repository).apply {
        load(CLINIC, NOTE)
        onEditClick()
        editActions.onItemValueChange(0, "위염 초기 소견")
        editActions.onItemValueChange(1, "혈액검사 시행")
        editActions.onItemValueChange(2, "2주분 처방")
        onEditDoneClick()
    }

    private fun editing() = viewModel().apply { load(CLINIC, NOTE) }.apply { onEditClick() }

    private fun loaded() = content(viewModel().apply { load(CLINIC, NOTE) })

    private fun content(viewModel: VisitRecordViewModel) = viewModel.uiState.value as VisitRecordUiState.Content

    private companion object {
        const val CLINIC = "서울OO병원 내과"
        const val NOTE = "배가 아파서 갔더니 위염이래요"
        val TODAY: LocalDate = LocalDate.of(2026, 9, 12)
    }
}
