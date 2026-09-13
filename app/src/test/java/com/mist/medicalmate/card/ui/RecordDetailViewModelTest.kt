package com.mist.medicalmate.card.ui

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.NewAppointment
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.FakeVisitRepository
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitItem
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
        val viewModel = viewModel(repository = repository)

        viewModel.load("card-없음")

        assertEquals(RecordDetailUiState.Failed, viewModel.uiState.value)
        assertNull(repository.requestedId)
    }

    @Test
    fun `불러오면 그 id로 읽는다`() {
        val repository = FakeVisitRepository()

        viewModel(repository = repository).load("77")

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
        assertEquals(listOf("소견", "검사", "약"), step.items.map { it.key })
        assertEquals(listOf("위염 초기", "혈액검사", "2주분"), step.items.map { it.value })
    }

    @Test
    fun `원문은 담지 않는다`() {
        // 시안의 진료 후 기록 단계에는 저장된 항목만 있다. 원문은 1q-1에서 확인하고 저장하는
        // 값이고, 기록 상세는 나중에 다시 읽는 자리다.
        val step = content(FULL).detail.steps.single() as RecordStep.Block

        assertNull(step.quote)
    }

    @Test
    fun `카드가 있으면 브리핑 카드 단계가 붙는다`() {
        val steps = content(FULL, card = CARD).detail.steps

        assertEquals(2, steps.size)
        val card = steps.last() as RecordStep.Block
        assertEquals("브리핑 카드", card.at)
        assertEquals(listOf("부위", "기간", "양상"), card.items.map { it.key })
    }

    @Test
    fun `펼치면 나오는 것까지 카드 단계가 들고 있다`() {
        // 1j-3-X가 펼친 상태다. 접으면 앞 세 줄, 펴면 강도·알러지·질문까지 나온다.
        val card = content(FULL, card = CARD).detail.steps.last() as RecordStep.Block

        assertEquals(3, card.card?.collapsedItemCount)
        assertEquals(MedicalMateSeverity.LEVEL_3, card.card?.severity)
        assertEquals(listOf("페니실린"), card.card?.allergies)
        assertEquals(listOf("검사를 받아야 하나요?"), card.card?.questions)
    }

    @Test
    fun `카드를 못 읽어도 기록은 나온다`() {
        // 상세를 여는 사람이 보려는 것은 진료에서 들은 말이다. 준비물이 없다고 그것까지
        // 감출 이유가 없다.
        val steps = content(FULL).detail.steps

        assertEquals(1, steps.size)
        assertEquals("진료에서 들은 것", (steps.single() as RecordStep.Block).title)
    }

    @Test
    fun `그 카드로 잡힌 일정이 있으면 예정이 맨 위다`() {
        val steps = content(FULL, card = CARD, appointments = listOf(NEXT)).detail.steps

        assertEquals(3, steps.size)
        val pending = steps.first() as RecordStep.Pending
        assertEquals("09.26 예정", pending.at)
        assertEquals("재방문 예약됨", pending.message)
        assertEquals("서울OO병원 내과 · 오전 10:30", pending.detail)
    }

    @Test
    fun `다른 카드의 일정은 끌어오지 않는다`() {
        val other = listOf(com.mist.medicalmate.calendar.data.AppointmentCard(id = 99, title = null))
        val steps = content(FULL, appointments = listOf(NEXT.copy(cards = other))).detail.steps

        assertTrue(steps.none { it is RecordStep.Pending })
    }

    @Test
    fun `카드 제목이 있으면 그것이 머리글이다`() {
        // 병원 이름보다 무엇 때문에 갔는지가 먼저 읽힌다.
        assertEquals("복부 통증 · 3주", content(FULL, card = CARD).detail.title)
    }

    @Test
    fun `모르는 축도 줄로 나온다`() {
        // 항목 이름이 닫힌 목록이 아니다. 아는 것만 그리면 환자가 적은 줄이 사라진다.
        val extra = VisitItem(axis = "referral", label = "referral", value = "큰 병원 가보래요")
        val step =
            content(FULL.copy(items = FULL.items + extra)).detail.steps.single { it is RecordStep.Block }
                as RecordStep.Block

        assertEquals("큰 병원 가보래요", step.items.last().value)
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
        val viewModel = viewModel(repository = FakeVisitRepository(detail = FakeVisitRepository.OFFLINE))

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

    private fun viewModel(
        repository: FakeVisitRepository? = null,
        visit: Visit? = null,
        card: BriefCard? = null,
        appointments: List<Appointment> = emptyList(),
    ) = RecordDetailViewModel(
        repository ?: FakeVisitRepository(detail = visit?.let { ApiResult.Success(it) } ?: ApiResult.Success(FULL)),
        // 카드가 없는 시험이 대부분이다. 없으면 카드 단계가 빠지고 나머지는 그대로다.
        FakeCardRepository(result = card?.let { ApiResult.Success(it) } ?: FakeVisitRepository.OFFLINE),
        FakeUpcomingRepository(appointments),
    )

    private fun content(
        visit: Visit,
        card: BriefCard? = null,
        appointments: List<Appointment> = emptyList(),
    ): RecordDetailUiState.Content {
        val viewModel = viewModel(visit = visit, card = card, appointments = appointments)
        viewModel.load("77")
        return viewModel.uiState.value as RecordDetailUiState.Content
    }

    private companion object {
        /** 그 진료를 준비한 카드. */
        val CARD =
            BriefCard(
                id = "3",
                title = "복부 통증 · 3주",
                status = BriefCard.Status.CONFIRMED,
                patientLine = "김OO · 32세 여",
                items =
                listOf(
                    BriefCardItem(key = "부위", value = "복부"),
                    BriefCardItem(key = "기간", value = "3주 전 시작"),
                    BriefCardItem(key = "양상", value = "식후 쓰림"),
                ),
                severity = MedicalMateSeverity.LEVEL_3,
                allergies = listOf("페니실린"),
                questions = listOf("검사를 받아야 하나요?"),
            )

        /** 그 카드로 잡힌 재방문. */
        val NEXT =
            Appointment(
                id = 9,
                title = "서울OO병원 내과",
                on = java.time.LocalDate.of(2026, 9, 26),
                time = java.time.LocalTime.of(10, 30),
                status = com.mist.medicalmate.calendar.data.AppointmentStatus.SCHEDULED,
                cards =
                listOf(
                    com.mist.medicalmate.calendar.data.AppointmentCard(id = 3, title = "복부 통증 · 3주"),
                ),
            )

        val FULL =
            Visit(
                id = "77",
                cardId = 3,
                clinic = "서울OO병원 내과",
                visitedOn = LocalDate.of(2026, 9, 12),
                items =
                listOf(
                    VisitItem(axis = "findings", label = "소견", value = "위염 초기"),
                    VisitItem(axis = "tests", label = "검사", value = "혈액검사"),
                    VisitItem(axis = "medication_instructions", label = "약", value = "2주분"),
                ),
                followUp = null,
                patientNotes = emptyList(),
                rawNote = "배가 아파서 갔더니 위염이래요",
            )
    }
}

/** 앞으로의 일정만 돌려주는 대역. 예정 단계가 붙는지를 본다. */
private class FakeUpcomingRepository(private val list: List<Appointment>) : AppointmentRepository {
    override suspend fun month(month: java.time.YearMonth) = ApiResult.Success(list)

    override suspend fun day(date: LocalDate) = ApiResult.Success(list)

    override suspend fun upcoming() = ApiResult.Success(list)

    override suspend fun create(appointment: NewAppointment) = ApiResult.Success(list.first())

    override suspend fun update(id: Long, edit: AppointmentEdit) = ApiResult.Success(list.first())

    override suspend fun delete(id: Long): ApiResult<Unit> = ApiResult.Success(Unit)
}
