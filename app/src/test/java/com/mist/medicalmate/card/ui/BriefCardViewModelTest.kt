package com.mist.medicalmate.card.ui

import com.mist.medicalmate.card.data.AxisEdit
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiErrorCode
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        val viewModel = BriefCardViewModel(FakeCardRepository())

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
        val viewModel = BriefCardViewModel(FakeCardRepository())
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
    fun `이미 있는 카드는 응답의 병원을 쓴다`() {
        // 카드가 병원을 들게 되면서(Backend#101) 라우트로 나르던 값을 얹지 않는다. 얹으면
        // 캘린더에서 연 카드처럼 넘길 값이 없는 경로에서 병원이 지워진다.
        val stored = testCard.copy(hospital = BriefCardHospital(name = "서울OO병원 내과", address = "서울 관악구"))
        val viewModel = BriefCardViewModel(FakeCardRepository(card = stored))

        viewModel.open(1, hospital = BriefCardHospital(name = "엉뚱한병원"))

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals("서울OO병원 내과", content.card.hospital?.name)
        assertEquals("서울 관악구", content.card.hospital?.address)
    }

    @Test
    fun `들고 있는 카드는 다시 읽지 않는다`() {
        // 병원을 고르러 갔다 오면 조합이 다시 시작되며 이 호출이 한 번 더 온다. 그때 다시
        // 읽으면 편집 중이던 사본이 날아가고, 돌아온 병원이 Loading에 도착해 버려진다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.open(1)

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertTrue(content.editing)
    }

    @Test
    fun `고친 뒤 화면이 다시 열려도 옛 버전으로 돌아가지 않는다`() {
        // 고치면 서버가 새 버전을 만들어 id가 달라지는데 라우트는 누를 때의 옛 id를 그대로
        // 들고 있다. 그 둘을 견줘서 다시 읽으면 옛 버전으로 되돌아간다(Backend#114).
        val repository = FakeCardRepository(nextVersion = testCard.copy(id = "20"))
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()
        viewModel.onEditDoneClick()

        viewModel.open(1)

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals("20", content.card.id)
    }

    @Test
    fun `고친 뒤의 수정은 새 버전으로 간다`() {
        // 옛 id로 다시 보내면 서버가 같은 부모에서 가지를 친다. 가지에 들어간 편집은
        // 목록이 최신 한 장만 내면서 영영 보이지 않는다.
        val repository = FakeCardRepository(nextVersion = testCard.copy(id = "20"))
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()
        viewModel.onEditDoneClick()
        viewModel.open(1)

        viewModel.onHospitalPicked("서울OO병원 내과", null)

        assertEquals(20L, repository.updatedId)
    }

    @Test
    fun `이미 고친 카드라고 막히면 최신 카드로 갈아타고 다시 보낸다`() {
        // 옛 id를 들고 있는 경로는 #219에서 막았지만 못 찾은 자리가 남아 있으면 여기서
        // 드러난다. 서버가 갈아탈 카드를 알려주므로(Backend#117) 고친 값을 잃지 않는다.
        val repository = FakeCardRepository()
        repository.updateRejections += alreadyEdited(latestCardId = 20)
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(listOf(1L, 20L), repository.updatedIds)
        assertFalse(viewModel.content().saveFailed)
    }

    @Test
    fun `갈아탄 뒤에도 막히면 실패로 알린다`() {
        // 계속 따라가면 어디서 멈출지 알 수 없고, 다른 데서 고치고 있는 중이라면 그 편집을
        // 덮어쓰기를 반복하게 된다.
        val repository = FakeCardRepository()
        repository.updateRejections += alreadyEdited(latestCardId = 20)
        repository.updateRejections += alreadyEdited(latestCardId = 21)
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(listOf(1L, 20L), repository.updatedIds)
        assertTrue(viewModel.content().saveFailed)
    }

    @Test
    fun `갈아탈 카드를 알려주지 않으면 다시 보내지 않는다`() {
        // 어디로 보낼지 모르는 채로 한 번 더 보내면 막힌 그 카드로 또 간다.
        val repository = FakeCardRepository()
        repository.updateRejections += alreadyEdited(latestCardId = null)
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(listOf(1L), repository.updatedIds)
        assertTrue(viewModel.content().saveFailed)
    }

    @Test
    fun `다른 이유로 거절되면 그대로 실패다`() {
        // 갈아타는 것은 이미 고친 카드일 때만이다. 아무 거절에나 다시 보내면 서버가 막은
        // 것을 두 번 보내게 된다.
        val repository = FakeCardRepository()
        repository.updateRejections +=
            ApiResult.Rejected(
                code = ApiErrorCode.INVALID_REQUEST,
                message = null,
                requestId = null,
                retryable = false,
            )
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(listOf(1L), repository.updatedIds)
        assertTrue(viewModel.content().saveFailed)
    }

    /** 서버가 이미 고친 카드라고 막는 응답. 갈아탈 카드는 `details`에 온다. */
    private fun alreadyEdited(latestCardId: Long?) = ApiResult.Rejected(
        code = ApiErrorCode.CARD_ALREADY_EDITED,
        message = null,
        requestId = null,
        retryable = false,
        details =
        latestCardId?.let { buildJsonObject { put("latestCardId", it) } },
    )

    @Test
    fun `변경에서 고른 병원을 서버에 보낸다`() {
        // 화면에만 얹으면 다시 열었을 때 되돌아가 있다. 카드가 병원을 들게 됐다(Backend#101).
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)

        viewModel.onHospitalPicked("서울OO병원 내과", "서울 관악구 남부순환로 1820, 3층")

        assertEquals("서울OO병원 내과", repository.updatedClinic?.name)
        assertEquals("서울 관악구 남부순환로 1820, 3층", repository.updatedClinic?.address)
        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals("서울OO병원 내과", content.card.hospital?.name)
    }

    @Test
    fun `병원 변경이 거절되면 카드를 바꾸지 않는다`() {
        // 화면에만 바꿔 두면 다시 열었을 때 되돌아가 있다.
        val repository = FakeCardRepository()
        repository.updateFails = true
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)

        viewModel.onHospitalPicked("서울OO병원 내과", null)

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertTrue(content.saveFailed)
        assertNull(content.card.hospital)
    }

    @Test
    fun `주소가 비어 있으면 병원 이름만 보낸다`() {
        // 심평원에 주소가 없는 곳이 있다. 빈 문자열을 그대로 두면 빈 줄이 그려진다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)

        viewModel.onHospitalPicked("서울OO병원 내과", "")

        assertEquals("서울OO병원 내과", repository.updatedClinic?.name)
        assertNull(repository.updatedClinic?.address)
    }

    @Test
    fun `뱃지는 확정이 아니라 진료를 마쳤는지로 갈린다`() {
        // 저장하기가 카드를 확정하게 되면서(#196) 확정으로 판단하면 진료를 받기도 전에
        // "진료 완료"가 뜬다. 서버도 둘을 다른 축으로 둔다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)

        viewModel.onSaveClick(onSaved = {})

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertEquals(BriefCard.Status.CONFIRMED, content.card.status)
        assertFalse(content.card.visited)
    }

    @Test
    fun `저장하기가 카드를 확정한다`() {
        // 확정하지 않으면 진료 후 기록을 남길 수 없다. 서버가 확정한 카드에만 받는다(#196).
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        var left = false

        viewModel.onSaveClick(onSaved = { left = true })

        assertEquals(1L, repository.confirmedId)
        assertTrue(left)
    }

    @Test
    fun `이미 확정한 카드는 다시 확정하지 않는다`() {
        // 두 번 확정하면 400이다.
        val repository = FakeCardRepository(card = testCard.copy(status = BriefCard.Status.CONFIRMED))
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        var left = false

        viewModel.onSaveClick(onSaved = { left = true })

        assertNull(repository.confirmedId)
        assertTrue(left)
    }

    @Test
    fun `확정이 거절되면 화면을 나가지 않고 알린다`() {
        // 나가 버리면 저장되지 않은 것을 저장된 것으로 알게 된다.
        val repository = FakeCardRepository()
        repository.confirmFails = true
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        var left = false

        viewModel.onSaveClick(onSaved = { left = true })

        val content = viewModel.uiState.value as BriefCardUiState.Content
        assertTrue(content.saveFailed)
        assertFalse(left)
    }

    private fun editing(): BriefCardViewModel {
        val viewModel = BriefCardViewModel(FakeCardRepository())
        viewModel.open(1)
        viewModel.onEditClick()
        return viewModel
    }

    private fun BriefCardViewModel.content(): BriefCardUiState.Content = uiState.value as BriefCardUiState.Content

    private fun loadedContent(): BriefCardUiState.Content {
        val viewModel = BriefCardViewModel(FakeCardRepository())
        viewModel.open(1)
        return viewModel.uiState.value as BriefCardUiState.Content
    }

    @Test
    fun `고친 축만 보낸다`() {
        // PATCH라 보낸 것만 바뀐다. 안 건드린 축까지 실어 보내면 서버가 그것도 환자가 고친
        // 값으로 남긴다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.editActions.onItemValueChange(1, "2주 전 시작")
        viewModel.onEditDoneClick()

        assertEquals(listOf(AxisEdit("onset", "2주 전 시작")), repository.updatedAxes)
    }

    @Test
    fun `아무것도 안 고치면 축을 보내지 않는다`() {
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
        viewModel.open(1)
        viewModel.onEditClick()

        viewModel.onEditDoneClick()

        assertEquals(emptyList<AxisEdit>(), repository.updatedAxes)
    }

    @Test
    fun `줄을 지워도 남은 축의 자리가 밀리지 않는다`() {
        // 원본과 사본을 자리로 맞추면 첫 줄을 지웠을 때 나머지가 한 칸씩 밀려 전부 바뀐 것이 된다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)
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
    fun `카드가 없으면 문답으로 만든다`() {
        // 1c-5에서 곧장 오든 병원을 먼저 찾고 오든 카드가 아직 없다. 만드는 자리는 여기
        // 하나다. 예전에는 "new"라는 가짜 id가 넘어와 화면이 빈 채로 열렸다.
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards)

        viewModel.open(cardId = null, sessionId = 7)

        assertEquals(7L, cards.createdFrom)
        assertEquals(testCard.id, (viewModel.uiState.value as BriefCardUiState.Content).card.id)
    }

    @Test
    fun `카드를 만들 때 고른 병원을 함께 보낸다`() {
        // 서버가 그 값을 카드에 담는다(Backend#101). 화면에만 얹으면 다시 열 때 사라진다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)

        viewModel.open(cardId = null, sessionId = 7, hospital = BriefCardHospital(name = "서울OO병원 내과"))

        assertEquals("서울OO병원 내과", repository.createdClinic?.name)
    }

    @Test
    fun `병원을 안 골랐으면 보내지 않는다`() {
        // 1m-B에 건너뛰기가 있다. 안 보내면 병원 없이 만들어진다.
        val repository = FakeCardRepository()
        val viewModel = BriefCardViewModel(repository)

        viewModel.open(cardId = null, sessionId = 7)

        assertNull(repository.createdClinic)
    }

    @Test
    fun `화면이 다시 조합돼도 카드를 두 번 만들지 않는다`() {
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards)

        viewModel.open(cardId = null, sessionId = 7)
        cards.createdFrom = null
        viewModel.open(cardId = null, sessionId = 7)

        assertNull(cards.createdFrom)
    }

    @Test
    fun `카드도 문답도 없으면 실패다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository())

        viewModel.open(cardId = null, sessionId = null)

        assertEquals(BriefCardUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `지우면 서버에 나가고 화면을 나간다`() {
        val cards = FakeCardRepository()
        val viewModel = BriefCardViewModel(cards).apply { open(testCard.id.toLong()) }
        var left = false

        viewModel.onDeleteConfirm { left = true }

        assertEquals(listOf(testCard.id.toLong()), cards.deleted)
        assertTrue(left)
    }

    @Test
    fun `못 지우면 화면에 남는다`() {
        // 나가 버리면 안 지워진 카드를 지운 것으로 알게 된다.
        val cards = FakeCardRepository(deleteFails = setOf(testCard.id))
        val viewModel = BriefCardViewModel(cards).apply { open(testCard.id.toLong()) }
        var left = false

        viewModel.onDeleteConfirm { left = true }

        assertFalse(left)
    }

    @Test
    fun `불러오기 전에는 지우지 않는다`() {
        val cards = FakeCardRepository()

        BriefCardViewModel(cards).onDeleteConfirm {}

        assertEquals(emptyList<Long>(), cards.deleted)
    }
}

internal class FakeCardRepository(
    private val card: BriefCard = testCard,
    private val result: ApiResult<BriefCard>? = null,
    /** 고치면 서버가 내주는 새 버전. 확정한 카드를 고칠 때의 모양이다. */
    private val nextVersion: BriefCard? = null,
    private val list: ApiResult<List<CardListItem>> = ApiResult.Success(emptyList()),
    /** 지우기가 실패해야 하는 시험이 있다. 다른 호출의 [result]와 따로 둔다. */
    var deleteFails: Set<String> = emptySet(),
) : CardRepository {
    var updatedQuestions: List<String>? = null
    var confirmedId: Long? = null
    var deleted = mutableListOf<Long>()

    /** 어느 문답으로 만들었는지. 카드 없이 들어온 경로가 이 값을 채운다. */
    var createdFrom: Long? = null

    /** 카드를 만들 때 함께 보낸 병원. 고른 것이 서버로 가는지가 관심사다. */
    var createdClinic: BriefCardHospital? = null

    override suspend fun createFromSession(sessionId: Long, clinic: BriefCardHospital?): ApiResult<BriefCard> {
        createdFrom = sessionId
        createdClinic = clinic
        return result ?: ApiResult.Success(card)
    }

    override suspend fun cards() = list

    override suspend fun card(cardId: Long) = result ?: ApiResult.Success(card)

    var updatedAxes: List<AxisEdit>? = null

    /** `변경`으로 보낸 병원. 화면에만 남지 않고 서버로 가는지가 관심사다. */
    var updatedClinic: BriefCardHospital? = null

    /** 어느 id로 보냈는지. 옛 버전으로 가면 서버에서 가지가 난다. */
    var updatedId: Long? = null

    /** 보낸 차례대로의 id. 막힌 뒤에 어느 카드로 갈아탔는지가 관심사다. */
    val updatedIds = mutableListOf<Long>()

    /** 서버가 막는 응답. 앞에서부터 하나씩 쓰고 다 쓰면 보통대로 답한다. */
    val updateRejections = mutableListOf<ApiResult.Rejected>()

    override suspend fun update(
        cardId: Long,
        axes: List<AxisEdit>,
        questions: List<String>,
        clinic: BriefCardHospital?,
    ): ApiResult<BriefCard> {
        updatedId = cardId
        updatedIds += cardId
        updatedAxes = axes
        updatedQuestions = questions
        updatedClinic = clinic
        return when {
            updateFails -> OFFLINE
            updateRejections.isNotEmpty() -> updateRejections.removeAt(0)
            nextVersion != null -> ApiResult.Success(nextVersion.copy(hospital = clinic ?: nextVersion.hospital))
            else -> result ?: ApiResult.Success(card.copy(hospital = clinic ?: card.hospital))
        }
    }

    /** 수정만 실패해야 하는 시험이 있다. 카드 조회까지 실패하면 화면이 Content가 아니다. */
    var updateFails: Boolean = false

    /** 확정만 실패해야 하는 시험이 있다. 카드 조회까지 실패하면 화면이 Content가 아니다. */
    var confirmFails: Boolean = false

    override suspend fun confirm(cardId: Long): ApiResult<BriefCard> {
        confirmedId = cardId
        return when {
            confirmFails -> OFFLINE
            else -> result ?: ApiResult.Success(card.copy(status = BriefCard.Status.CONFIRMED))
        }
    }

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
