package com.mist.medicalmate.card.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 브리핑 카드의 하단 버튼과 저장 직후 일정 시트(#314). 시안 1e-1 네 장과 1e-1-S다. */
@OptIn(ExperimentalCoroutinesApi::class)
class BriefCardScheduleTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `일정이 없으면 진료 일정 잡기다`() {
        // 저장하기는 없다(#338). 카드는 만들 때부터 확정돼 있다(Backend#181).
        assertEquals(BriefCardFooterAction.SCHEDULE, saved().footerAction(TODAY))
    }

    @Test
    fun `앞날 일정이면 진료 일정 보기다`() {
        val card = saved().copy(appointment = BriefCardAppointment("7", TODAY.plusDays(1)))

        assertEquals(BriefCardFooterAction.VIEW_SCHEDULE, card.footerAction(TODAY))
    }

    @Test
    fun `당일이거나 지났으면 진료 후 기록하기다`() {
        // 당일은 진료를 다녀왔을 수 있다. 시안 1e-1 "진료 지남"과 같은 버튼이다.
        val today = saved().copy(appointment = BriefCardAppointment("7", TODAY))
        val past = saved().copy(appointment = BriefCardAppointment("7", TODAY.minusDays(3)))

        assertEquals(BriefCardFooterAction.RECORD_VISIT, today.footerAction(TODAY))
        assertEquals(BriefCardFooterAction.RECORD_VISIT, past.footerAction(TODAY))
    }

    @Test
    fun `문답에서 막 만든 카드면 일정 시트가 뜬다`() {
        // 전에는 처음 저장하기를 누를 때 떴다. 저장 단계가 없어져 카드를 처음 여는 이때로 옮겼다(#338).
        val viewModel = BriefCardViewModel(FakeCardRepository()).apply { open(cardId = null, sessionId = 3) }

        assertTrue(viewModel.content().scheduleSheet)
    }

    @Test
    fun `이미 있는 카드를 열면 시트를 띄우지 않는다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository()).apply { open(1) }

        assertFalse(viewModel.content().scheduleSheet)
    }

    @Test
    fun `그 카드로 잡은 일정이 이미 있으면 시트를 띄우지 않는다`() {
        val repository = FakeCardRepository(card = testCard.copy(appointment = BriefCardAppointment("7", TODAY)))
        val viewModel = BriefCardViewModel(repository).apply { open(cardId = null, sessionId = 3) }

        assertFalse(viewModel.content().scheduleSheet)
    }

    @Test
    fun `아직 안 정했다고 하면 시트만 닫는다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository()).apply { open(cardId = null, sessionId = 3) }

        viewModel.onOverlayDismiss()

        val content = viewModel.content()
        assertFalse(content.scheduleSheet)
        assertEquals(BriefCardFooterAction.SCHEDULE, content.card.footerAction(TODAY))
    }

    @Test
    fun `돌아오면 그 카드로 잡은 일정을 다시 읽는다`() {
        // 하단의 진료 일정 잡기로 일정을 잡고 오면 진료 일정 보기로 바뀌어야 한다.
        val repository = FakeCardRepository(card = saved())
        val viewModel = BriefCardViewModel(repository).apply { open(1) }
        repository.card = saved().copy(appointment = BriefCardAppointment("7", TODAY.plusDays(2)))

        viewModel.refreshSchedule()

        assertEquals(BriefCardFooterAction.VIEW_SCHEDULE, viewModel.content().card.footerAction(TODAY))
    }

    @Test
    fun `고치는 중에는 다시 읽지 않는다`() {
        val repository = FakeCardRepository(card = saved())
        val viewModel = BriefCardViewModel(repository).apply { open(1) }
        viewModel.onEditClick()
        repository.card = saved().copy(appointment = BriefCardAppointment("7", TODAY))

        viewModel.refreshSchedule()

        val content = viewModel.content()
        assertTrue(content.editing)
        assertEquals(null, content.card.appointment)
    }

    private fun saved() = testCard

    private fun BriefCardViewModel.content(): BriefCardUiState.Content = uiState.value as BriefCardUiState.Content

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
    }
}
