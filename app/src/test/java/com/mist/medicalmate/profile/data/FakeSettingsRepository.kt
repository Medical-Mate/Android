package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.model.ReminderSettings
import com.mist.medicalmate.core.network.ApiResult
import java.io.IOException
import java.time.LocalTime

/**
 * 계정 설정 대역. 읽기 결과를 정하고, 쓰기는 [writeFails]가 아니면 받은 값을 반영해 돌려준다.
 */
internal class FakeSettingsRepository(
    private val read: ApiResult<ReminderSettings> = ApiResult.Success(ReminderSettings()),
    private val writeFails: Boolean = false,
) : SettingsRepository {
    /** 마지막으로 보낸 값. 화면만 바뀌고 안 나갔는지가 관심사다. */
    var savedBeforeVisit: Boolean? = null
    var savedAfterVisit: Boolean? = null
    var savedTime: LocalTime? = null

    override suspend fun reminders(): ApiResult<ReminderSettings> = read

    override suspend fun update(
        beforeVisit: Boolean?,
        afterVisit: Boolean?,
        untimedTime: LocalTime?,
    ): ApiResult<ReminderSettings> {
        beforeVisit?.let { savedBeforeVisit = it }
        afterVisit?.let { savedAfterVisit = it }
        untimedTime?.let { savedTime = it }
        if (writeFails) return OFFLINE
        val current = (read as? ApiResult.Success)?.value ?: ReminderSettings()
        return ApiResult.Success(
            current.copy(
                beforeVisit = beforeVisit ?: current.beforeVisit,
                afterVisit = afterVisit ?: current.afterVisit,
                beforeVisitTime = untimedTime ?: current.beforeVisitTime,
                afterVisitTime = untimedTime ?: current.afterVisitTime,
            ),
        )
    }

    companion object {
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
