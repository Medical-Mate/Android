package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateQuoteBlock

/**
 * 타임라인 한 단계의 내용. Figma `735:3850`.
 *
 * `Card` 컴포넌트를 쓰지 않는다. 그쪽은 반경 20에 여백 20이고 최소 높이가 116인데, 이
 * 블록은 반경 16에 여백 16이고 재방문 예정처럼 두 줄로 끝나는 것도 있다. 층은 같은
 * `Elevation/Card`로 준다.
 */
@Composable
internal fun RecordStepBlock(step: RecordStep.Block, onActionClick: (RecordStepAction.Target) -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.md,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .background(MedicalMateTheme.colors.bgSurface, MedicalMateRadius.md)
            .padding(MedicalMateSpace.s16),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        Text(
            text = step.title,
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
        )
        step.items.forEach { item -> StepItemRow(item) }
        step.quote?.let { MedicalMateQuoteBlock(label = it.label, text = it.text) }
        step.action?.let { action ->
            StepOpen(label = action.label, onClick = { onActionClick(action.target) })
        }
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
 * 그 단계를 여는 줄. Figma `735:3864`.
 *
 * `Button`을 쓰지 않는다. 그쪽은 높이 56에 반경 16인 화면의 주 행동이고, 이건 블록 안에서
 * 원래 화면으로 건너가는 옅은 줄이다.
 */
@Composable
private fun StepOpen(label: String, onClick: () -> Unit) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgPrimaryFaint, MedicalMateRadius.sm)
            .clickable(onClick = onClick)
            .padding(vertical = OpenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgPrimary,
            textAlign = TextAlign.Center,
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
    MedicalMateNotice(title = step.message, body = step.detail)
}

/** 문서 8.3의 KV Row보다 좁은 키 열. 블록 안 요약이라 한 단계 작다. */
private val ItemKeyWidth = 52.dp

private val OpenPadding = 11.dp
