package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 하단 고정 `Footer`. `Wireframe 10-15` 화면들이 쓰는 모양이다.
 *
 * `bg/surface` 불투명에 효과가 없다. 패딩은 위 12 · 좌우 20 · 아래 24이고 버튼은 폭을 채운다. 버튼을
 * 둘 쌓으면 10씩 벌린다(1c-5).
 *
 * **[MedicalMateBottomCtaBar]와 다른 것은 그림자다.** 그쪽은 마스터 `Bottom CTA Bar`(지금은 Archive)의
 * `Elevation/Float`를 달고 있다. 10-15 화면의 Footer에는 효과가 없다. 화면을 새 시안으로 옮길 때 이것으로
 * 바꾼다.
 */
@Composable
fun MedicalMateFooter(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                top = MedicalMateSpace.s12,
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                bottom = MedicalMateSize.safeBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        content = content,
    )
}
