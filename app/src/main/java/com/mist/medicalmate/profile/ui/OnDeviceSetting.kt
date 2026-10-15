package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType

/**
 * "휴대폰에서 기록 정리"(#289)의 자리.
 *
 * 음성 호출(#273)과 같은 모양이다. 켜면 바로 1.2GB를 받기 시작하고, 다 받기 전까지는
 * 받는 중이라고 적는다. 꺼도 모델은 남아서 꺼 둔 채 모델이 있으면 지우는 버튼을 둔다. 다른 점은
 * 마이크 권한이 없다는 것뿐이다.
 *
 * @param supported 이 기기에서 켤 수 있는지(NPU · 메모리). 아니면 토글을 그리지 않는다.
 * @param modelReady 받아 둔 모델이 있다. 켤 때 다시 받지 않는다.
 */
data class OnDeviceUi(
    val supported: Boolean = false,
    val status: OnDeviceStatus = OnDeviceStatus.Idle,
    val modelReady: Boolean = false,
)

/** 토글 아래 한 줄. 문구는 화면이 고른다. */
sealed interface OnDeviceStatus {
    data object Idle : OnDeviceStatus

    data class Downloading(val percent: Int) : OnDeviceStatus

    data object Failed : OnDeviceStatus
}

/** @param onDeleteModel "정리 모델 지우기"를 눌렀다. */
data class OnDeviceCallbacks(val onDeleteModel: () -> Unit = {})

/** 설정 카드 아래 한 줄. 할 말이 없으면 그리지 않는다. 음성 모델의 줄과 같은 규칙이다. */
@Composable
internal fun OnDeviceStatusLine(status: OnDeviceStatus, modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    val text =
        when (status) {
            OnDeviceStatus.Idle -> return
            is OnDeviceStatus.Downloading -> stringResource(R.string.my_profile_on_device_downloading, status.percent)
            OnDeviceStatus.Failed -> stringResource(R.string.my_profile_on_device_failed)
        }
    Text(
        text = text,
        style = MedicalMateTheme.typography.bodyS,
        color = if (status is OnDeviceStatus.Downloading) colors.fgSubtle else colors.fgDanger,
        modifier = modifier.padding(start = MedicalMateSpace.s4, top = MedicalMateSpace.s8),
    )
}

/**
 * 받아 둔 모델을 지우는 버튼.
 *
 * 꺼 둔 채 모델이 남아 있을 때만 그린다. 다시 받으면 되는 것이라 확인 창을 거치지 않는다.
 * **시안에 없는 버튼이라 디자인 트랙 확인이 필요하다.**
 */
@Composable
internal fun OnDeviceModelDelete(onClick: () -> Unit, modifier: Modifier = Modifier) {
    MedicalMateButton(
        onClick = onClick,
        label = stringResource(R.string.my_profile_on_device_delete_model),
        type = MedicalMateButtonType.GHOST,
        size = MedicalMateButtonSize.S,
        modifier = modifier.padding(top = MedicalMateSpace.s4),
    )
}
