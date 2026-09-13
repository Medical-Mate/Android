package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 짚은 자리를 찾는 메시.
 *
 * 손으로 만든 작은 메시로 본다. 실제 인체 메시로 보면 어디가 틀렸는지가 드러나지 않고,
 * 교차 판정은 좌표가 무엇이든 같은 식이다. 실제 에셋으로 보는 것은
 * [BodyMap3dPickTest]에 따로 있다.
 */
class BodyMap3dMeshTest {
    @Test
    fun `앞을 막은 면에 닿는다`() {
        val mesh = quad(z = 0f)

        val hit = mesh.hit(downRay(x = 0f, y = 0f))

        assertEquals(0.0, hit?.z?.toDouble() ?: Double.NaN, DELTA)
    }

    @Test
    fun `면 밖으로 나간 반직선은 닿지 않는다`() {
        val mesh = quad(z = 0f)

        assertNull(mesh.hit(downRay(x = 5f, y = 0f)))
    }

    @Test
    fun `뒤에 있는 면은 닿지 않는다`() {
        // 반직선은 한쪽으로만 뻗는다. 카메라 뒤의 몸이 걸리면 등을 짚은 것이 된다.
        val mesh = quad(z = 10f)

        assertNull(mesh.hit(downRay(x = 0f, y = 0f)))
    }

    @Test
    fun `두 면이 겹치면 가까운 쪽에 닿는다`() {
        val mesh = quads(0f, 2f)

        val hit = mesh.hit(downRay(x = 0f, y = 0f))

        assertEquals(2.0, hit?.z?.toDouble() ?: Double.NaN, DELTA)
    }

    @Test
    fun `뒷면도 센다`() {
        // 확대해 몸 안으로 들어간 카메라에서는 뒷면만 남는다. 앞면만 세면 아무 데도
        // 짚히지 않는 화면이 된다.
        val mesh = quad(z = 0f)
        val inside = BodyMap3dRay(BodyMap3dVector(0f, 0f, -5f), BodyMap3dVector(0f, 0f, 1f))

        assertEquals(0.0, mesh.hit(inside)?.z?.toDouble() ?: Double.NaN, DELTA)
    }

    @Test
    fun `파일에서 읽은 좌표가 살아 있다`() {
        val mesh = readBodyMap3dMesh(encode(quadPositions(z = 0.25f), QUAD_INDICES))

        assertEquals(2, mesh.triangleCount)
        assertEquals(0.25, mesh.hit(downRay(x = 0.1f, y = 0.1f))?.z?.toDouble() ?: Double.NaN, QUANTIZED_DELTA)
    }

    @Test
    fun `다른 파일은 읽지 않는다`() {
        val bytes = encode(quadPositions(z = 0f), QUAD_INDICES).copyOf()
        bytes[0] = 'X'.code.toByte()

        assertThrows(IllegalArgumentException::class.java) { readBodyMap3dMesh(bytes) }
    }

    /** +Z 쪽에서 몸을 향해 쏘는 반직선. */
    private fun downRay(x: Float, y: Float) = BodyMap3dRay(BodyMap3dVector(x, y, 5f), BodyMap3dVector(0f, 0f, -1f))

    private fun quadPositions(z: Float) = floatArrayOf(
        -1f, -1f, z,
        1f, -1f, z,
        1f, 1f, z,
        -1f, 1f, z,
    )

    private fun quad(z: Float) = BodyMap3dMesh(quadPositions(z), QUAD_INDICES)

    private fun quads(first: Float, second: Float) = BodyMap3dMesh(
        quadPositions(first) + quadPositions(second),
        QUAD_INDICES + QUAD_INDICES.map { it + 4 }.toIntArray(),
    )

    /** 생성 스크립트가 쓰는 형식 그대로. */
    private fun encode(positions: FloatArray, indices: IntArray): ByteArray {
        val minimum = FloatArray(3) { axis -> positions.filterIndexed { i, _ -> i % 3 == axis }.min() }
        val maximum = FloatArray(3) { axis -> positions.filterIndexed { i, _ -> i % 3 == axis }.max() }
        val scale = FloatArray(3) { axis -> ((maximum[axis] - minimum[axis]) / UNSIGNED_MAX).coerceAtLeast(1e-9f) }
        val buffer = ByteBuffer
            .allocate(HEADER_BYTES + positions.size * 2 + indices.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("MMC1".toByteArray())
        buffer.putInt(positions.size / 3)
        buffer.putInt(indices.size / 3)
        minimum.forEach { buffer.putFloat(it) }
        scale.forEach { buffer.putFloat(it) }
        positions.forEachIndexed { index, value ->
            val axis = index % 3
            buffer.putShort(((value - minimum[axis]) / scale[axis]).toInt().toShort())
        }
        indices.forEach { buffer.putShort(it.toShort()) }
        return buffer.array()
    }

    private companion object {
        const val DELTA = 1e-4

        /** 16비트로 접었다 편 좌표의 오차. */
        const val QUANTIZED_DELTA = 1e-3
        const val UNSIGNED_MAX = 65535f
        const val HEADER_BYTES = 36
        val QUAD_INDICES = intArrayOf(0, 1, 2, 0, 2, 3)
    }
}
