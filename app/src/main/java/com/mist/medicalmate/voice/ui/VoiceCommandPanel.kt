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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mist.medicalmate.core.designsystem.component.MateWaveform
import com.mist.medicalmate.core.designsystem.component.MateWaveformShape
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.designsystem.component.rememberSpeechActivityLevel
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
 * **고를 것은 위 가이드 자리에 선다**(#320, 사용자 결정). 물음에 고를 것(부위 · 강도 · 추천 질문 · 후보)이 딸려 있으면
 * 추천 말 대신 그 목록을 가이드 줄 모양으로 위에 세우고, 아랫줄에는 물음만 남긴다. 전에는 아랫줄에 한 줄에
 * 하나씩 붙여 패널이 화면 절반을 덮었다. 줄은 세 줄까지 보이고 넘치면 그 안에서 굴린다.
 *
 * 물음은 말하는 동안에도 남긴다(#284) — 목록을 보며 답하는 중에 목록이 사라지면 안 된다. 그래서 말하는 중에
 * 물음이 있으면 "듣고 있어요" 대신 물음을 적는다. 마스터의 `답` 상태(물은 것에 답하는 본문)는 그런 질문을
 * 알아듣는 명령이 아직 없어 쓰는 자리가 없다.
 */
@Composable
internal fun VoiceCommandPanel(panel: VoicePanel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MedicalMateRadius.xl
    val question = panel.question?.resolve()?.sentencePerLine()?.let(::splitChoices)
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
        when {
            question != null && question.items.isNotEmpty() -> {
                ChoiceGuide(question)
                MedicalMateDivider()
            }

            panel.suggestions.isNotEmpty() -> {
                Suggestions(panel.suggestions)
                MedicalMateDivider()
            }
        }
        Main(panel = panel, question = question?.rest, onDismiss = onDismiss)
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

/**
 * 고를 것(#320). 가이드와 같은 줄 모양이고 원 안에 몇 번째인지를 적는다 — "세 번째"로도 고르게. 이미 담은 것은
 * 오른쪽에 그렇게 적는다. 세 줄까지 보이고 넘치면 굴린다.
 */
@Composable
private fun ChoiceGuide(choices: VoiceChoices) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = MedicalMateSpace.s6),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
    ) {
        Text(
            text = choices.title ?: stringResource(R.string.voice_choice_title),
            style = MedicalMateTheme.typography.labelM,
            color = colors.fgSubtle,
        )
        Spacer(modifier = Modifier.height(MedicalMateSpace.s4))
        Column(
            modifier =
            Modifier
                .heightIn(max = GuideRowMinHeight * MAX_CHOICE_ROWS + MedicalMateSpace.s2 * (MAX_CHOICE_ROWS - 1))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
        ) {
            choices.items.forEachIndexed { index, choice -> ChoiceRow(number = index + 1, choice = choice) }
        }
        Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
    }
}

@Composable
private fun ChoiceRow(number: Int, choice: VoiceChoice) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = GuideRowMinHeight),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(GuideIconCircle).background(colors.bgSubtle, MedicalMateRadius.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = MedicalMateTheme.typography.labelS, color = colors.fgSubtle)
        }
        Text(
            text = choice.text,
            style = MedicalMateTheme.typography.bodyMStrong,
            color = colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        choice.note?.let { Text(text = it, style = MedicalMateTheme.typography.bodyS, color = colors.fgSubtle) }
    }
}

/** 아랫줄. 파형 · 지금 할 말과 그 아래 한 줄 · 닫기다. [question]은 고를 것을 떼어 낸 물음이다. */
@Composable
private fun Main(panel: VoicePanel, question: String?, onDismiss: () -> Unit) {
    val colors = MedicalMateTheme.colors
    val speaking = panel.heard.isNotBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wave(heard = panel.heard)
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
                text = question ?: stringResource(panel.voice.titleRes),
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
 * 마스터의 `Wave`([MateWaveformShape.Panel]). **말이 들릴 때만 움직인다**(#320, 사용자 결정 — 증상 문답 · 진료 후
 * 메모의 카드와 같다). 받아쓴 글([heard])이 새로 들어오면 움직이고, 들리지 않으면 마스터의 조용한 높이에 85%
 * 불투명으로 멈춘다. 색만으로 알리지 않는다 — 아랫줄 글이 함께 바뀐다.
 */
@Composable
private fun Wave(heard: String) {
    MateWaveform(
        level = rememberSpeechActivityLevel(partialText = heard),
        shape = MateWaveformShape.Panel,
        modifier = Modifier.clearAndSetSemantics { },
    )
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

/** 막대가 고를 것. [note]는 오른쪽에 적는 말("담음")이다. */
internal data class VoiceChoice(val text: String, val note: String? = null)

/**
 * 물음에서 떼어 낸 고를 것. [title]은 목록 바로 위의 안내("이 중에서 말씀해 주세요")이고 [rest]는 남은 물음이다.
 */
internal data class VoiceChoices(val title: String?, val items: List<VoiceChoice>, val rest: String?)

/**
 * 물음을 고를 것과 남은 물음으로 나눈다(#320). 화면들이 고를 것을 "· 어깨"나 "1. 질문" 꼴로 한 줄에 하나씩 적는다.
 * 목록 바로 위 줄이 물음표로 끝나지 않으면 목록의 안내로 보고 머리에 올린다. "(담음)"은 오른쪽에 적는다.
 */
internal fun splitChoices(question: String): VoiceChoices {
    val lines = question.lines().filter { it.isNotBlank() }
    val first = lines.indexOfFirst { CHOICE_LINE.containsMatchIn(it) }
    if (first < 0) return VoiceChoices(title = null, items = emptyList(), rest = question)
    val titleIndex = (first - 1).takeIf { it >= 0 && !lines[it].trimEnd().endsWith("?") }
    val items =
        lines.filter { CHOICE_LINE.containsMatchIn(it) }.map { line ->
            val text = line.replace(CHOICE_LINE, "").trim()
            val marked = text.endsWith(MARKED)
            VoiceChoice(text = text.removeSuffix(MARKED).trim(), note = MARKED_NOTE.takeIf { marked })
        }
    val rest =
        lines.filterIndexed { index, line -> index != titleIndex && !CHOICE_LINE.containsMatchIn(line) }
            .joinToString("\n")
            .ifBlank { null }
    return VoiceChoices(title = titleIndex?.let { lines[it].trim() }, items = items, rest = rest)
}

/** 고를 것의 줄 머리. "· 어깨", "2. 약 먹어도 되나요?". */
private val CHOICE_LINE = Regex("""^\s*(?:·|\d{1,2}\.)\s+""")

private const val MARKED = "(담음)"

private const val MARKED_NOTE = "담음"

/** 고를 것이 이보다 많으면 그 안에서 굴린다. */
private const val MAX_CHOICE_ROWS = 3

/** 문장 끝 뒤의 띄어쓰기. 닫는 따옴표가 붙어 있어도 본다. "…해 주세요." 다음 줄. "1. "의 번호 뒤는 아니다(#324). */
private val SENTENCE_GAP = Regex("""(?<![0-9]\.)(?<=[.?!]["”']?)[ \t]+""")

private const val HEARD_MAX_LINES = 3

/** 마스터 `Guide Row`의 최소 높이 36과 아이콘 원 28. 크기 토큰에 없는 값이다. */
private val GuideRowMinHeight = 36.dp

private val GuideIconCircle = 28.dp

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
