package com.mist.medicalmate.card.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.card.data.CardListItem
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 말로 브리핑 카드를 찾아 연다(#286). */
@OptIn(ExperimentalCoroutinesApi::class)
class BriefCardVoiceTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `몇 번째나 제목의 낱말이나 병원으로 찾는다`() {
        assertEquals(listOf("2"), CARDS.findByVoice("두 번째 카드 열어 줘", TODAY)?.map { it.id })
        assertEquals(listOf("3"), CARDS.findByVoice("마지막 거", TODAY)?.map { it.id })
        // 최근 카드는 맨 위다. 새로 만들자는 말은 찾는 말이 아니라 전역 명령(증상 정리)이다(#320).
        assertEquals(listOf("1"), CARDS.findByVoice("최근 카드 보여 줘", TODAY)?.map { it.id })
        assertEquals(null, CARDS.findByVoice("새 카드 만들래", TODAY))
        assertEquals(listOf("1", "3"), CARDS.findByVoice("두통 카드", TODAY)?.map { it.id })
        assertEquals(listOf("2"), CARDS.findByVoice("오른쪽 어깨 카드 보여 줘", TODAY)?.map { it.id })
        assertEquals(listOf("1"), CARDS.findByVoice("고려튼튼 거", TODAY)?.map { it.id })
    }

    @Test
    fun `쓴 날이나 달로 찾고 다른 단서와 함께 좁힌다`() {
        assertEquals(listOf("1"), CARDS.findByVoice("9월 4일 카드", TODAY)?.map { it.id })
        assertEquals(listOf("2"), CARDS.findByVoice("어제 쓴 카드", TODAY)?.map { it.id })
        assertEquals(listOf("1"), CARDS.findByVoice("지난달 두통 카드", TODAY)?.map { it.id })
        assertEquals(listOf("3"), CARDS.findByVoice("두통 카드 중에 두 번째", TODAY)?.map { it.id })
    }

    @Test
    fun `단서가 없으면 찾으라는 말이 아니다`() {
        assertNull(CARDS.findByVoice("브리핑 카드 보여 줘", TODAY))
        assertEquals(emptyList<String>(), CARDS.findByVoice("무릎 카드", TODAY)?.map { it.id })
    }

    @Test
    fun `하나면 열고 여럿이면 후보를 적어 묻고 다음 말로 그중에서 고른다`() = runTest {
        val viewModel = BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(CARDS)))
        viewModel.load()
        val opened = mutableListOf<String>()

        val asked = viewModel.onVoice("두통 카드", TODAY) { opened += it } as VoiceReply.Done
        assertEquals(R.string.voice_card_which, asked.message)
        assertEquals(listOf("· 두통 · 어지러움 (9.4)\n· 두통 재발 (10.10)"), asked.question?.args)

        viewModel.onVoice("두 번째", TODAY) { opened += it }
        assertEquals(listOf("3"), opened)
    }

    @Test
    fun `없으면 알리고 단서가 없는 말은 넘긴다`() = runTest {
        val viewModel = BriefCardListViewModel(FakeCardRepository(list = ApiResult.Success(CARDS)))
        viewModel.load()

        val none = viewModel.onVoice("무릎 카드", TODAY) {} as VoiceReply.Done
        assertEquals(R.string.voice_card_none, none.message)
        assertEquals(VoiceReply.NotMine, viewModel.onVoice("홈으로 가", TODAY) {})
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 13)

        val CARDS =
            listOf(
                CardListItem("1", "두통 · 어지러움", true, true, "고려튼튼통증의학과", LocalDate.of(2026, 9, 4)),
                CardListItem("2", "오른쪽 어깨 통증", true, false, null, LocalDate.of(2026, 10, 12)),
                CardListItem("3", "두통 재발", false, false, "서울내과", LocalDate.of(2026, 10, 10)),
            )
    }
}
