package com.mist.medicalmate.voice.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceInput
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState

/**
 * "메이트야"로 부르면 뜨는 패널(#273).
 *
 * 증상 문답과 같은 `Voice Input`을 `Bottom Sheet`에 얹었다. 말할 때 보는 모양이 앱 안에서
 * 하나여야 한다. **시안에 없는 화면이라 디자인 트랙 확인이 필요하다.**
 *
 * 둘째 줄이 지금 무엇을 해야 하는지를 적는다. 아직 못 들었으면 말할 수 있는 예를, 듣는 중이면
 * 들은 말을, 못 알아들었으면 그 말과 함께 다시 말해 달라고 적는다.
 */
@OptIn(ExperimentalMaterial3Api::class) // `Bottom Sheet`이 `ModalBottomSheet` 위에 얹혀 있다.
@Composable
internal fun VoiceCommandPanel(panel: VoicePanel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    MedicalMateBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        MedicalMateVoiceInput(
            state = panel.voice,
            onMicClick = onDismiss,
            onTypeInsteadClick = onDismiss,
            description = description(panel),
            alternativeLabel = stringResource(R.string.voice_command_close),
            modifier =
            Modifier.padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s16,
                bottom = MedicalMateSpace.s24,
            ),
        )
    }
}

@Composable
private fun description(panel: VoicePanel): String? = when {
    panel.missed -> stringResource(R.string.voice_command_missed, panel.heard)
    panel.voice == MedicalMateVoiceState.LISTENING && panel.heard.isNotBlank() -> panel.heard
    panel.voice == MedicalMateVoiceState.IDLE -> stringResource(R.string.voice_command_hint)
    else -> null
}

@Preview
@Composable
private fun VoiceCommandPanelPreview() {
    MedicalMateTheme {
        VoiceCommandPanel(
            panel = VoicePanel(voice = MedicalMateVoiceState.LISTENING, heard = "캘린더 열어"),
            onDismiss = {},
        )
    }
}
