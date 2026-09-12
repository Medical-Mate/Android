package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Voice Input`의 `State` variant.
 *
 * 문구가 상태마다 정해져 있어 컴포넌트가 들고 있다. Figma 마스터(`313:993`)에서 읽었다.
 */
enum class MedicalMateVoiceState(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
    IDLE(R.string.voice_idle_title, R.string.voice_idle_description),
    LISTENING(R.string.voice_listening_title, R.string.voice_listening_description),
    PROCESSING(R.string.voice_processing_title, R.string.voice_processing_description),
    DENIED(R.string.voice_denied_title, R.string.voice_denied_description),
}

/**
 * DESIGN.md의 `Voice Input`.
 *
 * 문답의 주 입력이다. 마이크 지름 88이고 콘텐츠 폭 350이다.
 *
 * **모든 상태에 직접 입력 대안을 같은 자리에 둔다.** 문서의 컴포넌트 규격과 9절이 요구하는 것이고,
 * 위치가 상태마다 움직이면 말하기 어려운 사용자가 매번 찾아야 한다. 조용한 곳이 아니거나
 * 목소리가 잘 안 나오는 환자에게 음성만 남기면 앱을 쓸 수 없다.
 *
 * [MedicalMateVoiceState.PROCESSING]에서는 마이크를 누를 수 없다. 정리 중에 다시 눌러
 * 녹음이 겹치면 어느 말이 반영됐는지 알 수 없다.
 *
 * 파형은 [MedicalMateVoiceState.LISTENING]에서만 보인다. 움직임 축소 설정에서 흔들림을
 * 줄이더라도 "듣고 있어요" 문구는 남아야 하므로(문서의 접근성 기준), 상태를 파형으로만 알리지
 * 않는다. 지금 파형은 정지 막대이고 애니메이션은 넣지 않았다.
 */
@Composable
fun MedicalMateVoiceInput(
    state: MedicalMateVoiceState,
    onMicClick: () -> Unit,
    onTypeInsteadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MedicalMateTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        MicButton(state = state, onClick = onMicClick)
        Text(
            text = stringResource(state.titleRes),
            style = MedicalMateTheme.typography.headingS,
            color = colors.fgDefault,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(state.descriptionRes),
            style = MedicalMateTheme.typography.bodyS,
            color = if (state == MedicalMateVoiceState.DENIED) colors.fgDanger else colors.fgSubtle,
            textAlign = TextAlign.Center,
        )
        MedicalMateButton(
            onClick = onTypeInsteadClick,
            label = stringResource(R.string.voice_type_instead),
            type = MedicalMateButtonType.GHOST,
            size = MedicalMateButtonSize.S,
        )
    }
}

/**
 * 마이크 버튼.
 *
 * 접근성 이름이 상태에 따라 바뀐다. 듣는 중에는 "말하기 끝내기"다. 같은 버튼이 시작과
 * 종료를 겸하므로 이름이 고정되면 스크린 리더 사용자가 무엇이 일어날지 모른다.
 */
@Composable
private fun MicButton(state: MedicalMateVoiceState, onClick: () -> Unit) {
    val colors = micColors(state)
    val ring =
        if (state == MedicalMateVoiceState.LISTENING) {
            Modifier.border(
                width = ListeningRingWidth,
                color = MedicalMateTheme.colors.bgPrimarySubtle,
                shape = MedicalMateRadius.full,
            )
        } else {
            Modifier
        }

    Surface(
        onClick = onClick,
        // 정리 중에 다시 눌러 녹음이 겹치면 어느 말이 반영됐는지 알 수 없다.
        enabled = state != MedicalMateVoiceState.PROCESSING,
        shape = MedicalMateRadius.full,
        color = colors.container,
        contentColor = colors.content,
        modifier =
        Modifier
            .size(MedicalMateSize.mic)
            .then(ring),
    ) {
        Box(contentAlignment = Alignment.Center) {
            MicContent(state = state, tint = colors.content)
        }
    }
}

private class MicColors(val container: Color, val content: Color)

@Composable
private fun micColors(state: MedicalMateVoiceState): MicColors {
    val colors = MedicalMateTheme.colors
    return when (state) {
        MedicalMateVoiceState.IDLE, MedicalMateVoiceState.LISTENING ->
            MicColors(colors.bgPrimary, colors.fgOnPrimary)

        MedicalMateVoiceState.PROCESSING ->
            MicColors(colors.bgSubtle, colors.fgMuted)

        MedicalMateVoiceState.DENIED ->
            MicColors(colors.bgDanger, colors.fgDanger)
    }
}

@Composable
private fun MicContent(state: MedicalMateVoiceState, tint: Color) {
    if (state == MedicalMateVoiceState.PROCESSING) {
        CircularProgressIndicator(
            color = tint,
            modifier = Modifier.size(MedicalMateSize.iconLg),
        )
        return
    }
    // 듣는 중에는 마이크 대신 파형이 원 안에 들어간다. 마스터가 그렇게 그린다. 원 위에 따로
    // 띄우면 무엇을 누르면 멈추는지가 흐려지고, 듣는 중이라는 신호가 두 군데로 갈린다.
    val icon =
        when (state) {
            MedicalMateVoiceState.DENIED -> MedicalMateIcons.MicOff
            MedicalMateVoiceState.LISTENING -> MedicalMateIcons.Waveform
            else -> MedicalMateIcons.Mic
        }
    val descriptionRes =
        if (state == MedicalMateVoiceState.LISTENING) R.string.voice_mic_stop else R.string.voice_mic_start
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(descriptionRes),
        modifier = Modifier.size(MedicalMateSize.iconLg),
    )
}

/** 문서의 "6px brand ring". */
private val ListeningRingWidth = 6.dp
