package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.calendar.data.AppointmentOrigin
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.ui.FakeCardRepository
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 진료를 마친 카드로 잡은 일정은 재진이다(#272). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleAddRevisitTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `진료를 마친 카드를 고르면 재진으로 저장한다`() {
        // 같은 카드로 진료 후 기록을 남긴 뒤 다시 잡은 일정이다. 기록 상세가 재방문으로 적는 진료를
        // 일자 화면과 홈이 초진으로 적었다.
        val repository = RecordingAppointmentRepository()
        val viewModel = filled(repository)

        viewModel.onCardPickChange(VISITED.id, true)
        viewModel.onSaveClick {}

        assertEquals(AppointmentOrigin.VISIT_FOLLOW_UP, repository.createdOrigin)
    }

    @Test
    fun `아직 진료를 받지 않은 카드는 초진이다`() {
        val repository = RecordingAppointmentRepository()
        val viewModel = filled(repository)

        viewModel.onCardPickChange(FRESH.id, true)
        viewModel.onSaveClick {}

        assertEquals(AppointmentOrigin.MANUAL, repository.createdOrigin)
    }

    @Test
    fun `카드를 고르지 않으면 초진이다`() {
        val repository = RecordingAppointmentRepository()

        filled(repository).onSaveClick {}

        assertEquals(AppointmentOrigin.MANUAL, repository.createdOrigin)
    }

    @Test
    fun `진료를 마친 카드로 들어오면 미리 골라진 채 재진이다`() {
        // 캘린더의 "이 카드로 일정 만들기"로 들어온 길이다.
        val repository = RecordingAppointmentRepository()
        val viewModel =
            ScheduleAddViewModel(repository, FakeCardRepository(list = ApiResult.Success(CARDS)), FakeTextReader())
                .apply {
                    load(cardId = VISITED.id)
                    onDateConfirm(LocalDate.of(2026, 10, 26))
                }

        viewModel.onSaveClick {}

        assertEquals(AppointmentOrigin.VISIT_FOLLOW_UP, repository.createdOrigin)
    }

    private fun filled(repository: RecordingAppointmentRepository) =
        ScheduleAddViewModel(repository, FakeCardRepository(list = ApiResult.Success(CARDS)), FakeTextReader())
            .apply {
                load()
                onHospitalPicked("서울OO병원 내과")
                onDateConfirm(LocalDate.of(2026, 10, 26))
            }

    private companion object {
        val VISITED =
            CardListItem(
                id = "1",
                title = "복부 통증 · 3주",
                confirmed = true,
                visited = true,
                clinic = "서울OO병원 내과",
                writtenOn = LocalDate.of(2026, 9, 4),
            )
        val FRESH = VISITED.copy(id = "2", title = "두통", visited = false)
        val CARDS = listOf(VISITED, FRESH)
    }
}
