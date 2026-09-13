package com.mist.medicalmate.intake.ui

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * 짚은 자리를 찾는 데만 쓰는 인체 메시.
 *
 * 화면에 그리는 것은 `body3d/body.glb`이고 이 메시는 그것을 정점 군집화로 줄여 16비트로
 * 양자화한 사본이다(4만 6천 삼각형). 그리는 메시를 그대로 쓰지 않는 이유는 두 가지다.
 * 압축을 푼 정점을 렌더러에서 되받을 길이 마땅치 않고, 22만 6천 삼각형을 한 번 짚을
 * 때마다 도는 것도 그만큼 느리다.
 *
 * 줄여도 판정이 달라지지 않는 것은 확인했다. 앞뒤 두 카메라에서 구역 점 46개를 겨냥해
 * 쏜 92번이 원본과 같은 구역을 골랐고, 닿은 지점의 차이는 평균 신장의 0.01% · 최대
 * 0.14%였다. 채택 문턱이 10%다.
 *
 * 좌표를 [BodyMap3dVector] 배열로 들지 않고 원시 배열로 둔다. 한 번 짚을 때마다 전부
 * 도는 자리라 삼각형마다 객체를 만들면 그만큼 쓰레기가 쌓인다.
 */
internal class BodyMap3dMesh(private val positions: FloatArray, private val indices: IntArray) {
    val triangleCount: Int get() = indices.size / 3

    /**
     * 반직선이 처음 닿는 표면의 한 점. 닿지 않으면 null이다.
     *
     * Möller–Trumbore이고 뒷면도 센다. 몸이 닫힌 면이라 밖에서 쏘면 앞면이 먼저 걸리지만,
     * 확대해 몸 안쪽에 들어간 카메라에서는 뒷면만 남는다.
     */
    fun hit(ray: BodyMap3dRay): BodyMap3dVector? {
        var nearest = Float.MAX_VALUE
        var index = 0
        while (index < indices.size) {
            val distance = intersect(ray, indices[index] * 3, indices[index + 1] * 3, indices[index + 2] * 3)
            if (distance < nearest) nearest = distance
            index += 3
        }
        return if (nearest == MISS) null else ray.at(nearest)
    }

    /**
     * 삼각형 하나와의 거리. 빗나가면 [MISS]다.
     *
     * 세 조건을 따로 빠져나가지 않고 끝에서 한 번에 본다. 무게중심 좌표가 `u ≥ 0` ·
     * `v ≥ 0` · `u + v ≤ 1`이면 삼각형 안이고, `u ≤ 1`은 뒤의 둘에서 따라 나온다.
     */
    private fun intersect(ray: BodyMap3dRay, a: Int, b: Int, c: Int): Float {
        val e1x = positions[b] - positions[a]
        val e1y = positions[b + 1] - positions[a + 1]
        val e1z = positions[b + 2] - positions[a + 2]
        val e2x = positions[c] - positions[a]
        val e2y = positions[c + 1] - positions[a + 1]
        val e2z = positions[c + 2] - positions[a + 2]
        val direction = ray.direction
        val px = direction.y * e2z - direction.z * e2y
        val py = direction.z * e2x - direction.x * e2z
        val pz = direction.x * e2y - direction.y * e2x
        val determinant = e1x * px + e1y * py + e1z * pz
        if (abs(determinant) < EPSILON) return MISS
        val inverse = 1f / determinant
        val tx = ray.origin.x - positions[a]
        val ty = ray.origin.y - positions[a + 1]
        val tz = ray.origin.z - positions[a + 2]
        val u = (tx * px + ty * py + tz * pz) * inverse
        val qx = ty * e1z - tz * e1y
        val qy = tz * e1x - tx * e1z
        val qz = tx * e1y - ty * e1x
        val v = (direction.x * qx + direction.y * qy + direction.z * qz) * inverse
        val distance = (e2x * qx + e2y * qy + e2z * qz) * inverse
        val inside = u >= 0f && v >= 0f && u + v <= 1f
        return if (inside && distance > EPSILON) distance else MISS
    }
}

/** 빗나간 삼각형의 거리. 가장 가까운 것을 고르는 자리에서 늘 지는 값이다. */
private const val MISS = Float.MAX_VALUE

private const val EPSILON = 1e-7f

/**
 * 충돌 메시 파일을 읽는다.
 *
 * 형식은 리틀엔디언이고 헤더가 `MMC1` · 정점 수 · 삼각형 수 · 양자화 기준점 셋 ·
 * 눈금 셋이다. 그다음이 정점 좌표(부호 없는 16비트 셋)와 삼각형 색인(부호 없는 16비트
 * 셋)이다. 정점이 6만 5천을 넘지 않게 줄였기 때문에 색인도 16비트에 들어간다.
 *
 * 파일이 깨졌으면 예외를 던진다. 에셋이라 기기에서 달라질 수 없고, 조용히 빈 메시를
 * 돌려주면 아무 데도 짚히지 않는 화면이 된다.
 */
internal fun readBodyMap3dMesh(bytes: ByteArray): BodyMap3dMesh {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val magic = ByteArray(MAGIC.length).also { buffer.get(it) }.decodeToString()
    require(magic == MAGIC) { "충돌 메시 파일이 아닙니다: $magic" }
    val vertexCount = buffer.int
    val triangleCount = buffer.int
    val origin = FloatArray(3) { buffer.float }
    val scale = FloatArray(3) { buffer.float }
    val positions = FloatArray(vertexCount * 3)
    for (index in positions.indices) {
        val axis = index % 3
        positions[index] = origin[axis] + (buffer.short.toInt() and UNSIGNED) * scale[axis]
    }
    val indices = IntArray(triangleCount * 3) { buffer.short.toInt() and UNSIGNED }
    return BodyMap3dMesh(positions, indices)
}

private const val MAGIC = "MMC1"
private const val UNSIGNED = 0xFFFF
