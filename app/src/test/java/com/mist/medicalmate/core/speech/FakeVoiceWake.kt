package com.mist.medicalmate.core.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * 음성 호출 대역.
 *
 * 기본은 "이 기기에서 켤 수 없음"이다. 내 정보 시험 대부분이 음성 호출과 상관없다.
 *
 * [enable]은 실제처럼 켜고 모델을 받기 시작한다. 받기가 끝나는 것은 시험이 [model]을 직접
 * 바꿔 흉내 낸다.
 */
internal class FakeVoiceWake(
    private val usable: Boolean = false,
    enabled: Boolean = false,
    model: WakeWordModelState = WakeWordModelState.Absent,
) : VoiceWake {
    override val enabled = MutableStateFlow(enabled)

    override val model = MutableStateFlow(model)

    var enableCalls = 0
    var disableCalls = 0

    override suspend fun supported(): Boolean = usable

    override suspend fun enable() {
        enableCalls++
        enabled.value = true
        model.value = WakeWordModelState.Downloading(0f)
    }

    override suspend fun disable() {
        disableCalls++
        enabled.value = false
        model.value = WakeWordModelState.Absent
    }
}

/**
 * 호출어 감지기 대역.
 *
 * 시험이 [wake]를 부르면 듣고 있는 쪽에 호출이 한 번 간다. 듣는 쪽이 없으면 사라진다 —
 * 실제로도 마이크를 잡고 있지 않으면 들을 수 없다.
 */
internal class FakeWakeWordDetector : WakeWordDetector {
    private val heard = MutableSharedFlow<Unit>()

    /** 지금 듣고 있는 수. 0 또는 1이어야 한다. */
    var listeners = 0
        private set

    override fun detections(): Flow<Unit> = flow {
        listeners++
        try {
            heard.collect { emit(it) }
        } finally {
            listeners--
        }
    }

    suspend fun wake() {
        heard.emit(Unit)
    }
}
