package com.mist.medicalmate.card.ui
import com.mist.medicalmate.R
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.OpenIntake
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
        BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(testItems)), LIST_CLOCK).apply {
            load()
        }

    private fun BriefCardListViewModel.content(): BriefCardListUiState.Content =
        uiState.value as BriefCardListUiState.Content

    @Test
    fun `불러오기 전에는 편집으로 들어갈 수 없다`() {
        val viewModel = BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(testItems)), LIST_CLOCK)

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
    fun `카드를 지우라는 말은 열지 않고 화면에서 하라고 한다`() = runTest {
        // 완성된 카드는 말로 지우지 않는다(#296). 전엔 "두 번째 카드 지워 줘"가 그 카드를 열었다(#333).
        val viewModel = loaded()
        var opened: String? = null

        val reply = viewModel.onVoice("두 번째 카드 지워 줘", LocalDate.of(2026, 10, 19)) { opened = it }

        assertNull(opened)
        assertEquals(VoiceReply.Done(R.string.voice_by_hand), reply)
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
        viewModel.onSelectChange("101", true)

        viewModel.onEditCancel()

        assertFalse(viewModel.content().editing)
        assertNull(viewModel.content().selectedIds)
    }

    @Test
    fun `취소해도 목록은 그대로다`() {
        val viewModel = loaded()
        val before = viewModel.content().groups
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)

        viewModel.onEditCancel()

        assertEquals(before, viewModel.content().groups)
    }

    @Test
    fun `같은 줄을 두 번 고르면 켜고 꺼진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()

        viewModel.onSelectChange("101", true)
        assertEquals(setOf("101"), viewModel.content().selectedIds)

        viewModel.onSelectChange("101", false)
        assertEquals(emptySet<String>(), viewModel.content().selectedIds)
    }

    @Test
    fun `편집이 아닐 때 고르기는 아무 일도 하지 않는다`() {
        val viewModel = loaded()

        viewModel.onSelectChange("101", true)

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
        viewModel.onSelectChange("101", true)

        viewModel.onDeleteClick()

        assertTrue(viewModel.content().deleteRequested)
    }

    @Test
    fun `확인을 닫으면 고른 것은 남는다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteDismiss()

        assertFalse(viewModel.content().deleteRequested)
        assertEquals(setOf("101"), viewModel.content().selectedIds)
    }

    @Test
    fun `지우면 그 줄만 사라지고 편집에서 나온다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)
        viewModel.onSelectChange("103", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("102"), ids)
        assertFalse(viewModel.content().editing)
        assertFalse(viewModel.content().deleteRequested)
    }

    @Test
    fun `묶음이 비면 그 달도 사라진다`() {
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("102", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf("2026년 9월"), viewModel.content().groups.map { it.monthLabel })
    }

    @Test
    fun `지운 것을 서버에도 지운다`() {
        val cards = FakeCardRepository(list = ApiResult.Success(testItems))
        val viewModel = BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf(101L), cards.deleted)
    }

    @Test
    fun `안 지워진 것은 목록에 남는다`() {
        // 지워지지 않은 카드가 사라지면 다시 열었을 때 되살아난 것으로 읽힌다.
        val cards = FakeCardRepository(list = ApiResult.Success(testItems), deleteFails = setOf("103"))
        val viewModel = BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)
        viewModel.onSelectChange("103", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("103", "102"), ids)
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

    // 카드 없는 문답 줄(#338)

    @Test
    fun `카드는 진료를 마쳤으면 진료 완료 아니면 진료 전이다`() {
        // 카드는 만들 때부터 확정돼 있다(Backend#181). 저장하지 않은 카드라는 상태가 없다.
        val rows = loaded().content().groups.flatMap { it.items }.associate { it.id to it.status }

        assertEquals(RecordItem.Status.CONFIRMED, rows["101"])
        assertEquals(RecordItem.Status.BEFORE_VISIT, rows["103"])
    }

    @Test
    fun `문답 중이면 작성 중 줄이 날짜순으로 선다`() {
        val viewModel = withIntake(OpenIntake(sessionId = 7, site = "무릎", tempSaved = false, startedOn = TODAY))

        // 오늘 시작한 문답이라 10월 묶음의 맨 위다.
        val october = viewModel.content().groups.first()
        val row = october.items.first()
        assertEquals("2026년 10월", october.monthLabel)
        assertEquals("intake:7", row.id)
        assertEquals("무릎", row.title)
        assertEquals(RecordItem.Status.IN_PROGRESS, row.status)
        // 홈의 이어서 하기처럼 끝낸 단계를 센다. 문답 중이면 부위와 강도까지 두 단계다.
        assertEquals("오늘 · 3단계 중 2단계", row.meta)
        assertEquals("이어서 정리하기", row.resumeLabel)
    }

    @Test
    fun `질문 고르기까지 마쳤고 카드가 없으면 임시 저장이다`() {
        val viewModel =
            withIntake(OpenIntake(sessionId = 7, site = "무릎", tempSaved = true, startedOn = LocalDate.of(2026, 9, 10)))

        // 9월 묶음에 들고, 그보다 먼저 쓴 9월 4일 카드보다 위다.
        val september = viewModel.content().groups.first { it.monthLabel == "2026년 9월" }.items.map { it.id }
        assertTrue(september.indexOf("intake:7") < september.indexOf("101"))
        val row = viewModel.content().groups.flatMap { it.items }.first { it.id == "intake:7" }
        assertEquals(RecordItem.Status.TEMP_SAVED, row.status)
        assertEquals("09.10 · 3단계 중 3단계", row.meta)
    }

    @Test
    fun `문답 줄을 누르면 이어서 하기로 가고 카드 줄은 아니다`() {
        assertEquals(7L, intakeSessionOf("intake:7"))
        assertNull(intakeSessionOf("101"))
    }

    @Test
    fun `문답을 못 읽어도 카드 목록은 그린다`() {
        val cards = FakeCardRepository(list = ApiResult.Success(testItems))
        cards.intake = ApiResult.NetworkUnavailable(java.io.IOException("offline"))

        val viewModel = BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }

        assertEquals(3, viewModel.content().groups.sumOf { it.items.size })
    }

    @Test
    fun `편집에서 문답 줄을 고르면 문답을 지운다`() {
        val cards = FakeCardRepository(list = ApiResult.Success(testItems))
        cards.intake = ApiResult.Success(OpenIntake(sessionId = 7, site = "무릎", tempSaved = true, startedOn = TODAY))
        val viewModel = BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("intake:7", true)
        viewModel.onSelectChange("101", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertEquals(listOf(7L), cards.deletedIntakes)
        assertEquals(listOf(101L), cards.deleted)
        val ids = viewModel.content().groups.flatMap { group -> group.items.map { it.id } }
        assertEquals(listOf("103", "102"), ids)
    }

    @Test
    fun `문답을 못 지우면 그 줄이 남는다`() {
        val cards = FakeCardRepository(list = ApiResult.Success(testItems))
        cards.intake = ApiResult.Success(OpenIntake(sessionId = 7, site = "무릎", tempSaved = true, startedOn = TODAY))
        cards.intakeDeleteFails = true
        val viewModel = BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }
        viewModel.onEditStart()
        viewModel.onSelectChange("intake:7", true)
        viewModel.onDeleteClick()

        viewModel.onDeleteConfirm()

        assertTrue(viewModel.content().groups.flatMap { it.items }.any { it.id == "intake:7" })
    }

    @Test
    fun `편집 중에는 다시 읽지 않는다`() {
        // 화면에 돌아올 때마다 다시 읽는데, 고르는 중이면 고른 것이 사라진다.
        val viewModel = loaded()
        viewModel.onEditStart()
        viewModel.onSelectChange("101", true)

        viewModel.load()

        assertEquals(setOf("101"), viewModel.content().selectedIds)
    }

    private fun withIntake(intake: OpenIntake): BriefCardListViewModel {
        val cards = FakeCardRepository(list = ApiResult.Success(testItems))
        cards.intake = ApiResult.Success(intake)
        return BriefCardListViewModel(cards, LIST_CLOCK).apply { load() }
    }
}

/** 2026-10-20 낮(한국). 목록 시험의 오늘이다. */
internal val LIST_CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-20T03:00:00Z"), ZoneId.of("Asia/Seoul"))

private val TODAY = LocalDate.of(2026, 10, 20)

private val testItems =
    listOf(
        CardListItem(
            id = "101",
            title = "복부 통증 · 3주",
            visited = true,
            clinic = "서울OO병원 내과",
            writtenOn = LocalDate.of(2026, 9, 4),
        ),
        CardListItem(
            id = "103",
            title = "무릎 통증",
            visited = false,
            clinic = null,
            writtenOn = LocalDate.of(2026, 9, 20),
        ),
        CardListItem(
            id = "102",
            title = "두통 · 잦은 어지러움",
            visited = false,
            clinic = null,
            writtenOn = LocalDate.of(2026, 8, 21),
        ),
    )
