package com.mist.medicalmate.card.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateNoticeTone
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuoteBlock
import com.mist.medicalmate.core.designsystem.component.MedicalMateSeverityReadout

/**
 * 타임라인 한 단계의 내용. Figma `735:3850`.
 *
 * `Card` 컴포넌트를 쓰지 않는다. 그쪽은 반경 20에 여백 20이고 최소 높이가 116인데, 이
 * 블록은 반경 16에 여백 16이고 재방문 예정처럼 두 줄로 끝나는 것도 있다. 층은 같은
 * `Elevation/Card`로 준다.
 *
 * **카드를 펼치고 접을 때 높이가 이어진다**(#243). 줄이 몇 개 더 붙고 통증 눈금·알러지·질문
 * 블록까지 들어오는데 한 프레임에 튀면 무엇이 늘어난 것인지 눈이 따라가지 못한다. 기기에서
 * 애니메이션을 껐으면 즉시 바뀐다.
 *
 * **펼친 뒤에는 그 블록으로 화면을 옮긴다.** 브리핑 카드는 타임라인의 마지막 단계라 화면
 * 아래쪽에 있고, 펼치면 새로 나온 내용이 화면 밖으로 나간다. 높이가 자라는 동안이 아니라
 * 자리를 잡은 뒤에 옮긴다 — 자라는 중에 부르면 옛 높이를 기준으로 서서 아래가 다시 잘린다.
 */
@Composable
internal fun RecordStepBlock(step: RecordStep.Block, expanded: Boolean, onExpandToggle: () -> Unit) {
    val block = remember { BringIntoViewRequester() }
    var settled by remember { mutableIntStateOf(0) }

    LaunchedEffect(settled) {
        if (settled > 0 && expanded) block.bringIntoView()
    }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            // 그림자가 크기 애니메이션보다 바깥이어야 한다. `animateContentSize`는 내용을
            // 자기 크기로 잘라서, 안쪽에 두면 옆으로 퍼지는 그림자가 잘려 카드 좌우가 칼로
            // 벤 것처럼 보인다.
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.md,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .animateContentSize(
                animationSpec = tween(EXPAND_DURATION, easing = FastOutSlowInEasing),
                finishedListener = { _, _ -> settled += 1 },
            )
            .bringIntoViewRequester(block)
            .background(MedicalMateTheme.colors.bgSurface, MedicalMateRadius.md)
            .padding(MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        Text(
            text = step.title,
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
        )
        val shown =
            if (step.card == null || expanded) step.items else step.items.take(step.card.collapsedItemCount)
        shown.forEach { item -> StepItemRow(item) }
        step.quote?.let { MedicalMateQuoteBlock(label = it.label, text = it.text) }
        step.card?.let { card ->
            if (expanded) ExpandedCard(card)
            StepExpandRow(expanded = expanded, onClick = onExpandToggle)
        }
    }
}

/** 펼침·접힘이 도는 시간. 화면 전환과 같은 Material 표준 값이다. */
private const val EXPAND_DURATION = 320

/**
 * 펼쳤을 때 줄 아래 붙는 것들.
 *
 * 통증 강도·알러지·질문은 브리핑 카드 화면(1e-1)과 같은 컴포넌트를 쓴다. 같은 카드를 다른
 * 자리에서 보는 것이라 모양이 갈리면 안 된다.
 */
@Composable
private fun ExpandedCard(card: RecordStepCard) {
    card.severity?.let { MedicalMateSeverityReadout(severity = it) }
    if (card.allergies.isNotEmpty()) {
        AllergyNotice(allergies = card.allergies)
    }
    if (card.questions.isNotEmpty()) {
        QuestionsCallout(questions = card.questions)
    }
}

/**
 * 한 줄. 키 폭을 고정해서 값이 세로로 맞는다.
 *
 * 브리핑 카드의 `KV Row`를 쓰지 않는다. 그쪽은 키 폭 72에 값이 `Body/L`인 본문이고, 이건
 * 블록 안 요약이라 키 폭 52에 값이 `Body/M`으로 한 단계 작다.
 */
@Composable
private fun StepItemRow(item: RecordDetailItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MedicalMateSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        Text(
            text = item.key,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
            modifier = Modifier.width(ItemKeyWidth),
        )
        Text(
            text = item.value,
            style = MedicalMateTheme.typography.bodyM,
            color = itemValueColor(item.tone),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun itemValueColor(tone: RecordDetailItem.Tone): Color = when (tone) {
    RecordDetailItem.Tone.DEFAULT -> MedicalMateTheme.colors.fgDefault
    RecordDetailItem.Tone.WARNING -> MedicalMateTheme.colors.fgWarning
    RecordDetailItem.Tone.LINK -> MedicalMateTheme.colors.fgPrimary
}

/**
 * 카드를 접었다 펴는 줄. Figma 1j-3 `735:3864`, 1j-3-X `1038:2768`.
 *
 * 위에 구분선을 두고 왼쪽에 글자, 오른쪽에 화살표다. 전에는 가운데 정렬한 옅은 면의 줄로
 * 그렸는데 시안이 구분선과 화살표로 바꿨다. 누르면 화면을 옮기는 것이 아니라 이 자리가
 * 늘어나는 조작이라, 면을 채운 버튼보다 접기·펴기로 읽히는 모양이 맞는다.
 *
 * `Button`을 쓰지 않는다. 그쪽은 높이 56에 반경 16인 화면의 주 행동이다.
 */
@Composable
private fun StepExpandRow(expanded: Boolean, onClick: () -> Unit) {
    MedicalMateDivider()
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = OpenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(if (expanded) R.string.record_detail_collapse else R.string.record_detail_expand),
            style = MedicalMateTheme.typography.bodyMStrong,
            color = MedicalMateTheme.colors.fgPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(if (expanded) MedicalMateIcons.ChevronUp else MedicalMateIcons.ChevronDown),
            contentDescription = null,
            tint = MedicalMateTheme.colors.fgPrimary,
        )
    }
}

/**
 * 아직 오지 않은 단계.
 *
 * 점선 테두리로 그리던 것을 [MedicalMateNotice]로 바꿨다. 시안 `1j-3`이 여기에 ⓘ 아이콘이
 * 있는 옅은 브랜드 면을 쓴다. 앞으로 올 일을 알리는 자리라 통증 강도 화면의 안내와 같은
 * 컴포넌트이고, 점선은 "입력할 자리"로 읽혀 뜻이 달랐다.
 */
@Composable
internal fun RecordStepPending(step: RecordStep.Pending) {
    // 시안(`1j-3`)의 예정 알림은 브랜드색으로 채운 면이다. 타임라인의 다른 블록이 흰 카드라
    // 앞으로 갈 일 하나만 색으로 선다.
    MedicalMateNotice(title = step.message, body = step.detail, tone = MedicalMateNoticeTone.BRAND)
}

/** 문서의 KV Row보다 좁은 키 열. 블록 안 요약이라 한 단계 작다. */
private val ItemKeyWidth = 52.dp

private val OpenPadding = 11.dp
