package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/**
 * 큰 부위를 짚었을 때 카메라가 가는 자리.
 *
 * 2D 인체도의 확대 화면에 해당한다. 거기서는 부위마다 그려 둔 이미지를 바꿔 끼웠는데,
 * 3D에서는 같은 몸을 다른 자리에서 볼 뿐이라 그 자리를 계산으로 만든다.
 */
class BodyMap3dFrameTest {
    @Test
    fun `앵커마다 확대할 자리가 있다`() {
        // 하나라도 비면 그 부위를 짚었을 때 아무 일도 일어나지 않는다.
        bodyMap3dAnchors.map { it.anchorId }.distinct().forEach { anchorId ->
            assertNotNull("확대할 자리가 없는 앵커: $anchorId", bodyMap3dFrameOf(anchorId))
        }
    }

    @Test
    fun `거리는 부위 크기에서 나온다`() {
        // 머리와 팔은 크기가 네 배 가까이 다르다. 같은 거리로 보면 한쪽이 화면을 넘친다.
        val head = frame("ANC:001").camera(BodyMapSide.CENTER, BodyMap3dCamera())
        val arm = frame("ANC:013").camera(BodyMapSide.RIGHT, BodyMap3dCamera())

        assertEquals(0.094 * 2.5 + 0.07, head.distance.toDouble(), DELTA)
        assertTrue(arm.distance > head.distance)
    }

    @Test
    fun `한쪽만 담는 부위는 왼쪽에서 좌우가 뒤집힌다`() {
        // 몸이 좌우 대칭이라 자료에 오른쪽 자리만 있다.
        val right = frame("ANC:013").camera(BodyMapSide.RIGHT, BodyMap3dCamera())
        val left = frame("ANC:013").camera(BodyMapSide.LEFT, BodyMap3dCamera())

        assertEquals(-left.target.x.toDouble(), right.target.x.toDouble(), DELTA)
        assertTrue(right.target.x < 0f)
    }

    @Test
    fun `좌우가 함께 보이는 부위는 뒤집지 않는다`() {
        val middle = frame("ANC:004").camera(BodyMapSide.LEFT, BodyMap3dCamera())

        assertEquals(0.0, middle.target.x.toDouble(), DELTA)
    }

    @Test
    fun `뒷면 부위는 뒤에서 본다`() {
        val back = frame("ANC:012").camera(BodyMapSide.CENTER, BodyMap3dCamera())

        assertEquals(BodyMapView.BACK, back.view())
        assertEquals(PI, back.yaw.toDouble(), DELTA)
    }

    @Test
    fun `돌려 둔 상태에서도 가까운 쪽으로 돈다`() {
        // 두 바퀴 돌려 둔 채로 부위를 짚으면 0으로 되감지 않는다.
        val spun = BodyMap3dCamera(yaw = (4 * PI).toFloat())

        val camera = frame("ANC:003").camera(BodyMapSide.CENTER, spun)

        assertEquals(4 * PI, camera.yaw.toDouble(), DELTA)
    }

    @Test
    fun `보는 깊이는 절반만 쓴다`() {
        // 구역 점이 표면 위에 있어서 그 자리를 그대로 보면 카메라가 몸에 붙는다.
        val frame = frame("ANC:004")

        val camera = frame.camera(BodyMapSide.CENTER, BodyMap3dCamera())

        assertEquals((frame.target.z / 2f).toDouble(), camera.target.z.toDouble(), DELTA)
    }

    private fun frame(anchorId: String) = requireNotNull(bodyMap3dFrameOf(anchorId))

    private companion object {
        const val DELTA = 1e-4
    }
}
