package com.mist.medicalmate.core.ondevice

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * 휴대폰 정리(#289) 대역.
 *
 * 기본은 "이 기기에서 켤 수 없음"이다. 내 정보 시험 대부분이 이것과 상관없다.
 *
 * [enable]은 실제처럼 켜고, 모델이 없을 때만 받기 시작한다. 받기가 끝나는 것은 시험이
 * [model]을 직접 바꿔 흉내 낸다. [disable]은 실제처럼 모델을 남긴다.
 */
internal class FakeOnDeviceModel(
    private val usable: Boolean = false,
    enabled: Boolean = false,
    model: OnDeviceModelState = OnDeviceModelState.Absent,
) : OnDeviceModel {
    override val enabled = MutableStateFlow(enabled)

    override val model = MutableStateFlow(model)

    var enableCalls = 0
    var disableCalls = 0
    var deleteCalls = 0

    override suspend fun supported(): Boolean = usable

    override suspend fun enable() {
        enableCalls++
        enabled.value = true
        if (model.value != OnDeviceModelState.Ready) model.value = OnDeviceModelState.Downloading(0f)
    }

    override suspend fun disable() {
        disableCalls++
        enabled.value = false
    }

    override suspend fun deleteModel() {
        deleteCalls++
        model.value = OnDeviceModelState.Absent
    }

    override suspend fun readyFile(): File? = null
}
