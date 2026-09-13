package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** 계정 설정 대역. 읽기와 쓰기 결과를 따로 정한다. */
internal class FakeSettingsRepository(
    private val read: ApiResult<Boolean> = ApiResult.Success(true),
    private val write: ApiResult<Boolean> = ApiResult.Success(true),
) : SettingsRepository {
    /** 저장하라고 받은 값. 화면만 바뀌고 안 나갔는지가 관심사다. */
    var saved: Boolean? = null

    override suspend fun visitReminder(): ApiResult<Boolean> = read

    override suspend fun setVisitReminder(enabled: Boolean): ApiResult<Boolean> {
        saved = enabled
        return write
    }

    companion object {
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}

/** 이 기기 설정 대역. `DataStore` 없이 값만 든다. */
internal class FakeLocalSettingsStore(cardAutoSave: Boolean = true, handoffBrightness: Boolean = false) :
    LocalSettingsStore {
    private val autoSave = MutableStateFlow(cardAutoSave)
    private val brightness = MutableStateFlow(handoffBrightness)

    override val cardAutoSave: Flow<Boolean> = autoSave

    override val handoffBrightness: Flow<Boolean> = brightness

    override suspend fun setCardAutoSave(enabled: Boolean) {
        autoSave.value = enabled
    }

    override suspend fun setHandoffBrightness(enabled: Boolean) {
        brightness.value = enabled
    }
}
