package com.mist.medicalmate.core.speech

import android.app.ActivityManager
import android.content.Context
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

/**
 * "헤이 메이트"로 부르기(#273·#278)를 켜고 끄는 자리.
 *
 * 내 정보(1s)의 토글과 앱 전체의 호출어 감지가 함께 본다. 두 도메인이 쓰는 값이라 `core`에
 * 둔다.
 *
 * **켠다는 것은 모델을 받는다는 것이다.** 켜면 41MB를 받고 끄면 지운다. 받다가 실패하면 다시
 * 꺼진다 — 켜져 있는데 듣지 않는 토글은 고장으로 읽힌다.
 *
 * 이 기기에서만 쓰는 값이라 `DataStore`다. 기기마다 모델을 받아야 하므로 계정에 붙일 이유가
 * 없다.
 */
interface VoiceWake {
    /**
     * 이 기기에서 켤 수 있는지.
     *
     * 메모리가 넉넉해야 하고(모델이 500MB 가까이 쓴다) 받아쓰기를 쓸 수 있어야 한다. 부른 뒤의
     * 명령은 받아쓰기가 듣는다. 안 되는 기기에서는 토글을 그리지 않는다.
     */
    suspend fun supported(): Boolean

    val enabled: Flow<Boolean>

    val model: StateFlow<WakeWordModelState>

    /** 켜고 모델을 받는다. 마이크 권한은 화면이 먼저 받아 둔다. */
    suspend fun enable()

    /** 끄고 받아 둔 모델을 지운다. */
    suspend fun disable()
}

private val Context.speechDataStore: DataStore<Preferences> by preferencesDataStore(name = "speech")

@Singleton
internal class DefaultVoiceWake
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val store: WakeWordModelStore,
    private val speech: SpeechToText,
    private val trace: VoiceWakeTrace,
) : VoiceWake {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 켜 둔 채 모델이 없으면 받는다. 한국어 모델로 켜 두었던 기기가 영어 모델로 넘어오는
        // 길이다(#278). 켤 때 이미 받기로 한 사람이라 다시 묻지 않는다.
        scope.launch {
            // `enabled`는 이 블록보다 아래에 선언돼 있어 아직 비어 있을 수 있다. 저장소를 직접 읽는다.
            val on = context.speechDataStore.data.first()[enabledKey] == true
            if (on && store.state.value == WakeWordModelState.Absent) {
                trace.log { "모델: 켜져 있는데 모델이 없어 받는다" }
                store.download()
            }
        }
        // 받다가 실패하면 토글을 내린다. 화면이 떠 있지 않아도 내려가야 다음에 들어왔을 때 맞다.
        scope.launch {
            store.state
                .filter { it == WakeWordModelState.Failed }
                .collect { context.speechDataStore.edit { it[enabledKey] = false } }
        }
    }

    override suspend fun supported(): Boolean {
        val memory = enoughMemory()
        val dictation = speech.available()
        trace.log { "기기: 메모리 ${if (memory) "충분" else "부족"} · 받아쓰기 ${if (dictation) "됨" else "안 됨"}" }
        return memory && dictation
    }

    override val enabled: Flow<Boolean> = context.speechDataStore.data.map { it[enabledKey] ?: false }

    override val model: StateFlow<WakeWordModelState> = store.state

    override suspend fun enable() {
        context.speechDataStore.edit { it[enabledKey] = true }
        store.download()
    }

    override suspend fun disable() {
        context.speechDataStore.edit { it[enabledKey] = false }
        store.delete()
    }

    /**
     * 메모리가 넉넉한지.
     *
     * 한국어 모델은 올리면 480MB, 듣는 동안 590MB까지 썼다. 그때 6GB 기기가 `totalMem`으로
     * 5.3~5.6GB를 알리는 것에 맞춰 선을 5GiB에 뒀다. 영어 모델(#278)은 PC에서 150~180MB라 이
     * 선을 낮출 수 있다. 기기에서 재 본 뒤에 고친다.
     */
    private fun enoughMemory(): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        trace.log { "기기: totalMem ${info.totalMem / BYTES_PER_MB}MB · 저사양 ${manager.isLowRamDevice}" }
        return !manager.isLowRamDevice && info.totalMem >= MIN_TOTAL_MEMORY_BYTES
    }

    private companion object {
        val enabledKey = booleanPreferencesKey("voice_wake")

        const val MIN_TOTAL_MEMORY_BYTES = 5L * 1024 * 1024 * 1024

        const val BYTES_PER_MB = 1024 * 1024
    }
}
