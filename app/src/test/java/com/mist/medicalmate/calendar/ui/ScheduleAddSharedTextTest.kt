package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.calendar.share.AppointmentTextReader
import com.mist.medicalmate.calendar.share.SharedAppointment
import com.mist.medicalmate.card.ui.FakeCardRepository
import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** 예약 문자를 공유받아 일정 추가가 열리는 길(#270). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleAddSharedTextTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `문자 없이 열리면 아무것도 읽지 않는다`() {
        val reader = FakeTextReader()
        val viewModel = viewModel(reader)

        viewModel.load(sharedText = null)

        assertEquals(ScheduleAddShareRead.NONE, viewModel.uiState.value.shareRead)
        assertEquals(0, reader.calls)
    }

    @Test
    fun `읽은 병원과 날짜와 시간으로 칸을 채운다`() {
        val viewModel = viewModel(FakeTextReader(SharedAppointment("서울OO병원 내과", SEP_26, TEN_THIRTY)))

        viewModel.load(sharedText = SMS)

        val state = viewModel.uiState.value
        assertEquals("서울OO병원 내과", state.hospital)
        assertEquals(SEP_26, state.date)
        assertEquals(TEN_THIRTY, state.time)
        assertEquals(ScheduleAddShareRead.FILLED, state.shareRead)
    }

    @Test
    fun `읽는 동안 사람이 고른 값은 덮지 않는다`() {
        // 한국어 모델을 처음 받으면 몇 초가 걸린다. 그 사이 고른 것이 나중의 뜻이다.
        val answer = CompletableDeferred<SharedAppointment>()
        val viewModel = viewModel(FakeTextReader(answer = answer))

        viewModel.load(sharedText = SMS)
        assertEquals(ScheduleAddShareRead.READING, viewModel.uiState.value.shareRead)
        viewModel.onDateConfirm(LocalDate.of(2026, 9, 27))
        answer.complete(SharedAppointment("서울OO병원 내과", SEP_26, TEN_THIRTY))

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 9, 27), state.date)
        assertEquals("서울OO병원 내과", state.hospital)
        assertEquals(TEN_THIRTY, state.time)
    }

    @Test
    fun `날짜를 못 찾으면 그렇다고 알리고 찾은 병원은 채운다`() {
        val viewModel = viewModel(FakeTextReader(SharedAppointment(hospital = "연세이비인후과의원")))

        viewModel.load(sharedText = SMS)

        val state = viewModel.uiState.value
        assertEquals(ScheduleAddShareRead.MISSED, state.shareRead)
        assertEquals("연세이비인후과의원", state.hospital)
        assertNull(state.date)
    }

    @Test
    fun `다른 앱이 넘긴 날짜와 시각이 있으면 글에서 날짜를 못 찾아도 채운 것이다`() {
        // 갤럭시 메시지의 "일정 추가"는 날짜를 숫자로 넘기고 글은 병원을 찾는 데만 쓴다.
        val viewModel = viewModel(FakeTextReader(SharedAppointment(hospital = "튼튼정형외과")))

        // 화면이 여는 차례 그대로다. 넘겨받은 날짜를 먼저 채우고 글을 읽는다.
        viewModel.onDatePrefilled(SEP_26, TEN_THIRTY)
        viewModel.load(sharedText = "튼튼정형외과 예약")

        val state = viewModel.uiState.value
        assertEquals("튼튼정형외과", state.hospital)
        assertEquals(SEP_26, state.date)
        assertEquals(TEN_THIRTY, state.time)
        assertEquals(ScheduleAddShareRead.FILLED, state.shareRead)
    }

    @Test
    fun `화면이 다시 조합돼도 한 번만 읽는다`() {
        // 병원을 고르러 나갔다 돌아오면 같은 호출이 또 온다. 다시 읽으면 지운 시각이 되살아난다.
        val reader = FakeTextReader(SharedAppointment(date = SEP_26, time = TEN_THIRTY))
        val viewModel = viewModel(reader)

        viewModel.load(sharedText = SMS)
        viewModel.load(sharedText = SMS)

        assertEquals(1, reader.calls)
    }

    private fun viewModel(reader: FakeTextReader) = ScheduleAddViewModel(
        RecordingAppointmentRepository(),
        FakeCardRepository(list = ApiResult.Success(emptyList())),
        reader,
    )

    private companion object {
        const val SMS = "[Web발신]\n[서울OO병원] 9월 26일(토) 오전 10:30 내과 진료 예약되었습니다."
        val SEP_26: LocalDate = LocalDate.of(2026, 9, 26)
        val TEN_THIRTY: LocalTime = LocalTime.of(10, 30)
    }
}

/** 정해 둔 값을 돌려주는 문자 읽기. [answer]가 있으면 그것이 끝날 때까지 기다린다. */
internal class FakeTextReader(
    private val result: SharedAppointment = SharedAppointment(),
    private val answer: CompletableDeferred<SharedAppointment>? = null,
) : AppointmentTextReader {
    var calls = 0
        private set

    override suspend fun read(text: String): SharedAppointment {
        calls++
        return answer?.await() ?: result
    }
}

/** 문자에서 읽은 병원 이름으로 병원 찾기를 여는 것(#270). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleAddHospitalSearchTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `문자에서 병원을 읽으면 그 이름으로 병원 찾기를 연다`() {
        val viewModel = viewModel(SharedAppointment(hospital = "태릉씨티치과"))

        viewModel.load(sharedText = "태릉씨티치과입니다")

        assertEquals("태릉씨티치과", viewModel.uiState.value.hospitalSearch)
    }

    @Test
    fun `한 번 열면 다시 열지 않는다`() {
        // 병원을 고르고 돌아오면 화면이 다시 조합된다. 그때 또 열리면 고른 것을 덮는 길로 다시 간다.
        val viewModel = viewModel(SharedAppointment(hospital = "태릉씨티치과"))
        viewModel.load(sharedText = "태릉씨티치과입니다")

        viewModel.onHospitalSearchOpened()
        viewModel.onHospitalPicked("태릉씨티치과의원")

        assertNull(viewModel.uiState.value.hospitalSearch)
        assertEquals("태릉씨티치과의원", viewModel.uiState.value.hospital)
    }

    @Test
    fun `이미 고른 병원이 있으면 열지 않는다`() {
        val answer = CompletableDeferred<SharedAppointment>()
        val viewModel = viewModel(answer = answer)

        viewModel.load(sharedText = "태릉씨티치과입니다")
        viewModel.onHospitalPicked("서울OO병원")
        answer.complete(SharedAppointment(hospital = "태릉씨티치과"))

        assertNull(viewModel.uiState.value.hospitalSearch)
        assertEquals("서울OO병원", viewModel.uiState.value.hospital)
    }

    @Test
    fun `병원을 못 읽으면 열지 않는다`() {
        val viewModel = viewModel(SharedAppointment(date = LocalDate.of(2026, 10, 19)))

        viewModel.load(sharedText = "10월19일 예약")

        assertNull(viewModel.uiState.value.hospitalSearch)
    }

    private fun viewModel(
        result: SharedAppointment = SharedAppointment(),
        answer: CompletableDeferred<SharedAppointment>? = null,
    ) = ScheduleAddViewModel(
        RecordingAppointmentRepository(),
        FakeCardRepository(list = ApiResult.Success(emptyList())),
        FakeTextReader(result, answer),
    )
}
