package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 3D 인체도를 보는 자리.
 *
 * 몸을 축으로 도는 궤도 카메라다. [yaw] 0이 앞면(+Z)이고 π가 뒷면이다. [target]은 보는
 * 지점이고 전신에서는 몸의 중심, 부위를 확대하면 그 부위다.
 *
 * 화면에 그리는 것과 짚은 자리를 판정하는 것이 **같은 값**을 써야 한다. 렌더러에게
 * 넘기는 시야각과 [ray]가 쓰는 시야각이 갈리면 보이는 자리와 짚히는 자리가 어긋난다.
 * 그래서 투영에 필요한 값을 여기 모아 두고 양쪽이 함께 읽는다.
 */
@Immutable
internal data class BodyMap3dCamera(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val distance: Float = BODY_3D_HOME_DISTANCE,
    val target: BodyMap3dVector = BODY_3D_HOME_TARGET,
) {
    /** 카메라가 놓인 자리. */
    val eye: BodyMap3dVector
        get() = target + BodyMap3dVector(
            distance * sin(yaw) * cos(pitch),
            distance * sin(pitch),
            distance * cos(yaw) * cos(pitch),
        )
}

/** 전신을 볼 때 보는 지점. 몸의 중심보다 조금 위다 — 머리와 발이 같은 여백으로 남는다. */
internal val BODY_3D_HOME_TARGET = BodyMap3dVector(0f, 0.02f, 0f)

/** 세로 시야각. 기본 거리에서 신장 1.0이 화면 높이에 꼭 맞는 값이다. */
internal const val BODY_3D_FOV_DEGREES: Float = 28f

/**
 * 전신이 들어오는 거리.
 *
 * 시야각만 보면 2.05에서 신장 1.0이 화면 높이에 꼭 맞지만, 그러면 발이 판 아래로 잘린다.
 * 발끝이 원점에서 0.5보다 조금 더 내려가 있고 발등이 앞으로 나와 있어서다. 기기에서 보고
 * 정한 값이다.
 */
internal const val BODY_3D_HOME_DISTANCE: Float = 2.3f

private const val MIN_DISTANCE = 0.45f
private const val MAX_DISTANCE = 2.6f

/** 위아래로 도는 한계. 정수리와 발바닥을 넘어가면 위 벡터가 뒤집혀 화면이 돈다. */
private const val PITCH_LIMIT = 1.2f

/** 카메라 반대쪽을 보는 점을 가리는 문턱. 자료가 정한 값이다. */
private const val FACING_LIMIT = -0.12f

/** 화면에 그릴 수 있는 가장 가까운 깊이. 이보다 앞의 점은 투영이 뒤집힌다. */
private const val NEAR = 0.01f

/**
 * 카메라의 세 축.
 *
 * [forward]는 보는 방향이고 [right]·[up]이 화면의 가로세로다. 앞면을 볼 때 [right]가
 * 월드 +X라, 화면 오른쪽에 환자의 **왼쪽**이 온다. 마주 선 사람을 보는 것과 같다.
 */
internal class BodyMap3dBasis(val forward: BodyMap3dVector, val right: BodyMap3dVector, val up: BodyMap3dVector)

private val WORLD_UP = BodyMap3dVector(0f, 1f, 0f)

internal fun BodyMap3dCamera.basis(): BodyMap3dBasis {
    val forward = (target - eye).normalized()
    val right = (forward cross WORLD_UP).normalized()
    return BodyMap3dBasis(forward = forward, right = right, up = right cross forward)
}

/**
 * 화면의 한 점을 지나는 반직선.
 *
 * [ndcX]·[ndcY]는 −1..1이고 y는 위가 양수다. 화면 좌표를 그대로 받지 않는 이유는 판의
 * 크기를 이 계산이 몰라도 되게 하려는 것이다.
 */
internal fun BodyMap3dCamera.ray(ndcX: Float, ndcY: Float, aspect: Float): BodyMap3dRay {
    val basis = basis()
    val half = tan(Math.toRadians(BODY_3D_FOV_DEGREES / 2.0)).toFloat()
    val direction = basis.forward + basis.right * (ndcX * half * aspect) + basis.up * (ndcY * half)
    return BodyMap3dRay(origin = eye, direction = direction.normalized())
}

/**
 * 점을 화면 좌표로 옮긴다. 카메라 뒤에 있으면 null이다.
 *
 * 돌려주는 값은 [ray]가 받는 것과 같은 −1..1이다. 둘이 서로의 역이라 짚은 자리에 표시가
 * 정확히 얹힌다.
 */
internal fun BodyMap3dCamera.project(point: BodyMap3dVector, aspect: Float): Pair<Float, Float>? {
    val basis = basis()
    val offset = point - eye
    val depth = offset dot basis.forward
    if (depth <= NEAR) return null
    val half = tan(Math.toRadians(BODY_3D_FOV_DEGREES / 2.0)).toFloat()
    return (offset dot basis.right) / (depth * half * aspect) to (offset dot basis.up) / (depth * half)
}

/**
 * 이 노멀을 가진 점이 카메라 쪽을 보고 있는지.
 *
 * 표시를 가리는 데만 쓴다. 판정에 넣으면 뒷면 점이 허리·엉덩이뿐이라 뒤에서 짚은 것이
 * 전부 그쪽으로 빨려 들어간다.
 */
internal fun BodyMap3dCamera.facesCamera(point: BodyMap3dPoint): Boolean =
    BodyMap3dVector(point.nx, point.ny, point.nz) dot basis().forward <= FACING_LIMIT

/** 끌어서 돌린 만큼. 위아래는 뒤집히지 않게 막는다. */
internal fun BodyMap3dCamera.turned(deltaYaw: Float, deltaPitch: Float): BodyMap3dCamera = copy(
    yaw = yaw + deltaYaw,
    pitch = (pitch + deltaPitch).coerceIn(-PITCH_LIMIT, PITCH_LIMIT),
)

/** 오므리고 벌린 만큼. 1보다 크면 가까워진다. */
internal fun BodyMap3dCamera.zoomed(scale: Float): BodyMap3dCamera =
    copy(distance = (distance / scale).coerceIn(MIN_DISTANCE, MAX_DISTANCE))

/** 그 면을 정면으로 보는, 전신이 다 들어오는 자리. */
internal fun BodyMap3dCamera.facing(view: BodyMapView): BodyMap3dCamera = BodyMap3dCamera(
    yaw = yawNear(if (view == BodyMapView.BACK) PI.toFloat() else 0f),
    pitch = 0f,
    distance = BODY_3D_HOME_DISTANCE,
    target = BODY_3D_HOME_TARGET,
)

/**
 * 지금 각도에서 가장 가까운, [angle]과 같은 방향의 각.
 *
 * 각도를 0..2π로 접지 않고 그대로 쌓아 두기 때문에 필요하다. 몇 바퀴 돌려 둔 상태에서
 * 면을 바꾸면 카메라가 왔던 길을 되감는다.
 */
internal fun BodyMap3dCamera.yawNear(angle: Float): Float {
    val turns = Math.round((yaw - angle) / (2f * PI.toFloat()))
    return angle + turns * 2f * PI.toFloat()
}

/** 지금 보고 있는 면. 앞뒤 토글의 선택 상태가 회전을 따라오게 한다. */
internal fun BodyMap3dCamera.view(): BodyMapView = if (cos(yaw) >= 0f) BodyMapView.FRONT else BodyMapView.BACK
