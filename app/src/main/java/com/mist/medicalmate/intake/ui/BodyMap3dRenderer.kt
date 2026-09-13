package com.mist.medicalmate.intake.ui

import android.content.Context
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
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
 * 빛은 카메라를 따라 돈다. 한자리에 박아 두면 몸을 돌렸을 때 뒤가 어둠에 잠긴다.
 *
 * 엔진을 건드리는 곳에 자물쇠를 건다. 모델을 읽는 것은 Draco 압축을 푸는 일이라 짧지
 * 않아서 다른 갈래에서 하는데, 그 사이에 표면이 만들어지면 스왑체인을 만드는 쪽과
 * 겹친다.
 */
internal class BodyMap3dRenderer(context: Context) : Choreographer.FrameCallback {
    val surfaceView: SurfaceView = SurfaceView(context)

    /** Compose가 매 프레임 읽어 가는 자리. 그리는 쪽도 판정하는 쪽도 이 값을 본다. */
    var camera: BodyMap3dCamera = BodyMap3dCamera()

    private val lock = Any()

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
        uiHelper.attachTo(surfaceView)
    }

    /** 판이 지운 자리에 남는 색. 화면의 판 배경과 같아야 경계가 보이지 않는다. */
    fun setBackground(red: Float, green: Float, blue: Float) {
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(toLinear(red), toLinear(green), toLinear(blue), 1.0)
        }
    }

    /** 모델을 읽어 장면에 올린다. 압축을 푸는 동안 막히므로 다른 갈래에서 부른다. */
    fun load(bytes: ByteArray) {
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        synchronized(lock) {
            val loaded = assetLoader.createAsset(buffer) ?: error("3D 인체도를 읽지 못했습니다.")
            resourceLoader.loadResources(loaded)
            scene.addEntities(loaded.entities)
            loaded.releaseSourceData()
            asset = loaded
        }
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
        synchronized(lock) {
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
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)
        synchronized(lock) { renderFrame(frameTimeNanos) }
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
            BODY_3D_TARGET.x.toDouble(), BODY_3D_TARGET.y.toDouble(), BODY_3D_TARGET.z.toDouble(),
            0.0, 1.0, 0.0,
        )
        val forward = state.basis().forward
        val lights = engine.lightManager
        lights.setDirection(lights.getInstance(sunEntity), forward.x, forward.y, forward.z)
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            synchronized(lock) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }
        }

        override fun onDetachedFromSurface() {
            synchronized(lock) {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                    swapChain = null
                }
            }
        }

        override fun onResized(width: Int, height: Int) {
            synchronized(lock) {
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
    }

    private companion object {
        /** 노출. 실내에서 찍은 것처럼 보이는 조합이다. */
        const val APERTURE = 16f
        const val SHUTTER_SPEED = 1f / 125f
        const val SENSITIVITY = 100f
        const val SUN_LUX = 70_000f
        const val AMBIENT = 0.6f
        const val AMBIENT_LUX = 30_000f
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
