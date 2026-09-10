package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 환자가 말한 원문 블록. Figma 1j-3 `735:3861`, 1q-1 `617:2744`.
 *
 * AI가 정리한 값 아래에 원문을 그대로 남긴다(문서의 접근성 기준). 정리된 문장만 보여주면 환자는
 * 자기가 한 말이 어떻게 바뀌었는지 확인할 수 없고, 진료실에서 틀린 내용을 그대로 의사에게
 * 건네게 된다.
 *
 * 면 색을 [MedicalMateBubble]의 환자 버블과 같은 `bg/primary-subtle`로 둔다. 화면이 달라도
 * "이건 내가 한 말"이라는 신호가 같아야 한다.
 *
 * [MedicalMateSourceQuote] 안의 원문 블록과 규격이 다르다. 그쪽은 항목 하나를 감싸는
 * 카드의 일부여서 본문이 `Body/M`이고, 이것은 카드 안에 들어가는 인용이라 `Body/S`다.
 */
@Composable
fun MedicalMateQuoteBlock(label: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgPrimarySubtle, MedicalMateRadius.sm)
            .padding(MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.labelS,
            color = MedicalMateTheme.colors.fgPrimary,
        )
        Text(
            text = text,
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgDefault,
        )
    }
}
