package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/**
 * 진료 하루 전 알림을 받을지.
 *
 * 계정에 붙는 유일한 설정이다. 나머지 토글 둘은 [LocalSettingsStore]에 있다.
 */
interface SettingsRepository {
    suspend fun visitReminder(): ApiResult<Boolean>

    suspend fun setVisitReminder(enabled: Boolean): ApiResult<Boolean>
}

internal class DefaultSettingsRepository
@Inject
constructor(private val api: SettingsApi, private val json: Json) :
    SettingsRepository {
    override suspend fun visitReminder(): ApiResult<Boolean> =
        apiCall(json) { api.settings() }.map { it.visitReminderEnabled }

    override suspend fun setVisitReminder(enabled: Boolean): ApiResult<Boolean> =
        apiCall(json) { api.update(SettingsRequest(visitReminderEnabled = enabled)) }
            .map { it.visitReminderEnabled }
}
