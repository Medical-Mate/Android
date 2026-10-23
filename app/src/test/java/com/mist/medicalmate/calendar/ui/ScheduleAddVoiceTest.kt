package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.calendar.share.SharedAppointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.ui.FakeCardRepository
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun `할 일을 다른 말로 가리켜도 알아듣는다`() {
        // "할 일" 대신 준비물·챙길 거·체크리스트라고 한다.
        assertEquals(ScheduleAddVoice.TodoAdd("신분증"), parseScheduleAddVoice("준비물에 신분증 넣어 줘"))
        assertEquals(ScheduleAddVoice.TodoAdd("보험 카드 사본"), parseScheduleAddVoice("챙길 거 보험 카드 사본 추가"))
        assertEquals(ScheduleAddVoice.TodoAdd("물 마시지 않기"), parseScheduleAddVoice("체크리스트에 물 마시지 않기 적어 줘"))
        assertEquals(ScheduleAddVoice.TodoAdd("신분증 챙기기"), parseScheduleAddVoice("신분증 챙기기 할 일로 추가해 줘"))
    }

    @Test
    fun `더하라는 말이 있으면 글 속의 빼기나 체크에 끌려가지 않는다`() {
        assertEquals(ScheduleAddVoice.TodoAdd("약 빼먹지 않기"), parseScheduleAddVoice("할 일 추가, 약 빼먹지 않기"))
        assertEquals(ScheduleAddVoice.TodoAdd("혈압 체크하기"), parseScheduleAddVoice("할 일에 혈압 체크하기 넣어 줘"))
    }

    @Test
    fun `할 일 체크와 풀기와 지우기를 여러 말로 한다`() {
        assertEquals(true, (parseScheduleAddVoice("첫째 할 일 다 했어") as ScheduleAddVoice.TodoCheck).done)
        assertEquals(true, (parseScheduleAddVoice("준비물 신분증 챙겼어") as ScheduleAddVoice.TodoCheck).done)
        assertEquals(false, (parseScheduleAddVoice("두 번째 할 일 아직 안 했어") as ScheduleAddVoice.TodoCheck).done)
        assertEquals(false, (parseScheduleAddVoice("맨 위 할 일 체크 풀어 줘") as ScheduleAddVoice.TodoCheck).done)
        assertEquals(
            ScheduleAddVoice.TodoDelete(VoiceWords.LAST, "마지막 할 일 없애 줘"),
            parseScheduleAddVoice("마지막 할 일 없애 줘"),
        )
        assertEquals(ScheduleAddVoice.TodoDelete(0, "첫째 준비물 삭제"), parseScheduleAddVoice("첫째 준비물 삭제"))
    }

    @Test
    fun `고칠 글이 고치라는 말 앞에 와도 읽는다`() {
        assertEquals(ScheduleAddVoice.TodoEdit(1, "8시간 공복"), parseScheduleAddVoice("두 번째 할 일을 8시간 공복으로 바꿔 줘"))
        assertEquals(ScheduleAddVoice.TodoEdit(0, "신분증 챙기기"), parseScheduleAddVoice("첫 번째 할 일 신분증 챙기기로 수정해 줘"))
        // 무엇으로 고칠지 없으면 글이 빈 채로 가고 화면이 묻는다.
        assertEquals(ScheduleAddVoice.TodoEdit(0, ""), parseScheduleAddVoice("첫 번째 할 일 바꿔 줘"))
    }

    @Test
    fun `카드를 다른 말로 가리켜도 알아듣는다`() {
        assertEquals(ScheduleAddVoice.CardPick(null, "두통 브리핑 가져갈게"), parseScheduleAddVoice("두통 브리핑 가져갈게"))
        assertEquals(ScheduleAddVoice.CardDrop(null, "카드는 필요 없어"), parseScheduleAddVoice("카드는 필요 없어"))
        assertEquals(ScheduleAddVoice.CardDrop(0, "첫째 카드 제외해 줘"), parseScheduleAddVoice("첫째 카드 제외해 줘"))
        assertEquals(ScheduleAddVoice.CardNew, parseScheduleAddVoice("브리핑 카드 새로 작성할래"))
    }

    @Test
    fun `화면 조작을 여러 말로 한다`() {
        listOf("그냥 나갈게", "작성 취소", "일정 등록 안 할래", "다음에 할래", "화면 닫아 줘").forEach {
            assertEquals(it, ScheduleAddVoice.Exit, parseScheduleAddVoice(it))
        }
        listOf("병원 다시 고를래", "병원을 변경할래", "다른 병원으로 할래", "병원 검색").forEach {
            assertEquals(it, ScheduleAddVoice.OpenHospital, parseScheduleAddVoice(it))
        }
        listOf("날짜 바꿀래", "날짜를 다시 정할래", "언제 갈지 고를래").forEach {
            assertEquals(it, ScheduleAddVoice.OpenSheet(ScheduleAddSheet.DATE), parseScheduleAddVoice(it))
        }
        listOf("시간 바꿀래", "몇 시인지 정할래").forEach {
            assertEquals(it, ScheduleAddVoice.OpenSheet(ScheduleAddSheet.TIME), parseScheduleAddVoice(it))
        }
        listOf("시간은 아직 몰라", "몇 시인지 모르겠어", "시간 안 정했어", "시간 지워 줘").forEach {
            assertEquals(it, ScheduleAddVoice.ClearTime, parseScheduleAddVoice(it))
        }
        listOf("일정 잡아 줘", "이걸로 할게", "마무리해 줘", "등록할래").forEach {
            assertEquals(it, ScheduleAddVoice.Save, parseScheduleAddVoice(it))
        }
    }

    @Test
    fun `날짜나 시각이 들어 있으면 시트를 여는 대신 채운다`() {
        assertEquals(ScheduleAddVoice.Fill("날짜 20일로 바꿔 줘"), parseScheduleAddVoice("날짜 20일로 바꿔 줘"))
        assertEquals(ScheduleAddVoice.Fill("시간은 오후 2시로 해 줘"), parseScheduleAddVoice("시간은 오후 2시로 해 줘"))
        assertEquals(ScheduleAddVoice.Fill("내일로 할래"), parseScheduleAddVoice("내일로 할래"))
    }

    @Test
    fun `받아쓴 시각의 우리말 수를 숫자로 바꾼다`() {
        assertEquals("오후 3시로 해 줘", spokenNumbers("오후 세 시로 해 줘"))
        assertEquals("2시 반", spokenNumbers("두 시 반"))
        assertEquals("오전 11시", spokenNumbers("오전 열한 시"))
        assertEquals("오후3시", spokenNumbers("오후세시"))
        assertEquals("12시에", spokenNumbers("정오에"))
        // 시각이 아닌 말은 그대로 둔다.
        assertEquals("한 시간 뒤", spokenNumbers("한 시간 뒤"))
        assertEquals("적어 두시면 돼", spokenNumbers("적어 두시면 돼"))
    }

    @Test
    fun `말에 날짜가 있는지 본다`() {
        assertTrue(mentionsDay("10월 20일 오후 3시"))
        assertTrue(mentionsDay("20일로"))
        assertTrue(mentionsDay("다음 주 화요일"))
        assertTrue(mentionsDay("10/20"))
        assertFalse(mentionsDay("오후 3시로 해 줘"))
        assertFalse(mentionsDay("3시 반"))
    }

    @Test
    fun `우리말 수로 받아쓴 시각도 시트를 여는 대신 채운다`() {
        assertEquals(ScheduleAddVoice.Fill("시간 세 시로 바꿔 줘"), parseScheduleAddVoice("시간 세 시로 바꿔 줘"))
        assertEquals(ScheduleAddVoice.Fill("두 시 반으로 해 줘"), parseScheduleAddVoice("두 시 반으로 해 줘"))
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
    fun `시각만 말하면 적어 둔 날짜는 그대로 두고 시간만 바꾼다`() = runTest {
        // 추출기는 날짜 없는 시각을 오늘로 읽어 돌려줄 수 있다.
        val viewModel = viewModel(SharedAppointment(date = LocalDate.of(2026, 10, 13), time = THREE_PM))
        viewModel.onDateConfirm(OCT_20)

        val reply = viewModel.voice.handle("오후 3시로 해 줘", ScheduleAddVoiceRoutes())

        assertEquals(OCT_20, viewModel.uiState.value.date)
        assertEquals(THREE_PM, viewModel.uiState.value.time)
        assertEquals(R.string.voice_schedule_filled, (reply as VoiceReply.Done).message)
    }

    @Test
    fun `추출기가 시각을 못 읽어도 말에서 읽는다`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateConfirm(OCT_20)

        viewModel.voice.handle("두 시 반으로 바꿔 줘", ScheduleAddVoiceRoutes())

        assertEquals(OCT_20, viewModel.uiState.value.date)
        assertEquals(LocalTime.of(14, 30), viewModel.uiState.value.time)
    }

    @Test
    fun `시트가 떠 있으면 뒤로는 시트만 닫는다`() = runTest {
        // 전에는 전역 "뒤로"로 가서 화면째 빠졌다(#334).
        val viewModel = viewModel().apply { onSheetOpen(ScheduleAddSheet.DATE) }

        val reply = viewModel.voice.handle("뒤로", ScheduleAddVoiceRoutes())

        assertEquals(ScheduleAddSheet.NONE, viewModel.uiState.value.sheet)
        assertEquals(R.string.voice_sheet_closed, (reply as VoiceReply.Done).message)
    }

    @Test
    fun `읽을 것이 없는 말은 이 화면의 말이 아니다`() = runTest {
        // "뒤로", "홈" 같은 전역 명령이 이 길로 간다.
        assertEquals(VoiceReply.NotMine, viewModel().voice.handle("뒤로 가", ScheduleAddVoiceRoutes()))
    }

    @Test
    fun `필수 칸이 차면 저장하기 전에 묻고 네라고 하면 저장한다`() = runTest {
        val viewModel = viewModel(cards = listOf(card("7", "오른쪽 어깨 통증")))
        viewModel.load()
        viewModel.onHospitalPicked("고려튼튼통증의학과의원")
        viewModel.onDateConfirm(OCT_20)
        viewModel.onCardPickChange("7", true)
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

        // 빠진 칸을 알리고 이어서 그 칸을 묻는다(#282).
        assertEquals(
            VoiceReply.Done(R.string.voice_schedule_missing, question = VoiceText(R.string.voice_ask_hospital)),
            reply,
        )
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

        assertEquals(R.string.voice_schedule_todo_ask, (reply as VoiceReply.Done).message)
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

    // 되묻기(#282)

    @Test
    fun `처리하고 나면 빠진 칸을 이어서 묻는다`() = runTest {
        val viewModel = viewModel(SharedAppointment(date = OCT_20))
        val routes = ScheduleAddVoiceRoutes()

        // 날짜만 채우면 병원을 묻는다.
        val first = viewModel.voice.handle("20일", routes) as VoiceReply.Done
        assertEquals(VoiceText(R.string.voice_ask_hospital), first.question)

        // 병원까지 차면 가져갈 카드를 묻는다(#312). 고를 카드가 없으면 새로 만들라고 한다.
        viewModel.onHospitalPicked("고려튼튼통증의학과의원")
        val second = viewModel.voice.handle("할 일 추가, 신분증", routes) as VoiceReply.Done
        assertEquals(VoiceText(R.string.voice_ask_card_new), second.question)
    }

    @Test
    fun `카드가 비면 고르라고 하고 고르면 저장하라고 한다`() {
        val cards = listOf(pick("1", "복부 통증 · 3주"))
        val state = ScheduleAddUiState(hospital = "고려튼튼통증의학과의원", date = OCT_20, cards = cards)

        assertEquals(VoiceText(R.string.voice_ask_card), state.nextQuestion())
        assertEquals(
            VoiceText(R.string.voice_ask_done),
            state.copy(cards = cards.map { it.copy(picked = true) }).nextQuestion(),
        )
    }

    @Test
    fun `병원이 있고 날짜가 없으면 날짜를 묻는다`() {
        assertEquals(
            VoiceText(R.string.voice_ask_date),
            ScheduleAddUiState(hospital = "고려튼튼통증의학과의원").nextQuestion(),
        )
    }

    @Test
    fun `병원 찾기로 떠나는 말에는 묻지 않는다`() = runTest {
        // 옮겨 간 병원 찾기가 묻는다. 여기서 "어느 병원인가요?"를 붙이면 두 물음이 겹친다.
        val viewModel = viewModel(SharedAppointment(hospital = "고려튼튼통증의학과"))

        val reply = viewModel.voice.handle("고려튼튼통증의학과", ScheduleAddVoiceRoutes()) as VoiceReply.Done

        assertNull(reply.question)
        assertTrue(viewModel.uiState.value.hospitalSearchByVoice)
    }

    @Test
    fun `말로 연 일정 추가에서 읽은 병원은 말로 찾은 것으로 표시한다`() = runTest {
        val viewModel = viewModel(SharedAppointment(hospital = "고려튼튼통증의학과", date = OCT_20))

        viewModel.openedByVoice = true
        viewModel.load(sharedText = "10월 20일 고려튼튼통증의학과 일정 추가해 줘")

        assertTrue(viewModel.uiState.value.hospitalSearchByVoice)
        viewModel.onHospitalSearchOpened()
        assertFalse(viewModel.uiState.value.hospitalSearchByVoice)
    }

    @Test
    fun `예약 문자에서 읽은 병원은 말로 찾은 것이 아니다`() = runTest {
        // 예약 문자는 지금처럼 사람이 주소를 보고 고른다(#270).
        val viewModel = viewModel(SharedAppointment(hospital = "고려튼튼통증의학과", date = OCT_20))

        viewModel.load(sharedText = "[고려튼튼통증의학과] 10월 20일 예약")

        assertFalse(viewModel.uiState.value.hospitalSearchByVoice)
    }

    @Test
    fun `맞는 카드가 여럿이면 고르지 않고 후보를 적어 되묻는다`() = runTest {
        val viewModel =
            viewModel(cards = listOf(card("1", "두통 · 어지러움"), card("2", "두통 · 메스꺼움")))
        viewModel.load()

        val reply = viewModel.voice.handle("두통 카드 가져갈게", ScheduleAddVoiceRoutes()) as VoiceReply.Done

        assertEquals(R.string.voice_schedule_card_which, reply.message)
        assertEquals(listOf("2"), reply.args)
        assertEquals(
            VoiceText(R.string.voice_schedule_card_ask_ordinal, listOf("· 두통 · 어지러움\n· 두통 · 메스꺼움")),
            reply.question,
        )
        assertTrue(viewModel.uiState.value.cards.none { it.picked })

        // 몇 번째로 답하면 고른다.
        viewModel.voice.handle("두 번째 카드", ScheduleAddVoiceRoutes())
        assertTrue(viewModel.uiState.value.cards[1].picked)
    }

    @Test
    fun `맞는 할 일이 여럿이면 되묻는다`() = runTest {
        val viewModel = viewModel()
        viewModel.voice.handle("할 일 추가, 약 챙기기", ScheduleAddVoiceRoutes())
        viewModel.voice.handle("할 일 추가, 약 봉투 챙기기", ScheduleAddVoiceRoutes())

        val reply = viewModel.voice.handle("챙기기 할 일 완료", ScheduleAddVoiceRoutes()) as VoiceReply.Done

        assertEquals(R.string.voice_schedule_todo_which, reply.message)
        assertTrue(viewModel.uiState.value.todos.none { it.done })
    }

    @Test
    fun `되물은 뒤 번호로 답하면 후보 안에서 센다`() = runTest {
        // 후보는 1번과 3번이다. 전에는 "두 번째"를 전체 목록에서 세어 후보가 아니던 2번을 골랐다(#346).
        val cards = listOf(card("1", "두통 · 어지러움"), card("2", "복부 통증"), card("3", "두통 재진"))
        val viewModel = viewModel(cards = cards)
        viewModel.load()
        viewModel.voice.handle("두통 카드 가져갈게", ScheduleAddVoiceRoutes())

        viewModel.voice.handle("두 번째", ScheduleAddVoiceRoutes())

        assertEquals(listOf("3"), viewModel.uiState.value.cards.filter { it.picked }.map { it.id })
    }

    @Test
    fun `되물은 할 일도 번호로 답하면 후보 안에서 센다`() = runTest {
        val viewModel = viewModel()
        viewModel.voice.handle("할 일 추가, 약 챙기기", ScheduleAddVoiceRoutes())
        viewModel.voice.handle("할 일 추가, 신분증", ScheduleAddVoiceRoutes())
        viewModel.voice.handle("할 일 추가, 약 봉투 챙기기", ScheduleAddVoiceRoutes())
        viewModel.voice.handle("챙기기 할 일 완료", ScheduleAddVoiceRoutes())

        viewModel.voice.handle("두 번째 할 일 완료", ScheduleAddVoiceRoutes())

        assertEquals(listOf("약 봉투 챙기기"), viewModel.uiState.value.todos.filter { it.done }.map { it.label })
    }

    @Test
    fun `고치러 연 화면은 그 화면을 고치자는 말만 받는다`() {
        // 전에는 "홈으로 가" · "뒤로"까지 받아 화면을 떠날 수 없었다(#346).
        assertFalse(isScheduleEditRequest("홈으로 가", OCT_20))
        assertFalse(isScheduleEditRequest("뒤로", OCT_20))
        assertFalse(isScheduleEditRequest("배가 아파요", OCT_20))
        assertTrue(isScheduleEditRequest("10월 25일로 바꿔 줘", OCT_20))
        assertTrue(isScheduleEditRequest("오후 3시로 해 줘", OCT_20))
        assertTrue(isScheduleEditRequest("할 일 추가, 약 챙기기", OCT_20))
    }

    // 말로 바뀐 자리 보이기

    @Test
    fun `말로 채운 칸을 보인다`() = runTest {
        val viewModel = viewModel(SharedAppointment(date = OCT_20, time = THREE_PM))

        viewModel.voice.handle("10월 20일 오후 3시", ScheduleAddVoiceRoutes())

        assertEquals(
            listOf(ScheduleAddFocusTarget.Date, ScheduleAddFocusTarget.Time),
            viewModel.uiState.value.voiceFocus?.targets,
        )
    }

    @Test
    fun `할 일을 더하거나 고치면 그 줄을 지우면 할 일 구역을 보인다`() = runTest {
        val viewModel = viewModel()
        val routes = ScheduleAddVoiceRoutes()

        viewModel.voice.handle("할 일 추가, 공복 유지", routes)
        val added = viewModel.uiState.value.todos.single().id
        assertEquals(listOf(ScheduleAddFocusTarget.Todo(added)), viewModel.uiState.value.voiceFocus?.targets)

        viewModel.voice.handle("첫 번째 할 일 지워 줘", routes)
        assertEquals(listOf(ScheduleAddFocusTarget.Todos), viewModel.uiState.value.voiceFocus?.targets)
    }

    @Test
    fun `맞는 카드가 여럿이면 그 카드들을 보인다`() = runTest {
        val viewModel =
            viewModel(cards = listOf(card("1", "두통 · 어지러움"), card("2", "두통 · 메스꺼움"), card("3", "복통")))
        viewModel.load()

        viewModel.voice.handle("두통 카드 가져갈게", ScheduleAddVoiceRoutes())

        assertEquals(
            listOf(ScheduleAddFocusTarget.Card("1"), ScheduleAddFocusTarget.Card("2")),
            viewModel.uiState.value.voiceFocus?.targets,
        )
    }

    @Test
    fun `빠진 칸이 있어 저장하지 못하면 그 칸을 보인다`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateConfirm(OCT_20)

        viewModel.voice.handle("저장해 줘", ScheduleAddVoiceRoutes())

        assertEquals(listOf(ScheduleAddFocusTarget.Hospital), viewModel.uiState.value.voiceFocus?.targets)
    }

    @Test
    fun `같은 칸을 다시 말하면 새로 보인다`() = runTest {
        val viewModel = viewModel()
        viewModel.voice.handle("시간 미정", ScheduleAddVoiceRoutes())
        val first = viewModel.uiState.value.voiceFocus

        viewModel.voice.handle("시간 미정", ScheduleAddVoiceRoutes())

        assertNotEquals(first, viewModel.uiState.value.voiceFocus)
    }

    @Test
    fun `말로 연 병원 찾기에서 돌아오면 병원 칸을 보이고 손으로 연 것은 보이지 않는다`() = runTest {
        val viewModel = viewModel(SharedAppointment(hospital = "고려튼튼통증의학과"))
        viewModel.voice.handle("고려튼튼통증의학과", ScheduleAddVoiceRoutes())
        viewModel.onHospitalSearchOpened()
        val before = viewModel.uiState.value.voiceFocus

        viewModel.onHospitalPicked("고려튼튼통증의학과의원")
        val after = viewModel.uiState.value.voiceFocus
        assertNotEquals(before, after)
        assertEquals(listOf(ScheduleAddFocusTarget.Hospital), after?.targets)

        // 손으로 병원 칸을 눌러 다녀온 것은 사람이 이미 그 칸을 보고 있다.
        viewModel.onHospitalPicked("다른병원")
        assertEquals(after, viewModel.uiState.value.voiceFocus)
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
        visited = false,
        clinic = clinic,
        writtenOn = LocalDate.of(2026, 10, 1),
    )

    private companion object {
        val OCT_20: LocalDate = LocalDate.of(2026, 10, 20)
        val THREE_PM: LocalTime = LocalTime.of(15, 0)
    }
}
