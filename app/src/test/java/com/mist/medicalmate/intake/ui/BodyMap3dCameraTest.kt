package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/**
 * 3D 인체도를 보는 자리의 계산.
 *
 * 그리는 쪽(Filament)과 짚은 자리를 판정하는 쪽이 같은 값을 써야 보이는 자리와 짚히는
 * 자리가 맞는다. 그 값을 만드는 것이 여기 있는 함수들이고, [ray]와 [project]가 서로의
 * 역인지가 그 조건을 그대로 옮긴 시험이다.
 */
class BodyMap3dCameraTest {
    @Test
    fun `앞면에서는 카메라가 +Z에 선다`() {
        val eye = BodyMap3dCamera().eye

        assertEquals(0.0, eye.x.toDouble(), DELTA)
        assertEquals(BODY_3D_HOME_DISTANCE.toDouble(), eye.z.toDouble(), DELTA)
    }

    @Test
    fun `뒷면에서는 카메라가 -Z에 선다`() {
        val eye = BodyMap3dCamera().facing(BodyMapView.BACK).eye

        assertEquals((-BODY_3D_HOME_DISTANCE).toDouble(), eye.z.toDouble(), DELTA)
    }

    @Test
    fun `앞면을 볼 때 화면 오른쪽이 월드 +X다`() {
        // 마주 선 사람을 보는 것과 같다. 환자의 오른쪽(−x)이 화면 왼쪽에 온다.
        val basis = BodyMap3dCamera().basis()

        assertEquals(1.0, basis.right.x.toDouble(), DELTA)
    }

    @Test
    fun `화면 한가운데를 지나는 반직선은 보는 지점으로 간다`() {
        val camera = BodyMap3dCamera()
        val ray = camera.ray(ndcX = 0f, ndcY = 0f, aspect = ASPECT)

        val toTarget = (BODY_3D_TARGET - ray.origin).normalized()
        assertEquals(toTarget.x.toDouble(), ray.direction.x.toDouble(), DELTA)
        assertEquals(toTarget.y.toDouble(), ray.direction.y.toDouble(), DELTA)
        assertEquals(toTarget.z.toDouble(), ray.direction.z.toDouble(), DELTA)
    }

    @Test
    fun `투영과 반직선이 서로의 역이다`() {
        // 화면에 점을 얹는 계산과 짚은 자리에서 반직선을 만드는 계산이 어긋나면, 보이는
        // 점과 그 점을 눌렀을 때 찾아지는 자리가 달라진다.
        val camera = BodyMap3dCamera(yaw = 0.4f, pitch = 0.2f, distance = 1.4f)
        val point = BodyMap3dVector(0.05f, 0.18f, 0.04f)

        val projected = camera.project(point, ASPECT)
        assertNotNull(projected)
        val ray = camera.ray(projected!!.first, projected.second, ASPECT)

        val distance = point.distanceTo(ray.origin)
        val onRay = ray.at(distance)
        assertEquals(point.x.toDouble(), onRay.x.toDouble(), DELTA)
        assertEquals(point.y.toDouble(), onRay.y.toDouble(), DELTA)
        assertEquals(point.z.toDouble(), onRay.z.toDouble(), DELTA)
    }

    @Test
    fun `카메라 뒤의 점은 화면에 얹지 않는다`() {
        val camera = BodyMap3dCamera()

        assertNull(camera.project(BodyMap3dVector(0f, 0f, 10f), ASPECT))
    }

    @Test
    fun `위아래로 도는 것은 뒤집히기 전에 멈춘다`() {
        val turned = BodyMap3dCamera().turned(deltaYaw = 0f, deltaPitch = 100f)

        assertTrue(turned.pitch < (PI / 2).toFloat())
    }

    @Test
    fun `좌우로 도는 것은 막지 않는다`() {
        // 몸을 몇 바퀴 돌려도 되는 조작이다. 각도를 접으면 되감기는 전환이 생긴다.
        val turned = BodyMap3dCamera().turned(deltaYaw = 100f, deltaPitch = 0f)

        assertEquals(100.0, turned.yaw.toDouble(), DELTA)
    }

    @Test
    fun `확대에는 한계가 있다`() {
        val near = BodyMap3dCamera().zoomed(scale = 1000f)
        val far = BodyMap3dCamera().zoomed(scale = 0.001f)

        assertTrue(near.distance in 0f..BODY_3D_HOME_DISTANCE)
        assertTrue(far.distance in BODY_3D_HOME_DISTANCE..(BODY_3D_HOME_DISTANCE * 2f))
    }

    @Test
    fun `면을 바꿀 때는 가까운 쪽으로 돈다`() {
        // 세 바퀴 돌려 둔 상태에서 앞면을 누르면 0으로 되감지 않고 그 자리의 앞면으로 간다.
        val spun = BodyMap3dCamera(yaw = (6 * PI).toFloat() + 0.2f)

        val front = spun.facing(BodyMapView.FRONT)

        assertEquals(6 * PI, front.yaw.toDouble(), DELTA)
    }

    @Test
    fun `돌리다 보면 보고 있는 면이 바뀐다`() {
        // 앞뒤 토글의 선택 상태가 손가락을 따라와야 한다.
        assertEquals(BodyMapView.FRONT, BodyMap3dCamera(yaw = 0.3f).view())
        assertEquals(BodyMapView.BACK, BodyMap3dCamera(yaw = PI.toFloat()).view())
        assertEquals(BodyMapView.BACK, BodyMap3dCamera(yaw = -PI.toFloat() * 0.9f).view())
    }

    private companion object {
        const val DELTA = 1e-4

        /** 화면 판의 비율. 폭 320에 높이 505다. */
        const val ASPECT = 320f / 505f
    }
}
