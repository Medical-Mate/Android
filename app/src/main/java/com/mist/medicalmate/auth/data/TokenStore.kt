package com.mist.medicalmate.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/**
 * 서버가 발급한 JWT를 기기에 보관한다.
 *
 * 카카오 토큰은 여기 넣지 않는다. SDK가 자체 저장소에 갖고 있고, 서버도 저장하지
 * 않기로 했다. 같은 자격증명을 세 곳에 두면 처리 범위만 늘어난다.
 *
 * 평문 DataStore다. 매니페스트에서 백업과 기기 간 전송을 막아 파일이 앱 샌드박스
 * 밖으로 나가지 않게 했다. 루팅된 기기까지 막으려면 AndroidKeyStore 기반 암호화가
 * 필요한데, 설치 사용자가 없는 지금은 저장 방식을 바꾸는 비용이 재로그인 한 번이라
 * 미뤘다.
 */
@Singleton
class TokenStore
@Inject
constructor(@ApplicationContext private val context: Context) {
    suspend fun readRefreshToken(): String? = context.authDataStore.data
        .first()[refreshTokenKey]

    suspend fun readAccessToken(): String? = context.authDataStore.data
        .first()[accessTokenKey]

    suspend fun save(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    private companion object {
        val accessTokenKey = stringPreferencesKey("access_token")
        val refreshTokenKey = stringPreferencesKey("refresh_token")
    }
}
