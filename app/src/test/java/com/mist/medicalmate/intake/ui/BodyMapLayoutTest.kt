package com.mist.medicalmate.intake.ui

import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 인체도를 얼마나 크게 보여줘야 48dp 조작 영역이 겹치지 않는지 본다.
 *
 * 디자인 트랙이 `touch_48dp_check.csv`에 같은 값을 계산해 보냈다. 여기서 다시 계산해
 * 그 표와 맞추는 이유는, 좌표가 바뀌면 필요한 크기도 바뀌는데 화면이 그것을 모르고
 * 예전 높이를 쓰면 점이 겹치기 때문이다. 겹치면 짚은 곳과 골라진 부위가 달라진다.
 */
class BodyMapLayoutTest {
    @Test
    fun `앞면은 220dp보다 좁아지면 점이 겹친다`() {
        val dots = bodyMapAnchorDots(BodyMapView.FRONT, null)

        assertEquals(220.0, bodyMapMinWidth(dots, bodyMapFront.aspectRatio).value.toDouble(), CSV_TOLERANCE)
        assertEquals(505.2, bodyMapMinHeight(dots, bodyMapFront.aspectRatio).value.toDouble(), CSV_TOLERANCE)
    }

    @Test
    fun `확대 화면의 최소 폭이 좌표표의 값과 같다`() {
        val expected =
            mapOf(
                "ANC:001" to 234.8,
                "ANC:002" to 232.3,
                "ANC:003" to 179.3,
                "ANC:004" to 156.2,
                "ANC:013" to 157.1,
                "ANC:014" to 136.0,
                "ANC:012" to 209.0,
            )

        expected.forEach { (anchorId, minWidth) ->
            val anchor = bodyMapAnchorOf(anchorId)
            val detail = requireNotNull(anchor.detail)
            val dots = bodyMapZoneDots(anchor, BodyMapSide.RIGHT, null)

            assertEquals(anchorId, minWidth, bodyMapMinWidth(dots, detail.aspectRatio).value.toDouble(), CSV_TOLERANCE)
        }
    }

    @Test
    fun `가장 좁은 곳은 앞면의 목과 가슴이다`() {
        val dots = bodyMapAnchorDots(BodyMapView.FRONT, null)
        val width = bodyMapMinWidth(dots, bodyMapFront.aspectRatio)

        val neck = dots.single { it.id == "ANC:002@CENTER" }
        val chest = dots.single { it.id == "ANC:003@CENTER" }
        val height = (width / bodyMapFront.aspectRatio).value
        val gap = height * (chest.y - neck.y)

        assertEquals(48.0, gap.toDouble(), CSV_TOLERANCE)
    }

    @Test
    fun `계산한 높이에서 어느 두 점도 겹치지 않는다`() {
        val cases =
            bodyMapAnchorsOn(BodyMapView.FRONT).let {
                listOf(bodyMapAnchorDots(BodyMapView.FRONT, null) to bodyMapFront) +
                    bodyMapAnchors.mapNotNull { anchor ->
                        anchor.detail?.let { bodyMapZoneDots(anchor, BodyMapSide.RIGHT, null) to it }
                    }
            }

        cases.forEach { (dots, image) ->
            val height = bodyMapMinHeight(dots, image.aspectRatio).value
            val width = height * image.aspectRatio
            dots.indices.forEach { i ->
                (i + 1 until dots.size).forEach { j ->
                    val dx = width * (dots[i].x - dots[j].x)
                    val dy = height * (dots[i].y - dots[j].y)
                    val apart = maxOf(dx, -dx, dy, -dy)
                    assertTrue(
                        "${dots[i].id} ↔ ${dots[j].id} 가 ${apart}dp 떨어져 있다",
                        apart >= TOUCH_MIN - ROUNDING,
                    )
                }
            }
        }
    }

    @Test
    fun `뒷면은 점이 하나라 겹칠 상대가 없다`() {
        val dots = bodyMapAnchorDots(BodyMapView.BACK, null)

        assertEquals(1, dots.size)
        assertEquals(0.dp, bodyMapMinWidth(dots, bodyMapBack.aspectRatio))
    }

    @Test
    fun `전신 판은 앞뒤가 같은 높이다`() {
        assertEquals(505.2, bodyMapBodyHeight().value.toDouble(), CSV_TOLERANCE)
    }

    @Test
    fun `확대 판은 최소치와 전신 판 사이에 있다`() {
        bodyMapAnchors.forEach { anchor ->
            val detail = anchor.detail ?: return@forEach
            val dots = bodyMapZoneDots(anchor, BodyMapSide.RIGHT, null)
            val height = bodyMapCardHeight(dots, detail)

            assertTrue(anchor.id, height >= bodyMapMinHeight(dots, detail.aspectRatio))
            assertTrue(anchor.id, height <= bodyMapBodyHeight())
            assertTrue(anchor.id, height * detail.aspectRatio <= MedicalMateSize.contentWidth)
        }
    }

    private companion object {
        /** 좌표표가 소수 한 자리로 반올림한 값을 싣고 있다. */
        const val CSV_TOLERANCE = 0.1

        const val TOUCH_MIN = 48f

        /** 최소 크기를 정확히 맞춘 지점은 부동소수 오차만큼 모자랄 수 있다. */
        const val ROUNDING = 0.01f
    }
}
