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

    private fun loaded() = HospitalPickViewModel().apply { load() }
}
