package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.3 `Bubble`의 `Sender` variant. */
enum class MedicalMateBubbleSender {
    AI,
    PATIENT,
}

/**
 * DESIGN.md 8.3 `Bubble`.
 *
 * 환자의 말을 가장 강하게 보여준다는 원칙이 이 컴포넌트에서 가장 잘 드러난다(문서 1항 5번).
 * 환자 발화는 `bg/primary-subtle` 면을 갖고, AI 발화는 면 없이 본문만 둔다. AI를 브랜드
 * 색으로 강조하지 않는다.
 *
 * [MedicalMateBubbleSender.AI]는 위에 발화 주체 라벨을 붙인다. 누가 한 말인지 구분되지
 * 않으면 환자가 AI의 정리를 자기 말로 착각한다.
 *
 * 문서는 환자 버블에 꼬리를 쓴다고 적었지만 Figma 마스터에는 꼬리가 보이지 않는다. 여기서는
 * 마스터를 따라 모서리만 둥근 면으로 두고, 꼬리가 필요하면 디자인 트랙에서 확정한다.
 */
@Composable
fun MedicalMateBubble(
    text: String,
    modifier: Modifier = Modifier,
    sender: MedicalMateBubbleSender = MedicalMateBubbleSender.AI,
    senderLabel: String? = null,
) {
    val colors = MedicalMateTheme.colors
    val typography = MedicalMateTheme.typography

    when (sender) {
        MedicalMateBubbleSender.AI ->
            Column(
                modifier = modifier.widthIn(max = BubbleMaxWidth),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                senderLabel?.let {
                    Text(text = it, style = typography.labelS, color = colors.fgMuted)
                }
                Text(text = text, style = typography.bodyL, color = colors.fgDefault)
            }

        MedicalMateBubbleSender.PATIENT ->
            Surface(
                shape = MedicalMateRadius.md,
                color = colors.bgPrimarySubtle,
                contentColor = colors.fgDefault,
                modifier = modifier.widthIn(max = BubbleMaxWidth),
            ) {
                Text(
                    text = text,
                    style = typography.bodyL,
                    modifier =
                    Modifier.padding(
                        horizontal = MedicalMateSpace.s16,
                        vertical = MedicalMateSpace.s12,
                    ),
                )
            }
    }
}

/** 문서 8.3이 지정한 폭. AI 280x72, Patient 280x50이라 최대 폭이 같다. */
private val BubbleMaxWidth = 280.dp
