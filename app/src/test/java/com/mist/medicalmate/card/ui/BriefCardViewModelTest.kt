package com.mist.medicalmate.card.ui

import com.mist.medicalmate.card.data.AxisEdit
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.FakeHealthProfileRepository
import com.mist.medicalmate.profile.data.HealthEdit
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthStatus
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

@OptIn(ExperimentalCoroutinesApi::class)
class BriefCardViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음은 Loading이고 불러오면 카드가 나온다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())

        assertEquals(BriefCardUiState.Loading, viewModel.uiState.value)

        viewModel.open(1)

        assertTrue(viewModel.uiState.value is BriefCardUiState.Content)
    }

    @Test
    fun `강조한 항목은 많아야 하나다`() {
        // 서버가 어느 항목을 세울지 주지 않아 지금은 아무 데도 붙지 않는다. 문서의 컴포넌트
        // 규격이 카드마다 하나를 넘기지 말라고 해서, 값이 생겨도 이 선은 지켜야 한다.
        val card = loadedContent().card

        assertTrue(card.items.count { it.emphasized } <= 1)
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
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())
        viewModel.open(1)
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
    fun `병원은 라우트가 넘긴 값이 카드에 얹힌다`() {
        // 서버 카드 응답에 병원이 없다. 1m-B에서 방금 고른 것만 실린다(#139).
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())

        viewModel.open(1, hospital = BriefCardHospital(name = "서울OO병원 내과", address = "서울 관악구"))

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals("서울OO병원 내과", content.card.hospital?.name)
    }

    private fun editing(): BriefCardViewModel {
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())
        viewModel.open(1)
        viewModel.onEditClick()
        return viewModel
    }

    private fun BriefCardViewModel.content(): BriefCardUiState.Content = uiState.value as BriefCardUiState.Content

    private fun loadedContent(): BriefCardUiState.Content {
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())
        viewModel.open(1)
        return viewModel.uiState.value as BriefCardUiState.Content
    }

    @Test
    fun `고친 축만 보낸다`() {
        // PATCH라 보낸 것만 바뀐다. 안 건드린 축까지 실어 보내면 서버가 그것도 환자가 고친
        // 값으로 남긴다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository, FakeHealthProfileRepository())
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.editActions.onItemValueChange(1, "2주 전 시작")
        viewModel.onEditDoneClick()

        assertEquals(listOf(AxisEdit("onset", "2주 전 시작")), repository.updatedAxes)
    }

    @Test
    fun `아무것도 안 고치면 축을 보내지 않는다`() {
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository, FakeHealthProfileRepository())
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(emptyList<AxisEdit>(), repository.updatedAxes)
    }

    @Test
    fun `줄을 지워도 남은 축의 자리가 밀리지 않는다`() {
        // 원본과 사본을 자리로 맞추면 첫 줄을 지웠을 때 나머지가 한 칸씩 밀려 전부 바뀐 것이 된다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository, FakeHealthProfileRepository())
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.editActions.onItemDeleteClick(0)
        viewModel.onEditDoneClick()

        assertEquals(emptyList<AxisEdit>(), repository.updatedAxes)
    }
}

/** 픽스처 카드 하나를 돌려주는 저장소. 시험마다 응답을 바꿀 수 있다. */
/** 1e-1의 카드 하나 지우기. */
class BriefCardDeleteTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프로필의 복용약과 기저질환이 카드 줄로 온다`() {
        // 카드 응답에 없는 값이다. 화면이 프로필에서 읽어 얹는다.
        val viewModel = BriefCardViewModel(FakeCardRepository(), filledProfile())

        viewModel.open(1)

        val card = (viewModel.uiState.value as BriefCardUiState.Content).card
        assertEquals(listOf("복용약", "기저질환"), card.health.map { it.key })
        assertEquals(listOf("혈압약 · 진통제", "고혈압"), card.health.map { it.value })
    }

    @Test
    fun `알러지는 줄이 아니라 경고로 간다`() {
        // 처방을 바꾸는 값이라 카드 밖 경고 면에 얹힌다.
        val viewModel = BriefCardViewModel(FakeCardRepository(), filledProfile())

        viewModel.open(1)

        val card = (viewModel.uiState.value as BriefCardUiState.Content).card
        assertEquals(listOf("페니실린"), card.allergies)
        assertFalse(card.health.any { it.key == "알러지" })
    }

    @Test
    fun `적은 것이 없는 갈래는 줄을 만들지 않는다`() {
        // "없어요"와 "잘 모르겠어요"를 카드에 적지 않는다. 비어 있다는 것은 빈 자리로 전해진다.
        val profile = FakeHealthProfileRepository(read = ApiResult.Success(EMPTY_HEALTH))
        val viewModel = BriefCardViewModel(FakeCardRepository(), profile)

        viewModel.open(1)

        val card = (viewModel.uiState.value as BriefCardUiState.Content).card
        assertTrue(card.health.isEmpty())
        assertTrue(card.allergies.isEmpty())
    }

    @Test
    fun `프로필을 못 읽어도 카드는 그린다`() {
        // 카드 본문은 이미 서버에서 왔다. 건강 정보가 없다고 화면 전체를 실패로 둘 수 없다.
        val profile = FakeHealthProfileRepository(read = FakeHealthProfileRepository.OFFLINE)
        val viewModel = BriefCardViewModel(FakeCardRepository(), profile)

        viewModel.open(1)

        val state = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals(testCard.id, state.card.id)
        assertTrue(state.card.health.isEmpty())
    }

    @Test
    fun `카드가 없으면 문답으로 만든다`() {
        // 1c-5에서 곧장 오든 병원을 먼저 찾고 오든 카드가 아직 없다. 만드는 자리는 여기
        // 하나다. 예전에는 "new"라는 가짜 id가 넘어와 화면이 빈 채로 열렸다.
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards, FakeHealthProfileRepository())

        viewModel.open(cardId = null, sessionId = 7)

        assertEquals(7L, cards.createdFrom)
        assertEquals(testCard.id, (viewModel.uiState.value as BriefCardUiState.Content).card.id)
    }

    @Test
    fun `만든 카드에 방금 고른 병원이 얹힌다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())

        viewModel.open(cardId = null, sessionId = 7, hospital = BriefCardHospital(name = "서울OO병원 내과"))

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals("서울OO병원 내과", content.card.hospital?.name)
    }

    @Test
    fun `화면이 다시 조합돼도 카드를 두 번 만들지 않는다`() {
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards, FakeHealthProfileRepository())

        viewModel.open(cardId = null, sessionId = 7)
        cards.createdFrom = null
        viewModel.open(cardId = null, sessionId = 7)

        assertNull(cards.createdFrom)
    }

    @Test
    fun `카드도 문답도 없으면 실패다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository(), FakeHealthProfileRepository())

        viewModel.open(cardId = null, sessionId = null)

        assertEquals(BriefCardUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `지우면 서버에 나가고 화면을 나간다`() {
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards, FakeHealthProfileRepository()).apply { open(testCard.id.toLong()) }
        var left = false

        viewModel.onDeleteConfirm { left = true }

        assertEquals(listOf(testCard.id.toLong()), cards.deleted)
        assertTrue(left)
    }

    @Test
    fun `못 지우면 화면에 남는다`() {
        // 나가 버리면 안 지워진 카드를 지운 것으로 알게 된다.
        val cards = FakeCardRepository(deleteFails = setOf(testCard.id))
        val viewModel = BriefCardViewModel(cards, FakeHealthProfileRepository()).apply { open(testCard.id.toLong()) }
        var left = false

        viewModel.onDeleteConfirm { left = true }

        assertFalse(left)
    }

    @Test
    fun `불러오기 전에는 지우지 않는다`() {
        val cards = FakeCardRepository()

        BriefCardViewModel(cards, FakeHealthProfileRepository()).onDeleteConfirm {}

        assertEquals(emptyList<Long>(), cards.deleted)
    }
}

internal class FakeCardRepository(
    private val card: BriefCard = testCard,
    private val result: ApiResult<BriefCard>? = null,
    private val list: ApiResult<List<CardListItem>> = ApiResult.Success(emptyList()),
    /** 지우기가 실패해야 하는 시험이 있다. 다른 호출의 [result]와 따로 둔다. */
    var deleteFails: Set<String> = emptySet(),
) : CardRepository {
    var updatedQuestions: List<String>? = null
    var confirmedId: Long? = null
    var deleted = mutableListOf<Long>()

    /** 어느 문답으로 만들었는지. 카드 없이 들어온 경로가 이 값을 채운다. */
    var createdFrom: Long? = null

    override suspend fun createFromSession(sessionId: Long): ApiResult<BriefCard> {
        createdFrom = sessionId
        return result ?: ApiResult.Success(card)
    }

    override suspend fun cards() = list

    override suspend fun card(cardId: Long) = result ?: ApiResult.Success(card)

    var updatedAxes: List<AxisEdit>? = null

    override suspend fun update(cardId: Long, axes: List<AxisEdit>, questions: List<String>): ApiResult<BriefCard> {
        updatedAxes = axes
        updatedQuestions = questions
        return result ?: ApiResult.Success(card)
    }

    override suspend fun confirm(cardId: Long): ApiResult<BriefCard> {
        confirmedId = cardId
        return result ?: ApiResult.Success(card.copy(status = BriefCard.Status.CONFIRMED))
    }

    override suspend fun handoff(cardId: Long) = result ?: ApiResult.Success(card)

    override suspend fun delete(cardId: Long): ApiResult<Unit> {
        deleted += cardId
        return if (cardId.toString() in deleteFails) OFFLINE else ApiResult.Success(Unit)
    }

    override suspend fun deleteAll(cardIds: Set<String>): Set<String> =
        cardIds.filter { id -> id.toLongOrNull()?.let { delete(it) is ApiResult.Success } == true }.toSet()

    private companion object {
        val OFFLINE = ApiResult.NetworkUnavailable(java.io.IOException("offline"))
    }
}

internal val testCard =
    BriefCard(
        id = "1",
        title = "복부 통증 · 3주",
        status = BriefCard.Status.BEFORE_VISIT,
        patientLine = "김OO · 32세 여 · 2026.09.04 작성",
        items =
        listOf(
            BriefCardItem(key = "부위", value = "복부 (명치 아래)", axis = "site"),
            BriefCardItem(key = "시작", value = "3주 전 시작", axis = "onset"),
            BriefCardItem(key = "양상", value = "식후 쓰림", axis = "character"),
        ),
        severity = null,
        allergies = listOf("페니실린"),
        questions =
        listOf(
            "검사를 받아야 하나요?",
            "지금 진통제 계속 먹어도 되나요?",
            "어떤 증상이면 바로 다시 와야 하나요?",
        ),
    )

/** 신상정보에 세 갈래를 다 적어 둔 프로필. */
private fun filledProfile() = FakeHealthProfileRepository(
    read =
    ApiResult.Success(
        FakeHealthProfileRepository.PROFILE.copy(
            health =
            HealthEdit(
                medications = HealthField(listOf("혈압약", "진통제")),
                conditions = HealthField(listOf("고혈압")),
                allergies = HealthField(listOf("페니실린")),
            ),
        ),
    ),
)

/** 세 갈래가 모두 비어 있는 프로필. */
private val EMPTY_HEALTH =
    FakeHealthProfileRepository.PROFILE.copy(
        health =
        HealthEdit(
            medications = HealthField(emptyList(), HealthStatus.NONE),
            conditions = HealthField(emptyList(), HealthStatus.UNKNOWN),
            allergies = HealthField(emptyList(), HealthStatus.NONE),
        ),
    )
