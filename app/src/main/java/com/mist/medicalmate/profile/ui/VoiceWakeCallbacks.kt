package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialog
import com.mist.medicalmate.core.designsystem.component.MedicalMateDialogTone

/**
 * 음성 호출을 켤 때의 확인 창(#273).
 *
 * @param onConfirm 켜기를 눌렀다. 화면이 마이크 권한을 묻고, 받으면 켠다.
 */
data class VoiceWakeCallbacks(val onConfirm: () -> Unit = {}, val onCancel: () -> Unit = {})

/**
 * 받을 크기와 쓰일 공간을 먼저 알린다.
 *
 * 토글 하나에 41MB가 걸려 있다. 데이터 요금제에서 모르고 켜면 안 된다. 소리가 기기 밖으로
 * 나가지 않는다는 것도 여기서 말한다 — 늘 듣는 기능에서 사람들이 가장 먼저 묻는 것이다.
 * **시안에 없는 창이라 디자인 트랙 확인이 필요하다.**
 */
@Composable
internal fun VoiceWakeConfirmDialog(callbacks: VoiceWakeCallbacks) {
    MedicalMateDialog(
        title = stringResource(R.string.my_profile_voice_wake_confirm_title),
        message = stringResource(R.string.my_profile_voice_wake_confirm_message),
        confirmLabel = stringResource(R.string.my_profile_voice_wake_confirm),
        onConfirm = callbacks.onConfirm,
        dismissLabel = stringResource(R.string.my_profile_voice_wake_cancel),
        onDismissRequest = callbacks.onCancel,
        tone = MedicalMateDialogTone.NEUTRAL,
    )
}

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
