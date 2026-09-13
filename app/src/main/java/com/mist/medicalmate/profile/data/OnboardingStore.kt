package com.mist.medicalmate.profile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

/**
 * 온보딩을 마쳤다는 기록.
 *
 * 온보딩은 가입하고 한 번만 나와야 한다. 서버의 `onboardingRequired`만 보면 그렇게 되지
 * 않는다. 두 가지 이유다.
 *
 * 하나는 `refresh` 응답이 이 값을 항상 false로 준다는 것이다. 자동 로그인으로 들어오면
 * 온보딩이 필요한 사람도 필요 없다고 나온다. 다른 하나는 반대 방향이다. 프로필을 서버에
 * 저장하기 전까지 로그인 응답은 계속 true라서, 온보딩을 마치고 로그아웃했다 들어오면 또
 * 나온다.
 *
 * 그래서 기기에 마쳤다는 사실을 남기고 서버 값과 함께 본다. 둘 중 하나라도 마쳤다고 하면
 * 다시 보여주지 않는다.
 *
 * **`PUT /api/me/health-profile`을 연결하면 서버의 `onboardingCompleted`가 정본이 된다.**
 * 이 기록은 서버에 묻기 전에 답을 아는 빠른 길로 남는다.
 *
 * 로그아웃과 탈퇴에서 지우지 않는다. 지우면 같은 사람이 다시 들어올 때 또 나오는데, 그게
 * 지금 막으려는 것이다. 탈퇴하고 새로 가입한 경우에는 온보딩이 건너뛰어진다. 서버 값이
 * 정본이 되면 사라지는 문제라 그대로 뒀다.
 */
@Singleton
class OnboardingStore
@Inject
constructor(@ApplicationContext private val context: Context) {
    val completed: Flow<Boolean> = context.profileDataStore.data.map { it[completedKey] ?: false }

    suspend fun markCompleted() {
        context.profileDataStore.edit { preferences -> preferences[completedKey] = true }
    }

    private companion object {
        val completedKey = booleanPreferencesKey("onboarding_completed")
    }
}
