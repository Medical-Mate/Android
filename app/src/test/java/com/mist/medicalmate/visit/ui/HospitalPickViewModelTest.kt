package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.network.ApiResult
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HospitalPickViewModelTest {
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
    fun `처음에는 결과가 비어 있고 완료를 누를 수 없다`() {
        val state = viewModel().uiState.value

        assertEquals(emptyList<Hospital>(), state.results)
        assertFalse(state.canSubmit)
        assertFalse(state.showSubmit)
    }

    @Test
    fun `검색 전에는 보여줄 것이 없다`() = runTest(dispatcher) {
        // 전에는 진료 후(1m)에 후보 네 곳을 먼저 보여줬는데 그것이 픽스처였다.
        val repository = FakeHospitalRepository()
        val viewModel = viewModel(repository)

        viewModel.load(HospitalPickPurpose.AFTER_VISIT)
        advanceUntilIdle()

        assertEquals(emptyList<Hospital>(), viewModel.uiState.value.results)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `검색어가 멎은 뒤에 한 번만 부른다`() = runTest(dispatcher) {
        // 한 글자마다 부르면 심평원까지 왕복이 그만큼 간다.
        val repository = FakeHospitalRepository()
        val viewModel = viewModel(repository)

        viewModel.onQueryChange("서")
        viewModel.onQueryChange("서울")
        viewModel.onQueryChange("서울병")
        advanceUntilIdle()

        assertEquals(1, repository.calls)
        assertEquals("서울병", repository.lastQuery)
    }

    @Test
    fun `찾으면 결과와 건수가 들어온다`() = runTest(dispatcher) {
        val viewModel = searched(FakeHospitalRepository(hospitals = SEOUL, total = 2))

        val state = viewModel.uiState.value
        assertEquals(listOf("서울OO병원 내과", "서울OO병원 이비인후과"), state.results.map { it.name })
        assertFalse(state.searching)
        assertFalse(state.truncated)
        assertTrue(state.showSubmit)
    }

    @Test
    fun `받은 것보다 많으면 알린다`() = runTest(dispatcher) {
        // 부분 일치라 "서울"이면 4천 건이 넘는다. 더 받는 것이 아니라 좁혀야 한다.
        val viewModel = searched(FakeHospitalRepository(hospitals = SEOUL, total = 4231))

        assertTrue(viewModel.uiState.value.truncated)
        assertEquals(4231, viewModel.uiState.value.total)
    }

    @Test
    fun `0건이면 직전 결과를 남긴다`() = runTest(dispatcher) {
        // 한글 조합 중간 상태가 0건으로 지나간다. 그대로 그리면 목록이 깜빡인다.
        val repository = FakeHospitalRepository(hospitals = SEOUL, total = 2)
        val viewModel = searched(repository)

        repository.hospitals = emptyList()
        viewModel.onQueryChange("서울ㅇ")
        advanceUntilIdle()

        assertEquals(SEOUL, viewModel.uiState.value.results)
    }

    @Test
    fun `검색어를 비우면 결과도 비운다`() = runTest(dispatcher) {
        val viewModel = searched()

        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertEquals(emptyList<Hospital>(), viewModel.uiState.value.results)
        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun `빈 검색어로는 부르지 않는다`() = runTest(dispatcher) {
        // 서버가 q를 필수로 두고, 부분 일치라 무엇이든 받으면 수천 건이 온다.
        val repository = FakeHospitalRepository()
        val viewModel = viewModel(repository)

        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        assertEquals(0, repository.calls)
    }

    @Test
    fun `고르면 완료를 누를 수 있다`() = runTest(dispatcher) {
        val viewModel = searched()

        viewModel.onHospitalClick("서울OO병원 내과")

        assertEquals(Hospital("서울OO병원 내과"), viewModel.uiState.value.selected)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `고른 병원이 결과에서 빠지면 선택을 지운다`() = runTest(dispatcher) {
        // 보이지 않는 것이 골라져 있으면 완료를 눌렀을 때 무엇이 저장되는지 알 수 없다.
        val repository = FakeHospitalRepository(hospitals = SEOUL, total = 2)
        val viewModel = searched(repository)
        viewModel.onHospitalClick("서울OO병원 내과")

        repository.hospitals = listOf(Hospital("OO정형외과의원"))
        viewModel.onQueryChange("정형")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun `못 닿으면 못 찾은 것과 다르게 알린다`() = runTest(dispatcher) {
        // 앞은 잠시 뒤 다시 할 일이고 뒤는 검색어를 바꿀 일이다.
        val viewModel = searched(FakeHospitalRepository(result = OFFLINE))

        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.searching)
    }

    @Test
    fun `다시 찾으면 실패 표시가 걷힌다`() = runTest(dispatcher) {
        val repository = FakeHospitalRepository(result = OFFLINE)
        val viewModel = searched(repository)

        repository.result = null
        repository.hospitals = SEOUL
        viewModel.onQueryChange("서울OO")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.failed)
        assertEquals(SEOUL, viewModel.uiState.value.results)
    }

    @Test
    fun `기다리는 동안 화면에서 먼저 좁힌다`() = runTest(dispatcher) {
        // 왕복이 1.6초다. 그동안 목록이 그대로면 친 글자가 아무 일도 안 하는 것처럼 보인다.
        val viewModel = searched(FakeHospitalRepository(hospitals = SEOUL + Hospital("OO정형외과의원"), total = 3))

        viewModel.onQueryChange("이비인후")

        // 아직 서버를 부르기 전인데 목록이 줄어 있다.
        assertEquals(listOf("서울OO병원 이비인후과"), viewModel.uiState.value.results.map { it.name })
    }

    @Test
    fun `좁혀서 비면 목록을 그대로 둔다`() = runTest(dispatcher) {
        // 보이는 20곳에 없다고 전국에 없는 것이 아니다. 한글 조합 중간 상태도 여기서 빈다.
        val viewModel = searched()

        viewModel.onQueryChange("서울ㅂ")

        assertEquals(SEOUL, viewModel.uiState.value.results)
    }

    @Test
    fun `좁힌 결과는 서버 답으로 바뀐다`() = runTest(dispatcher) {
        val repository = FakeHospitalRepository(hospitals = SEOUL, total = 2)
        val viewModel = searched(repository)
        val fromServer = listOf(Hospital("서울OO병원 이비인후과"), Hospital("OO이비인후과의원"))

        viewModel.onQueryChange("이비인후")
        repository.hospitals = fromServer
        advanceUntilIdle()

        // 화면에서 좁힐 때는 보이던 것 중에서만 걸렀는데, 서버가 못 보던 것을 더 준다.
        assertEquals(fromServer, viewModel.uiState.value.results)
    }

    @Test
    fun `한 번 받은 검색어는 다시 부르지 않는다`() = runTest(dispatcher) {
        // 지우고 다시 치는 일이 잦은데 그때마다 1.6초를 기다릴 이유가 없다.
        val repository = FakeHospitalRepository(hospitals = SEOUL, total = 2)
        val viewModel = searched(repository)

        viewModel.onQueryChange("")
        viewModel.onQueryChange("서울")
        advanceUntilIdle()

        assertEquals(1, repository.calls)
        assertEquals(SEOUL, viewModel.uiState.value.results)
    }

    @Test
    fun `다시 열면 받아 둔 것도 버린다`() = runTest(dispatcher) {
        // 개원·폐원이 계속 생기는 데이터라 오래 들고 있을 값이 아니다.
        val repository = FakeHospitalRepository(hospitals = SEOUL, total = 2)
        val viewModel = searched(repository)

        viewModel.load()
        viewModel.onQueryChange("서울")
        advanceUntilIdle()

        assertEquals(2, repository.calls)
    }

    @Test
    fun `진료 전에는 고르지 않아도 넘어간다`() {
        val viewModel = viewModel()

        viewModel.load(HospitalPickPurpose.BEFORE_VISIT)

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `다시 열면 앞선 검색이 남지 않는다`() = runTest(dispatcher) {
        val viewModel = searched()

        viewModel.load(HospitalPickPurpose.SCHEDULE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals(emptyList<Hospital>(), state.results)
        assertEquals(HospitalPickPurpose.SCHEDULE, state.purpose)
    }

    private fun viewModel(repository: FakeHospitalRepository = FakeHospitalRepository()) =
        HospitalPickViewModel(repository)

    private fun TestScope.searched(
        repository: FakeHospitalRepository = FakeHospitalRepository(hospitals = SEOUL, total = 2),
    ): HospitalPickViewModel = viewModel(repository).apply {
        onQueryChange("서울")
        advanceUntilIdle()
    }

    /** 병원 검색 저장소 대역. 응답을 시험 도중에 바꿀 수 있어야 한다. */
    private class FakeHospitalRepository(
        var hospitals: List<Hospital> = emptyList(),
        var total: Int = 0,
        var result: ApiResult<HospitalSearchResult>? = null,
    ) : HospitalRepository {
        var calls = 0
        var lastQuery: String? = null

        override suspend fun search(query: String): ApiResult<HospitalSearchResult> {
            calls += 1
            lastQuery = query
            return result ?: ApiResult.Success(HospitalSearchResult(hospitals, total.coerceAtLeast(hospitals.size)))
        }
    }

    private companion object {
        val SEOUL = listOf(Hospital("서울OO병원 내과"), Hospital("서울OO병원 이비인후과"))

        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
