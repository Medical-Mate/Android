package com.mist.medicalmate.voice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState

/**
 * "헤이 메이트"로 부르면 화면 아래에 뜨는 막대(#273·#282).
 *
 * **화면을 가리지 않는다.** 화면을 보면서 말로 조작하는 것이 목표라, 처음의 `Bottom Sheet`처럼
 * 스크림으로 화면을 덮으면 무엇이 바뀌는지 볼 수 없다. 막대 하나만 띄우고 뒤의 화면은 그대로
 * 누를 수도 있다.
 *
 * 윗줄은 지금 할 말이다. 처음에는 말할 수 있는 예, 무엇을 했으면 그 결과, 저장 같은 일이면
 * "할까요?"다. 아랫줄은 말하는 중에 들리는 글이다. 윗줄은 바뀔 때마다 스크린 리더가 읽는다.
 *
 * **시안에 없는 막대라 디자인 트랙 확인이 필요하다.**
 */
@Composable
internal fun VoiceCommandPanel(panel: VoicePanel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12)
            .shadow(
                elevation = MedicalMateElevation.float,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .background(colors.bgSurface, MedicalMateRadius.lg)
            .padding(start = MedicalMateSpace.s16, top = MedicalMateSpace.s12, bottom = MedicalMateSpace.s12),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicBadge(listening = panel.voice == MedicalMateVoiceState.LISTENING)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
            Text(
                text = panel.message.resolve(),
                style = MedicalMateTheme.typography.bodyLStrong,
                color = if (panel.confirming) colors.fgPrimary else colors.fgDefault,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            // 화면이 되묻는 것. 고를 것을 한 줄에 하나씩 적을 수 있어(#284) 자르지 않는다. 말하는 동안에도
            // 남긴다 — 목록을 보며 답하는 중에 목록이 사라지면 안 된다.
            val question = panel.question
            if (question != null) {
                Text(
                    text = question.resolve().sentencePerLine(),
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgPrimary,
                )
            }
            // 말하는 중이면 들리는 글을 물음 아래에 따로 적는다. 물음도 들리는 글도 없으면 듣는 상태다.
            if (panel.heard.isNotBlank() || question == null) {
                Text(
                    text = panel.heard.ifBlank { stringResource(panel.voice.titleRes) },
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgSubtle,
                    maxLines = HEARD_MAX_LINES,
                )
            }
        }
        MedicalMateIconButton(
            onClick = onDismiss,
            icon = MedicalMateIcons.Close,
            contentDescription = stringResource(R.string.voice_command_close),
            size = MedicalMateIconButtonSize.M,
        )
    }
}

/** 문구에 값을 끼운다. 값은 많아야 둘이라 펼쳐 넘기지 않고 하나씩 넘긴다. */
@Composable
private fun VoiceMessage.resolve(): String = when (args.size) {
    0 -> stringResource(text)
    1 -> stringResource(text, args[0])
    else -> stringResource(text, args[0], args[1])
}

/** 듣는 중이면 채운 원, 기다리는 중이면 옅은 원. 색만으로 알리지 않고 아랫줄 글이 함께 바뀐다. */
@Composable
private fun MicBadge(listening: Boolean) {
    val colors = MedicalMateTheme.colors
    Box(
        modifier =
        Modifier
            .size(MicBadgeSize)
            .background(if (listening) colors.bgPrimary else colors.bgPrimarySubtle, MedicalMateRadius.full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (listening) MedicalMateIcons.MicListening else MedicalMateIcons.Mic),
            contentDescription = null,
            tint = if (listening) colors.fgOnPrimary else colors.fgPrimary,
            modifier = Modifier.size(MicIconSize),
        )
    }
}

/**
 * 막대가 묻는 말을 문장마다 줄을 바꾼다(#284). "어디가 아프세요? 이 중에서 말씀해 주세요"가 한 줄에
 * 이어지면 무엇을 묻고 무엇을 하라는지가 섞여 읽힌다. 마침표·물음표·느낌표 뒤의 띄어쓰기를 줄바꿈으로
 * 바꾼다. 폭을 넘는 문장은 글꼴의 어절 단위 줄바꿈이 맡는다.
 */
internal fun String.sentencePerLine(): String = replace(SENTENCE_GAP, "\n")

/** 문장 끝 뒤의 띄어쓰기. 닫는 따옴표가 붙어 있어도 본다. "…해 주세요." 다음 줄. */
private val SENTENCE_GAP = Regex("""(?<=[.?!]["”']?)[ \t]+""")

private const val HEARD_MAX_LINES = 3

private val MicBadgeSize = 40.dp

private val MicIconSize = 20.dp

@Preview
@Composable
private fun VoiceCommandPanelPreview() {
    MedicalMateTheme {
        VoiceCommandPanel(
            panel =
            VoicePanel(
                voice = MedicalMateVoiceState.LISTENING,
                heard = "10월 20일 오후 3시",
                message = VoiceMessage(R.string.voice_command_hint),
            ),
            onDismiss = {},
        )
    }
}
