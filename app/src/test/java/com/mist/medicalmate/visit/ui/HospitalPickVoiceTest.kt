package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
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
import org.junit.Assert.assertNull
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

    private fun TestScope.searched(): HospitalPickViewModel = HospitalPickViewModel(SearchRepository()).apply {
        onQueryChange("서울")
        advanceUntilIdle()
    }

    private class SearchRepository : HospitalRepository {
        var lastQuery: String? = null

        override suspend fun search(query: String): ApiResult<HospitalSearchResult> {
            lastQuery = query
            return ApiResult.Success(HospitalSearchResult(SEOUL, SEOUL.size))
        }
    }

    private companion object {
        val SEOUL = listOf(Hospital("서울OO병원 내과"), Hospital("서울OO병원 이비인후과"))
    }
}
