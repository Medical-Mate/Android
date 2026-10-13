package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.speech.FakeSpeechToText
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.visit.data.AXIS_FOLLOW_UP
import com.mist.medicalmate.visit.data.AXIS_MEDICATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 진료 후 기록을 말로 하는 것(#288). */
@OptIn(ExperimentalCoroutinesApi::class)
class VisitVoiceTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1m-12

    @Test
    fun `병원 확인에 맞다거나 다른 병원이라고 답한다`() {
        assertEquals(true, parseClinicConfirm("네 맞아요"))
        assertEquals(true, parseClinicConfirm("응"))
        assertEquals(false, parseClinicConfirm("다른 병원이에요"))
        assertEquals(false, parseClinicConfirm("아니요"))
        assertNull(parseClinicConfirm("음"))
    }

    // 1p

    @Test
    fun `메모의 명령은 짧은 말일 때만이다`() {
        assertEquals(NoteVoice.DONE, parseNoteVoice("다 했어"))
        assertEquals(NoteVoice.DONE, parseNoteVoice("정리해 줘"))
        assertEquals(NoteVoice.UNDO, parseNoteVoice("방금 거 지워"))
        assertEquals(NoteVoice.CLEAR, parseNoteVoice("다 지워 줘"))
        // 들은 말에 섞인 낱말은 명령이 아니다.
        assertNull(parseNoteVoice("다음 주에 다시 오라고 했어요"))
        assertNull(parseNoteVoice("약은 빼고 드시래요"))
    }

    @Test
    fun `막대가 메모를 받아 적고 방금 것을 지우고 다 했으면 넘어간다`() {
        val viewModel = noteViewModel()

        viewModel.onVoiceNote("단순 근육통이래요", {})
        viewModel.onVoiceNote("파스 붙이고 2주 뒤에 오래요", {})
        assertEquals("단순 근육통이래요 파스 붙이고 2주 뒤에 오래요", viewModel.uiState.value.note)

        viewModel.onVoiceNote("방금 거 지워", {})
        assertEquals("단순 근육통이래요", viewModel.uiState.value.note)

        var saved: String? = null
        viewModel.onVoiceNote("다 했어", { saved = it })
        assertEquals("단순 근육통이래요", saved)
    }

    @Test
    fun `적은 게 없으면 넘어가지 않고 다 지우기는 묻는다`() {
        val viewModel = noteViewModel()
        var saved = false

        val empty = viewModel.onVoiceNote("다 했어", { saved = true }) as VoiceReply.Done
        assertEquals(R.string.voice_note_empty, empty.message)
        assertEquals(false, saved)

        viewModel.onVoiceNote("근육통이래요", {})
        val clear = viewModel.onVoiceNote("다 지워", {}) as VoiceReply.Confirm
        clear.onConfirm()
        assertEquals("", viewModel.uiState.value.note)
    }

    // 1q-1

    @Test
    fun `기록 확인의 말을 읽는다`() {
        assertEquals(RecordVoice.FollowUp(LocalDate.of(2026, 11, 3)), parseRecordVoice("재방문 11월 3일로 바꿔 줘", TODAY))
        assertEquals(RecordVoice.Edit("약", null, "타이레놀"), parseRecordVoice("약은 타이레놀로 바꿔 줘", TODAY))
        assertEquals("단순 근육통", (parseRecordVoice("소견 고쳐, 단순 근육통", TODAY) as RecordVoice.Edit).value)
        assertTrue(parseRecordVoice("검사 지워 줘", TODAY) is RecordVoice.Delete)
        assertEquals(RecordVoice.Save, parseRecordVoice("저장해 줘", TODAY))
        assertNull(parseRecordVoice("홈으로 가", TODAY))
    }

    @Test
    fun `항목을 고치고 지우고 고친 줄을 보인다`() {
        val holder = RecordHolder()

        holder.actions.handle("약은 타이레놀로 바꿔 줘", TODAY) {}
        assertEquals("타이레놀", holder.content.items[1].value)
        assertEquals(listOf(1), holder.content.voiceFocus?.targets)

        holder.actions.handle("첫 번째 항목 지워 줘", TODAY) {}
        assertEquals(listOf("약", "재방문"), holder.content.items.map { it.key })
    }

    @Test
    fun `재방문 날짜를 바꾸면 줄과 저장할 날짜가 함께 바뀐다`() {
        val holder = RecordHolder()

        holder.actions.handle("재방문 11월 3일로", TODAY) {}

        assertEquals(LocalDate.of(2026, 11, 3), holder.content.record.followUp?.date)
        assertEquals("11월 3일 (화)", holder.content.items.first { it.axis == AXIS_FOLLOW_UP }.value)
    }

    @Test
    fun `어느 항목인지 모르면 항목을 적어 묻고 저장은 확인을 받는다`() {
        val holder = RecordHolder()

        val which = holder.actions.handle("그거 고쳐, 타이레놀", TODAY) {} as VoiceReply.Done
        assertEquals(R.string.voice_record_which, which.message)
        assertEquals(listOf("· 소견\n· 약\n· 재방문"), which.question?.args)

        var saved = 0
        val confirm = holder.actions.handle("저장", TODAY) { saved++ } as VoiceReply.Confirm
        assertEquals(0, saved)
        confirm.onConfirm()
        assertEquals(1, saved)
    }

    private fun noteViewModel() = VisitNoteViewModel(
        Clock.fixed(Instant.parse("2026-10-13T01:00:00Z"), ZoneId.of("Asia/Seoul")),
        FakeSpeechToText(),
    )

    /** 화면 상태를 들고 있는 대역. ViewModel 없이 말 조작만 본다. */
    private class RecordHolder {
        var content =
            VisitRecordUiState.Content(
                record =
                VisitRecord(
                    id = "1",
                    clinic = "서울내과",
                    clinicLine = "서울내과 · 10.13",
                    items =
                    listOf(
                        VisitRecordItem("소견", "근육통", axis = "findings"),
                        VisitRecordItem("약", "진통제", axis = AXIS_MEDICATION),
                        VisitRecordItem("재방문", "2주 뒤", VisitRecordItem.Tone.LINK, AXIS_FOLLOW_UP),
                    ),
                    memo = "근육통이래요",
                ),
            )

        val actions = VisitRecordVoiceActions(state = { content }, update = { content = it(content) })
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 13)
    }
}
