package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.calendar.share.SharedAppointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.ui.FakeCardRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** 일정 추가를 말로 하는 것(#282). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleAddVoiceTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `할 일로 시작하면 그 뒤가 할 일이다`() {
        assertEquals(ScheduleAddVoice.Todo("복용 중인 약 챙기기"), parseScheduleAddVoice("할 일 추가, 복용 중인 약 챙기기"))
        assertEquals(ScheduleAddVoice.Todo("약 챙기기"), parseScheduleAddVoice("할일 추가해 줘 약 챙기기."))
        // 할 일 속의 시각이 진료 시각이 되면 안 된다.
        assertEquals(ScheduleAddVoice.Todo("10시 전에 금식"), parseScheduleAddVoice("할 일 10시 전에 금식"))
        assertNull(parseScheduleAddVoice("할 일 추가"))
    }

    @Test
    fun `카드라는 말이 있으면 카드를 고른다`() {
        assertEquals(ScheduleAddVoice.Card(1, "두 번째 카드"), parseScheduleAddVoice("두 번째 카드"))
        assertEquals(ScheduleAddVoice.Card(null, "두통 카드 가져갈게"), parseScheduleAddVoice("두통 카드 가져갈게"))
    }

    @Test
    fun `날짜 없이 저장하라고 하면 저장이다`() {
        assertEquals(ScheduleAddVoice.Save, parseScheduleAddVoice("저장해 줘"))
        // 날짜가 섞이면 날짜를 고치는 말이 먼저다.
        assertEquals(ScheduleAddVoice.Fill("20일로 저장해 줘"), parseScheduleAddVoice("20일로 저장해 줘"))
    }

    @Test
    fun `카드는 몇 번째로도 제목의 낱말로도 고른다`() {
        val cards =
            listOf(
                ScheduleAddCard(id = "1", title = "복부 통증 · 3주", meta = ""),
                ScheduleAddCard(id = "2", title = "두통 · 잦은 어지러움", meta = ""),
            )

        assertEquals("2", cards.pickByVoice(ScheduleAddVoice.Card(1, ""))?.id)
        assertEquals("2", cards.pickByVoice(ScheduleAddVoice.Card(null, "두통 카드"))?.id)
        assertEquals("1", cards.pickByVoice(ScheduleAddVoice.Card(null, "복부 통증 카드 가져갈게"))?.id)
        // "3주"의 숫자나 "·"로는 고르지 않는다.
        assertNull(cards.pickByVoice(ScheduleAddVoice.Card(null, "3주 카드")))
        assertNull(cards.pickByVoice(ScheduleAddVoice.Card(5, "")))
    }

    @Test
    fun `날짜와 시간과 병원을 말하면 칸을 채우고 병원 찾기를 연다`() = runTest {
        val viewModel = viewModel(SharedAppointment("고려튼튼통증의학과", OCT_20, THREE_PM))

        val reply = viewModel.voice.handle("10월 20일 오후 3시 고려튼튼통증의학과") {}

        val state = viewModel.uiState.value
        assertEquals(OCT_20, state.date)
        assertEquals(THREE_PM, state.time)
        assertEquals("고려튼튼통증의학과", state.hospital)
        assertEquals("고려튼튼통증의학과", state.hospitalSearch)
        assertEquals(R.string.voice_schedule_filled_hospital, (reply as VoiceReply.Done).message)
    }

    @Test
    fun `말로 고친 날짜는 이미 있던 날짜를 덮는다`() = runTest {
        // 예약 문자는 빈 칸만 채웠다. 말로 고친 것은 사람이 나중에 고른 것이다.
        val viewModel = viewModel(SharedAppointment(date = OCT_20))
        viewModel.onDateConfirm(LocalDate.of(2026, 10, 15))

        viewModel.voice.handle("20일로 바꿔 줘") {}

        assertEquals(OCT_20, viewModel.uiState.value.date)
    }

    @Test
    fun `읽을 것이 없는 말은 이 화면의 말이 아니다`() = runTest {
        // "뒤로", "홈" 같은 전역 명령이 이 길로 간다.
        assertEquals(VoiceReply.NotMine, viewModel().voice.handle("뒤로 가") {})
    }

    @Test
    fun `병원과 날짜가 있으면 저장하기 전에 묻고 네라고 하면 저장한다`() = runTest {
        val viewModel = viewModel()
        viewModel.onHospitalPicked("고려튼튼통증의학과의원")
        viewModel.onDateConfirm(OCT_20)
        var saved = 0

        val reply = viewModel.voice.handle("저장해 줘") { saved++ }

        val confirm = reply as VoiceReply.Confirm
        assertEquals(R.string.voice_schedule_confirm_save, confirm.prompt)
        assertTrue(confirm.args.single().contains("고려튼튼통증의학과의원"))
        assertEquals(0, saved)
        confirm.onConfirm()
        assertEquals(1, saved)
    }

    @Test
    fun `빠진 칸이 있으면 저장을 묻지 않고 그 칸을 알린다`() = runTest {
        val viewModel = viewModel()

        val reply = viewModel.voice.handle("저장해 줘") {}

        assertEquals(VoiceReply.Done(R.string.voice_schedule_missing), reply)
        assertTrue(viewModel.uiState.value.showErrors)
    }

    @Test
    fun `할 일을 말하면 한 줄이 더해진다`() = runTest {
        val viewModel = viewModel()

        viewModel.voice.handle("할 일 추가, 복용 중인 약 챙기기") {}

        assertEquals(listOf("복용 중인 약 챙기기"), viewModel.uiState.value.todos.map { it.label })
    }

    @Test
    fun `카드를 말하면 고르고 그 카드의 병원을 채운다`() = runTest {
        val viewModel = viewModel(cards = listOf(card("7", "오른쪽 어깨 통증", clinic = "서울OO정형외과")))
        viewModel.load()

        viewModel.voice.handle("어깨 카드 가져갈게") {}

        val state = viewModel.uiState.value
        assertTrue(state.cards.single().picked)
        assertEquals("서울OO정형외과", state.hospital)
    }

    private fun viewModel(read: SharedAppointment = SharedAppointment(), cards: List<CardListItem> = emptyList()) =
        ScheduleAddViewModel(
            RecordingAppointmentRepository(),
            FakeCardRepository(list = ApiResult.Success(cards)),
            FakeTextReader(read),
        )

    private fun card(id: String, title: String, clinic: String? = null) = CardListItem(
        id = id,
        title = title,
        confirmed = true,
        visited = false,
        clinic = clinic,
        writtenOn = LocalDate.of(2026, 10, 1),
    )

    private companion object {
        val OCT_20: LocalDate = LocalDate.of(2026, 10, 20)
        val THREE_PM: LocalTime = LocalTime.of(15, 0)
    }
}
