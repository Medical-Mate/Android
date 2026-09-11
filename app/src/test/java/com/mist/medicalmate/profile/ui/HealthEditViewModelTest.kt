package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.profile.data.FakeHealthProfileRepository
import com.mist.medicalmate.profile.data.HealthEdit
import com.mist.medicalmate.profile.data.HealthField
import com.mist.medicalmate.profile.data.HealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthEditViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 비어 있고 불러오면 저장된 값이 들어온다`() {
        val viewModel = HealthEditViewModel(repository())

        assertEquals(emptySet<String>(), viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))

        viewModel.load()

        assertEquals(setOf("혈압약", "진통제"), viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))
        assertEquals(setOf("고혈압"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
        assertEquals(setOf("페니실린"), viewModel.uiState.value.chosenIn(ProfileSetupStep.ALLERGIES))
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `읽어 온 값은 목록에 없어도 칩으로 선다`() {
        // 1b에서 직접 적어 넣은 것이 여기서 사라지면 저장할 때 지워진다.
        val viewModel = loaded()

        assertTrue("진통제" in viewModel.uiState.value.extrasIn(ProfileSetupStep.MEDICATIONS))
    }

    @Test
    fun `읽지 못하면 빈 화면으로 연다`() {
        val viewModel = HealthEditViewModel(FakeHealthProfileRepository(read = FakeHealthProfileRepository.OFFLINE))

        viewModel.load()

        assertEquals(emptySet<String>(), viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `고른 것을 저장하고 나간다`() {
        val repository = repository()
        val viewModel = HealthEditViewModel(repository).apply { load() }
        viewModel.onOptionClick(ProfileSetupStep.CONDITIONS, "당뇨")
        var left = false

        viewModel.onSaveClick { left = true }

        assertTrue(left)
        assertEquals(setOf("고혈압", "당뇨"), repository.saved?.conditions?.items?.toSet())
        assertEquals(HealthStatus.KNOWN, repository.saved?.medications?.status)
        assertEquals("김OO", repository.base?.name)
        assertEquals(1994, repository.base?.birthYear)
        assertEquals("FEMALE", repository.base?.sex)
    }

    @Test
    fun `다 빼면 모름으로 간다`() {
        val repository = repository()
        val viewModel = HealthEditViewModel(repository).apply { load() }
        viewModel.onOptionClick(ProfileSetupStep.ALLERGIES, "페니실린")

        viewModel.onSaveClick {}

        assertEquals(HealthStatus.UNKNOWN, repository.saved?.allergies?.status)
        assertEquals(emptyList<String>(), repository.saved?.allergies?.items)
    }

    @Test
    fun `저장하지 못하면 나가지 않고 알린다`() {
        val repository = FakeHealthProfileRepository(read = read(), written = FakeHealthProfileRepository.OFFLINE)
        val viewModel = HealthEditViewModel(repository).apply { load() }
        var left = false

        viewModel.onSaveClick { left = true }

        assertFalse(left)
        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.saving)
    }

    @Test
    fun `불러오기 전에는 저장하지 않는다`() {
        // 되돌려 보낼 이름·출생연도·성별이 없다.
        val repository = repository()
        val viewModel = HealthEditViewModel(repository)

        viewModel.onSaveClick {}

        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `누르면 고르고 다시 누르면 뺀다`() {
        val viewModel = loaded()

        viewModel.onOptionClick(ProfileSetupStep.CONDITIONS, "당뇨")
        assertEquals(setOf("고혈압", "당뇨"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))

        viewModel.onOptionClick(ProfileSetupStep.CONDITIONS, "당뇨")
        assertEquals(setOf("고혈압"), viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
    }

    @Test
    fun `갈래마다 따로 담긴다`() {
        val viewModel = loaded()

        viewModel.onOptionClick(ProfileSetupStep.MEDICATIONS, "위장약")

        assertTrue("위장약" in viewModel.uiState.value.chosenIn(ProfileSetupStep.MEDICATIONS))
        assertFalse("위장약" in viewModel.uiState.value.chosenIn(ProfileSetupStep.CONDITIONS))
    }

    @Test
    fun `직접 추가를 누르면 그 갈래가 열린다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)

        assertEquals(ProfileSetupStep.ALLERGIES, viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `열린 갈래를 다시 누르면 닫힌다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)

        assertNull(viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `다른 갈래를 누르면 그쪽으로 옮겨가고 적던 것은 사라진다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onAddClick(ProfileSetupStep.MEDICATIONS)

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.adding)
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `추가하면 목록에 붙고 고른 것으로 표시된다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        val state = viewModel.uiState.value
        // 읽어 온 값이 먼저 들어 있고 그 뒤에 붙는다.
        assertEquals(listOf("페니실린", "메밀"), state.extrasIn(ProfileSetupStep.ALLERGIES))
        assertTrue("메밀" in state.chosenIn(ProfileSetupStep.ALLERGIES))
        assertEquals("", state.draft)
    }

    @Test
    fun `앞뒤 공백은 잘라낸다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("  메밀  ")
        viewModel.onDraftSubmit()

        assertEquals(listOf("페니실린", "메밀"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    @Test
    fun `빈 값은 넣지 않는다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("   ")
        assertFalse(viewModel.uiState.value.canAddDraft)

        viewModel.onDraftSubmit()

        assertEquals(listOf("페니실린"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    @Test
    fun `같은 것을 두 번 추가하지 않는다`() {
        val viewModel = loaded()

        viewModel.onAddClick(ProfileSetupStep.ALLERGIES)
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()
        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        assertEquals(listOf("페니실린", "메밀"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
        assertEquals("", viewModel.uiState.value.draft)
    }

    @Test
    fun `열지 않고 추가를 눌러도 아무 일이 없다`() {
        val viewModel = loaded()

        viewModel.onDraftChange("메밀")
        viewModel.onDraftSubmit()

        assertEquals(listOf("페니실린"), viewModel.uiState.value.extrasIn(ProfileSetupStep.ALLERGIES))
    }

    private fun loaded() = HealthEditViewModel(repository()).apply { load() }

    private fun repository() = FakeHealthProfileRepository(read = read())

    /** Figma 1s-2에서 골라져 있는 값이 서버에 있는 상태. */
    private fun read() = ApiResult.Success(
        FakeHealthProfileRepository.PROFILE.copy(
            health =
            HealthEdit(
                medications = HealthField(listOf("혈압약", "진통제")),
                conditions = HealthField(listOf("고혈압")),
                allergies = HealthField(listOf("페니실린")),
            ),
        ),
    )
}
