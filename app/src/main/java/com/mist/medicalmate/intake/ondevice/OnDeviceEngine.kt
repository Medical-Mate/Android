package com.mist.medicalmate.intake.ondevice

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 엔진 열기와 장치 확인.
 *
 * 추론은 아직 없다. 이 묶음에서 확인하는 것은 **이 기기에서 백엔드가 실제로 잡히는지**다.
 * NPU가 안 잡히면 에러 없이 CPU로 떨어지고, 그 차이가 첫 호출에서 1.7초와 14.9초다. 조용히
 * 느려지는 것을 나중에 추론 결과만 보고 알아내기는 어렵다.
 *
 * 라이브러리를 부를 수 없는 기기가 있다. arm64가 아니거나 엔진을 넣지 않고 빌드한 경우다.
 * 그때는 [state]가 [OnDeviceEngineState.Unavailable]이고 서버 추출로 넘어간다.
 */
@Singleton
class OnDeviceEngine
@Inject
constructor(@ApplicationContext private val context: Context) {
    private val mutex = Mutex()

    private var opened: OnDeviceEngineState? = null

    /**
     * 엔진을 열고 무엇으로 돌 수 있는지 본다.
     *
     * 한 번만 연다. `llama_backend_init`과 백엔드 적재는 프로세스에 한 번이면 되고, 두 번
     * 부르면 같은 백엔드가 다시 등록된다.
     */
    suspend fun state(): OnDeviceEngineState = mutex.withLock {
        opened ?: open().also { opened = it }
    }

    private suspend fun open(): OnDeviceEngineState = withContext(Dispatchers.IO) {
        // `libggml.so`와 `libllama.so`가 OpenCL 백엔드를 DT_NEEDED로 걸고 있고, 그것이 다시
        // 벤더 라이브러리 `libOpenCL.so`를 건다. 앱 네임스페이스가 그 이름을 자동으로 찾지
        // 못해 먼저 열어 둔다. 실패해도 넘어간다. 그러면 아래에서 엔진이 안 열릴 뿐이다.
        VENDOR_LIBRARIES.forEach { name ->
            val result = runCatching { System.loadLibrary(name) }
            Log.i(TAG, "벤더 $name ${if (result.isSuccess) "열림" else "막힘"}")
        }

        val loaded = runCatching { System.loadLibrary(LIBRARY) }
        loaded.exceptionOrNull()?.let { cause ->
            // 라이브러리가 없는 것은 고장이 아니다. 엔진 없이 빌드했거나 arm64가 아닌 기기다.
            Log.i(TAG, "온디바이스 엔진 없음: ${cause.message}")
            return@withContext OnDeviceEngineState.Unavailable
        }

        val htpDir = unpackHtpImages()
        nativeOpen(context.applicationInfo.nativeLibraryDir, htpDir.absolutePath)

        val devices = nativeDevices().lineSequence().filter(String::isNotBlank).map(::parseDevice).toList()
        OnDeviceEngineState.Ready(version = nativeVersion(), devices = devices)
    }

    /**
     * HTP 이미지를 앱 폴더로 푼다.
     *
     * 자산에서 꺼내 파일로 둔다. DSP 로더가 `ADSP_LIBRARY_PATH` 아래에서 **파일**을 찾아가고
     * APK 안의 자산은 그 경로로 보이지 않는다. 이미 있고 크기가 같으면 다시 쓰지 않는다.
     */
    private fun unpackHtpImages(): File {
        val target = File(context.filesDir, HTP_DIR).apply { mkdirs() }
        val assets = context.assets
        assets.list(HTP_ASSET_DIR).orEmpty().forEach { name ->
            val file = File(target, name)
            assets.open("$HTP_ASSET_DIR/$name").use { input ->
                if (file.length() > 0 && file.length() == input.available().toLong()) return@forEach
                file.outputStream().use(input::copyTo)
            }
        }
        return target
    }

    private external fun nativeOpen(nativeLibDir: String, htpDir: String)

    private external fun nativeVersion(): String

    private external fun nativeDevices(): String

    private companion object {
        const val TAG = "OnDeviceEngine"
        const val LIBRARY = "medicalmate_ondevice"

        /**
         * 엔진이 기대는 벤더 라이브러리.
         *
         * 앱이 직접 쓰지 않지만 열리는지 먼저 본다. `OpenCL`은 `libggml.so`가 DT_NEEDED로
         * 걸고 있어 못 열면 엔진 자체가 안 열리고, `cdsprpc`는 Hexagon 백엔드가 DSP와
         * 말하는 통로다. 둘 다 `/vendor/etc/public.libraries.txt`에 있는데 그 목록이 앱
         * 네임스페이스에 열려 있는지는 기기마다 다르다. 막힌 것을 로그에 남겨야 "왜 CPU로
         * 떨어졌는지"를 나중에 찾을 수 있다.
         */
        val VENDOR_LIBRARIES = listOf("OpenCL", "cdsprpc")

        /** 엔진 묶음의 HTP 이미지가 실리는 자산 폴더. `fetchOnDeviceEngine`이 채운다. */
        const val HTP_ASSET_DIR = "ondevice/htp"

        const val HTP_DIR = "ondevice/htp"
    }
}

/** 엔진 상태. */
sealed interface OnDeviceEngineState {
    /** 이 기기에서 엔진을 열 수 없다. 서버 추출로 넘어간다. */
    data object Unavailable : OnDeviceEngineState

    /**
     * 열렸다. [devices]에 잡힌 장치가 들어 있다.
     *
     * 장치가 CPU 하나뿐이면 NPU가 안 잡힌 것이다. 돌기는 하지만 첫 호출이 열 배 가까이
     * 느리다. 그 사실이 [npuReady]로 드러나야 원인을 찾을 수 있다.
     */
    data class Ready(val version: String, val devices: List<OnDeviceDevice>) : OnDeviceEngineState {
        val npuReady: Boolean get() = devices.any { it.isNpu }
    }
}

/** 잡힌 장치 하나. */
data class OnDeviceDevice(val name: String, val type: String, val description: String) {
    /**
     * NPU인지.
     *
     * ggml이 HTP를 가속기(`ACCEL`)로 등록한다. 이름으로 보지 않는 이유는 그 문자열이 엔진
     * 버전에 묶여 있어서다.
     */
    val isNpu: Boolean get() = type == "ACCEL"
}

private fun parseDevice(line: String): OnDeviceDevice {
    val parts = line.split("|")
    return OnDeviceDevice(
        name = parts.getOrElse(0) { "" },
        type = parts.getOrElse(1) { "" },
        description = parts.getOrElse(2) { "" },
    )
}
