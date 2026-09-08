package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Preview용 캔버스.
 *
 * 컴포넌트를 실제 화면과 같은 조건에 놓는다. `bg/canvas` 위, 좌우 거터 20이다. 흰
 * 배경에서만 보면 흰 면을 쓰는 컴포넌트의 경계가 보이지 않는다.
 *
 * 8.1·8.3·8.4 묶음의 Preview 파일에는 각자 private 사본이 있다. 그때는 파일마다 하나씩
 * 두었는데, 8.2에서 파일을 쪼개면서 공용으로 뺐다. 나머지도 이걸 쓰게 정리할 수 있다.
 */
@Composable
internal fun ComponentPreviewSurface(gap: Dp = MedicalMateSpace.s12, content: @Composable ColumnScope.() -> Unit) {
    MedicalMateTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(MedicalMateTheme.colors.bgCanvas)
                .padding(MedicalMateSize.gutter),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}
