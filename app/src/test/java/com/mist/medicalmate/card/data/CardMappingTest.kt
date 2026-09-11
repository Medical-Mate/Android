package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 응답을 카드로 옮기는 부분.
 *
 * 서버가 본문을 고정 필드에서 가변 목록으로 바꾸는 중이라 [CardMapping]만 갈아 끼우면 되게
 * 모아 뒀다. 이 시험도 그때 함께 바뀐다.
 */
class CardMappingTest {
    @Test
    fun `고정 필드를 KV 줄로 편다`() {
        val card = response().toBriefCard()

        assertEquals(listOf("부위", "기간", "양상", "복용약"), card.items.map { it.key })
        assertEquals("복부 (명치 아래)", card.items.first().value)
    }

    @Test
    fun `값이 안 온 필드는 줄도 만들지 않는다`() {
        val card = CardResponse(cardId = 1, onset = known("3주 전")).toBriefCard()

        assertEquals(listOf("기간"), card.items.map { it.key })
    }

    @Test
    fun `없어요와 모르겠어요를 다르게 적는다`() {
        // 둘을 같게 그리면 의사용 카드에 "본인 확인 못 함"을 찍을 수 없다.
        val card =
            CardResponse(
                cardId = 1,
                onset = TextFieldResponse(status = "NONE"),
                pattern = TextFieldResponse(status = "UNKNOWN"),
            ).toBriefCard()

        assertEquals("없음", card.items.first { it.key == "기간" }.value)
        assertEquals("잘 모르겠어요", card.items.first { it.key == "양상" }.value)
    }

    @Test
    fun `복용약은 이름과 메모를 한 줄로 잇는다`() {
        val card = response().toBriefCard()

        assertEquals("혈압약 아침", card.items.first { it.key == "복용약" }.value)
    }

    @Test
    fun `알러지는 쉼표로 나눈다`() {
        assertEquals(listOf("페니실린", "아스피린"), response().toBriefCard().allergies)
    }

    @Test
    fun `알러지가 없으면 경고 줄도 없다`() {
        // 없다는 것은 경고할 일이 아니다.
        val card = CardResponse(cardId = 1, allergies = TextFieldResponse(status = "NONE")).toBriefCard()

        assertTrue(card.allergies.isEmpty())
    }

    @Test
    fun `알러지를 확인 못 했으면 그 사실을 남긴다`() {
        val card = CardResponse(cardId = 1, allergies = TextFieldResponse(status = "UNKNOWN")).toBriefCard()

        assertEquals(listOf("잘 모르겠어요"), card.allergies)
    }

    @Test
    fun `환자 줄은 이름 나이 성별 작성일이다`() {
        assertEquals("김OO · 32세 여 · 2026.09.04 작성", response().toBriefCard().patientLine)
    }

    @Test
    fun `확정 여부가 상태가 된다`() {
        assertEquals(BriefCard.Status.CONFIRMED, response("CONFIRMED").toBriefCard().status)
        assertEquals(BriefCard.Status.BEFORE_VISIT, response("DRAFT").toBriefCard().status)
    }

    @Test
    fun `강도와 병원은 서버에 자리가 없어 비운다`() {
        val card = response().toBriefCard()

        assertNull(card.severity)
        assertNull(card.hospital)
    }

    @Test
    fun `전달 응답은 넘겨받은 id와 확정 상태를 쓴다`() {
        // 전달 경로는 확정한 카드만 열린다. 응답에 id가 없어 부른 쪽이 넘긴다.
        val card =
            HandoffResponse(
                patient = PatientResponse(name = "김OO", age = 32, sex = "FEMALE"),
                title = "복부 통증",
                onset = known("3주 전"),
                confirmedAt = "2026-09-04T09:00:00+09:00",
            ).toBriefCard(cardId = 42)

        assertEquals("42", card.id)
        assertEquals(BriefCard.Status.CONFIRMED, card.status)
        assertEquals("복부 통증", card.title)
    }

    private fun known(text: String) = TextFieldResponse(status = "KNOWN", text = text)

    private fun response(status: String = "DRAFT") = CardResponse(
        cardId = 1,
        status = status,
        patient = PatientResponse(name = "김OO", age = 32, sex = "FEMALE"),
        title = "복부 통증 · 3주",
        onset = known("3주 전 시작"),
        pattern = known("식후 쓰림"),
        site = SiteResponse(status = "KNOWN", text = "복부 (명치 아래)"),
        medications =
        MedicationsResponse(status = "KNOWN", items = listOf(MedicationResponse(name = "혈압약", note = "아침"))),
        allergies = known("페니실린, 아스피린"),
        questions = listOf("검사를 받아야 하나요?"),
        createdAt = "2026-09-04T09:00:00+09:00",
    )
}
