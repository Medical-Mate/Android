package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Immutable
import kotlin.math.sqrt

/**
 * 3D 인체도의 좌표 하나.
 *
 * 자료의 좌표계를 그대로 쓴다. Y 업 · 신장 1.0 · 원점이 발바닥과 정수리의 가운데 ·
 * 앞면이 +Z다. 길이가 신장을 1로 재는 값이라 [BODY_3D_PICK_MAX_DISTANCE]와 그대로
 * 견줄 수 있다.
 *
 * Compose의 `Offset`처럼 값 타입으로 두고 연산자를 붙였다. 판정이 한 번에 4만 6천
 * 삼각형을 도는데, 메시 쪽은 이 타입을 쓰지 않고 원시 배열을 직접 읽는다. 이 타입은
 * 카메라와 점 계산처럼 몇 번 부르지 않는 자리의 것이다.
 */
@Immutable
internal data class BodyMap3dVector(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: BodyMap3dVector) = BodyMap3dVector(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: BodyMap3dVector) = BodyMap3dVector(x - other.x, y - other.y, z - other.z)

    operator fun times(scale: Float) = BodyMap3dVector(x * scale, y * scale, z * scale)

    infix fun dot(other: BodyMap3dVector): Float = x * other.x + y * other.y + z * other.z

    infix fun cross(other: BodyMap3dVector) = BodyMap3dVector(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun normalized(): BodyMap3dVector {
        val length = sqrt(this dot this)
        return if (length == 0f) this else this * (1f / length)
    }

    fun distanceTo(other: BodyMap3dVector): Float = sqrt((this - other).let { it dot it })
}

/**
 * 카메라에서 화면의 한 점을 지나 뻗는 반직선.
 *
 * [direction]은 단위 벡터다. 교차 판정이 돌려주는 `t`를 그대로 거리로 읽으려면 그래야
 * 한다.
 */
@Immutable
internal data class BodyMap3dRay(val origin: BodyMap3dVector, val direction: BodyMap3dVector) {
    fun at(distance: Float): BodyMap3dVector = origin + direction * distance
}
