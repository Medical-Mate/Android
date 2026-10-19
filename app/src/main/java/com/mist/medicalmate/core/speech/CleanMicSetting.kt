package com.mist.medicalmate.core.speech

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mist.medicalmate.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 받아쓰기에 마이크 소리를 다듬어 넘길지(#328). 시험용이다.
 *
 * 다듬는 방식([CleanMic])이 지금 방식보다 나은지는 기기와 장소마다 달라, 둘을 바꿔 가며 같은 말로 비교한다.
 * 여러 곳에서 PC 없이 바꿔야 해서 내 정보에 토글을 둔다(사용자 결정). **debug 빌드에만 있다** — release는
 * 늘 지금 방식이다. 비교가 끝나면 한쪽과 이 토글을 함께 걷어낸다.
 */
interface CleanMicSetting {
    /** 토글을 그릴지. debug 빌드에서만 참이다. */
    val available: Boolean

    val enabled: Flow<Boolean>

    suspend fun set(enabled: Boolean)
}

internal class DataStoreCleanMicSetting
@Inject
constructor(@ApplicationContext private val context: Context) :
    CleanMicSetting {
    override val available: Boolean = BuildConfig.DEBUG

    override val enabled: Flow<Boolean> = context.speechDataStore.data.map { available && it[enabledKey] == true }

    override suspend fun set(enabled: Boolean) {
        context.speechDataStore.edit { it[enabledKey] = enabled }
    }

    private companion object {
        val enabledKey = booleanPreferencesKey("clean_mic")
    }
}
