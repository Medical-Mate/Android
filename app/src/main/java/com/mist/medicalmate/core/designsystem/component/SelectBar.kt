package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Select Bar`(`1129:9200`) 320x40.
 *
 * 편집 모드에서 몇 건을 골랐는지 알리는 줄이다. 목록 위에 놓인다.
 *
 * **면색이 없다.** 마스터에 반경 12가 있는데 채움은 묶여 있지 않다. 채우지 않은 면에
 * 반경은 보이지 않으므로 반경도 넣지 않았다. 디자인 트랙에 확인을 넘겼다.
 *
 * 개수를 문장으로 받는다. 세는 규칙("2건 선택됨")이 화면마다 다르고 복수형도 없어서
 * 컴포넌트가 숫자를 문장으로 만들면 문구를 여기서 정하게 된다.
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
            .padding(horizontal = MedicalMateSpace.s14, vertical = MedicalMateSpace.s10),
    )
}
