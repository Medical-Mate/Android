package com.mist.medicalmate.profile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 이 기기에서만 쓰는 설정.
 *
 * 브리핑 카드 자동 저장과 진료실 화면 밝기 최대 둘이다. **이 기기에서 어떻게 보일지의
 * 문제라 서버가 읽을 일이 없다**는 것이 백엔드와 정한 것이다(Backend#85). 계정에 붙는 것은
 * 진료 하루 전 알림 하나뿐이고 그쪽은 [SettingsApi]다.
 *
 * 온보딩 기록과 같은 `DataStore`를 쓴다. 둘 다 이 기기의 프로필에 딸린 값이고, 파일을 나누면
 * 읽는 곳만 늘어난다.
 */
interface LocalSettingsStore {
    val cardAutoSave: Flow<Boolean>

    val handoffBrightness: Flow<Boolean>

    suspend fun setCardAutoSave(enabled: Boolean)

    suspend fun setHandoffBrightness(enabled: Boolean)
}

@Singleton
internal class DefaultLocalSettingsStore
@Inject
constructor(@ApplicationContext private val context: Context) :
    LocalSettingsStore {
    override val cardAutoSave: Flow<Boolean> = context.profileDataStore.data.map { it[cardAutoSaveKey] ?: true }

    override val handoffBrightness: Flow<Boolean> =
        context.profileDataStore.data.map { it[handoffBrightnessKey] ?: false }

    override suspend fun setCardAutoSave(enabled: Boolean) {
        context.profileDataStore.edit { it[cardAutoSaveKey] = enabled }
    }

    override suspend fun setHandoffBrightness(enabled: Boolean) {
        context.profileDataStore.edit { it[handoffBrightnessKey] = enabled }
    }

    private companion object {
        val cardAutoSaveKey = booleanPreferencesKey("card_auto_save")

        val handoffBrightnessKey = booleanPreferencesKey("handoff_brightness")
    }
}
