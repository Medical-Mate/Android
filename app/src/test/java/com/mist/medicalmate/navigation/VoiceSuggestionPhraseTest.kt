package com.mist.medicalmate.navigation

import com.mist.medicalmate.calendar.ui.DayVoice
import com.mist.medicalmate.calendar.ui.parseDayVoice
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceScroll
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.core.voice.parseVoiceScroll
import com.mist.medicalmate.intake.ui.BodyVoicePick
import com.mist.medicalmate.intake.ui.IntakeDoneChoice
import com.mist.medicalmate.intake.ui.QuestionVoice
import com.mist.medicalmate.intake.ui.isBackVoice
import com.mist.medicalmate.intake.ui.isNextVoice
import com.mist.medicalmate.intake.ui.isOppositeVoice
import com.mist.medicalmate.intake.ui.isResetVoice
import com.mist.medicalmate.intake.ui.isUnsureVoice
import com.mist.medicalmate.intake.ui.parseDoneVoice
import com.mist.medicalmate.intake.ui.parseQuestionVoice
import com.mist.medicalmate.intake.ui.parseSeverityVoice
import com.mist.medicalmate.intake.ui.pickBodyPartByVoice
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 막대가 추천하는 말이 실제로 알아듣는 말인지(#292). 예시대로 말했는데 못 알아들으면 예시가 거짓말이 된다.
 *
 * 추천 문구는 리소스라 JVM에서 읽지 못해 같은 문장을 여기에 적는다. 예시를 바꾸면 이 문장도 바꾼다.
 * 여러 도메인의 말 풀이를 함께 보므로 조합 루트인 `navigation`에 둔다.
 */
class VoiceSuggestionPhraseTest {
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `공통 예시는 전역 명령이다`() {
        assertEquals(VoiceCommand.INTAKE, parseVoiceCommand("배가 아파서 정리할래"))
        assertEquals(VoiceCommand.VISIT_RECORD, parseVoiceCommand("진료 끝났어"))
        assertEquals(VoiceCommand.CALENDAR, parseVoiceCommand("10월 20일 일정 보여 줘"))
    }

    @Test
    fun `캘린더와 일자 화면의 예시를 알아듣는다`() {
        assertTrue(parseVoiceDate("20일 일정 보여 줘", today) is VoiceDate.Day)
        assertTrue(parseVoiceDate("다음 달", today) is VoiceDate.Month)
        // 캘린더 화면이 받지 않는 말이라 전역 명령으로 넘어간다.
        assertNull(parseVoiceDate("일정 추가해 줘", today))
        assertEquals(VoiceCommand.SCHEDULE_ADD, parseVoiceCommand("일정 추가해 줘"))
        assertEquals(DayVoice.RECORD_ADD, parseDayVoice("진료 후 기록할래"))
        assertEquals(DayVoice.CARD, parseDayVoice("카드 보여 줘"))
        assertEquals(DayVoice.RECORD_OPEN, parseDayVoice("기록 보여 줘"))
    }

    @Test
    fun `공용 말 예시를 알아듣는다`() {
        assertTrue(VoiceWords.isSave("저장해 줘"))
        assertTrue(VoiceWords.isRepeat("다시 보여 줘"))
        assertEquals(VoiceScroll.DOWN, parseVoiceScroll("아래로"))
    }

    @Test
    fun `증상 정리의 예시를 알아듣는다`() {
        assertTrue(pickBodyPartByVoice("오른쪽 어깨", null) is BodyVoicePick.Picked)
        assertTrue(pickBodyPartByVoice("다리가 아파요", null) is BodyVoicePick.AskSide)
        assertTrue(isUnsureVoice("잘 모르겠어요"))
        // 부위를 고른 뒤의 추천(#324)
        assertTrue(isOppositeVoice("반대쪽"))
        assertTrue(isResetVoice("다시 고를래"))
        assertEquals(MedicalMateSeverity.LEVEL_4, parseSeverityVoice("많이 아파요"))
        assertEquals(MedicalMateSeverity.LEVEL_4, parseSeverityVoice("7점"))
        assertTrue(parseQuestionVoice("약 계속 먹어도 되나요?") is QuestionVoice.Add)
        assertEquals(QuestionVoice.Pick(1), parseQuestionVoice("두 번째 담아 줘"))
        assertTrue(isNextVoice("다음"))
        assertTrue(isBackVoice("뒤로"))
        assertEquals(IntakeDoneChoice.CARD, parseDoneVoice("카드 만들어 줘"))
        assertEquals(IntakeDoneChoice.HOSPITAL, parseDoneVoice("병원부터 찾을래"))
    }

    @Test
    fun `병원 찾기와 진료 후 기록의 예시를 알아듣는다`() {
        assertEquals(HospitalPickVoice.Pick(0), parseHospitalPickVoice("첫 번째"))
        assertTrue(parseHospitalPickVoice("고려튼튼의원 찾아 줘") is HospitalPickVoice.Search)
        assertEquals(true, parseClinicConfirm("네, 맞아요"))
        assertEquals(false, parseClinicConfirm("다른 병원이에요"))
        // 메모의 예시는 명령이 아니라 메모에 적히는 말이다.
        assertNull(parseNoteVoice("약은 식후에 드시래요"))
        assertEquals(NoteVoice.UNDO, parseNoteVoice("방금 거 지워"))
        assertEquals(NoteVoice.DONE, parseNoteVoice("다 했어"))
        assertEquals(RecordVoice.SAVE, parseRecordVoice("저장해 줘"))
    }
}
