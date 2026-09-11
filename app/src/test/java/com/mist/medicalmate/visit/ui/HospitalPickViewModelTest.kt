package com.mist.medicalmate.visit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HospitalPickViewModelTest {
    @Test
    fun `처음에는 결과가 비어 있고 완료를 누를 수 없다`() {
        val state = HospitalPickViewModel().uiState.value

        assertEquals(emptyList<Hospital>(), state.results)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `불러오면 전체 목록이 나온다`() {
        val viewModel = HospitalPickViewModel()

        viewModel.load()

        assertEquals(previewHospitals, viewModel.uiState.value.results)
    }

    @Test
    fun `이름으로 좁힌다`() {
        val viewModel = loaded()

        viewModel.onQueryChange("서울OO병원")

        val results = viewModel.uiState.value.results
        assertEquals(2, results.size)
        assertTrue(results.all { it.name.contains("서울OO병원") })
    }

    @Test
    fun `주소로도 좁힌다`() {
        val viewModel = loaded()

        viewModel.onQueryChange("봉천로")

        assertEquals(listOf("OO이비인후과의원"), viewModel.uiState.value.results.map { it.name })
    }

    @Test
    fun `검색어를 지우면 전체가 돌아온다`() {
        val viewModel = loaded()

        viewModel.onQueryChange("봉천로")
        viewModel.onQueryChange("")

        assertEquals(previewHospitals, viewModel.uiState.value.results)
    }

    @Test
    fun `앞뒤 공백은 무시한다`() {
        val viewModel = loaded()

        viewModel.onQueryChange("  봉천로  ")

        assertEquals(1, viewModel.uiState.value.results.size)
    }

    @Test
    fun `하나를 고르면 완료를 누를 수 있다`() {
        val viewModel = loaded()

        viewModel.onHospitalClick("hospital-2")

        assertEquals("hospital-2", viewModel.uiState.value.selectedId)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `다시 고르면 하나만 남는다`() {
        val viewModel = loaded()

        viewModel.onHospitalClick("hospital-2")
        viewModel.onHospitalClick("hospital-3")

        assertEquals("hospital-3", viewModel.uiState.value.selectedId)
    }

    @Test
    fun `고른 병원이 결과에서 빠지면 선택이 풀린다`() {
        val viewModel = loaded()

        viewModel.onHospitalClick("hospital-4")
        viewModel.onQueryChange("서울OO병원")

        assertNull(viewModel.uiState.value.selectedId)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `고른 병원이 결과에 남으면 선택이 유지된다`() {
        val viewModel = loaded()

        viewModel.onHospitalClick("hospital-1")
        viewModel.onQueryChange("서울OO병원")

        assertEquals("hospital-1", viewModel.uiState.value.selectedId)
    }

    @Test
    fun `진료 전에는 검색어가 없으면 결과를 비운다`() {
        // 1m-B는 입력 전 상태를 빈 화면으로 그린다. 아직 아무것도 찾지 않은 사람에게 후보
        // 네 곳을 보여주면 그중 하나를 골라야 하는 것으로 읽힌다.
        val viewModel = loadedBefore()

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun `진료 전에도 검색하면 결과가 나온다`() {
        val viewModel = loadedBefore()

        viewModel.onQueryChange("이비인후과")

        assertEquals(listOf("서울OO병원 이비인후과", "OO이비인후과의원"), viewModel.uiState.value.results.map { it.name })
    }

    @Test
    fun `진료 전에 검색어를 지우면 다시 빈다`() {
        val viewModel = loadedBefore()
        viewModel.onQueryChange("이비인후과")

        viewModel.onQueryChange("")

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun `진료 전에는 고르지 않아도 넘어갈 수 있다`() {
        // 건너뛰기와 같은 곳으로 간다. 병원은 진료 후에도 등록할 수 있다.
        assertTrue(loadedBefore().uiState.value.canSubmit)
    }

    @Test
    fun `진료 후에는 골라야 넘어갈 수 있다`() {
        assertFalse(loaded().uiState.value.canSubmit)
    }

    @Test
    fun `진료 전 입력 전에는 하단 바를 두지 않는다`() {
        // 시안의 1m-B 입력 전 프레임(1092:3858)에 Footer가 없다.
        assertFalse(loadedBefore().uiState.value.showSubmit)
    }

    @Test
    fun `진료 전에 검색해서 결과가 나오면 하단 바가 생긴다`() {
        val viewModel = loadedBefore()

        viewModel.onQueryChange("서울")

        assertTrue(viewModel.uiState.value.showSubmit)
    }

    @Test
    fun `진료 후에는 처음부터 하단 바가 있다`() {
        assertTrue(loaded().uiState.value.showSubmit)
    }

    @Test
    fun `찾은 것이 없으면 하단 바가 사라진다`() {
        // 비활성 버튼을 남기지 않는다. 결과가 비면 고른 것도 함께 풀린다.
        val viewModel = loaded()

        viewModel.onQueryChange("없는병원이름")

        assertFalse(viewModel.uiState.value.showSubmit)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    private fun loaded() = HospitalPickViewModel().apply { load() }

    private fun loadedBefore() = HospitalPickViewModel().apply { load(HospitalPickPurpose.BEFORE_VISIT) }
}
