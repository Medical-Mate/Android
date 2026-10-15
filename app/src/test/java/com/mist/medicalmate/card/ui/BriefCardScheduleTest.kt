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
    fun `저장하지 않은 카드는 저장하기다`() {
        // 일정이 있어도 먼저 저장해야 한다. 확정하지 않은 카드에는 진료 후 기록을 붙일 수 없다.
        val card = testCard.copy(appointment = BriefCardAppointment("7", TODAY))

        assertEquals(BriefCardFooterAction.SAVE, card.footerAction(TODAY))
    }

    @Test
    fun `저장했고 일정이 없으면 진료 일정 잡기다`() {
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
    fun `처음 저장하면 화면에 남고 일정 시트가 뜬다`() {
        // 전에는 저장하기가 곧 홈으로 나가기였다. 시안 1e-1-S는 저장한 카드 위에 시트를 띄운다.
        val viewModel = BriefCardViewModel(FakeCardRepository()).apply { open(1) }

        viewModel.onSaveClick()

        val content = viewModel.content()
        assertEquals(BriefCard.Status.CONFIRMED, content.card.status)
        assertTrue(content.scheduleSheet)
    }

    @Test
    fun `그 카드로 잡은 일정이 이미 있으면 시트를 띄우지 않는다`() {
        val repository = FakeCardRepository(card = testCard.copy(appointment = BriefCardAppointment("7", TODAY)))
        val viewModel = BriefCardViewModel(repository).apply { open(1) }

        viewModel.onSaveClick()

        assertFalse(viewModel.content().scheduleSheet)
    }

    @Test
    fun `아직 안 정했다고 하면 시트만 닫는다`() {
        val viewModel = BriefCardViewModel(FakeCardRepository()).apply { open(1) }
        viewModel.onSaveClick()

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

    private fun saved() = testCard.copy(status = BriefCard.Status.CONFIRMED)

    private fun BriefCardViewModel.content(): BriefCardUiState.Content = uiState.value as BriefCardUiState.Content

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
    }
}
