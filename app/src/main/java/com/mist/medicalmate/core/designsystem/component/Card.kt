package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint

/**
 * DESIGN.md 8.3 `Card`의 `Emphasis` variant.
 *
 * 큰 유색 면은 환자 콘텐츠에만 쓴다. 시스템 안내나 목록 배경으로 [BRAND]를 쓰지 않는다.
 */
enum class MedicalMateCardEmphasis {
    DEFAULT,
    BRAND,
    QUIET,
}

/**
 * DESIGN.md 8.3 `Card`.
 *
 * 이 시스템은 모든 요소에 테두리를 두르지 않는다. [MedicalMateCardEmphasis.DEFAULT]는
 * 테두리 없이 `Elevation/Card`로 층을 표현한다. 선은 목록 구분, 선택, 포커스, 접근성
 * 경계에만 쓴다(문서 5절).
 *
 * 그림자는 `Surface`의 `shadowElevation`이 아니라 [Modifier.shadow]로 준다. 전자는
 * 그림자 색을 받지 않아 검정으로 나오고, 문서 5절이 브랜드 틴트 `#1B255A`를 쓰라고 한다.
 * 틴트는 API 28부터 적용되므로 24~27에서는 여전히 검정이다.
 *
 * [onClick]을 주면 누를 수 있는 카드가 된다. 들어갈 상세가 있을 때만 준다.
 */
@Composable
fun MedicalMateCard(
    modifier: Modifier = Modifier,
    emphasis: MedicalMateCardEmphasis = MedicalMateCardEmphasis.DEFAULT,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MedicalMateTheme.colors
    val container =
        when (emphasis) {
            MedicalMateCardEmphasis.DEFAULT -> colors.bgSurface
            MedicalMateCardEmphasis.BRAND -> colors.bgPrimaryFaint
            MedicalMateCardEmphasis.QUIET -> colors.bgSubtle
        }

    // Default만 떠 있다. Brand와 Quiet는 면 색으로 구분되므로 그림자를 더하지 않는다.
    val outer =
        if (emphasis == MedicalMateCardEmphasis.DEFAULT) {
            modifier.shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
        } else {
            modifier
        }.fillMaxWidth()

    val body: @Composable () -> Unit = {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CardMinHeight)
                .padding(MedicalMateSpace.s20),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
            content = content,
        )
    }

    if (onClick == null) {
        Surface(
            shape = MedicalMateRadius.lg,
            color = container,
            contentColor = colors.fgDefault,
            modifier = outer,
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = MedicalMateRadius.lg,
            color = container,
            contentColor = colors.fgDefault,
            modifier = outer,
            content = body,
        )
    }
}

/** 문서 8.3이 지정한 최소 높이. */
private val CardMinHeight = 116.dp
