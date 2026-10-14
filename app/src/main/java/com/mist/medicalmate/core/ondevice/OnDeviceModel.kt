package com.mist.medicalmate.core.ondevice

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/**
 * "휴대폰에서 정리하기"(#289)를 켜고 끄는 자리.
 *
 * 내 정보(1s)의 토글과 진료 후 기록의 분류가 함께 본다. 두 도메인이 쓰는 값이라 `core`에 둔다.
 *
 * **켠다는 것은 모델을 받는다는 것이다.** 켜면 1.24GB를 받는다. 받다가 실패하면 다시 꺼진다 —
 * 켜져 있는데 서버로만 가는 토글은 고장으로 읽힌다. 다 받기 전까지는 서버가 나눈다.
 *
 * **꺼도 모델은 남긴다.** 껐다 켤 때마다 1GB를 다시 받을 수 없다. 공간을 돌려받는 것은
 * [deleteModel]이 따로 맡는다. 음성 호출(#280)과 같은 규칙이다.
 *
 * 이 기기에서만 쓰는 값이라 `DataStore`다. 기기마다 모델을 받아야 하므로 계정에 붙일 이유가 없다.
 */
interface OnDeviceModel {
    /**
     * 이 기기에서 켤 수 있는지.
     *
     * 엔진이 열리고 NPU가 잡혀야 한다. CPU로 돌면 첫 호출이 열 배 가까이 느리고 5분 안에 발열로
     * 무너진다(AI 실측). 메모리도 넉넉해야 한다. 안 되는 기기에서는 토글을 그리지 않는다.
     */
    suspend fun supported(): Boolean

    val enabled: Flow<Boolean>

    val model: StateFlow<OnDeviceModelState>

    /** 켜고 모델을 받는다. */
    suspend fun enable()

    /** 끈다. 받아 둔 모델은 남기고, 올려 둔 모델은 내린다. */
    suspend fun disable()

    /** 받아 둔 모델을 지워 공간을 돌려준다. 받는 중이면 멈춘다. 꺼 둔 상태에서만 부른다. */
    suspend fun deleteModel()

    /** 지금 폰에서 돌릴 수 있는 모델 파일. 꺼져 있거나 아직 다 받지 않았으면 null이다. */
    suspend fun readyFile(): File?
}

private val Context.onDeviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "ondevice")

@Singleton
internal class DefaultOnDeviceModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val store: OnDeviceModelStore,
    private val engine: OnDeviceEngine,
) : OnDeviceModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 켜 둔 채 모델이 없으면 받는다. 받다가 앱이 꺼진 경우다. 켤 때 이미 받기로 한 사람이라
        // 다시 묻지 않는다. 받던 조각이 있으면 거기서 잇는다.
        scope.launch {
            val on = context.onDeviceDataStore.data.first()[enabledKey] == true
            if (on && store.state.value == OnDeviceModelState.Absent) store.download()
        }
        // 받다가 실패하면 토글을 내린다. 화면이 떠 있지 않아도 내려가야 다음에 들어왔을 때 맞다.
        scope.launch {
            store.state
                .filter { it == OnDeviceModelState.Failed }
                .collect { context.onDeviceDataStore.edit { it[enabledKey] = false } }
        }
    }

    override suspend fun supported(): Boolean {
        val state = engine.state()
        val npu = state is OnDeviceEngineState.Ready && state.npuReady
        val memory = enoughMemory()
        Log.i(TAG, "기기: NPU ${if (npu) "잡힘" else "없음"} · 메모리 ${if (memory) "충분" else "부족"}")
        return npu && memory
    }

    override val enabled: Flow<Boolean> = context.onDeviceDataStore.data.map { it[enabledKey] ?: false }

    override val model: StateFlow<OnDeviceModelState> = store.state

    override suspend fun enable() {
        context.onDeviceDataStore.edit { it[enabledKey] = true }
        store.download()
    }

    override suspend fun disable() {
        context.onDeviceDataStore.edit { it[enabledKey] = false }
        engine.release()
    }

    override suspend fun deleteModel() {
        engine.release()
        store.delete()
    }

    override suspend fun readyFile(): File? {
        if (!enabled.first()) return null
        return store.file()
    }

    /**
     * 메모리가 넉넉한지.
     *
     * 모델이 1.24GB이고 올리면 KV·상태 버퍼까지 1.3GB 남짓이다. 8GB 기기가 `totalMem`으로 7GB대를
     * 알리는 것에 맞춰 선을 6GiB에 둔다. 그 아래에서는 화면 뒤의 앱이 밀려난다.
     */
    private fun enoughMemory(): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        return !manager.isLowRamDevice && info.totalMem >= MIN_TOTAL_MEMORY_BYTES
    }

    private companion object {
        const val TAG = "OnDeviceModel"
        val enabledKey = booleanPreferencesKey("ondevice")
        const val MIN_TOTAL_MEMORY_BYTES = 6L * 1024 * 1024 * 1024
    }
}
