package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Source Quote`.
 *
 * AI가 정리한 값 아래에 환자 원문과 수정 진입점을 **항상** 함께 둔다(문서의 접근성 기준). 정리된
 * 문장만 보여주면 환자는 자기가 한 말이 어떻게 바뀌었는지 확인할 수 없고, 진료실에서
 * 틀린 내용을 그대로 의사에게 건네게 된다.
 *
 * [editedBadge]를 주면 환자가 직접 고친 항목으로 표시하고 브랜드 경계를 두른다. 의사가
 * AI 정리와 환자 수정을 구분할 수 있어야 한다.
 *
 * [fieldLabel]과 [quoteLabel]은 파라미터로 받는다. 제품 카피를 디자인 시스템이 들지 않는다.
 */
@Composable
fun MedicalMateSourceQuote(
    fieldLabel: String,
    summary: String,
    quoteLabel: String,
    originalQuote: String,
    onEditClick: () -> Unit,
    editContentDescription: String,
    modifier: Modifier = Modifier,
    editedBadge: String? = null,
) {
    val colors = MedicalMateTheme.colors
    val typography = MedicalMateTheme.typography
    val edited = editedBadge != null

    Surface(
        shape = MedicalMateRadius.lg,
        color = colors.bgSurface,
        contentColor = colors.fgDefault,
        border = if (edited) BorderStroke(EditedBorderWidth, colors.borderPrimary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MedicalMateSpace.s16),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fieldLabel,
                    style = typography.bodyS,
                    color = colors.fgSubtle,
                    modifier = Modifier.weight(1f),
                )
                editedBadge?.let {
                    MedicalMateBadge(label = it, tone = MedicalMateBadgeTone.BRAND)
                }
                MedicalMateIconButton(
                    onClick = onEditClick,
                    icon = MedicalMateIcons.Edit,
                    contentDescription = editContentDescription,
                    size = MedicalMateIconButtonSize.S,
                )
            }
            Text(text = summary, style = typography.headingS)
            OriginalQuote(quoteLabel = quoteLabel, quote = originalQuote)
        }
    }
}

/**
 * 환자 원문 블록.
 *
 * 환자 발화 면(`bg/primary-subtle`)을 [MedicalMateBubble]의 환자 버블과 같은 색으로 둔다.
 * 화면이 달라도 "이건 내가 한 말"이라는 신호가 같아야 한다.
 */
@Composable
private fun OriginalQuote(quoteLabel: String, quote: String) {
    Surface(
        shape = MedicalMateRadius.sm,
        color = MedicalMateTheme.colors.bgPrimarySubtle,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MedicalMateSpace.s12),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
        ) {
            Text(
                text = quoteLabel,
                style = MedicalMateTheme.typography.labelS,
                color = MedicalMateTheme.colors.fgPrimary,
            )
            Text(text = quote, style = MedicalMateTheme.typography.bodyM)
        }
    }
}

/** 문서의 Edited 경계. Chip 선택 경계와 같은 두께를 쓴다. */
private val EditedBorderWidth = 1.5.dp
