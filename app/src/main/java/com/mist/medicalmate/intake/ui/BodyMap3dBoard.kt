package com.mist.medicalmate.intake.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI

/**
 * 3D 인체도 판.
 *
 * 끌면 돌고 오므리면 확대된다. 짚으면 반직선을 쏘아 표면에 닿은 자리를 찾고, 거기서 가장
 * 가까운 구역을 [onPick]으로 올린다. 어디에도 닿지 않았으면 null이다.
 *
 * 무엇이 후보이고 무엇을 그리는지는 [focus]가 가른다. 전신을 보는 중이면 앵커 점을 그리고
 * 확대했으면 그 앵커의 구역 점을 그린다. 2D 인체도의 두 화면과 같은 구분이다.
 *
 * 카메라를 밖에서 받는 이유는 앞뒤 토글과 부위 전환이 화면 쪽에 있기 때문이다.
 *
 * **스크린 리더로는 쓸 수 없는 판이다.** 2D 인체도와 같다. 좌표를 짚는 조작이라 읽어 줄
 * 것이 없고, 대신 목록에서 고르는 길이 그대로 남아 있다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BodyMap3dBoard(
    camera: BodyMap3dCameraState,
    focus: BodyMapSelection?,
    selection: BodyMapSelection?,
    onPick: (BodyMap3dPick?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val visible = remember { BringIntoViewRequester() }
    val renderer = remember { BodyMap3dRenderer(context) }
    var mesh by remember { mutableStateOf<BodyMap3dMesh?>(null) }
    var ready by remember { mutableStateOf(false) }
    val background = MedicalMateTheme.colors.bgSubtle
    val candidates = bodyMap3dCandidates(focus)

    LaunchedEffect(renderer) {
        renderer.setBackground(background.red, background.green, background.blue)
        mesh = loadBodyMap3d(context, renderer)
        renderer.start()
        ready = true
    }
    DisposableEffect(renderer) {
        onDispose { renderer.destroy() }
    }
    SideEffect { renderer.camera = camera.value }

    // 판이 505dp라 본문이 스크롤되고, 그 상태로는 판의 아래쪽이 화면 밖에 남는다. 열 때와
    // 부위를 바꿀 때, 그리고 손을 댈 때 판 전체를 화면 안으로 끌어온다.
    LaunchedEffect(focus) { visible.bringIntoView() }

    Box(modifier = modifier.bringIntoViewRequester(visible).clearAndSetSemantics { }) {
        // 다 읽기 전에는 표면을 붙이지 않는다. 붙여 두면 첫 프레임이 나갈 때까지 검은 판이
        // 판 자리를 덮는다.
        if (ready) {
            AndroidView(factory = { renderer.textureView }, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .bodyMap3dGestures(
                    BodyMap3dGestures(
                        camera = camera,
                        mesh = mesh,
                        candidates = candidates,
                        scope = scope,
                        onTouch = { scope.launch { visible.bringIntoView() } },
                        onPick = onPick,
                    ),
                ),
        ) {
            BodyMap3dDots(
                camera = camera.value,
                points = if (focus == null) bodyMap3dAnchors else candidates,
                selection = selection,
            )
        }
    }
}

/**
 * 모델과 충돌 메시를 읽는다.
 *
 * 파일을 읽는 것만 다른 갈래에서 한다. 엔진에 넘기는 것은 메인 갈래여야 한다 — Filament의
 * 작업 큐가 들이지 않은 갈래를 거절한다. 다 읽은 뒤에 그리기를 시작한다.
 */
private suspend fun loadBodyMap3d(context: Context, renderer: BodyMap3dRenderer): BodyMap3dMesh {
    val model = withContext(Dispatchers.IO) {
        context.assets.open(BODY_3D_MODEL_ASSET).use { it.readBytes() }
    }
    renderer.load(model)
    return withContext(Dispatchers.IO) {
        readBodyMap3dMesh(context.assets.open(BODY_3D_COLLISION_ASSET).use { it.readBytes() })
    }
}

/**
 * 판을 조작하는 데 필요한 것들.
 *
 * 수식어 함수의 인자를 하나로 묶은 것이다. 여섯을 나열하면 부르는 쪽에서 어느 자리가
 * 무엇인지 알아보기 어렵다.
 *
 * [onTouch]는 손이 닿은 순간 부른다. 눌러서 고르든 끌어서 돌리든 먼저 지나는 자리다.
 */
private class BodyMap3dGestures(
    val camera: BodyMap3dCameraState,
    val mesh: BodyMap3dMesh?,
    val candidates: List<BodyMap3dPoint>,
    val scope: CoroutineScope,
    val onTouch: () -> Unit,
    val onPick: (BodyMap3dPick?) -> Unit,
)

/**
 * 끌어서 돌리고 오므려 확대하고 짚어서 고른다.
 *
 * 짚는 판정은 다른 갈래에서 한다. 삼각형 4만 6천을 도는 일이라 손가락을 떼는 프레임에서
 * 하면 그 프레임이 밀린다.
 */
private fun Modifier.bodyMap3dGestures(gestures: BodyMap3dGestures): Modifier = this
    .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            val turn = (-pan.x / size.width * PI * 2f).toFloat()
            val tilt = (pan.y / size.height * PI).toFloat()
            val camera = gestures.camera
            gestures.scope.launch { camera.snapTo(camera.value.turned(turn, tilt).zoomed(zoom)) }
        }
    }
    .pointerInput(gestures.mesh, gestures.candidates) {
        detectTapGestures(
            onPress = { gestures.onTouch() },
            onTap = { offset ->
                val target = gestures.mesh ?: return@detectTapGestures
                val ray = gestures.camera.value.ray(
                    ndcX = offset.x / size.width * 2f - 1f,
                    ndcY = -(offset.y / size.height * 2f - 1f),
                    aspect = size.width.toFloat() / size.height.toFloat(),
                )
                gestures.scope.launch {
                    val hit = withContext(Dispatchers.Default) { target.hit(ray) }
                    gestures.onPick(hit?.let { bodyMap3dNearest(it, gestures.candidates) })
                }
            },
        )
    }

/**
 * 점을 판 위에 얹는다.
 *
 * 카메라 반대쪽을 보는 점은 그리지 않는다. 몸을 통과해 반대편 점이 비치면 어느 것이 앞인지
 * 알 수 없다. 가려지는 점(팔 뒤의 가슴 같은)까지 가려 내지는 않는다 — 그러려면 점마다
 * 반직선을 한 번씩 더 쏴야 한다.
 *
 * 점마다 흰 테를 두른다. 2D 인체도는 흰 판 위라 흐린 회색 점으로도 보이는데, 여기는 점이
 * 살갗 위에 얹히고 그 살갗이 빛에 따라 밝기가 변해서 회색만으로는 묻힌다.
 *
 * 고른 점은 2D와 같은 크기에 브랜드색이고 나머지는 한 치수 작다. 짚는 것은 점이 아니라
 * 몸이라, 점은 어디를 고를 수 있는지 알려주기만 하면 된다.
 */
@Composable
private fun BodyMap3dDots(camera: BodyMap3dCamera, points: List<BodyMap3dPoint>, selection: BodyMapSelection?) {
    val colors = MedicalMateTheme.colors
    val density = LocalDensity.current
    val haloRadius = with(density) { DotHaloSize.toPx() } / 2f
    val coreRadius = with(density) { DotCoreSize.toPx() } / 2f
    val plainHaloRadius = with(density) { PlainHaloSize.toPx() } / 2f
    val plainCoreRadius = with(density) { PlainCoreSize.toPx() } / 2f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val aspect = size.width / size.height
        points.forEach { point ->
            if (!camera.facesCamera(point)) return@forEach
            val projected = camera.project(BodyMap3dVector(point.x, point.y, point.z), aspect) ?: return@forEach
            val center = Offset(
                x = (projected.first + 1f) / 2f * size.width,
                y = (1f - projected.second) / 2f * size.height,
            )
            if (selection != null && point.toSelection() == selection) {
                drawCircle(colors.bgSurface, haloRadius, center)
                drawCircle(colors.bgPrimary, coreRadius, center)
            } else {
                drawCircle(colors.bgSurface, plainHaloRadius, center)
                drawCircle(colors.fgDefault, plainCoreRadius, center)
            }
        }
    }
}

/**
 * 카메라를 든 자리.
 *
 * 각도 셋과 보는 지점 셋을 [Animatable] 둘로 나눠 든다. Compose의 애니메이션 벡터가
 * 4차원까지라 여섯을 한 값으로 묶을 수 없어서다. 둘을 늘 같은 시간으로 함께 움직인다.
 */
@Stable
internal class BodyMap3dCameraState {
    private val orbit = Animatable(BodyMap3dCamera(), OrbitConverter)
    private val target = Animatable(BODY_3D_HOME_TARGET, TargetConverter)

    val value: BodyMap3dCamera get() = orbit.value.copy(target = target.value)

    /** 손가락을 따라오는 이동. 사이를 채우지 않는다. */
    suspend fun snapTo(camera: BodyMap3dCamera) {
        orbit.snapTo(camera)
        target.snapTo(camera.target)
    }

    /** 부위를 바꿀 때의 이동. 어느 부위로 들어갔는지 눈으로 따라갈 수 있어야 한다. */
    suspend fun glideTo(camera: BodyMap3dCamera) {
        coroutineScope {
            launch { orbit.animateTo(camera, tween(GLIDE_MILLIS, easing = FastOutSlowInEasing)) }
            launch { target.animateTo(camera.target, tween(GLIDE_MILLIS, easing = FastOutSlowInEasing)) }
        }
    }
}

@Composable
internal fun rememberBodyMap3dCamera(): BodyMap3dCameraState = remember { BodyMap3dCameraState() }

/** 보는 지점은 [BodyMap3dCamera]에 함께 들어 있지만 이 변환은 각도 셋만 옮긴다. */
private val OrbitConverter = TwoWayConverter<BodyMap3dCamera, AnimationVector3D>(
    convertToVector = { AnimationVector3D(it.yaw, it.pitch, it.distance) },
    convertFromVector = { BodyMap3dCamera(it.v1, it.v2, it.v3) },
)

private val TargetConverter = TwoWayConverter<BodyMap3dVector, AnimationVector3D>(
    convertToVector = { AnimationVector3D(it.x, it.y, it.z) },
    convertFromVector = { BodyMap3dVector(it.v1, it.v2, it.v3) },
)

/** 자료가 적어 둔 카메라 전환 시간. */
private const val GLIDE_MILLIS = 460

/** 고른 점은 2D 인체도의 `Point` 마스터와 같은 크기다. */
private val DotHaloSize = 22.dp
private val DotCoreSize = 14.dp

/** 고르지 않은 점. 한 치수 작지만 흰 테는 같이 두른다. */
private val PlainHaloSize = 16.dp
private val PlainCoreSize = 9.dp
