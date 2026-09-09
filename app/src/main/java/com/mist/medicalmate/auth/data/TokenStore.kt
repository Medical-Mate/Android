package com.mist.medicalmate.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mist.medicalmate.core.network.AccessTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
constructor(@ApplicationContext private val context: Context) : AccessTokenProvider {
    /**
     * 세션이 남아 있는지.
     *
     * refresh 토큰을 기준으로 본다. 액세스 토큰은 만료돼도 재발급으로 이어갈 수 있어서
     * 세션이 끝난 것이 아니다.
     *
     * 흐름으로 내보내는 이유는 세션이 화면 밖에서도 끝날 수 있기 때문이다. 재발급이 거절되면
     * `DefaultTokenRefresher`가 이 저장소를 지우는데, 그 자리는 OkHttp 스레드라 어느 화면이
     * 떠 있는지 모른다.
     */
    fun hasSession(): Flow<Boolean> = context.authDataStore.data
        .map { it[refreshTokenKey].isNullOrBlank().not() }
        .distinctUntilChanged()

    suspend fun readRefreshToken(): String? = context.authDataStore.data
        .first()[refreshTokenKey]

    override suspend fun accessToken(): String? = context.authDataStore.data
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
