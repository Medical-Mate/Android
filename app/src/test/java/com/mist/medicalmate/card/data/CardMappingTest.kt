package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 응답을 카드로 옮기는 부분.
 *
 * 2026-09-11에 서버가 본문을 고정 필드에서 `axes` 맵으로 바꿨다. 이 시험도 그때 함께
 * 바뀌었다.
 */
class CardMappingTest {
    @Test
    fun `채워진 축만 줄이 된다`() {
        // 8축이 늘 자리를 차지하고 1턴째는 대부분 NOT_ASKED다. 그대로 그리면 빈 줄 여덟 개다.
        val card = response().toBriefCard()

        assertEquals(listOf("부위", "시작"), card.items.map { it.key })
        assertEquals("왼쪽 무릎", card.items.first().value)
    }

    @Test
    fun `묻지 않은 축과 건너뛴 축은 줄을 만들지 않는다`() {
        val card =
            response(
                axes = mapOf(
                    "onset" to axis("NOT_ASKED"),
                    "character" to axis("SKIPPED"),
                ),
            ).toBriefCard()

        assertTrue(card.items.isEmpty())
    }

    @Test
    fun `모르겠다는 남긴다`() {
        // 확인하지 못했다는 것이 의사에게는 정보다.
        val card = response(axes = mapOf("onset" to axis("UNKNOWN"))).toBriefCard()

        assertEquals("잘 모르겠어요", card.items.single().value)
    }

    @Test
    fun `확실하지 않은 값은 그 사실을 함께 적는다`() {
        val card = response(axes = mapOf("onset" to axis("AMBIGUOUS", "3주 전"))).toBriefCard()

        assertEquals("3주 전 (확실하지 않아요)", card.items.single().value)
    }

    @Test
    fun `축 차례는 서버 순서가 아니라 읽는 차례다`() {
        // 맵이라 순서가 보장되지 않는다. 카드에서 부위가 먼저 온다.
        val card =
            response(
                axes = mapOf(
                    "onset" to axis("FILLED", "3주 전"),
                    "site" to axis("FILLED", "왼쪽 무릎"),
                ),
            ).toBriefCard()

        assertEquals(listOf("부위", "시작"), card.items.map { it.key })
    }

    @Test
    fun `강도는 줄이 아니라 눈금으로 간다`() {
        // 시안이 칩·낱말·NRS 등가를 한 줄로 그린다. KV 줄 하나로는 그 모양이 안 나온다.
        val card =
            response(
                axes = mapOf(
                    "site" to axis("FILLED", "왼쪽 무릎"),
                    "severity" to axis("FILLED", "3 (꽤 아파요)"),
                ),
            ).toBriefCard()

        assertEquals(MedicalMateSeverity.LEVEL_3, card.severity)
        assertEquals(listOf("부위"), card.items.map { it.key })
    }

    @Test
    fun `눈금 밖의 값은 그리지 않는다`() {
        // 눈금이 다섯 단계다. 모르는 값을 억지로 끼우면 환자가 고른 것과 다른 색이 나온다.
        val card = response(axes = mapOf("severity" to axis("FILLED", "9점"))).toBriefCard()

        assertNull(card.severity)
    }

    @Test
    fun `답하지 않은 강도는 눈금이 없다`() {
        val card = response(axes = mapOf("severity" to axis("NOT_ASKED"))).toBriefCard()

        assertNull(card.severity)
    }

    @Test
    fun `제목이 없으면 환자가 말한 것을 쓴다`() {
        // 서버가 title을 아직 내려주지 않는다.
        assertEquals("왼쪽 무릎이 아파요", response().toBriefCard().title)
    }

    @Test
    fun `확정 여부가 상태가 된다`() {
        assertEquals(BriefCard.Status.CONFIRMED, response(status = "CONFIRMED").toBriefCard().status)
        assertEquals(BriefCard.Status.BEFORE_VISIT, response(status = "DRAFT").toBriefCard().status)
    }

    @Test
    fun `환자 줄은 이름 나이 성별 작성일이다`() {
        assertEquals("김OO · 32세 여 · 2026.09.04 작성", response().toBriefCard().patientLine)
    }

    @Test
    fun `건강 정보와 병원은 카드 응답에 없어 비운다`() {
        // 알러지·복용약·기저질환은 화면이 프로필에서 읽어 얹는다(#167).
        val card = response().toBriefCard()

        assertTrue(card.allergies.isEmpty())
        assertTrue(card.health.isEmpty())
        assertNull(card.hospital)
    }

    @Test
    fun `전달 응답은 넘겨받은 id와 확정 상태를 쓴다`() {
        val card =
            HandoffResponse(
                patient = PatientResponse(name = "김OO", age = 32, sex = "FEMALE"),
                chiefComplaint = "왼쪽 무릎이 아파요",
                axes = mapOf("site" to axis("FILLED", "왼쪽 무릎")),
                confirmedAt = "2026-09-04T09:00:00+09:00",
            ).toBriefCard(cardId = 42)

        assertEquals("42", card.id)
        assertEquals(BriefCard.Status.CONFIRMED, card.status)
        assertEquals("왼쪽 무릎", card.items.single().value)
    }

    @Test
    fun `목록 제목이 길면 줄인다`() {
        // 서버가 환자 원문을 제목 자리에 준다. 목록 줄은 한 줄이라 넘치면 무엇인지 알 수 없다.
        val long = "왼쪽 무릎이 계단 내려갈 때마다 시큰거리고 밤에도 욱신거려요"

        val item = CardSummaryResponse(cardId = 1, chiefComplaint = long, createdAt = CREATED_AT).toListItem()

        assertTrue(item.title.length < long.length)
        assertTrue(item.title.endsWith("…"))
        // 자른 자리에 공백이 걸리면 함께 턴다. 말줄임 앞이 어색하게 벌어지지 않게.
        assertTrue(item.title.dropLast(1) == item.title.dropLast(1).trimEnd())
    }

    @Test
    fun `복용약과 기저질환이 카드 줄로 온다`() {
        // 전에는 카드 응답에 없어 화면이 프로필에서 읽어 얹었다. 이제 서버가 박아 준다(#181).
        val card = response(patient = FILLED_PATIENT).toBriefCard()

        assertEquals(listOf("복용약", "기저질환"), card.health.map { it.key })
        assertEquals(listOf("혈압약 · 진통제", "고혈압"), card.health.map { it.value })
    }

    @Test
    fun `알러지는 줄이 아니라 경고로 간다`() {
        // 처방을 바꾸는 값이라 카드 밖 경고 면에 얹힌다.
        val card = response(patient = FILLED_PATIENT).toBriefCard()

        assertEquals(listOf("페니실린", "조개"), card.allergies)
        assertTrue(card.health.none { it.key == "알러지" })
    }

    @Test
    fun `적은 것이 없는 갈래는 줄을 만들지 않는다`() {
        // "없어요"와 "잘 모르겠어요"를 카드에 적지 않는다. 비어 있다는 것은 빈 자리로 전해진다.
        val card =
            response(
                patient =
                PatientResponse(
                    medications = CardListFieldResponse(status = "NONE"),
                    conditions = CardListFieldResponse(status = "UNKNOWN"),
                    allergies = CardTextFieldResponse(status = "NONE"),
                ),
            ).toBriefCard()

        assertTrue(card.health.isEmpty())
        assertTrue(card.allergies.isEmpty())
    }

    @Test
    fun `건강 정보가 없는 응답도 카드가 된다`() {
        // 옛 카드에는 이 값이 없다. 그 줄만 빠지고 본문은 그대로 그린다.
        val card = response().toBriefCard()

        assertTrue(card.health.isEmpty())
        assertTrue(card.allergies.isEmpty())
        assertEquals("왼쪽 무릎이 아파요", card.title)
    }

    private fun axis(status: String, value: String? = null) =
        AxisResponse(status = status, value = value, evidence = listOfNotNull(value))

    private fun response(
        status: String = "DRAFT",
        patient: PatientResponse = PatientResponse(name = "김OO", age = 32, sex = "FEMALE"),
        axes: Map<String, AxisResponse> = mapOf(
            "site" to axis("FILLED", "왼쪽 무릎"),
            "onset" to axis("FILLED", "3주 전"),
            "character" to axis("NOT_ASKED"),
        ),
    ) = CardResponse(
        cardId = 1,
        status = status,
        patient = patient,
        chiefComplaint = "왼쪽 무릎이 아파요",
        axes = axes,
        questions = listOf("검사를 받아야 하나요?"),
        createdAt = CREATED_AT,
    )

    private companion object {
        const val CREATED_AT = "2026-09-04T09:00:00+09:00"

        /** 세 갈래를 다 적어 둔 환자. 알러지는 한 줄로 와서 쉼표로 나눈다. */
        val FILLED_PATIENT =
            PatientResponse(
                name = "김OO",
                age = 32,
                sex = "FEMALE",
                medications = CardListFieldResponse(status = "KNOWN", items = listOf("혈압약", "진통제")),
                conditions = CardListFieldResponse(status = "KNOWN", items = listOf("고혈압")),
                allergies = CardTextFieldResponse(status = "KNOWN", text = "페니실린, 조개"),
            )
    }
}
