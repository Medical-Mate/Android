package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Select Bar`(`1129:9200`) 320x40.
 *
 * 편집 모드에서 몇 건을 골랐는지 알리는 줄이다. 목록 위에 놓인다.
 *
 * 면은 `bg/primary-faint`에 반경 12다. 마스터에는 채움이 묶여 있지 않아 한동안 면 없이
 * 뒀는데, `1j-4-D2`(`1122:5033`)의 인스턴스가 `#F2F4FE`로 칠해져 있다. 그 값이
 * `bg/primary-faint`와 같아서 토큰으로 넣었다.
 *
 * 개수를 문장으로 받는다. 세는 규칙이 화면마다 다르고(카드는 "장", 기록은 "건") 복수형도
 * 없어서 컴포넌트가 숫자를 문장으로 만들면 문구를 여기서 정하게 된다.
 */
@Composable
fun MedicalMateSelectBar(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MedicalMateTheme.typography.bodySStrong,
        color = MedicalMateTheme.colors.fgPrimary,
        modifier =
        modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.sm)
            .padding(horizontal = MedicalMateSpace.s14, vertical = MedicalMateSpace.s10),
    )
}
