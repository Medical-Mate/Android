package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 좌표표와 온톨로지가 어긋나지 않는지 본다.
 *
 * 둘의 출처가 다르다. 좌표는 디자인 트랙의 `humanmap_coords.json`이고 이름과 진료과는
 * AI 트랙의 부위 온톨로지다. 한쪽만 갱신되면 화면에 id가 그대로 나오거나 짚을 수 없는
 * 부위가 생긴다. 눈으로 대조하지 않게 여기서 센다.
 */
class BodyMapGeometryTest {
    @Test
    fun `앵커 9개와 구역 25개다`() {
        assertEquals(9, bodyMapAnchors.size)
        assertEquals(25, bodyMapAnchors.sumOf { it.zones.size })
    }

    @Test
    fun `좌표표의 모든 id가 온톨로지에 있다`() {
        val missing =
            bodyMapAnchors.flatMap { anchor ->
                listOf(anchor.id) + anchor.zones.map { it.id }
            }.filterNot { it in bodyMapOntology }

        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `온톨로지의 모든 id가 좌표표에 있다`() {
        val known =
            bodyMapAnchors.flatMap { anchor -> listOf(anchor.id) + anchor.zones.map { it.id } }.toSet()

        assertEquals(emptySet<String>(), bodyMapOntology.keys - known)
    }

    @Test
    fun `좌표는 이미지 안에 있다`() {
        val outside =
            bodyMapAnchors.flatMap { anchor ->
                anchor.points + anchor.zones.flatMap { it.points }
            }.filterNot { it.x in 0f..1f && it.y in 0f..1f }

        assertEquals(emptyList<BodyMapPoint>(), outside)
    }

    @Test
    fun `전신과 피부는 인체도에 자리가 없다`() {
        assertEquals(listOf("ANC:010", "ANC:011"), bodyMapSideAnchors.map { it.id })
        bodyMapSideAnchors.forEach { anchor ->
            assertNull(anchor.view)
            assertNull(anchor.detail)
            assertTrue(anchor.points.isEmpty())
            assertTrue(anchor.zones.isEmpty())
        }
    }

    @Test
    fun `앞면에 앵커 6종 여덟 점 뒷면에 한 점이 있다`() {
        val front = bodyMapAnchorsOn(BodyMapView.FRONT)
        val back = bodyMapAnchorsOn(BodyMapView.BACK)

        assertEquals(6, front.size)
        assertEquals(8, front.sumOf { it.points.size })
        assertEquals(listOf("ANC:012"), back.map { it.id })
        assertEquals(1, back.sumOf { it.points.size })
    }

    @Test
    fun `팔과 다리만 좌우 공용 한 장을 쓴다`() {
        val mirrored = bodyMapAnchors.filter { it.detail?.mirrored == true }.map { it.id }

        assertEquals(listOf("ANC:013", "ANC:014"), mirrored)
    }

    @Test
    fun `좌우 공용 이미지의 구역은 기준점 하나다`() {
        bodyMapAnchors.filter { it.detail?.mirrored == true }.forEach { anchor ->
            anchor.zones.forEach { zone ->
                assertEquals(zone.id, 1, zone.points.size)
                assertEquals(zone.id, BodyMapSide.BASE, zone.points.single().side)
            }
        }
    }

    @Test
    fun `좌우가 갈리는 구역은 왼쪽과 오른쪽 두 점이다`() {
        bodyMapAnchors.filter { it.detail?.mirrored == false }.forEach { anchor ->
            anchor.zones.filter { it.points.size > 1 }.forEach { zone ->
                assertEquals(
                    zone.id,
                    setOf(BodyMapSide.LEFT, BodyMapSide.RIGHT),
                    zone.points.map { it.side }.toSet(),
                )
            }
        }
    }

    @Test
    fun `구역이 있는 앵커는 확대 이미지를 가진다`() {
        bodyMapAnchors.filter { it.zones.isNotEmpty() }.forEach { anchor ->
            assertNotNull(anchor.id, anchor.detail)
        }
    }

    @Test
    fun `왼쪽 다리를 짚으면 확대 좌표가 뒤집힌다`() {
        val leg = bodyMapAnchorOf("ANC:014")
        val knee = leg.zones.first { it.id == "SUR:091" }.points.single()

        val right = bodyMapZoneDots(leg, BodyMapSide.RIGHT, null).first { it.id == "SUR:091@RIGHT" }
        val left = bodyMapZoneDots(leg, BodyMapSide.LEFT, null).first { it.id == "SUR:091@LEFT" }

        assertEquals(knee.x, right.x, TOLERANCE)
        assertEquals(1f - knee.x, left.x, TOLERANCE)
        assertEquals(knee.y, left.y, TOLERANCE)
    }

    @Test
    fun `점 id는 좌우까지 구분한다`() {
        val eyes = bodyMapZoneDots(bodyMapAnchorOf("ANC:001"), BodyMapSide.CENTER, null)
            .filter { it.id.startsWith("SUR:002") }

        assertEquals(listOf("SUR:002@RIGHT", "SUR:002@LEFT"), eyes.map { it.id })
        assertEquals(listOf("오른쪽 눈", "왼쪽 눈"), eyes.map { it.label })
        assertEquals("SUR:002" to BodyMapSide.LEFT, parseDotId("SUR:002@LEFT"))
    }

    @Test
    fun `짚은 점만 고른 상태로 그려진다`() {
        val selection = BodyMapSelection("ANC:013", side = BodyMapSide.LEFT)
        val dots = bodyMapAnchorDots(BodyMapView.FRONT, selection)

        assertTrue(dots.single { it.id == "ANC:013@LEFT" }.selected)
        assertFalse(dots.single { it.id == "ANC:013@RIGHT" }.selected)
    }

    @Test
    fun `진료과가 빈 구역은 앵커의 진료과를 쓴다`() {
        val knee = BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT)
        val eye = BodyMapSelection("ANC:001", "SUR:002", BodyMapSide.LEFT)

        assertEquals(listOf("정형외과"), knee.departments())
        assertEquals(listOf("안과"), eye.departments())
    }

    @Test
    fun `좌우가 없는 부위에는 방향을 붙이지 않는다`() {
        val nose = BodyMapSelection("ANC:001", "SUR:004")

        assertEquals("코", nose.title())
        assertEquals("코", nose.label())
        assertEquals("왼쪽 눈", BodyMapSelection("ANC:001", "SUR:002", BodyMapSide.LEFT).title())
        assertEquals("눈(왼쪽)", BodyMapSelection("ANC:001", "SUR:002", BodyMapSide.LEFT).label())
    }

    private companion object {
        /** 좌표를 소수 넷째 자리까지 생성했다. 1080px에서 0.1px 안쪽이다. */
        const val TOLERANCE = 0.0001f
    }
}
