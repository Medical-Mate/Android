package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.speech.SpeechChunk
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VisitNoteViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음에는 비어 있어 저장할 수 없다`() {
        val state = viewModel().uiState.value

        assertEquals(VisitHeadline(LocalDate.of(2026, 9, 12)), state.visit)
        assertEquals("", state.note)
        assertFalse(state.canSave)
    }

    @Test
    fun `고른 병원과 카드가 머리말에 온다`() {
        // 앞 화면에서 고른 값이다. 픽스처를 보여주면 방금 고른 곳과 다른 병원이 적힌다.
        val viewModel = viewModel()

        viewModel.load(clinic = "가톨릭대학교 성빈센트병원", cardTitle = "갈비뼈 · 일주일", visitedOn = LocalDate.of(2026, 9, 12))

        assertEquals(
            VisitHeadline(
                visitedOn = LocalDate.of(2026, 9, 12),
                clinic = "가톨릭대학교 성빈센트병원",
                cardTitle = "갈비뼈 · 일주일",
            ),
            viewModel.uiState.value.visit,
        )
    }

    @Test
    fun `머리말을 다시 채워도 적던 글은 남는다`() {
        // 화면이 다시 조합되면 이 호출이 한 번 더 온다. 그때 적던 글이 사라지면 안 된다.
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.load(clinic = "가톨릭대학교 성빈센트병원", cardTitle = null, visitedOn = LocalDate.of(2026, 9, 12))

        assertEquals("위염이라고 하셨어요", viewModel.uiState.value.note)
    }

    @Test
    fun `적으면 저장할 수 있다`() {
        val viewModel = viewModel()

        viewModel.onNoteChange("위염이라고 하셨어요")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `공백만 적으면 저장할 수 없다`() {
        val viewModel = viewModel()

        viewModel.onNoteChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `처음에는 음성 패널이 없다`() {
        // 적는 화면이다. 마이크는 우하단에 떠 있고 패널은 누를 때 선다.
        assertNull(viewModel().uiState.value.voice)
    }

    @Test
    fun `우하단 마이크를 누르면 패널이 뜨고 바로 듣는다`() {
        // 누른 사람은 패널이 뜨자마자 말한다. 아직 아무것도 못 들었을 뿐이라 문구는
        // "말씀해 주세요"다.
        val viewModel = viewModel(FakeSpeechToText(keepOpen = true))

        viewModel.onVoiceClick()

        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `말소리가 들어오면 듣고 있어요가 된다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Partial("위염")), keepOpen = true)
        val viewModel = viewModel(speech)

        viewModel.onVoiceClick()

        assertEquals(MedicalMateVoiceState.LISTENING, viewModel.uiState.value.voice)
    }

    @Test
    fun `듣는 중에 패널 마이크를 누르면 멈춘다`() {
        // 다 말했을 때 누르는 자리다. 패널은 남고 상태만 돌아온다.
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Partial("위염")), keepOpen = true)
        val viewModel = viewModel(speech)
        viewModel.onVoiceClick()

        viewModel.onMicClick()

        assertEquals(MedicalMateVoiceState.IDLE, viewModel.uiState.value.voice)
    }

    @Test
    fun `받아쓴 글이 적던 메모 뒤에 붙는다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Final("2주 뒤에 오래요")), keepOpen = true)
        val viewModel = viewModel(speech)
        viewModel.onNoteChange("위염이래요. ")

        viewModel.onVoiceClick()

        assertEquals("위염이래요. 2주 뒤에 오래요", viewModel.uiState.value.note)
    }

    @Test
    fun `말이 끊겼다 이어져도 앞말이 남는다`() {
        // 인식기는 잠깐 멈추면 그 발화를 확정하고 다음 발화를 처음부터 다시 센다. 누를 때의
        // 글만 기준으로 삼으면 두 번째 말이 첫 번째 말을 지운다.
        val speech =
            FakeSpeechToText(
                chunks =
                listOf(
                    SpeechChunk.Final("위염이래요"),
                    SpeechChunk.Partial("약"),
                    SpeechChunk.Final("약 일주일치 받았어요"),
                ),
                keepOpen = true,
            )
        val viewModel = viewModel(speech)

        viewModel.onVoiceClick()

        assertEquals("위염이래요 약 일주일치 받았어요", viewModel.uiState.value.note)
    }

    @Test
    fun `이어 붙일 때 한 칸을 넣는다`() {
        // 인식기가 앞뒤를 붙여 주지 않아 그대로 두면 "위염이래요약"이 된다.
        val speech =
            FakeSpeechToText(
                chunks = listOf(SpeechChunk.Final("위염이래요"), SpeechChunk.Partial("약")),
                keepOpen = true,
            )
        val viewModel = viewModel(speech)

        viewModel.onVoiceClick()

        assertEquals("위염이래요 약", viewModel.uiState.value.note)
    }

    @Test
    fun `이미 띄어져 있으면 칸을 더 넣지 않는다`() {
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Final("약 받았어요")), keepOpen = true)
        val viewModel = viewModel(speech)
        viewModel.onNoteChange("위염이래요. ")

        viewModel.onVoiceClick()

        assertEquals("위염이래요. 약 받았어요", viewModel.uiState.value.note)
    }

    @Test
    fun `모델을 받는 동안은 정리하는 중이다`() {
        // 처음 쓸 때 한 번이다. 시안의 PROCESSING 자리가 이 상태다.
        val speech = FakeSpeechToText(chunks = listOf(SpeechChunk.Preparing), keepOpen = true)
        val viewModel = viewModel(speech)

        viewModel.onVoiceClick()

        assertEquals(MedicalMateVoiceState.PROCESSING, viewModel.uiState.value.voice)
    }

    @Test
    fun `마이크 권한을 거부하면 왜 안 되는지 적는다`() {
        val viewModel = viewModel()

        viewModel.onMicDenied()

        assertEquals(MedicalMateVoiceState.DENIED, viewModel.uiState.value.voice)
    }

    @Test
    fun `쓸 수 없는 기기에서는 마이크를 그리지 않는다`() {
        val viewModel = viewModel(FakeSpeechToText(usable = false))

        viewModel.checkVoice()

        assertFalse(viewModel.uiState.value.voiceAvailable)
    }

    @Test
    fun `직접 입력할게요를 누르면 패널이 닫힌다`() {
        val viewModel = viewModel()
        viewModel.onVoiceClick()

        viewModel.onTypeInsteadClick()

        assertNull(viewModel.uiState.value.voice)
    }

    @Test
    fun `음성을 켜도 적던 글은 남는다`() {
        // 증상 문답은 입력 자리를 통째로 갈아끼우지만 여기는 적어 둔 글 아래에 패널이 선다.
        val viewModel = viewModel()
        viewModel.onNoteChange("위염이라고 하셨어요")

        viewModel.onVoiceClick()

        assertEquals("위염이라고 하셨어요", viewModel.uiState.value.note)
    }

    @Test
    fun `정리하기 칩은 저장하기와 같은 자리로 간다`() {
        // 둘 다 메모를 분류로 보내는 같은 동작이다(#183). 나누는 것은 결과를 그리는 1q-1이
        // 부른다. 여기서는 메모를 들고 넘어갈 수 있는지만 본다.
        val viewModel = viewModel()

        viewModel.onNoteChange("위염이라고 하셨어요")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `그 날 진료면 오늘이라고 하지 않는다`() {
        // 시안의 "오늘 진료"는 그 날 바로 적는 경우다. 어제 진료를 오늘 적으면 거짓이 된다.
        val viewModel = viewModel()

        viewModel.load(clinic = null, cardTitle = null, visitedOn = LocalDate.of(2026, 9, 11))

        assertEquals(LocalDate.of(2026, 9, 11), viewModel.uiState.value.visit.visitedOn)
        assertFalse(viewModel.uiState.value.visit.today)
    }

    @Test
    fun `오늘 진료면 오늘이라고 한다`() {
        val viewModel = viewModel()

        viewModel.load(clinic = null, cardTitle = null, visitedOn = LocalDate.of(2026, 9, 12))

        assertTrue(viewModel.uiState.value.visit.today)
    }

    private fun viewModel(speech: FakeSpeechToText = FakeSpeechToText()) =
        VisitNoteViewModel(Clock.fixed(Instant.parse("2026-09-12T01:00:00Z"), ZoneId.of("Asia/Seoul")), speech)
}
