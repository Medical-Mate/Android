package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.HospitalRepository
import com.mist.medicalmate.visit.data.HospitalSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 일정 추가가 예약 문자에서 읽은 병원 이름을 들고 병원 찾기를 여는 길(#270). */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalPickInitialQueryTest {
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
    fun `들고 들어온 이름으로 바로 찾아 목록을 보인다`() = runTest(dispatcher) {
        val repository = ByQuery("태릉씨티치과" to listOf(TAERUNG, TAERUNG_OTHER))
        val viewModel = HospitalPickViewModel(repository)

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "태릉씨티치과")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("태릉씨티치과", state.query)
        assertEquals(listOf(TAERUNG, TAERUNG_OTHER), state.results)
        assertEquals(listOf("태릉씨티치과"), repository.queries)
    }

    @Test
    fun `못 찾으면 띄어쓰기를 빼고 다시 찾는다`() = runTest(dispatcher) {
        val goryeo = Hospital("고려튼튼통증의학과의원", "서울특별시 노원구")
        val repository = ByQuery("고려튼튼통증의학과" to listOf(goryeo))
        val viewModel = HospitalPickViewModel(repository)

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "고려튼튼 통증의학과")
        advanceUntilIdle()

        assertEquals(listOf(goryeo), viewModel.uiState.value.results)
        // 무엇으로 찾았는지 검색어 칸에 남긴다.
        assertEquals("고려튼튼통증의학과", viewModel.uiState.value.query)
        assertEquals(listOf("고려튼튼 통증의학과", "고려튼튼통증의학과"), repository.queries)
    }

    @Test
    fun `그래도 못 찾으면 마지막 낱말을 떼고 찾는다`() = runTest(dispatcher) {
        val seoul = Hospital("서울OO병원", "서울특별시 종로구")
        val repository = ByQuery("서울OO병원" to listOf(seoul))
        val viewModel = HospitalPickViewModel(repository)

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "서울OO병원 내과")
        advanceUntilIdle()

        assertEquals(listOf(seoul), viewModel.uiState.value.results)
        assertEquals(listOf("서울OO병원 내과", "서울OO병원내과", "서울OO병원"), repository.queries)
    }

    @Test
    fun `끝까지 없으면 처음 이름을 남기고 비운다`() = runTest(dispatcher) {
        val repository = ByQuery()
        val viewModel = HospitalPickViewModel(repository)

        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "없는 병원")
        advanceUntilIdle()

        assertEquals("없는 병원", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    /** 검색어마다 정해 둔 결과를 돌려준다. 없으면 0건이다. */
    private class ByQuery(vararg answers: Pair<String, List<Hospital>>) : HospitalRepository {
        private val answers = answers.toMap()
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): ApiResult<HospitalSearchResult> {
            queries += query
            val found = answers[query].orEmpty()
            return ApiResult.Success(HospitalSearchResult(found, found.size))
        }
    }

    private companion object {
        val TAERUNG = Hospital("태릉씨티치과의원", "서울특별시 노원구 화랑로")
        val TAERUNG_OTHER = Hospital("태릉씨티치과의원", "경기도 남양주시")
    }
}

/** 같은 이름의 지점이 여럿일 때 누른 줄이 골라지는지(#270). */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalPickSameNameTest {
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
    fun `같은 이름의 두 번째 지점을 누르면 그 지점이 골라진다`() = runTest(dispatcher) {
        val first = Hospital("태릉씨티치과의원", "서울특별시 노원구 화랑로")
        val second = Hospital("태릉씨티치과의원", "경기도 남양주시")
        val viewModel = HospitalPickViewModel(SameName(listOf(first, second)))
        viewModel.load(HospitalPickPurpose.SCHEDULE, initialQuery = "태릉씨티치과")
        advanceUntilIdle()

        viewModel.onHospitalClick(second)

        assertEquals(second, viewModel.uiState.value.selected)
    }

    private class SameName(private val hospitals: List<Hospital>) : HospitalRepository {
        override suspend fun search(query: String): ApiResult<HospitalSearchResult> =
            ApiResult.Success(HospitalSearchResult(hospitals, hospitals.size))
    }
}
