package com.mist.medicalmate.navigation

import com.mist.medicalmate.calendar.ui.DayVoice
import com.mist.medicalmate.calendar.ui.parseDayVoice
import com.mist.medicalmate.card.ui.BriefCardVoiceAction
import com.mist.medicalmate.card.ui.parseBriefCardVoice
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceScroll
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceClock
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.core.voice.parseVoiceScroll
import com.mist.medicalmate.home.ui.HomeVoice
import com.mist.medicalmate.home.ui.parseHomeVoice
import com.mist.medicalmate.profile.ui.AppSetting
import com.mist.medicalmate.profile.ui.ProfileVoice
import com.mist.medicalmate.profile.ui.parseProfileVoice
import com.mist.medicalmate.visit.ui.HospitalPickVoice
import com.mist.medicalmate.visit.ui.NoteVoice
import com.mist.medicalmate.visit.ui.RecordVoice
import com.mist.medicalmate.visit.ui.parseClinicConfirm
import com.mist.medicalmate.visit.ui.parseHospitalPickVoice
import com.mist.medicalmate.visit.ui.parseNoteVoice
import com.mist.medicalmate.visit.ui.parseRecordVoice
import com.mist.medicalmate.voice.ui.VoiceCommand
import com.mist.medicalmate.voice.ui.parseVoiceCommand
import com.mist.medicalmate.voice.ui.splitChoices
import com.mist.medicalmate.voice.ui.withoutWakeEcho
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * 리디자인 뒤 헤이 메이트를 전체로 다시 본 것(#320). 새 흐름 때문에 엉뚱하게 읽히던 말과, 같은 뜻의 다른 말이다.
 * 여러 도메인의 말 풀이를 함께 보므로 조합 루트인 `navigation`에 둔다.
 */
class VoiceAuditPhraseTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun `일부만 되던 버튼의 말을 알아듣는다`() {
        // #336. 홈 배너 둘, 카드의 병원 변경, 내 정보의 무엇인지 모를 "꺼 줘".
        assertEquals(HomeVoice.CARD_SCHEDULE, parseHomeVoice("카드로 진료 일정 잡아 줘"))
        assertEquals(HomeVoice.TODAY_RECORD, parseHomeVoice("진료 후 기록할래"))
        assertNotEquals(HomeVoice.TODAY_RECORD, parseHomeVoice("진료 후 기록 알림 꺼 줘"))
        assertEquals(BriefCardVoiceAction.HOSPITAL, parseBriefCardVoice("병원 바꿀래"))
        assertEquals(ProfileVoice.Which, parseProfileVoice("꺼 줘"))
        assertTrue(parseProfileVoice("진료 전 알림 꺼 줘") is ProfileVoice.Toggle)
    }

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

    @Test
    fun `브리핑 카드는 일정과 내보내기를 저장보다 먼저 본다`() {
        // 전에는 저장하라는 말만 알아들어 "진료 일정 잡아 줘"와 "이미지로 저장해 줘"가 카드 저장이 됐다.
        assertEquals(BriefCardVoiceAction.SCHEDULE, parseBriefCardVoice("진료 일정 잡아 줘"))
        assertEquals(BriefCardVoiceAction.SCHEDULE, parseBriefCardVoice("일정 추가해 줘"))
        assertEquals(BriefCardVoiceAction.EXPORT, parseBriefCardVoice("이미지로 저장해 줘"))
        assertEquals(BriefCardVoiceAction.EXPORT, parseBriefCardVoice("공유해 줘"))
        assertEquals(BriefCardVoiceAction.VIEW_SCHEDULE, parseBriefCardVoice("진료 일정 보여 줘"))
        assertEquals(BriefCardVoiceAction.RECORD_VISIT, parseBriefCardVoice("진료 후 기록할래"))
        assertEquals(BriefCardVoiceAction.LATER, parseBriefCardVoice("아직 안 정했어요"))
        assertEquals(BriefCardVoiceAction.SAVE, parseBriefCardVoice("저장해 줘"))
    }

    @Test
    fun `내 정보에서 알림을 켜고 끄고 알림 시간과 건강 정보를 연다`() {
        assertEquals(
            ProfileVoice.Toggle(listOf(AppSetting.RECORD_REMINDER), false),
            parseProfileVoice("진료 후 기록 알림 꺼 줘"),
        )
        assertEquals(ProfileVoice.Toggle(listOf(AppSetting.VISIT_REMINDER), true), parseProfileVoice("진료 전 알림 켜"))
        assertEquals(
            ProfileVoice.Toggle(listOf(AppSetting.VISIT_REMINDER, AppSetting.RECORD_REMINDER), false),
            parseProfileVoice("알림 다 꺼 줘"),
        )
        assertEquals(ProfileVoice.ReminderTime, parseProfileVoice("알림 시간 바꿀래"))
        assertEquals(ProfileVoice.ReminderTime, parseProfileVoice("알림 8시로 바꿔 줘"))
        assertEquals(ProfileVoice.HealthEdit, parseProfileVoice("먹는 약 바꿀래"))
        assertEquals(ProfileVoice.ByHand, parseProfileVoice("로그아웃 해 줘"))
        assertNull(parseProfileVoice("알림 설정"))
    }

    @Test
    fun `알림 시각을 숫자와 우리말 수로 읽는다`() {
        assertEquals(LocalTime.of(20, 0), parseVoiceClock("오후 8시로 해 줘"))
        assertEquals(LocalTime.of(20, 0), parseVoiceClock("여덟 시"))
        assertEquals(LocalTime.of(19, 30), parseVoiceClock("저녁 일곱 시 반"))
        assertEquals(LocalTime.of(9, 0), parseVoiceClock("아침 9시"))
        assertEquals(LocalTime.of(21, 30), parseVoiceClock("9시 삼십 분"))
    }

    @Test
    fun `홈에서 이어 하기와 다음 진료와 배너 링크를 말로 부른다`() {
        assertEquals(HomeVoice.RESUME, parseHomeVoice("이어서 할래"))
        assertEquals(HomeVoice.NEXT_VISIT, parseHomeVoice("다음 진료 언제야"))
        assertEquals(HomeVoice.TOMORROW_CARD, parseHomeVoice("내일 가져갈 카드 보여 줘"))
        assertEquals(HomeVoice.EXPORT_CARD, parseHomeVoice("카드 이미지로 저장해 줘"))
        assertEquals(HomeVoice.MESSAGE_GUIDE, parseHomeVoice("예약 문자로 일정 넣는 방법"))
        assertNull(parseHomeVoice("캘린더 보여 줘"))
    }

    @Test
    fun `받아쓰기에 남은 호출어는 뗀다`() {
        assertEquals("", withoutWakeEcho("A 웨이트."))
        assertEquals("", withoutWakeEcho("메이트"))
        assertEquals("홈으로 가 줘", withoutWakeEcho("헤이 메이트, 홈으로 가 줘"))
        assertEquals("다음 진료 언제야", withoutWakeEcho("다음 진료 언제야"))
        assertEquals("네이버", withoutWakeEcho("네이버"))
    }

    @Test
    fun `고를 것은 물음에서 떼어 가이드 자리로 옮긴다`() {
        val choices = splitChoices("왼쪽 팔 어디가 아프세요?\n이 중에서 말씀해 주세요\n· 어깨\n· 손목")
        assertEquals("이 중에서 말씀해 주세요", choices.title)
        assertEquals(listOf("어깨", "손목"), choices.items.map { it.text })
        assertEquals("왼쪽 팔 어디가 아프세요?", choices.rest)

        val questions = splitChoices("담을 질문을 골라 주세요\n1. 검사를 받아야 하나요? (담음)\n2. 약 먹어도 되나요?")
        assertEquals("담음", questions.items.first().note)
        assertEquals("검사를 받아야 하나요?", questions.items.first().text)
        assertNull(questions.rest)

        assertTrue(splitChoices("어느 병원에 가세요?").items.isEmpty())
    }
}
