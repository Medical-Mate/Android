package com.mist.medicalmate.intake.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI

/**
 * 3D 인체도 판.
 *
 * 끌면 돌고 오므리면 확대된다. 짚으면 반직선을 쏘아 표면에 닿은 자리를 찾고, 거기서 가장
 * 가까운 구역을 [onPick]으로 올린다. 어디에도 닿지 않았으면 null이다.
 *
 * 카메라를 밖에서 받는 이유는 앞뒤 토글이 화면 쪽에 있기 때문이다. 판이 들고 있으면 토글이
 * 판 안으로 들어와야 하는데, 그러면 2D와 화면 구성이 달라진다.
 *
 * **스크린 리더로는 쓸 수 없는 판이다.** 2D 인체도와 같다. 좌표를 짚는 조작이라 읽어 줄
 * 것이 없고, 대신 목록에서 고르는 길이 그대로 남아 있다.
 */
@Composable
internal fun BodyMap3dBoard(
    camera: Animatable<BodyMap3dCamera, AnimationVector3D>,
    selection: BodyMapSelection?,
    onPick: (BodyMap3dPick?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val renderer = remember { BodyMap3dRenderer(context) }
    var mesh by remember { mutableStateOf<BodyMap3dMesh?>(null) }
    val background = MedicalMateTheme.colors.bgSubtle

    LaunchedEffect(renderer) {
        renderer.setBackground(background.red, background.green, background.blue)
        // 모델과 충돌 메시를 다 읽은 뒤에 그리기를 시작한다. 읽는 동안 그리면 엔진을 두
        // 갈래에서 동시에 건드린다. Draco 압축을 푸는 데 걸리는 시간이 짧지 않다.
        val model = withContext(Dispatchers.IO) {
            context.assets.open(BODY_3D_MODEL_ASSET).use { it.readBytes() }
        }
        withContext(Dispatchers.Default) { renderer.load(model) }
        mesh = withContext(Dispatchers.IO) {
            readBodyMap3dMesh(context.assets.open(BODY_3D_COLLISION_ASSET).use { it.readBytes() })
        }
        renderer.start()
    }
    DisposableEffect(renderer) {
        onDispose { renderer.destroy() }
    }
    SideEffect { renderer.camera = camera.value }

    Box(modifier = modifier.clearAndSetSemantics { }) {
        AndroidView(factory = { renderer.surfaceView }, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val turn = (-pan.x / size.width * PI * 2f).toFloat()
                        val tilt = (pan.y / size.height * PI).toFloat()
                        scope.launch { camera.snapTo(camera.value.turned(turn, tilt).zoomed(zoom)) }
                    }
                }
                .pointerInput(mesh) {
                    detectTapGestures { offset ->
                        val target = mesh ?: return@detectTapGestures
                        val aspect = size.width.toFloat() / size.height.toFloat()
                        val ray = camera.value.ray(
                            ndcX = offset.x / size.width * 2f - 1f,
                            ndcY = -(offset.y / size.height * 2f - 1f),
                            aspect = aspect,
                        )
                        scope.launch {
                            val hit = withContext(Dispatchers.Default) { target.hit(ray) }
                            onPick(hit?.let { bodyMap3dNearest(it) })
                        }
                    }
                },
        ) {
            BodyMap3dDots(camera = camera.value, selection = selection)
        }
    }
}

/**
 * 구역 점을 판 위에 얹는다.
 *
 * 카메라 반대쪽을 보는 점은 그리지 않는다. 몸을 통과해 반대편 점이 비치면 어느 것이 앞인지
 * 알 수 없다. 가려지는 점(팔 뒤의 가슴 같은)까지 가려 내지는 않는다 — 그러려면 점마다
 * 반직선을 한 번씩 더 쏴야 한다.
 *
 * 모양은 2D 인체도의 점과 같다. 두 길이 같은 것을 고르는데 표시가 다르면 다른 조작으로
 * 읽힌다.
 */
@Composable
private fun BodyMap3dDots(camera: BodyMap3dCamera, selection: BodyMapSelection?) {
    val colors = MedicalMateTheme.colors
    val halo = colors.fgDefault.copy(alpha = HALO_ALPHA)
    val core = colors.fgDefault.copy(alpha = CORE_ALPHA)
    val density = LocalDensity.current
    val haloRadius = with(density) { DotHaloSize.toPx() } / 2f
    val coreRadius = with(density) { DotCoreSize.toPx() } / 2f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val aspect = size.width / size.height
        bodyMap3dRegions.forEach { point ->
            if (!camera.facesCamera(point)) return@forEach
            val projected = camera.project(BodyMap3dVector(point.x, point.y, point.z), aspect) ?: return@forEach
            val center = Offset(
                x = (projected.first + 1f) / 2f * size.width,
                y = (1f - projected.second) / 2f * size.height,
            )
            val picked = selection != null && point.toSelection() == selection
            drawCircle(if (picked) colors.bgSurface else halo, haloRadius, center)
            drawCircle(if (picked) colors.bgPrimary else core, coreRadius, center)
        }
    }
}

/** 각도 둘과 거리 하나를 한 값으로 애니메이션한다. 면을 바꿀 때 셋이 함께 미끄러져야 한다. */
private val CameraConverter = TwoWayConverter<BodyMap3dCamera, AnimationVector3D>(
    convertToVector = { AnimationVector3D(it.yaw, it.pitch, it.distance) },
    convertFromVector = { BodyMap3dCamera(it.v1, it.v2, it.v3) },
)

@Composable
internal fun rememberBodyMap3dCamera(): Animatable<BodyMap3dCamera, AnimationVector3D> =
    remember { Animatable(BodyMap3dCamera(), CameraConverter) }

/** 2D 인체도의 `Point` 마스터와 같은 크기다. */
private val DotHaloSize = 22.dp
private val DotCoreSize = 14.dp
private const val HALO_ALPHA = 0.10f
private const val CORE_ALPHA = 0.28f
