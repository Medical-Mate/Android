package com.mist.medicalmate.intake.ui

import android.content.Context
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.SwapChain
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * 3D 인체도를 그리는 자리. Filament를 얹는다.
 *
 * 카메라를 Filament의 조작기에 맡기지 않고 [camera]가 들고 있는 값으로 매 프레임
 * 덮어쓴다. 짚은 자리를 판정하는 [ray]와 그리는 카메라가 같은 값에서 나와야 보이는 자리와
 * 짚히는 자리가 맞는다.
 *
 * 빛은 카메라를 따라 돈다. 한자리에 박아 두면 몸을 돌렸을 때 뒤가 어둠에 잠긴다. 정면에
 * 두지 않고 왼쪽 위로 비껴 두는 이유는, 보는 방향과 같으면 그림자가 지지 않아 몸이 흰
 * 실루엣으로 납작해지기 때문이다.
 *
 * **엔진을 건드리는 것은 전부 메인 갈래에서 한다.** 모델을 읽는 것은 Draco 압축을 푸는
 * 일이라 짧지 않아서 처음에는 다른 갈래로 보냈는데, `ResourceLoader`가 Filament의 작업
 * 큐를 쓰면서 "This thread has not been adopted"로 죽었다. 큐에 갈래를 들이는 API가 Java
 * 쪽에 없다. 파일을 읽는 것만 다른 갈래에서 하고 엔진에 넘기는 것은 여기로 가져온다.
 */
internal class BodyMap3dRenderer(context: Context) : Choreographer.FrameCallback {
    /**
     * 그림이 나가는 곳.
     *
     * `SurfaceView`가 아니라 `TextureView`다. `SurfaceView`의 표면은 창 **뒤에** 놓이고
     * 그 자리의 창이 비어 있어야 보이는데, 판의 배경을 Compose가 그 위에 칠한다. 창 위로
     * 올리는 방법(`setZOrderOnTop`)도 있지만 그러면 이번에는 구역 점이 3D 뒤로 들어간다.
     * `TextureView`는 뷰 계층 안에서 합성돼 배경 위·점 아래에 그대로 놓인다.
     */
    val textureView: TextureView = TextureView(context)

    /** Compose가 매 프레임 읽어 가는 자리. 그리는 쪽도 판정하는 쪽도 이 값을 본다. */
    var camera: BodyMap3dCamera = BodyMap3dCamera()

    init {
        // Filament 네이티브 라이브러리를 올린다. 이 블록이 `engine`보다 **위에** 있어야 한다 —
        // 초기화는 적힌 차례대로 돌고, 라이브러리가 없으면 Engine.create()에서 죽는다.
        Gltfio.init()
    }

    private val engine = Engine.create()
    private val renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val view = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val filamentCamera = engine.createCamera(cameraEntity)
    private val sunEntity = EntityManager.get().create()
    private val materials = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materials, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private val choreographer = Choreographer.getInstance()
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)

    private var swapChain: SwapChain? = null
    private var asset: FilamentAsset? = null

    init {
        filamentCamera.setExposure(APERTURE, SHUTTER_SPEED, SENSITIVITY)
        view.camera = filamentCamera
        view.scene = scene
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .intensity(SUN_LUX)
            .direction(0f, 0f, -1f)
            .castShadows(false)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)
        scene.indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(AMBIENT, AMBIENT, AMBIENT))
            .intensity(AMBIENT_LUX)
            .build(engine)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(textureView)
    }

    /** 판이 지운 자리에 남는 색. 화면의 판 배경과 같아야 경계가 보이지 않는다. */
    fun setBackground(red: Float, green: Float, blue: Float) {
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(toLinear(red), toLinear(green), toLinear(blue), 1.0)
        }
    }

    /** 모델을 읽어 장면에 올린다. **메인 갈래에서** 부른다. 압축을 푸는 동안 화면이 멎는다. */
    fun load(bytes: ByteArray) {
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        val loaded = assetLoader.createAsset(buffer) ?: error("3D 인체도를 읽지 못했습니다.")
        resourceLoader.loadResources(loaded)
        scene.addEntities(loaded.entities)
        loaded.releaseSourceData()
        asset = loaded
    }

    fun start() {
        choreographer.postFrameCallback(this)
    }

    fun stop() {
        choreographer.removeFrameCallback(this)
    }

    fun destroy() {
        stop()
        uiHelper.detach()
        asset?.let { assetLoader.destroyAsset(it) }
        resourceLoader.destroy()
        materials.destroyMaterials()
        assetLoader.destroy()
        engine.destroyCameraComponent(cameraEntity)
        engine.destroyEntity(cameraEntity)
        engine.destroyEntity(sunEntity)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroy()
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)
        renderFrame(frameTimeNanos)
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val chain = swapChain
        if (chain == null || asset == null || !uiHelper.isReadyToRender) return
        placeCamera()
        if (renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun placeCamera() {
        val state = camera
        val eye = state.eye
        filamentCamera.lookAt(
            eye.x.toDouble(), eye.y.toDouble(), eye.z.toDouble(),
            state.target.x.toDouble(), state.target.y.toDouble(), state.target.z.toDouble(),
            0.0, 1.0, 0.0,
        )
        val basis = state.basis()
        val key = (basis.forward + basis.right * KEY_SIDE + basis.up * KEY_HEIGHT).normalized()
        val lights = engine.lightManager
        lights.setDirection(lights.getInstance(sunEntity), key.x, key.y, key.z)
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
        }

        override fun onDetachedFromSurface() {
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            view.viewport = Viewport(0, 0, width, height)
            filamentCamera.setProjection(
                BODY_3D_FOV_DEGREES.toDouble(),
                width.toDouble() / height.toDouble(),
                NEAR_PLANE,
                FAR_PLANE,
                Camera.Fov.VERTICAL,
            )
        }
    }

    private companion object {
        /** 노출. 실내에서 찍은 것처럼 보이는 조합이다. */
        const val APERTURE = 16f
        const val SHUTTER_SPEED = 1f / 125f
        const val SENSITIVITY = 100f
        const val SUN_LUX = 60_000f

        /** 채움광. 세게 주면 그림자가 사라지고 몸이 납작해진다. */
        const val AMBIENT = 0.5f
        const val AMBIENT_LUX = 12_000f

        /** 빛을 카메라에서 비껴 두는 정도. 화면 왼쪽 위에서 오는 것으로 읽힌다. */
        const val KEY_SIDE = 0.35f
        const val KEY_HEIGHT = -0.45f
        const val NEAR_PLANE = 0.05
        const val FAR_PLANE = 20.0

        /** Filament의 색은 선형이다. 테마에서 온 sRGB 값을 옮겨 준다. */
        fun toLinear(value: Float): Double =
            if (value <= 0.04045f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
}

/** 화면에 그리는 모델. Draco로 압축돼 있고 gltfio가 푼다. */
internal const val BODY_3D_MODEL_ASSET = "body3d/body.glb"

/** 짚은 자리를 찾는 메시. 그리는 모델을 줄인 사본이다. */
internal const val BODY_3D_COLLISION_ASSET = "body3d/body_collision.bin"
