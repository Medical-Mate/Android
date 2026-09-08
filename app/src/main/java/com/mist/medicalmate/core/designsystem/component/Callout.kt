package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.3 `Callout`.
 *
 * 브리핑 카드에서 "환자가 묻고 싶어 하는 것"을 한 번만 보여준다. 화면에 두 번 이상 두면
 * 무엇이 환자의 질문인지 흐려진다.
 *
 * 질문마다 번호와 흰색 pill을 쓴다. 의사가 진료실에서 순서대로 짚어야 하므로 번호가
 * 필요하다.
 *
 * 제목 문구는 파라미터로 받는다. 제품 카피를 디자인 시스템이 들고 있으면 문구를 바꿀 때
 * 공용 계층을 고쳐야 한다.
 */
@Composable
fun MedicalMateCallout(title: String, questions: List<String>, modifier: Modifier = Modifier) {
    Surface(
        shape = MedicalMateRadius.lg,
        color = MedicalMateTheme.colors.bgPrimarySubtle,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MedicalMateSpace.s16),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Chat),
                    contentDescription = null,
                    tint = MedicalMateTheme.colors.fgPrimary,
                    modifier = Modifier.size(MedicalMateSize.iconSm),
                )
                Text(
                    text = title,
                    style = MedicalMateTheme.typography.labelM,
                    color = MedicalMateTheme.colors.fgPrimary,
                )
            }
            questions.forEachIndexed { index, question ->
                QuestionPill(number = index + 1, question = question)
            }
        }
    }
}

@Composable
private fun QuestionPill(number: Int, question: String) {
    Surface(
        shape = MedicalMateRadius.sm,
        // 문서 8.3이 지정한 흰색 75%다. 뒤의 브랜드 tint가 살짝 배어 나온다.
        color = MedicalMateTheme.colors.bgSurface.copy(alpha = QUESTION_PILL_ALPHA),
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MedicalMateSpace.s12),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MedicalMateRadius.full,
                color = MedicalMateTheme.colors.bgPrimary,
                contentColor = MedicalMateTheme.colors.fgOnPrimary,
                modifier = Modifier.size(NumberSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = number.toString(), style = MedicalMateTheme.typography.labelM)
                }
            }
            Text(text = question, style = MedicalMateTheme.typography.bodyM)
        }
    }
}

private const val QUESTION_PILL_ALPHA = 0.75f

private val NumberSize = 24.dp
