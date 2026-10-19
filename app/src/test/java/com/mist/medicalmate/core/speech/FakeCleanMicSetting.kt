package com.mist.medicalmate.core.speech

import kotlinx.coroutines.flow.MutableStateFlow

/** 받아쓰기 방식 토글(#328). 시험에서 debug 빌드인지를 고른다. */
internal class FakeCleanMicSetting(override val available: Boolean = false) : CleanMicSetting {
    override val enabled = MutableStateFlow(false)

    override suspend fun set(enabled: Boolean) {
        this.enabled.value = enabled
    }
}
