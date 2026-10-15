package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.model.DEFAULT_UNTIMED_REMINDER
import com.mist.medicalmate.core.model.ReminderSettings
import com.mist.medicalmate.core.model.VisitReminderSetting
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 진료 알림 설정(#316). 계정에 붙는 설정은 이것뿐이다.
 *
 * 고치는 쪽은 바뀐 값만 보낸다. 서버가 보낸 필드만 바꾸고 나머지는 그대로 둔다.
 */
interface SettingsRepository {
    suspend fun reminders(): ApiResult<ReminderSettings>

    /**
     * 바꿀 것만 준다. [untimedTime]은 시간 미정 일정의 전날 · 당일 알림 시각에 함께 걸린다 — 알림 시간
     * 화면(1s-3)의 휠이 하나라서다.
     */
    suspend fun update(
        beforeVisit: Boolean? = null,
        afterVisit: Boolean? = null,
        untimedTime: LocalTime? = null,
    ): ApiResult<ReminderSettings>
}

internal class DefaultSettingsRepository
@Inject
constructor(private val api: SettingsApi, private val json: Json) :
    SettingsRepository,
    VisitReminderSetting {
    override suspend fun reminders(): ApiResult<ReminderSettings> =
        apiCall(json) { api.settings() }.map { it.toSettings() }

    override suspend fun settings(): ApiResult<ReminderSettings> = reminders()

    override suspend fun update(
        beforeVisit: Boolean?,
        afterVisit: Boolean?,
        untimedTime: LocalTime?,
    ): ApiResult<ReminderSettings> {
        val time = untimedTime?.format(HOUR_MINUTE)
        val request =
            SettingsRequest(
                visitReminderEnabled = beforeVisit,
                recordReminderEnabled = afterVisit,
                visitReminderTime = time,
                recordReminderTime = time,
            )
        return apiCall(json) { api.update(request) }.map { it.toSettings() }
    }
}

/** 시각을 못 읽으면 서버의 기본값으로 본다. 알림 하나 때문에 설정 전체를 실패로 만들지 않는다. */
private fun SettingsResponse.toSettings() = ReminderSettings(
    beforeVisit = visitReminderEnabled,
    afterVisit = recordReminderEnabled,
    beforeVisitTime = visitReminderTime.toTime(),
    afterVisitTime = recordReminderTime.toTime(),
)

private fun String?.toTime(): LocalTime =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: DEFAULT_UNTIMED_REMINDER

/** 서버가 `HH:mm`만 받는다. `LocalTime.toString()`은 초가 붙을 수 있다. */
private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
