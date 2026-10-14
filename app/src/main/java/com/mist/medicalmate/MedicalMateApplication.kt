package com.mist.medicalmate

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.mist.medicalmate.core.ondevice.OnDeviceEngine
import com.mist.medicalmate.core.ondevice.OnDeviceEngineState
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MedicalMateApplication : Application() {
    @Inject
    lateinit var onDeviceEngine: OnDeviceEngine

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        logOnDeviceEngine()
    }

    /**
     * 엔진이 이 기기에서 무엇으로 도는지 로그에 남긴다. 디버그 빌드만.
     *
     * 확인하는 것은 **NPU가 실제로 잡히는지**다. 안 잡히면 에러 없이 CPU로 떨어지고 첫 호출이
     * 열 배 가까이 느려진다. 나중에 추론 결과만 보고는 어느 쪽으로 돌았는지 알 수 없다.
     *
     * 릴리즈 빌드에서 부르지 않는 이유는 휴대폰 정리를 켜지 않은 사람에게까지 50MB짜리
     * 라이브러리를 여는 일이기 때문이다. 쓰는 쪽(내 정보의 토글, 메모 정리)이 필요할 때 연다.
     */
    private fun logOnDeviceEngine() {
        if (!BuildConfig.DEBUG) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            when (val state = onDeviceEngine.state()) {
                is OnDeviceEngineState.Unavailable -> Log.i(TAG, "온디바이스 엔진 없음")
                is OnDeviceEngineState.Ready -> {
                    Log.i(TAG, "온디바이스 엔진 ${state.version} · NPU ${if (state.npuReady) "잡힘" else "없음"}")
                    state.devices.forEach { Log.i(TAG, "  장치 ${it.type} ${it.name} — ${it.description}") }
                }
            }
        }
    }

    private companion object {
        const val TAG = "MedicalMate"
    }
}
