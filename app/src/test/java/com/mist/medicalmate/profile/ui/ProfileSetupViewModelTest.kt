package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.profile.data.FakeHealthProfileRepository
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
class ProfileSetupViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음은 복용약 단계다`() {
        val viewModel = viewModel()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun `칩을 누르면 들어가고 다시 누르면 빠진다`() {
        val viewModel = viewModel()

        viewModel.onOptionToggle("혈압약")
        assertEquals(setOf("혈압약"), viewModel.uiState.value.answer.chosen)

        viewModel.onOptionToggle("혈압약")
        assertEquals(emptySet<String>(), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `아무것도 고르지 않아도 다음 단계로 갈 수 있다`() {
        val viewModel = viewModel()

        viewModel.onNext()

        assertEquals(ProfileSetupStep.CONDITIONS, viewModel.uiState.value.step)
        assertEquals(emptySet<String>(), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `단계를 넘어갔다 돌아와도 답이 남아 있다`() {
        val viewModel = viewModel()
        viewModel.onOptionToggle("혈압약")

        viewModel.onNext()
        viewModel.onOptionToggle("고혈압")
        viewModel.onBack()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
        assertEquals(setOf("혈압약"), viewModel.uiState.value.answer.chosen)
    }

    @Test
    fun `첫 단계에서 뒤로 가면 아무 일도 일어나지 않는다`() {
        val viewModel = viewModel()

        viewModel.onBack()

        assertEquals(ProfileSetupStep.MEDICATIONS, viewModel.uiState.value.step)
    }

    @Test
    fun `단계 번호는 1부터 시작하고 마지막만 마지막이다`() {
        assertEquals(1, ProfileSetupStep.MEDICATIONS.number)
        assertEquals(3, ProfileSetupStep.ALLERGIES.number)
        assertEquals(3, ProfileSetupStep.total)
        assertFalse(ProfileSetupStep.MEDICATIONS.isLast)
        assertTrue(ProfileSetupStep.ALLERGIES.isLast)
    }

    @Test
    fun `마지막 단계에서 한 번만 저장하고 완료가 된다`() {
        // 단계마다 보내지 않는다. 서버가 부분 갱신을 주지 않는다.
        val repository = FakeHealthProfileRepository()
        val viewModel = viewModel(repository)

        viewModel.onNext()
        viewModel.onNext()
        assertEquals(0, repository.saveCount)
        assertFalse(viewModel.uiState.value.completed)

        viewModel.onNext()

        assertEquals(1, repository.saveCount)
        assertTrue(viewModel.uiState.value.completed)
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `고른 칩과 적어 넣은 것이 함께 간다`() {
        val repository = FakeHealthProfileRepository()
        val viewModel = filled(repository)

        viewModel.onNext()

        val saved = repository.saved
        assertEquals(listOf("혈압약", "타이레놀", "마그네슘"), saved?.medications?.items)
        assertEquals(listOf("고혈압"), saved?.conditions?.items)
        assertEquals(listOf("페니실린"), saved?.allergies?.items)
    }

    @Test
    fun `직접 입력은 쉼표로 나눈다`() {
        // 한 항목으로 두면 목록에서 약 하나로 세어진다.
        val repository = FakeHealthProfileRepository()
        val viewModel = viewModel(repository)
        viewModel.onNoteChange("타이레놀, 마그네슘")

        viewModel.onNext()
        viewModel.onNext()
        viewModel.onNext()

        assertEquals(listOf("타이레놀", "마그네슘"), repository.saved?.medications?.items)
    }

    @Test
    fun `빈 답은 없음이 아니라 모름으로 간다`() {
        // 아무것도 고르지 않고 넘어간 것이 "없어요"인지 "그냥 넘겼어요"인지 알 수 없다.
        val repository = FakeHealthProfileRepository()
        val viewModel = viewModel(repository)

        viewModel.onNext()
        viewModel.onNext()
        viewModel.onNext()

        val saved = repository.saved
        assertEquals(HealthStatus.UNKNOWN, saved?.medications?.status)
        assertEquals(HealthStatus.UNKNOWN, saved?.conditions?.status)
        assertEquals(HealthStatus.UNKNOWN, saved?.allergies?.status)
    }

    @Test
    fun `읽어 온 이름과 출생연도와 성별을 그대로 되돌려 보낸다`() {
        // 화면이 묻지 않는 값이고 서버가 필수로 둔 값이다.
        val repository = FakeHealthProfileRepository()
        val viewModel = filled(repository)

        viewModel.onNext()

        assertEquals(FakeHealthProfileRepository.PROFILE, repository.base)
    }

    @Test
    fun `저장하지 못하면 완료가 되지 않고 알린다`() {
        // 저장되지 않았는데 완료 화면(1b-4)이 나오면 등록됐다고 읽힌다.
        val repository = FakeHealthProfileRepository(written = FakeHealthProfileRepository.OFFLINE)
        val viewModel = filled(repository)

        viewModel.onNext()

        assertFalse(viewModel.uiState.value.completed)
        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.saving)
    }

    @Test
    fun `읽지 못하면 저장을 보내지 않는다`() {
        // 이름·출생연도·성별을 되돌려 보낼 수 없다.
        val repository = FakeHealthProfileRepository(read = FakeHealthProfileRepository.OFFLINE)
        val viewModel = filled(repository)

        viewModel.onNext()

        assertEquals(0, repository.saveCount)
        assertNull(repository.saved)
        assertTrue(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `다시 누르면 다시 보낸다`() {
        val repository = FakeHealthProfileRepository(written = FakeHealthProfileRepository.OFFLINE)
        val viewModel = filled(repository)

        viewModel.onNext()
        viewModel.onNext()

        assertEquals(2, repository.saveCount)
    }

    private fun viewModel(repository: FakeHealthProfileRepository = FakeHealthProfileRepository()) =
        ProfileSetupViewModel(repository)

    /** 세 단계를 채우고 마지막 단계에 서 있는 상태. */
    private fun filled(repository: FakeHealthProfileRepository) = viewModel(repository).apply {
        onOptionToggle("혈압약")
        onNoteChange("타이레놀, 마그네슘")
        onNext()
        onOptionToggle("고혈압")
        onNext()
        onOptionToggle("페니실린")
    }
}
