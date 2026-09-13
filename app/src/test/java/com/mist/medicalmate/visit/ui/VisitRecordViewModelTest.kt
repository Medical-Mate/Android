package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.VisitClassification
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
    fun `불러오면 나눈 줄과 원문이 함께 있다`() {
        val content = content(viewModel(sorted()).apply { load(CLINIC, NOTE, TODAY) })

        assertEquals(listOf("소견", "검사", "약", "재방문"), content.record.items.map { it.key })
        assertEquals(listOf("위염 초기", "혈액검사", "위장약", "2주 뒤"), content.record.items.map { it.value })
        assertEquals(NOTE, content.record.memo)
        assertFalse(content.editing)
    }

    @Test
    fun `병원과 오늘 날짜가 머리줄이 된다`() {
        assertEquals("서울OO병원 내과 · 2026.09.12", loaded().record.clinicLine)
    }

    @Test
    fun `병원을 고르지 않았으면 날짜만 적는다`() {
        val viewModel = viewModel().apply { load(clinic = null, note = NOTE, visitedOn = TODAY) }

        assertEquals("2026.09.12", content(viewModel).record.clinicLine)
    }

    @Test
    fun `AI가 찾은 항목만 줄이 된다`() {
        // 항목이 고정이 아니다. 못 찾은 것을 빈 줄로 깔면 AI가 무엇을 못 찾았는지가 지워진다.
        val record = content(viewModel(classifying()).apply { load(CLINIC, NOTE, TODAY) }).record

        assertEquals(listOf("소견", "검사", "재방문"), record.items.map { it.key })
        assertEquals(listOf("위염 초기", "혈액검사", "2주 뒤"), record.items.map { it.value })
        assertEquals(3, record.classifiedCount)
    }

    @Test
    fun `AI가 늘린 축도 그대로 줄이 된다`() {
        // 항목 이름이 닫힌 목록이 아니다. 아는 것만 그리면 환자가 들은 말이 사라진다.
        val repository =
            FakeVisitRepository().apply {
                classification =
                    ApiResult.Success(
                        CLASSIFIED.copy(
                            items = CLASSIFIED.items + VisitItem("referral", "referral", "큰 병원 가보래요"),
                        ),
                    )
            }

        val record = content(viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }).record

        assertEquals(4, record.items.size)
        assertEquals("큰 병원 가보래요", record.items.last().value)
    }

    @Test
    fun `나눈 결과에서도 재방문만 브랜드색이다`() {
        val record = content(viewModel(classifying()).apply { load(CLINIC, NOTE, TODAY) }).record

        assertEquals(
            listOf(VisitRecordItem.Tone.DEFAULT, VisitRecordItem.Tone.DEFAULT, VisitRecordItem.Tone.LINK),
            record.items.map { it.tone },
        )
    }

    @Test
    fun `나누지 못하면 줄이 없고 원문만 남는다`() {
        // 전에는 빈 네 자리를 열었는데, 네 항목을 묻고는 아무 답도 못 내놓는 모양이었다.
        // 적은 말은 사라지지 않는다 — 서버가 `patientNotes`로 돌려주고 원문은 `rawNote`로 간다.
        val record = content(unsorted()).record

        assertTrue(record.items.isEmpty())
        assertEquals(NOTE, record.memo)
    }

    @Test
    fun `나누지 못해도 적은 말은 저장에 실린다`() {
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals(NOTE, repository.request?.rawNote)
        assertTrue(repository.request?.items.orEmpty().isEmpty())
    }

    @Test
    fun `나눈 결과의 재방문 날짜와 남은 문장이 저장에 실린다`() {
        val repository = classifying()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals(LocalDate.of(2026, 9, 26), repository.request?.followUp?.date)
        assertEquals(listOf("접수 오래 걸렸어요"), repository.request?.patientNotes)
    }

    @Test
    fun `직전 분류를 돌려보낸다`() {
        // 안 보내면 AI 모델 호출이 다시 나가고 그 비용이 서버 크레딧에서 빠진다.
        val repository = classifying()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }

        viewModel.load(CLINIC, NOTE, TODAY)

        assertEquals(listOf(null, mapOf("0" to "findings")), repository.classifyRequests)
    }

    @Test
    fun `메모가 비어 있으면 나눠 달라고 하지 않는다`() {
        val repository = classifying()

        viewModel(repository).apply { load(CLINIC, "", TODAY) }

        assertTrue(repository.classifyRequests.isEmpty())
    }

    @Test
    fun `나누지 못하면 가짓수를 알리지 않는다`() {
        assertNull(loaded().record.classifiedCount)
    }

    @Test
    fun `재방문만 브랜드색으로 세운다`() {
        val record = content(viewModel(sorted()).apply { load(CLINIC, NOTE, TODAY) }).record
        val links = record.items.filter { it.tone == VisitRecordItem.Tone.LINK }

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
        assertEquals("위염 초기", content.record.items[0].value)
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
        assertEquals("혈액검사", content.record.items[1].value)
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
        val viewModel = viewModel().apply { load(CLINIC, NOTE, TODAY) }
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
    fun `불러오기 전에는 조작이 아무 일도 하지 않는다`() {
        val viewModel = viewModel()

        viewModel.onEditClick()
        viewModel.onDeleteClick()

        assertEquals(VisitRecordUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `저장하면 채운 값이 서버의 세 필드로 간다`() {
        val repository = FakeVisitRepository()
        val viewModel = filled(repository)
        var left = false

        viewModel.onSaveClick(cardId = "3", onSaved = { left = true })

        val request = repository.request
        assertEquals(3L, repository.cardId)
        assertEquals(CLINIC, request?.clinicName)
        assertEquals(TODAY, request?.visitedOn)
        assertEquals(
            listOf(
                "findings" to "위염 초기 소견",
                "tests" to "혈액검사 시행",
                "medication_instructions" to "2주분 처방",
                "follow_up" to "2주 뒤",
            ),
            request?.items?.map { it.axis to it.value },
        )
        assertEquals(NOTE, request?.rawNote)
        // 저장이 끝나면 흐름이 시작된 캘린더 일자로 돌아간다. 기록 id는 더 쓰지 않는다.
        assertTrue(left)
    }

    @Test
    fun `저장이 거절되면 화면을 나가지 않고 알린다`() {
        // 확정하지 않은 카드에 서버가 400을 준다(#196). 아무 말도 하지 않으면 버튼이 안 먹는
        // 것으로 읽고 계속 누른다 — 기기에서 세 번 눌러 세 번 거절당했다.
        val repository = FakeVisitRepository()
        repository.createFails = true
        val viewModel = filled(repository)
        var left = false

        viewModel.onSaveClick(cardId = "3", onSaved = { left = true })

        val content = viewModel.uiState.value as VisitRecordUiState.Content
        assertTrue(content.saveFailed)
        assertFalse(left)
    }

    @Test
    fun `다시 저장하면 실패 표시가 먼저 지워진다`() {
        // 남아 있으면 두 번째 시도가 성공해도 실패 문구가 그대로 보인다.
        val repository = FakeVisitRepository()
        repository.createFails = true
        val viewModel = filled(repository)
        viewModel.onSaveClick(cardId = "3", onSaved = {})

        repository.createFails = false
        viewModel.onSaveClick(cardId = "3", onSaved = {})

        val content = viewModel.uiState.value as VisitRecordUiState.Content
        assertFalse(content.saveFailed)
    }

    @Test
    fun `오늘이 아니라 흐름이 시작된 날로 저장한다`() {
        // 어제 진료를 오늘 적을 수 있다. 오늘로 박으면 그 일자 화면에 영영 나오지 않는다.
        val yesterday = TODAY.minusDays(1)
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, yesterday) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals(yesterday, repository.request?.visitedOn)
    }

    @Test
    fun `날짜가 없으면 오늘로 둔다`() {
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, null) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals(TODAY, repository.request?.visitedOn)
    }

    @Test
    fun `비운 줄은 보내지 않는다`() {
        // 안 적은 것과 빈 문자열은 다르다. 빈 줄을 보내면 서버가 UNKNOWN으로 박는다.
        val repository = FakeVisitRepository()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals(emptyList<Pair<String, String>>(), repository.request?.items?.map { it.axis to it.value })
        assertEquals(NOTE, repository.request?.rawNote)
    }

    @Test
    fun `재방문 줄도 축으로 간다`() {
        // follow_up 축이 생겼다(#178). 전에는 보낼 자리가 없어 버려졌다.
        val repository = sorted()
        val viewModel = viewModel(repository).apply { load(CLINIC, NOTE, TODAY) }
        viewModel.onEditClick()
        viewModel.editActions.onItemValueChange(0, "")
        viewModel.editActions.onItemValueChange(1, "")
        viewModel.editActions.onItemValueChange(2, "")
        viewModel.editActions.onItemValueChange(3, "2주 뒤")
        viewModel.onEditDoneClick()

        viewModel.onSaveClick(cardId = "3", onSaved = {})

        assertEquals("follow_up" to "2주 뒤", repository.request?.items?.single()?.let { it.axis to it.value })
    }

    @Test
    fun `카드가 없으면 저장하지 않는다`() {
        val repository = FakeVisitRepository()
        val viewModel = filled(repository)
        var left = false

        viewModel.onSaveClick(cardId = null, onSaved = { left = true })

        assertEquals(0, repository.createCount)
        assertFalse(left)
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
        var left = false

        viewModel.onSaveClick(cardId = "3", onSaved = { left = true })

        assertFalse(left)
        assertTrue(viewModel.uiState.value is VisitRecordUiState.Content)
    }

    /** AI가 소견·검사·재방문 셋을 찾은 경우. 약은 못 찾았다. */
    private fun classifying() = FakeVisitRepository().apply { classification = ApiResult.Success(CLASSIFIED) }

    /** 나눈 결과를 주는 저장소. 편집·저장 시험의 바탕이다. */
    private fun sorted() = FakeVisitRepository().apply { classification = ApiResult.Success(ALL_FOUR) }

    private fun viewModel(repository: FakeVisitRepository = FakeVisitRepository()) =
        VisitRecordViewModel(repository, Clock.fixed(Instant.parse("2026-09-12T01:00:00Z"), ZoneId.of("Asia/Seoul")))

    private fun filled(repository: FakeVisitRepository) = viewModel(
        repository.apply { classification = ApiResult.Success(ALL_FOUR) },
    ).apply {
        load(CLINIC, NOTE, TODAY)
        onEditClick()
        editActions.onItemValueChange(0, "위염 초기 소견")
        editActions.onItemValueChange(1, "혈액검사 시행")
        editActions.onItemValueChange(2, "2주분 처방")
        onEditDoneClick()
    }

    private fun editing() = viewModel(sorted()).apply { load(CLINIC, NOTE, TODAY) }.apply { onEditClick() }

    /** 아무것도 못 나눈 경우. 줄이 서지 않는다. */
    private fun unsorted() = viewModel(FakeVisitRepository()).apply { load(CLINIC, NOTE, TODAY) }

    private fun loaded() = content(viewModel().apply { load(CLINIC, NOTE, TODAY) })

    private fun content(viewModel: VisitRecordViewModel) = viewModel.uiState.value as VisitRecordUiState.Content

    private companion object {
        const val CLINIC = "서울OO병원 내과"
        const val NOTE = "배가 아파서 갔더니 위염이래요"
        val TODAY: LocalDate = LocalDate.of(2026, 9, 12)

        /**
         * 네 축을 모두 나눈 결과.
         *
         * 편집·저장 시험이 줄 번호로 값을 넣는다. 전에는 나누지 못했을 때 열리던 빈 네 줄이
         * 그 자리를 대신했는데, 그 줄이 사라지면서(#200) 나눈 결과를 주고 시작한다.
         */
        val ALL_FOUR =
            VisitClassification(
                items =
                listOf(
                    VisitItem("findings", "소견", "위염 초기"),
                    VisitItem("tests", "검사", "혈액검사"),
                    VisitItem("medication_instructions", "약", "위장약"),
                    VisitItem("follow_up", "재방문", "2주 뒤"),
                ),
                followUp =
                VisitFollowUp(date = LocalDate.of(2026, 9, 26), text = "2주 뒤", approximate = true),
                patientNotes = listOf("접수 오래 걸렸어요"),
                labels = mapOf("0" to "findings"),
            )

        val CLASSIFIED =
            VisitClassification(
                items =
                listOf(
                    VisitItem("findings", "소견", "위염 초기"),
                    VisitItem("tests", "검사", "혈액검사"),
                    VisitItem("follow_up", "재방문", "2주 뒤"),
                ),
                followUp =
                VisitFollowUp(date = LocalDate.of(2026, 9, 26), text = "2주 뒤", approximate = true),
                patientNotes = listOf("접수 오래 걸렸어요"),
                labels = mapOf("0" to "findings"),
            )
    }
}
