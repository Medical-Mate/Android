package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateGlass
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint

/**
 * DESIGN.md의 `Bottom CTA Bar`.
 *
 * 한 손으로 누를 주 액션을 화면 아래에 고정한다. 높이 92는 위 여백 12 + L 버튼 56 +
 * 아래 안전 여백 24다.
 *
 * **자식이 둘 이상이면 10씩 벌린다.** 전에는 간격이 없어서 버튼 둘이나 체크박스와 버튼이
 * 맞붙었다. 1c-5의 두 버튼이 시안에서 10 떨어져 있고(78 − 12 − 56), 하단에 나란히 놓이는
 * 것들이 붙어 있어야 할 이유가 없다. 자식이 하나면 이 값은 보이지 않는다.
 *
 * **기본값이 [MedicalMateSurfaceStyle.OPAQUE]다.** 문서의 고도 규칙은 스크롤 콘텐츠 위에
 * Glass를 쓰라고 하는데, 지금 구현에서 블러가 걸리지 않는다. compose-ui 1.10.5에는 뒤
 * 배경을 블러하는 API가 없다. `Modifier.blur`는 자기 콘텐츠를 블러하는 것이고
 * 클래스 목록에 `Backdrop` 계열이 없다. `RenderEffect`도 API 31부터라 minSdk 24에서는
 * 절반이 넘는 지원 범위에서 쓸 수 없다.
 *
 * 블러 없이 반투명만 주면 아래로 지나가는 글자가 버튼 라벨과 겹쳐 읽힌다. 블러가 하려던
 * 일이 그것을 막는 것이라서, 대안이 생기기 전까지 기본값을 불투명으로 둔다. [GLASS]는
 * 문서 값(`bg/surface` 78%)대로 남겨 뒀고 배경이 단순한 화면에서 쓸 수 있다.
 *
 * **불투명 변형에는 `Elevation/Float`이 걸린다.** 마스터(`294:652`)의 `Surface=Opaque`가
 * 그 그림자를 달고 있고 `Glass`는 블러만 있다. 블러가 없는 쪽은 그림자가 층을 만들어야
 * 한다 — 없으면 스크롤되는 본문이 바 위에서 잘릴 때 위에 뜬 층이 아니라 본문이 잘린
 * 것으로 읽힌다.
 *
 * 문서는 Glass에 테두리를 추가하지 말라고 한다. 불투명 변형에도 테두리를 두지 않는다.
 */
@Composable
fun MedicalMateBottomCtaBar(
    modifier: Modifier = Modifier,
    surface: MedicalMateSurfaceStyle = MedicalMateSurfaceStyle.OPAQUE,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background =
        when (surface) {
            MedicalMateSurfaceStyle.OPAQUE -> MedicalMateTheme.colors.bgSurface
            MedicalMateSurfaceStyle.GLASS ->
                MedicalMateTheme.colors.bgSurface.copy(alpha = MedicalMateGlass.BOTTOM_CTA_ALPHA)
        }

    val layer =
        when (surface) {
            // 마스터의 그림자는 y+4라 아래로 떨어진다. 화면 맨 아래에 붙는 바에서는 그쪽이
            // 잘리고 위쪽 ambient만 남는데, 본문과 바를 가르는 데는 그 한 겹이면 된다.
            MedicalMateSurfaceStyle.OPAQUE ->
                modifier.shadow(
                    elevation = MedicalMateElevation.float,
                    ambientColor = ShadowTint,
                    spotColor = ShadowTint,
                )

            MedicalMateSurfaceStyle.GLASS -> modifier
        }

    Column(
        modifier =
        layer
            .fillMaxWidth()
            .background(background)
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
