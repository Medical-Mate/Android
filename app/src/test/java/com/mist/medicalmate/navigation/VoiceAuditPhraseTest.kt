package com.mist.medicalmate.navigation

import com.mist.medicalmate.calendar.ui.DayVoice
import com.mist.medicalmate.calendar.ui.parseDayVoice
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceScroll
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.core.voice.parseVoiceScroll
import com.mist.medicalmate.visit.ui.HospitalPickVoice
import com.mist.medicalmate.visit.ui.NoteVoice
import com.mist.medicalmate.visit.ui.RecordVoice
import com.mist.medicalmate.visit.ui.parseClinicConfirm
import com.mist.medicalmate.visit.ui.parseHospitalPickVoice
import com.mist.medicalmate.visit.ui.parseNoteVoice
import com.mist.medicalmate.visit.ui.parseRecordVoice
import com.mist.medicalmate.voice.ui.VoiceCommand
import com.mist.medicalmate.voice.ui.parseVoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * 리디자인 뒤 헤이 메이트를 전체로 다시 본 것(#320). 새 흐름 때문에 엉뚱하게 읽히던 말과, 같은 뜻의 다른 말이다.
 * 여러 도메인의 말 풀이를 함께 보므로 조합 루트인 `navigation`에 둔다.
 */
class VoiceAuditPhraseTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun `알림 말은 기록이 아니라 내 정보다`() {
        // "진료 후 기록 알림 꺼 줘"가 "진료후기록"으로 진료 후 기록을 시작했다.
        assertEquals(VoiceCommand.PROFILE, parseVoiceCommand("진료 후 기록 알림 꺼 줘"))
        assertEquals(VoiceCommand.PROFILE, parseVoiceCommand("알림 시간 바꿔 줘"))
        assertEquals(VoiceCommand.PROFILE, parseVoiceCommand("나의 정보 보여 줘"))
        assertNull(parseDayVoice("진료 후 기록 알림 꺼 줘"))
    }

    @Test
    fun `뒤로는 다른 말보다 뒤에 본다`() {
        assertEquals(VoiceCommand.RECORD, parseVoiceCommand("이전 기록 보여 줘"))
        assertEquals(VoiceCommand.HOME, parseVoiceCommand("홈으로 돌아가"))
        assertEquals(VoiceCommand.BACK, parseVoiceCommand("뒤로 가 줘"))
        assertEquals(VoiceCommand.BACK, parseVoiceCommand("이전 화면"))
    }

    @Test
    fun `이어 하기와 다음 진료를 묻는 말을 알아듣는다`() {
        assertEquals(VoiceCommand.RESUME, parseVoiceCommand("증상 정리 이어서 할래"))
        assertEquals(VoiceCommand.RESUME, parseVoiceCommand("하던 거 마저 할래"))
        assertEquals(VoiceCommand.NEXT_VISIT, parseVoiceCommand("다음 진료 언제야"))
        assertEquals(VoiceCommand.NEXT_VISIT, parseVoiceCommand("병원 언제 가야 돼"))
        // 잡자는 말은 묻는 말이 아니다.
        assertEquals(VoiceCommand.SCHEDULE_ADD, parseVoiceCommand("다음 진료 일정 잡아 줘"))
    }

    @Test
    fun `아픈 곳과 다녀온 것을 다른 말로도 알아듣는다`() {
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("어깨가 결려"))
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("다리가 저려요"))
        assertEquals(VoiceCommand.VISIT_RECORD, parseVoiceCommand("병원 다녀왔어"))
        assertEquals(VoiceCommand.VISIT_RECORD, parseVoiceCommand("진료 받았어"))
        assertEquals(VoiceCommand.VISIT_RECORD, parseVoiceCommand("들은 말 남길래"))
    }

    @Test
    fun `닫기는 말 전체가 닫자는 말일 때만이다`() {
        listOf("그만", "그만해", "닫아 줘", "됐어요", "이제 됐어", "꺼 줘", "끝").forEach {
            assertTrue(it, VoiceWords.isStop(it))
        }
        listOf("끝까지", "끝났대요", "그만 드셔도 된대요", "그만 먹어도 되나요?").forEach {
            assertFalse(it, VoiceWords.isStop(it))
        }
        assertEquals(VoiceScroll.BOTTOM, parseVoiceScroll("끝까지"))
    }

    @Test
    fun `일자 화면은 보자는 말과 남기자는 말을 가린다`() {
        assertEquals(DayVoice.RECORD_ADD, parseDayVoice("진료 끝났어"))
        assertEquals(DayVoice.RECORD_ADD, parseDayVoice("병원 다녀왔어"))
        assertEquals(DayVoice.RECORD_OPEN, parseDayVoice("기록 보여 줘"))
    }

    @Test
    fun `이전 달도 지난달이다`() {
        val month = (parseVoiceDate("이전 달", today) as VoiceDate.Month).month
        assertEquals(YearMonth.of(2026, 9), month)
    }

    @Test
    fun `메모의 다음과 끝났대요는 적는 말이다`() {
        assertNull(parseNoteVoice("다음 주에 오래요"))
        assertNull(parseNoteVoice("다음 달에 검사"))
        assertNull(parseNoteVoice("끝났대요"))
        assertEquals(NoteVoice.DONE, parseNoteVoice("다음"))
        assertEquals(NoteVoice.DONE, parseNoteVoice("넘어가 줘"))
        assertEquals(NoteVoice.DONE, parseNoteVoice("끝났어"))
    }

    @Test
    fun `병원 확인의 취소는 다른 병원이 아니다`() {
        assertNull(parseClinicConfirm("취소"))
        assertNull(parseClinicConfirm("잠깐만"))
        assertEquals(false, parseClinicConfirm("아니요"))
    }

    @Test
    fun `다시 골라는 마치라는 말이 아니다`() {
        assertNotEquals(HospitalPickVoice.Submit, parseHospitalPickVoice("다시 골라"))
        assertEquals(HospitalPickVoice.Submit, parseHospitalPickVoice("이걸로 할게"))
    }

    @Test
    fun `기록 확인에서 편집하자는 말은 화면에서 하라고 한다`() {
        assertEquals(RecordVoice.BY_HAND, parseRecordVoice("편집할래"))
    }
}
