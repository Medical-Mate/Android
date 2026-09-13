package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Immutable
import kotlin.math.PI

/**
 * 앵커 하나를 확대해 볼 때의 카메라 자리. 좌표는 [bodyMap3dFrames]가 들고 있다.
 *
 * [span]은 그 앵커의 구역들이 차지하는 크기다. 카메라 거리를 여기서 계산한다 — 머리와
 * 팔은 크기가 네 배 가까이 달라서 같은 거리로 보면 한쪽이 화면을 넘치거나 점만 해진다.
 *
 * [oneSide]가 true면 좌우 중 한쪽만 화면에 담는 앵커다(팔·다리). 몸이 좌우 대칭이라
 * 프레임은 주체의 오른쪽(−x) 기준 하나만 있고, 왼쪽이면 [target]의 x 부호만 뒤집는다.
 */
@Immutable
internal data class BodyMap3dFrame(
    val anchorId: String,
    val back: Boolean,
    val oneSide: Boolean,
    val zoneCount: Int,
    val target: BodyMap3dVector,
    val span: Float,
)

/**
 * 그 앵커를 확대할 프레임.
 *
 * 한 앵커에 면마다 프레임이 있을 수 있어 구역이 많은 쪽을 고르고, 같으면 앞면을 쓴다.
 * 지금 자료는 앵커마다 하나씩이라 고를 일이 없지만, 뒷면 구역이 늘면 그때 갈린다.
 */
internal fun bodyMap3dFrameOf(anchorId: String): BodyMap3dFrame? = bodyMap3dFrames
    .filter { it.anchorId == anchorId }
    .sortedWith(compareByDescending<BodyMap3dFrame> { it.zoneCount }.thenBy { it.back })
    .firstOrNull()

/**
 * 이 프레임으로 들어가는 카메라.
 *
 * [from]은 지금 카메라다. 돌아갈 각을 거기서 가장 가까운 쪽으로 잡으려고 받는다.
 *
 * 보는 깊이를 절반만 쓴다. 구역 점은 표면 위에 있는데 그 자리를 그대로 보면 카메라가
 * 몸에 너무 붙어서 가장자리 구역이 화면 밖으로 밀린다.
 */
internal fun BodyMap3dFrame.camera(side: BodyMapSide, from: BodyMap3dCamera): BodyMap3dCamera = BodyMap3dCamera(
    yaw = from.yawNear(if (back) PI.toFloat() else 0f),
    pitch = 0f,
    distance = (span * SPAN_TO_DISTANCE + DISTANCE_MARGIN).coerceAtLeast(MIN_DISTANCE),
    target = BodyMap3dVector(
        x = if (oneSide && side == BodyMapSide.LEFT) -target.x else target.x,
        y = target.y,
        z = target.z * TARGET_DEPTH,
    ),
)

/**
 * 구역들이 차지하는 크기에서 카메라 거리를 만드는 값.
 *
 * **자료가 적어 둔 `span × 2.5 + 0.07`보다 멀리 잡는다.** 그 값은 가로로 넓은 뷰어에서 맞춘
 * 것이고, 우리 판은 320x505라 세로 시야각이 좁아 같은 거리면 몸에 코가 닿는다. 기기에서
 * 보니 가슴·배·목·허리가 전부 살갗만 차서 어디인지 알 수 없었다.
 *
 * [MIN_DISTANCE]는 그 아래로 더 다가가지 않는 바닥이다. 목(span 0.052)처럼 작은 부위는
 * 계산값이 0.3도 안 되는데, 그 거리면 부위만 보이고 그것이 몸 어디인지가 사라진다. 지금
 * 값에서는 어느 부위든 화면 높이의 절반쯤을 차지하고 나머지가 둘레로 남는다.
 */
private const val SPAN_TO_DISTANCE = 3.4f
private const val DISTANCE_MARGIN = 0.12f
private const val MIN_DISTANCE = 0.55f

/** 보는 깊이를 절반만 쓴다. */
private const val TARGET_DEPTH = 0.5f
