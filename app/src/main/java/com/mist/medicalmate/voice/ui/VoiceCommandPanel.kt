package com.mist.medicalmate.voice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.voice.VoiceSuggestion

/**
 * "헤이 메이트"로 부르면 뜨는 `Mate Card`(Figma `3532:5543`, #273·#282·#312).
 *
 * **화면을 가리지 않는다.** 딤 없이 좌우 12에 뜨고 뒤의 화면을 그대로 볼 수 있고 누를 수도 있다. 탭바 위 12에
 * 서는 자리는 그래프 쪽(`VoiceCommandHost`)이 정한다.
 *
 * 카드는 반경 24 · `Elevation/Float` 두 겹 · 왼쪽 16 오른쪽 10 위아래 16이다. 아랫줄(`Main`)은 파형 · 글 · 닫기를
 * 14 간격으로 둔다. 마스터의 상태를 이렇게 옮긴다.
 *
 * - **듣는 중 · 가이드**: 추천 말이 있으면(#292) "이렇게 말해도 돼요" 목록과 `Divider`가 위에 선다.
 * - **말하는 중**: 들리는 글이 제목 자리에 서고 파형이 커진다.
 * - **화면 이동**: 방금 한 일이 제목이고, 이어서 할 말(물음)이 `fg/primary` 작은 글로 아래에 선다.
 *
 * 물음은 말하는 동안에도 남긴다(#284) — 목록을 보며 답하는 중에 목록이 사라지면 안 된다. 그래서 말하는 중에
 * 물음이 있으면 "듣고 있어요" 대신 물음을 적는다. 마스터의 `답` 상태(물은 것에 답하는 본문)는 그런 질문을
 * 알아듣는 명령이 아직 없어 쓰는 자리가 없다.
 */
@Composable
internal fun VoiceCommandPanel(panel: VoicePanel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MedicalMateRadius.xl
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = MedicalMateSpace.s12)
            .dropShadow(shape, MedicalMateEffects.floatWide)
            .dropShadow(shape, MedicalMateEffects.floatTight)
            .clip(shape)
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSpace.s16,
                end = MedicalMateSpace.s10,
                top = MedicalMateSpace.s16,
                bottom = MedicalMateSpace.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        if (panel.suggestions.isNotEmpty()) {
            Suggestions(panel.suggestions)
            MedicalMateDivider()
        }
        Main(panel = panel, onDismiss = onDismiss)
    }
}

/**
 * 추천 말 목록(#292). 마스터의 `Guide`다 — `Label/M` 머리 아래 줄마다 28 원 아이콘 · 따옴표로 감싼 말 ·
 * 오른쪽의 하는 일이다.
 *
 * 누르는 줄이 아니다. 무엇을 말하면 되는지 읽히는 것이 목적이라 버튼 역할을 얹지 않는다. 손으로 할
 * 사람은 화면의 버튼을 누르면 된다.
 */
@Composable
private fun Suggestions(suggestions: List<VoiceSuggestion>) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = MedicalMateSpace.s6),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
    ) {
        Text(
            text = stringResource(R.string.voice_suggest_title),
            style = MedicalMateTheme.typography.labelM,
            color = colors.fgSubtle,
        )
        Spacer(modifier = Modifier.height(MedicalMateSpace.s4))
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = GuideRowMinHeight),
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(GuideIconCircle).background(colors.bgSubtle, MedicalMateRadius.full),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(suggestion.icon),
                        contentDescription = null,
                        tint = colors.fgSubtle,
                        modifier = Modifier.size(MedicalMateSize.iconSm),
                    )
                }
                Text(
                    text = stringResource(R.string.voice_suggest_quote, stringResource(suggestion.phrase)),
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = colors.fgDefault,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(suggestion.action),
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgSubtle,
                )
            }
        }
        Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
    }
}

/** 아랫줄. 파형 · 지금 할 말과 그 아래 한 줄 · 닫기다. */
@Composable
private fun Main(panel: VoicePanel, onDismiss: () -> Unit) {
    val colors = MedicalMateTheme.colors
    val speaking = panel.heard.isNotBlank()
    val question = panel.question
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wave(speaking = speaking)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
            Text(
                text = if (speaking) panel.heard else panel.message.resolve().sentencePerLine(),
                style = MedicalMateTheme.typography.headingS,
                color = colors.fgDefault,
                maxLines = if (speaking) HEARD_MAX_LINES else Int.MAX_VALUE,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            // 화면이 되묻는 것. 고를 것을 한 줄에 하나씩 적을 수 있어(#284) 자르지 않는다.
            Text(
                text = question?.resolve()?.sentencePerLine() ?: stringResource(panel.voice.titleRes),
                style = MedicalMateTheme.typography.bodyS,
                color = if (question != null) colors.fgPrimary else colors.fgSubtle,
            )
        }
        MedicalMateIconButton(
            onClick = onDismiss,
            icon = MedicalMateIcons.Close,
            contentDescription = stringResource(R.string.voice_command_close),
            size = MedicalMateIconButtonSize.M,
        )
    }
}

/**
 * 마스터의 `Wave`. 40×48 안에 3 폭 막대 다섯을 3 간격으로 세운다. 말이 들리면 막대가 커지고, 아니면 작고 85%
 * 불투명이다. 색만으로 알리지 않는다 — 아랫줄 글이 함께 바뀐다.
 */
@Composable
private fun Wave(speaking: Boolean) {
    val heights = if (speaking) SpeakingBars else QuietBars
    Row(
        modifier = Modifier.size(width = WaveWidth, height = MedicalMateSize.controlMd).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(WaveGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { height ->
            Box(
                modifier =
                Modifier
                    .size(width = WaveBarWidth, height = height)
                    .alpha(if (speaking) 1f else QUIET_ALPHA)
                    .background(MedicalMateTheme.colors.bgPrimary, WaveBarShape),
            )
        }
    }
}

/** 문구에 값을 끼운다. 값은 많아야 둘이라 펼쳐 넘기지 않고 하나씩 넘긴다. */
@Composable
private fun VoiceMessage.resolve(): String = when (args.size) {
    0 -> stringResource(text)
    1 -> stringResource(text, args[0])
    else -> stringResource(text, args[0], args[1])
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

/** 마스터 `Guide Row`의 최소 높이 36과 아이콘 원 28. 크기 토큰에 없는 값이다. */
private val GuideRowMinHeight = 36.dp

private val GuideIconCircle = 28.dp

private val WaveWidth = 40.dp

private val WaveGap = 3.dp

private val WaveBarWidth = 3.dp

private val WaveBarShape = RoundedCornerShape(1.5.dp)

/** 마스터의 막대 높이. 말하는 중과 그 밖의 두 벌이다. */
private val SpeakingBars = listOf(10.dp, 18.dp, 24.dp, 16.dp, 10.dp)

private val QuietBars = listOf(8.dp, 12.dp, 16.dp, 12.dp, 8.dp)

private const val QUIET_ALPHA = 0.85f

@Preview
@Composable
private fun VoiceCommandPanelPreview() {
    MedicalMateTheme {
        VoiceCommandPanel(
            panel =
            VoicePanel(
                voice = MedicalMateVoiceState.LISTENING,
                message = VoiceMessage(R.string.voice_command_hint),
                suggestions = GLOBAL_SUGGESTIONS,
            ),
            onDismiss = {},
        )
    }
}
