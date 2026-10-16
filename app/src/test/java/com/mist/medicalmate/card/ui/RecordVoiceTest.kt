package com.mist.medicalmate.card.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** 기록 탭에서 말로 기록을 찾는다(#324). */
class RecordVoiceTest {
    private val today = LocalDate.of(2026, 10, 16)

    private val records =
        listOf(
            record("3", "무릎 통증", "튼튼정형외과의원", LocalDate.of(2026, 10, 6)),
            record("2", "복부 통증", "서울하나내과의원", LocalDate.of(2026, 10, 2)),
            record("1", "두통", "서울하나내과의원", LocalDate.of(2026, 9, 12)),
        )

    @Test
    fun `몇 번째와 최근으로 연다`() {
        assertEquals(listOf("2"), records.findRecordByVoice("두 번째 기록 열어 줘", today)?.map { it.id })
        assertEquals(listOf("3"), records.findRecordByVoice("최근 기록 보여 줘", today)?.map { it.id })
        assertEquals(listOf("1"), records.findRecordByVoice("마지막 거", today)?.map { it.id })
    }

    @Test
    fun `날짜와 달로 찾는다`() {
        assertEquals(listOf("2"), records.findRecordByVoice("10월 2일 기록", today)?.map { it.id })
        assertEquals(listOf("3", "2"), records.findRecordByVoice("10월 기록 보여 줘", today)?.map { it.id })
    }

    @Test
    fun `병원과 제목의 낱말로 찾는다`() {
        assertEquals(listOf("3"), records.findRecordByVoice("정형외과 기록", today)?.map { it.id })
        assertEquals(listOf("2", "1"), records.findRecordByVoice("서울하나내과 다녀온 거", today)?.map { it.id })
        assertEquals(listOf("2"), records.findRecordByVoice("복부 통증 기록 열어 줘", today)?.map { it.id })
    }

    @Test
    fun `단서가 없으면 이 화면의 말이 아니다`() {
        assertNull(records.findRecordByVoice("기록 보여 줘", today))
        assertNull(records.findRecordByVoice("홈으로 가", today))
    }

    @Test
    fun `몇 건인지 묻는 말과 지우라는 말을 가린다`() {
        assertEquals(RecordVoiceIntent.COUNT, parseRecordIntent("기록 몇 개야"))
        assertEquals(RecordVoiceIntent.DELETE, parseRecordIntent("두 번째 기록 지워 줘"))
    }

    private fun record(id: String, title: String, clinic: String, on: LocalDate) = RecordItem(
        id = id,
        title = title,
        status = RecordItem.Status.CONFIRMED,
        clinic = clinic,
        meta = "",
        visitedOn = on,
    )
}
