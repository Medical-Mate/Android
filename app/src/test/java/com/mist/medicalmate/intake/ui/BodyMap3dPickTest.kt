package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI

/**
 * 짚은 자리에서 부위를 정하는 판정.
 *
 * 실제 에셋으로 본다. 좌표·충돌 메시·카메라·최근접 규칙이 한 줄로 이어져야 짚은 곳과
 * 정해지는 부위가 맞는데, 어느 하나만 어긋나도 화면에서만 드러나기 때문이다. 겨냥한
 * 구역이 그대로 나오는지를 앞뒤 두 면에서 본다.
 *
 * 충돌 메시는 그리는 모델을 줄인 사본이다. 줄인 것이 판정을 바꾸지 않는다는 확인은 만들
 * 때 했다 — 구역 점 46개를 앞뒤에서 겨냥한 92번이 원본과 같은 구역을 골랐다.
 */
class BodyMap3dPickTest {
    @Test
    fun `앞에서 겨냥한 구역이 그대로 나온다`() {
        assertPicks(yaw = 0f, zoneId = "SUR:002", side = BodyMapSide.RIGHT)
        assertPicks(yaw = 0f, zoneId = "SUR:021", side = BodyMapSide.CENTER)
        assertPicks(yaw = 0f, zoneId = "SUR:031", side = BodyMapSide.RIGHT)
        assertPicks(yaw = 0f, zoneId = "SUR:091", side = BodyMapSide.RIGHT)
        assertPicks(yaw = 0f, zoneId = "SUR:072", side = BodyMapSide.LEFT)
        assertPicks(yaw = 0f, zoneId = "SUR:051", side = BodyMapSide.LEFT)
    }

    @Test
    fun `뒤에서 겨냥한 구역이 그대로 나온다`() {
        assertPicks(yaw = PI.toFloat(), zoneId = "SUR:041", side = BodyMapSide.CENTER)
        assertPicks(yaw = PI.toFloat(), zoneId = "SUR:081", side = BodyMapSide.RIGHT)
    }

    @Test
    fun `몸 바깥을 짚으면 아무 데도 닿지 않는다`() {
        val camera = BodyMap3dCamera()

        assertNull(mesh.hit(camera.ray(ndcX = 0.95f, ndcY = 0f, aspect = ASPECT)))
    }

    @Test
    fun `너무 먼 지점은 채택하지 않는다`() {
        // 몸에서 한참 떨어진 자리다. 가장 가까운 구역은 늘 나오지만 고르지는 않는다.
        val pick = bodyMap3dNearest(BodyMap3dVector(0f, 2f, 0f))

        assertFalse(pick.accepted)
        assertTrue(pick.distance > BODY_3D_PICK_MAX_DISTANCE)
    }

    @Test
    fun `구역 점 위에서는 그 구역이 나온다`() {
        val eye = bodyMap3dRegions.first { it.zoneId == "SUR:004" }

        val pick = bodyMap3dNearest(BodyMap3dVector(eye.x, eye.y, eye.z))

        assertEquals("SUR:004", pick.point.zoneId)
        assertEquals(0.0, pick.distance.toDouble(), 1e-6)
    }

    @Test
    fun `좌우가 갈리는 구역은 짚은 쪽으로 정해진다`() {
        val left = bodyMap3dRegions.first { it.zoneId == "SUR:002" && it.side == BodyMapSide.LEFT }

        assertEquals(BodyMapSelection("ANC:001", "SUR:002", BodyMapSide.LEFT), left.toSelection())
    }

    @Test
    fun `좌우가 없는 구역에는 좌우를 붙이지 않는다`() {
        val middle = bodyMap3dRegions.first { it.zoneId == "SUR:041" }

        assertEquals(BodyMapSelection("ANC:012", "SUR:041", BodyMapSide.CENTER), middle.toSelection())
    }

    @Test
    fun `3D의 좌우가 2D의 갈래에 그대로 있다`() {
        // 두 길이 같은 값을 저장해야 한다. 3D 자료에만 있는 좌우가 있으면 2D에 없는 선택이
        // 저장되고, 인체도로 돌아왔을 때 고른 표시가 켜지지 않는다.
        bodyMap3dRegions.forEach { point ->
            val choices = bodyMapChoicesForZone(point.anchorId, point.zoneId)
            assertTrue("2D에 없는 좌우: $point", choices.any { it.side == point.side })
        }
    }

    @Test
    fun `3D 구역이 모두 온톨로지에 있다`() {
        // 3D에서 고른 것과 2D에서 고른 것이 같은 값이어야 한다. 여기가 어긋나면 3D로 고른
        // 부위만 이름 없이 id로 나온다.
        val zones = bodyMap3dRegions.map { it.zoneId }.toSet()

        zones.forEach { zoneId ->
            assertNotNull("온톨로지에 없는 구역: $zoneId", bodyMapAnchorIdOfZone(zoneId))
        }
    }

    @Test
    fun `3D 구역이 2D 구역을 모두 덮는다`() {
        // 한쪽에만 있는 부위가 생기면 3D로는 고를 수 없는 곳이 남는다.
        val flat = bodyMapGroups.flatMap { group -> group.zones.map { it.id } }.toSet()

        assertEquals(flat, bodyMap3dRegions.map { it.zoneId }.toSet())
    }

    /** 그 구역 점을 화면에서 겨냥해 짚었을 때 같은 구역이 나오는지. */
    private fun assertPicks(yaw: Float, zoneId: String, side: BodyMapSide) {
        val camera = BodyMap3dCamera(yaw = yaw)
        val target = bodyMap3dRegions.first { it.zoneId == zoneId && it.side == side }
        val projected = camera.project(BodyMap3dVector(target.x, target.y, target.z), ASPECT)
        assertNotNull("화면 밖의 점: $zoneId", projected)

        val hit = mesh.hit(camera.ray(projected!!.first, projected.second, ASPECT))
        assertNotNull("어디에도 닿지 않았다: $zoneId", hit)
        val pick = bodyMap3dNearest(hit!!)

        assertTrue("채택되지 않았다: $zoneId", pick.accepted)
        assertEquals(zoneId, pick.point.zoneId)
        assertEquals(side, pick.point.side)
    }

    private companion object {
        const val ASPECT = 320f / 505f

        /** 실제 에셋. 시험이 모듈 폴더에서 돌아서 그대로 열 수 있다. */
        val mesh: BodyMap3dMesh by lazy {
            val file = sequenceOf("src/main/assets", "app/src/main/assets")
                .map { File(it, "body3d/body_collision.bin") }
                .firstOrNull { it.exists() }
            readBodyMap3dMesh(requireNotNull(file) { "충돌 메시 에셋을 찾지 못했습니다." }.readBytes())
        }
    }
}
