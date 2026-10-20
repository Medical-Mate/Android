package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.visit.data.HospitalRepository
import com.mist.medicalmate.visit.data.HospitalSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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

/** 병원 찾기를 말로 하는 것(#282). */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalPickVoiceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `몇 번째인지 말하면 그 줄이다`() {
        assertEquals(HospitalPickVoice.Pick(0), parseHospitalPickVoice("첫 번째"))
        assertEquals(HospitalPickVoice.Pick(1), parseHospitalPickVoice("2번 병원"))
        assertEquals(HospitalPickVoice.Pick(VoiceWords.LAST), parseHospitalPickVoice("마지막 거"))
    }

    @Test
    fun `찾으라고 하거나 병원 이름이면 다시 찾는다`() {
        assertEquals(HospitalPickVoice.Search("고려튼튼"), parseHospitalPickVoice("고려튼튼을 찾아 줘"))
        assertEquals(HospitalPickVoice.Search("서울대병원"), parseHospitalPickVoice("서울대병원 검색해 줘"))
        assertEquals(HospitalPickVoice.Search("밝은안과"), parseHospitalPickVoice("밝은안과"))
    }

    @Test
    fun `병원 이름이 아닌 말은 이 화면의 말이 아니다`() {
        // 모든 말을 검색어로 받으면 "뒤로"도 병원 이름으로 찾는다.
        assertNull(parseHospitalPickVoice("뒤로 가"))
        assertNull(parseHospitalPickVoice("홈으로"))
    }

    @Test
    fun `이걸로라고 하면 골라 둔 병원으로 마친다`() {
        assertEquals(HospitalPickVoice.Submit, parseHospitalPickVoice("이걸로 할게"))
    }

    @Test
    fun `고르는 말과 찾는 말을 여러 말로 한다`() {
        assertEquals(HospitalPickVoice.Pick(0), parseHospitalPickVoice("맨 위에 거"))
        assertEquals(HospitalPickVoice.Pick(1), parseHospitalPickVoice("둘째 병원"))
        assertEquals(HospitalPickVoice.Submit, parseHospitalPickVoice("여기로 할게"))
        assertEquals(HospitalPickVoice.Submit, parseHospitalPickVoice("그걸로 해 줘"))
        assertEquals(HospitalPickVoice.Search("고려튼튼"), parseHospitalPickVoice("고려튼튼 다시 찾아 볼래"))
        assertEquals(HospitalPickVoice.Search("강남 세브란스"), parseHospitalPickVoice("강남 세브란스 보여 줘"))
    }

    @Test
    fun `이거 말고는 고르는 말이 아니라 다시 찾는 말이다`() {
        assertEquals(HospitalPickVoice.Search("서울대병원"), parseHospitalPickVoice("이거 말고 서울대병원"))
        assertNull(parseHospitalPickVoice("이거 말고"))
    }

    @Test
    fun `카드를 만들자는 말을 가린다`() {
        assertEquals(HospitalPickVoice.MakeCard, parseHospitalPickVoice("브리핑 카드 만들어 줘"))
        assertEquals(HospitalPickVoice.MakeCard, parseHospitalPickVoice("카드 만들자"))
    }

    @Test
    fun `진료 전에는 카드를 만들자는 말이 버튼처럼 넘어간다`() = runTest(dispatcher) {
        // 화면이 받지 않으면 전역 명령이 새 증상 정리를 열어 하던 흐름을 버렸다(#333). 병원은 안 골라도 된다.
        val viewModel = HospitalPickViewModel(SearchRepository()).apply { load(HospitalPickPurpose.BEFORE_VISIT) }
        var submitted = false

        val reply = viewModel.onVoice("브리핑 카드 만들어 줘") { submitted = true }

        assertTrue(submitted)
        assertEquals(VoiceReply.Done(R.string.voice_hospital_make_card), reply)
    }

    @Test
    fun `진료 전에는 병원을 안 골라도 이걸로 할게가 버튼처럼 넘어간다`() = runTest(dispatcher) {
        // 버튼이 병원 없이도 눌리는 화면이다(#336).
        val viewModel = HospitalPickViewModel(SearchRepository()).apply { load(HospitalPickPurpose.BEFORE_VISIT) }
        var submitted = false

        val reply = viewModel.onVoice("이걸로 할게") { submitted = it == null }

        assertTrue(submitted)
        assertEquals(VoiceReply.Done(R.string.voice_hospital_skipped), reply)
    }

    @Test
    fun `진료 후에는 병원을 골라야 넘어간다`() = runTest(dispatcher) {
        val viewModel = HospitalPickViewModel(SearchRepository()).apply { load(HospitalPickPurpose.AFTER_VISIT) }
        var submitted = false

        val reply = viewModel.onVoice("이걸로 할게") { submitted = true }

        assertFalse(submitted)
        assertEquals(VoiceReply.Done(R.string.voice_hospital_none_selected), reply)
    }

    @Test
    fun `진료 후에는 카드를 만들자는 말을 받지 않는다`() = runTest(dispatcher) {
        val viewModel = HospitalPickViewModel(SearchRepository()).apply { load(HospitalPickPurpose.AFTER_VISIT) }

        assertEquals(VoiceReply.NotMine, viewModel.onVoice("카드 만들어 줘") {})
    }

    @Test
    fun `몇 번째를 말하면 그 병원을 고르고 마친다`() = runTest(dispatcher) {
        val viewModel = searched()
        var submitted: Hospital? = null

        val reply = viewModel.onVoice("두 번째") { submitted = it }

        assertEquals(SEOUL[1], submitted)
        assertEquals(SEOUL[1], viewModel.uiState.value.selected)
        assertEquals(VoiceReply.Done(R.string.voice_hospital_picked, listOf(SEOUL[1].name)), reply)
    }

    @Test
    fun `목록에 없는 번호면 마치지 않는다`() = runTest(dispatcher) {
        val viewModel = searched()
        var submitted = false

        val reply = viewModel.onVoice("다섯 번째") { submitted = true }

        assertEquals(VoiceReply.Done(R.string.voice_hospital_no_row), reply)
        assertEquals(false, submitted)
    }

    @Test
    fun `병원 이름을 말하면 그것으로 다시 찾는다`() = runTest(dispatcher) {
        val repository = SearchRepository()
        val viewModel = HospitalPickViewModel(repository)

        viewModel.onVoice("고려튼튼을 찾아 줘") {}
        advanceUntilIdle()

        assertEquals("고려튼튼", viewModel.uiState.value.query)
        assertEquals("고려튼튼", repository.lastQuery)
    }

    // 말로 찾은 병원 고르기(#282)

    @Test
    fun `띄어쓰기와 끝의 의원을 빼고 같으면 같은 이름이다`() {
        assertTrue(sameHospitalName("고려튼튼 통증의학과", "고려튼튼통증의학과의원"))
        assertTrue(sameHospitalName("서울OO병원", "서울OO병원"))
        assertFalse(sameHospitalName("서울병원", "서울OO병원"))
    }

    @Test
    fun `이름이 같은 곳이 하나면 그곳을 고른다`() {
        val results = listOf(Hospital("고려튼튼통증의학과의원", "서울 노원구"), Hospital("고려튼튼정형외과의원", "서울 강남구"))

        assertEquals(HospitalVoiceDecision.Pick(results[0]), decideVoicePick("고려튼튼통증의학과", results))
    }

    @Test
    fun `이름이 같은 곳이 여럿이면 그곳들만 놓고 묻는다`() {
        val same = listOf(Hospital("태릉씨티치과의원", "서울 노원구 공릉동"), Hospital("태릉씨티치과의원", "서울 노원구 하계동"))
        val results = same + Hospital("태릉치과의원", "서울 노원구")

        assertEquals(HospitalVoiceDecision.Ask(HospitalAskReason.SAME_NAME, same), decideVoicePick("태릉씨티치과", results))
    }

    @Test
    fun `같은 이름이 없으면 결과 수로 정한다`() {
        val one = listOf(Hospital("밝은눈안과의원"))
        val many = listOf(Hospital("서울OO병원"), Hospital("서울XX병원"))

        assertEquals(HospitalVoiceDecision.Pick(one[0]), decideVoicePick("밝은안과", one))
        assertEquals(HospitalVoiceDecision.Ask(HospitalAskReason.MANY, many), decideVoicePick("서울병원", many))
        assertEquals(
            HospitalVoiceDecision.Ask(HospitalAskReason.NONE, emptyList()),
            decideVoicePick("없는병원", emptyList()),
        )
    }

    @Test
    fun `동네나 지점 이름으로 좁힌다`() {
        val same =
            listOf(
                Hospital("태릉씨티치과의원", "서울특별시 노원구 공릉동 123"),
                Hospital("태릉씨티치과의원", "서울특별시 노원구 하계동 45"),
                Hospital("태릉씨티치과의원 강남점", "서울특별시 강남구 역삼동 7"),
            )

        assertEquals(listOf(same[1]), same.byArea("하계동에 있는 거"))
        assertEquals(listOf(same[2]), same.byArea("강남점"))
        assertEquals(listOf(same[2]), same.byArea("역삼동 쪽"))
        // 노원구는 둘이다. 다시 좁혀 묻는다.
        assertEquals(same.take(2), same.byArea("노원구"))
        // 모든 곳에 있는 낱말(이름)로는 가리지 못한다.
        assertEquals(emptyList<Hospital>(), same.byArea("태릉씨티치과"))
    }

    @Test
    fun `말로 열린 병원 찾기는 하나로 좁혀지면 알아서 고른다`() = runTest(dispatcher) {
        val viewModel = HospitalPickViewModel(SearchRepository(listOf(Hospital("고려튼튼통증의학과의원", "서울 노원구"))))

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "고려튼튼통증의학과", voicePick = true)
        advanceUntilIdle()

        assertEquals(
            HospitalVoiceDecision.Pick(Hospital("고려튼튼통증의학과의원", "서울 노원구")),
            viewModel.uiState.value.voiceDecision,
        )
        viewModel.onVoiceDecisionHandled()
        assertNull(viewModel.uiState.value.voiceDecision)
    }

    @Test
    fun `손이나 예약 문자로 열린 병원 찾기는 알아서 고르지 않는다`() = runTest(dispatcher) {
        val viewModel = HospitalPickViewModel(SearchRepository(listOf(Hospital("고려튼튼통증의학과의원"))))

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "고려튼튼통증의학과")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.voiceDecision)
    }

    @Test
    fun `같은 이름이 여럿이면 되묻고 동네로 답하면 고르고 마친다`() = runTest(dispatcher) {
        val same = listOf(Hospital("태릉씨티치과의원", "서울 노원구 공릉동"), Hospital("태릉씨티치과의원", "서울 노원구 하계동"))
        val viewModel = HospitalPickViewModel(SearchRepository(same))
        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "태릉씨티치과", voicePick = true)
        advanceUntilIdle()
        assertEquals(
            HospitalVoiceDecision.Ask(HospitalAskReason.SAME_NAME, same),
            viewModel.uiState.value.voiceDecision,
        )
        var submitted: Hospital? = null

        val reply = viewModel.onVoice("하계동에 있는 거") { submitted = it }

        assertEquals(same[1], submitted)
        assertEquals(VoiceReply.Done(R.string.voice_hospital_picked, listOf("태릉씨티치과의원")), reply)
    }

    @Test
    fun `동네로 답했는데 여럿이 남으면 좁혀 두고 다시 묻는다`() = runTest(dispatcher) {
        val same =
            listOf(
                Hospital("태릉씨티치과의원", "서울 노원구 공릉동"),
                Hospital("태릉씨티치과의원", "서울 노원구 하계동"),
                Hospital("태릉씨티치과의원", "서울 강남구 역삼동"),
            )
        val viewModel = HospitalPickViewModel(SearchRepository(same))
        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "태릉씨티치과", voicePick = true)
        advanceUntilIdle()

        val reply = viewModel.onVoice("노원구") {} as VoiceReply.Done

        assertEquals(R.string.voice_hospital_narrowed, reply.message)
        assertEquals(VoiceText(R.string.voice_hospital_ask_which), reply.question)
        assertEquals(same.take(2), viewModel.uiState.value.voiceCandidates)
    }

    @Test
    fun `목록에 없는 동네면 없다고 알리고 동네가 아닌 말은 넘긴다`() = runTest(dispatcher) {
        val same = listOf(Hospital("태릉씨티치과의원", "서울 노원구 공릉동"), Hospital("태릉씨티치과의원", "서울 노원구 하계동"))
        val viewModel = HospitalPickViewModel(SearchRepository(same))
        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "태릉씨티치과", voicePick = true)
        advanceUntilIdle()

        assertEquals(R.string.voice_hospital_no_area, (viewModel.onVoice("서초구 쪽") {} as VoiceReply.Done).message)
        assertEquals(VoiceReply.NotMine, viewModel.onVoice("뒤로 가") {})
    }

    @Test
    fun `말로 다시 찾아도 결과가 오면 고르거나 되묻는다`() = runTest(dispatcher) {
        val viewModel = HospitalPickViewModel(SearchRepository(listOf(Hospital("밝은눈안과의원"))))

        viewModel.onVoice("밝은눈안과 찾아 줘") {}
        advanceUntilIdle()

        assertEquals(HospitalVoiceDecision.Pick(Hospital("밝은눈안과의원")), viewModel.uiState.value.voiceDecision)
    }

    private fun TestScope.searched(): HospitalPickViewModel = HospitalPickViewModel(SearchRepository()).apply {
        onQueryChange("서울")
        advanceUntilIdle()
    }

    private class SearchRepository(private val hospitals: List<Hospital> = SEOUL) : HospitalRepository {
        var lastQuery: String? = null

        override suspend fun search(query: String): ApiResult<HospitalSearchResult> {
            lastQuery = query
            return ApiResult.Success(HospitalSearchResult(hospitals, hospitals.size))
        }
    }

    private companion object {
        val SEOUL = listOf(Hospital("서울OO병원 내과"), Hospital("서울OO병원 이비인후과"))
    }
}
