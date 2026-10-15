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

/** @param onDeleteModel "음성 모델 지우기"를 눌렀다(#280). */
data class VoiceWakeCallbacks(val onDeleteModel: () -> Unit = {})

/**
 * 설정 카드 아래 한 줄. 할 말이 없으면 그리지 않는다.
 *
 * 받는 중은 기다리면 되는 일이라 흐린 글자이고, 나머지는 사람이 무언가 해야 하는 일이라
 * 위험 색이다.
 */
@Composable
internal fun VoiceWakeStatusLine(status: VoiceWakeStatus, modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    val text =
        when (status) {
            VoiceWakeStatus.Idle -> return
            is VoiceWakeStatus.Downloading -> stringResource(R.string.my_profile_voice_wake_downloading, status.percent)
            VoiceWakeStatus.Failed -> stringResource(R.string.my_profile_voice_wake_failed)
            VoiceWakeStatus.Denied -> stringResource(R.string.my_profile_voice_wake_denied)
        }
    Text(
        text = text,
        style = MedicalMateTheme.typography.bodyS,
        color = if (status is VoiceWakeStatus.Downloading) colors.fgSubtle else colors.fgDanger,
        modifier = modifier.padding(start = MedicalMateSpace.s4, top = MedicalMateSpace.s8),
    )
}

/**
 * 받아 둔 모델을 지우는 버튼(#280).
 *
 * 꺼 둔 채 모델이 남아 있을 때만 그린다. 다시 받으면 되는 것이라 확인 창을 거치지 않는다.
 * **시안에 없는 버튼이라 디자인 트랙 확인이 필요하다.**
 */
@Composable
internal fun VoiceWakeModelDelete(onClick: () -> Unit, modifier: Modifier = Modifier) {
    MedicalMateButton(
        onClick = onClick,
        label = stringResource(R.string.my_profile_voice_wake_delete_model),
        type = MedicalMateButtonType.GHOST,
        size = MedicalMateButtonSize.S,
        modifier = modifier.padding(top = MedicalMateSpace.s4),
    )
}
