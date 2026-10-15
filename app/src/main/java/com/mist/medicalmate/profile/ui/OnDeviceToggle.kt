package com.mist.medicalmate.profile.ui

import com.mist.medicalmate.core.ondevice.OnDeviceModel
import com.mist.medicalmate.core.ondevice.OnDeviceModelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 내 정보의 "휴대폰에서 기록 정리" 토글(#289).
 *
 * 켜면 바로 모델(1.2GB)을 받기 시작한다. 확인 창은 걷어냈다(#316, 사용자 결정). 다 받을 때까지 진행률을
 * 적고, 꺼도 모델이 남아 지우는 버튼이 따로 있다. 이 갈래들을 [MyProfileViewModel]에
 * 늘어놓으면 알림·음성 호출 토글과 섞여 어느 것이 어느 토글의 일인지 읽기 어려워 따로 둔다.
 *
 * 토글의 값은 [OnDeviceModel.enabled]를 따라간다 — 받다가 실패하면 그쪽이 내리고 화면도 따라
 * 내려간다.
 */
class OnDeviceToggle internal constructor(
    private val scope: CoroutineScope,
    private val model: OnDeviceModel,
    private val state: MutableStateFlow<MyProfileUiState>,
) {
    init {
        scope.launch {
            model.enabled.collect { enabled ->
                state.update { it.copy(settings = it.settings + (AppSetting.ON_DEVICE to enabled)) }
            }
        }
        scope.launch {
            model.model.collect { model ->
                update { it.copy(status = model.toStatus(), modelReady = model == OnDeviceModelState.Ready) }
            }
        }
    }

    /** 이 기기에서 켤 수 있는지 본다. 안 되면 줄을 그리지 않는다. */
    suspend fun loadSupported() {
        val supported = model.supported()
        update { it.copy(supported = supported) }
    }

    /** 켜면 모델을 받기 시작하고 끄면 바로 끈다. 받아 둔 모델은 남긴다. */
    fun change(enabled: Boolean) {
        state.update { it.copy(settings = it.settings + (AppSetting.ON_DEVICE to enabled)) }
        scope.launch { if (enabled) model.enable() else model.disable() }
    }

    /** "정리 모델 지우기"를 눌렀다. 꺼 둔 상태에서만 버튼이 있다. */
    fun deleteModel() {
        scope.launch { model.deleteModel() }
    }

    private fun update(change: (OnDeviceUi) -> OnDeviceUi) {
        state.update { it.copy(onDevice = change(it.onDevice)) }
    }
}

/** 받는 중이면 백분율, 실패면 다시 켜라는 줄. 다 받았거나 지웠으면 비운다. */
private fun OnDeviceModelState.toStatus(): OnDeviceStatus = when (this) {
    is OnDeviceModelState.Downloading -> OnDeviceStatus.Downloading((progress * PERCENT).toInt())
    OnDeviceModelState.Failed -> OnDeviceStatus.Failed
    OnDeviceModelState.Ready, OnDeviceModelState.Absent -> OnDeviceStatus.Idle
}

private const val PERCENT = 100
