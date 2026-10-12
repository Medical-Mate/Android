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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** 일정 추가를 말로 하는 것(#282). 화면의 칸과 버튼을 모두 말로 부른다. */
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

    // 말 풀이

    @Test
    fun `할 일로 시작하면 그 뒤가 더할 할 일이다`() {
        assertEquals(ScheduleAddVoice.TodoAdd("복용 중인 약 챙기기"), parseScheduleAddVoice("할 일 추가, 복용 중인 약 챙기기"))
        assertEquals(ScheduleAddVoice.TodoAdd("약 챙기기"), parseScheduleAddVoice("할일 추가해 줘 약 챙기기."))
        // 할 일 속의 시각이 진료 시각이 되면 안 된다.
        assertEquals(ScheduleAddVoice.TodoAdd("10시 전에 금식"), parseScheduleAddVoice("할 일 10시 전에 금식"))
        assertEquals(ScheduleAddVoice.TodoAdd(""), parseScheduleAddVoice("할 일 추가"))
    }

    @Test
    fun `할 일을 체크하고 풀고 지우고 고친다`() {
        assertEquals(ScheduleAddVoice.TodoCheck(0, "첫 번째 할 일 완료", true), parseScheduleAddVoice("첫 번째 할 일 완료"))
        assertEquals(
            ScheduleAddVoice.TodoCheck(0, "첫 번째 할 일 완료 취소", false),
            parseScheduleAddVoice("첫 번째 할 일 완료 취소"),
        )
        assertEquals(ScheduleAddVoice.TodoDelete(1, "두 번째 할 일 지워 줘"), parseScheduleAddVoice("두 번째 할 일 지워 줘"))
        assertEquals(ScheduleAddVoice.TodoEdit(0, "약 챙기기"), parseScheduleAddVoice("첫 번째 할 일 고쳐, 약 챙기기"))
    }

    @Test
    fun `카드라는 말이 있으면 고르거나 빼거나 새로 만든다`() {
        assertEquals(ScheduleAddVoice.CardPick(1, "두 번째 카드"), parseScheduleAddVoice("두 번째 카드"))
        assertEquals(ScheduleAddVoice.CardDrop(null, "두통 카드 빼 줘"), parseScheduleAddVoice("두통 카드 빼 줘"))
        assertEquals(ScheduleAddVoice.CardNew, parseScheduleAddVoice("새 카드 만들래"))
    }

    @Test
    fun `화면 조작은 그 말로 시작할 때만이다`() {
        assertEquals(ScheduleAddVoice.Exit, parseScheduleAddVoice("나갈래"))
        assertEquals(ScheduleAddVoice.OpenHospital, parseScheduleAddVoice("병원 바꿀래"))
        assertEquals(ScheduleAddVoice.OpenSheet(ScheduleAddSheet.DATE), parseScheduleAddVoice("날짜 고를래"))
        assertEquals(ScheduleAddVoice.OpenSheet(ScheduleAddSheet.TIME), parseScheduleAddVoice("시간 선택"))
        assertEquals(ScheduleAddVoice.ClearTime, parseScheduleAddVoice("시간 미정이야"))
        // 병원 이름이 들어 있으면 찾기만 여는 것이 아니라 그 이름으로 읽는다.
        assertEquals(ScheduleAddVoice.Fill("고려튼튼병원으로 바꿔 줘"), parseScheduleAddVoice("고려튼튼병원으로 바꿔 줘"))
    }

    @Test
    fun `날짜 없이 저장하라고 하면 저장이다`() {
        assertEquals(ScheduleAddVoice.Save, parseScheduleAddVoice("저장해 줘"))
        // 날짜가 섞이면 날짜를 고치는 말이 먼저다.
        assertEquals(ScheduleAddVoice.Fill("20일로 저장해 줘"), parseScheduleAddVoice("20일로 저장해 줘"))
    }

    @Test
    fun `카드는 몇 번째로도 제목의 낱말로도 고른다`() {
        val cards = listOf(pick("1", "복부 통증 · 3주"), pick("2", "두통 · 잦은 어지러움"))

        assertEquals("2", cards.byVoice(1, "")?.id)
        assertEquals("2", cards.byVoice(null, "두통 카드")?.id)
        assertEquals("1", cards.byVoice(null, "복부 통증 카드 가져갈게")?.id)
        // "3주"의 숫자나 "·"로는 고르지 않는다.
        assertNull(cards.byVoice(null, "3주 카드"))
        assertNull(cards.byVoice(5, ""))
    }

    // 화면 동작

    @Test
    fun `날짜와 시간과 병원을 말하면 칸을 채우고 병원 찾기를 연다`() = runTest {
        val viewModel = viewModel(SharedAppointment("고려튼튼통증의학과", OCT_20, THREE_PM))

        val reply = viewModel.voice.handle("10월 20일 오후 3시 고려튼튼통증의학과", ScheduleAddVoiceRoutes())

        val state = viewModel.uiState.value
        assertEquals(OCT_20, state.date)
        assertEquals(THREE_PM, state.time)
        assertEquals("고려튼튼통증의학과", state.hospital)
        assertEquals("고려튼튼통증의학과", state.hospitalSearch)
        assertEquals(R.string.voice_schedule_filled_hospital, (reply as VoiceReply.Done).message)
    }

    @Test
    fun `말로 고친 날짜는 이미 있던 날짜를 덮고 열린 시트를 닫는다`() = runTest {
        // 예약 문자는 빈 칸만 채웠다. 말로 고친 것은 사람이 나중에 고른 것이다.
        val viewModel = viewModel(SharedAppointment(date = OCT_20))
        viewModel.onDateConfirm(LocalDate.of(2026, 10, 15))
        viewModel.onSheetOpen(ScheduleAddSheet.DATE)

        viewModel.voice.handle("20일로 바꿔 줘", ScheduleAddVoiceRoutes())

        assertEquals(OCT_20, viewModel.uiState.value.date)
        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
    }

    @Test
    fun `읽을 것이 없는 말은 이 화면의 말이 아니다`() = runTest {
        // "뒤로", "홈" 같은 전역 명령이 이 길로 간다.
        assertEquals(VoiceReply.NotMine, viewModel().voice.handle("뒤로 가", ScheduleAddVoiceRoutes()))
    }

    @Test
    fun `병원과 날짜가 있으면 저장하기 전에 묻고 네라고 하면 저장한다`() = runTest {
        val viewModel = viewModel()
        viewModel.onHospitalPicked("고려튼튼통증의학과의원")
        viewModel.onDateConfirm(OCT_20)
        var saved = 0

        val reply = viewModel.voice.handle("저장해 줘", ScheduleAddVoiceRoutes(save = { saved++ }))

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

        val reply = viewModel.voice.handle("저장해 줘", ScheduleAddVoiceRoutes())

        assertEquals(VoiceReply.Done(R.string.voice_schedule_missing), reply)
        assertTrue(viewModel.uiState.value.showErrors)
    }

    @Test
    fun `적은 것이 없으면 바로 나가고 있으면 버려도 되는지 묻는다`() = runTest {
        var exits = 0
        val routes = ScheduleAddVoiceRoutes(exit = { exits++ })
        val empty = viewModel()

        empty.voice.handle("나갈래", routes)
        assertEquals(1, exits)

        val written = viewModel()
        written.onDateConfirm(OCT_20)
        val reply = written.voice.handle("나갈래", routes)
        assertEquals(1, exits)
        (reply as VoiceReply.Confirm).onConfirm()
        assertEquals(2, exits)
    }

    @Test
    fun `병원 찾기와 새 카드는 버튼과 같은 길로 간다`() = runTest {
        var hospital = 0
        var card = 0
        val routes = ScheduleAddVoiceRoutes(openHospital = { hospital++ }, newCard = { card++ })
        val viewModel = viewModel()

        viewModel.voice.handle("병원 바꿀래", routes)
        viewModel.voice.handle("새 카드 만들래", routes)

        assertEquals(1, hospital)
        assertEquals(1, card)
    }

    @Test
    fun `날짜와 시간 시트를 열고 시간을 비운다`() = runTest {
        val viewModel = viewModel()
        viewModel.onTimeConfirm(THREE_PM)

        viewModel.voice.handle("날짜 고를래", ScheduleAddVoiceRoutes())
        assertEquals(ScheduleAddSheet.DATE, viewModel.uiState.value.sheet)

        viewModel.voice.handle("시간 미정", ScheduleAddVoiceRoutes())
        assertNull(viewModel.uiState.value.time)
        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
    }

    @Test
    fun `할 일을 더하고 체크하고 풀고 고치고 지운다`() = runTest {
        val viewModel = viewModel()
        val routes = ScheduleAddVoiceRoutes()

        viewModel.voice.handle("할 일 추가, 복용 중인 약 챙기기", routes)
        viewModel.voice.handle("할 일 추가, 공복 유지", routes)
        assertEquals(listOf("복용 중인 약 챙기기", "공복 유지"), viewModel.uiState.value.todos.map { it.label })

        viewModel.voice.handle("약 챙기기 할 일 완료", routes)
        assertTrue(viewModel.uiState.value.todos[0].done)
        viewModel.voice.handle("첫 번째 할 일 완료 취소", routes)
        assertFalse(viewModel.uiState.value.todos[0].done)

        viewModel.voice.handle("두 번째 할 일 고쳐, 8시간 공복", routes)
        assertEquals("8시간 공복", viewModel.uiState.value.todos[1].label)

        viewModel.voice.handle("첫 번째 할 일 지워 줘", routes)
        assertEquals(listOf("8시간 공복"), viewModel.uiState.value.todos.map { it.label })
    }

    @Test
    fun `무엇을 적을지 없이 할 일 추가라고 하면 묻기만 한다`() = runTest {
        // 빈 줄을 만들어 두면 저장할 때 걸러야 한다.
        val viewModel = viewModel()

        val reply = viewModel.voice.handle("할 일 추가", ScheduleAddVoiceRoutes())

        assertEquals(VoiceReply.Done(R.string.voice_schedule_todo_ask), reply)
        assertTrue(viewModel.uiState.value.todos.isEmpty())
    }

    @Test
    fun `카드를 말하면 고르고 그 카드의 병원을 채우고 빼라고 하면 뺀다`() = runTest {
        val viewModel = viewModel(cards = listOf(card("7", "오른쪽 어깨 통증", clinic = "서울OO정형외과")))
        viewModel.load()

        viewModel.voice.handle("어깨 카드 가져갈게", ScheduleAddVoiceRoutes())
        assertTrue(viewModel.uiState.value.cards.single().picked)
        assertEquals("서울OO정형외과", viewModel.uiState.value.hospital)

        // 골라 둔 카드가 한 장이면 무엇을 뺄지 말하지 않아도 된다.
        viewModel.voice.handle("카드 빼 줘", ScheduleAddVoiceRoutes())
        assertFalse(viewModel.uiState.value.cards.single().picked)
    }

    private fun viewModel(read: SharedAppointment = SharedAppointment(), cards: List<CardListItem> = emptyList()) =
        ScheduleAddViewModel(
            RecordingAppointmentRepository(),
            FakeCardRepository(list = ApiResult.Success(cards)),
            FakeTextReader(read),
        )

    private fun pick(id: String, title: String) = ScheduleAddCard(id = id, title = title, meta = "")

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
