package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint

/**
 * Figma `Card Pick`(`1129:9198`) 320x84.
 *
 * 일정에 붙일 브리핑 카드를 고르는 행이다. 카드 모양이지만 읽는 카드가 아니라 고르는
 * 컨트롤이라 [MedicalMateCard]를 쓰지 않는다. 그 컴포넌트는 여백 20에 테두리가 없고,
 * 이쪽은 여백 18/16에 골랐다는 표시로 테두리를 두른다.
 *
 * 고른 표시가 둘이다. 체크 상자가 채워지고 테두리가 브랜드색으로 바뀐다. 색만으로
 * 구분하지 않는다는 문서의 D11을 지키려면 형태가 함께 바뀌어야 한다.
 *
 * 행 전체가 hit area다. 24 상자만 누를 수 있으면 접근성 기준 48에 못 미친다. 그래서 상자에
 * 조작을 걸지 않고 행이 [Role.Checkbox]로 받는다. [MedicalMateCheckbox]를 안에 넣지 않는
 * 이유도 같다. 그쪽은 라벨과 행 조작을 함께 들고 있어서 조작이 두 겹이 된다.
 *
 * 마스터의 제목·메타 간격은 2다. 카드 제목과 그 카드를 식별하는 메타라 한 덩어리로 붙여
 * 둔 것이고 `space/2`가 그 값이다.
 *
 * 면색은 `bg/surface`다. 마스터에는 채움이 묶여 있지 않은데, 그림자와 반경만 있고 면이
 * 없으면 카드로 보이지 않는다. 디자인 트랙에 확인을 넘겼다.
 */
@Composable
fun MedicalMateCardPick(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    title: String,
    meta: String,
    modifier: Modifier = Modifier,
) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .clip(MedicalMateRadius.lg)
            .background(colors.bgSurface)
            .border(
                width = BorderWidth,
                color = if (selected) colors.bgPrimary else colors.borderSubtle,
                shape = MedicalMateRadius.lg,
            )
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = onSelectedChange,
            )
            .padding(horizontal = HorizontalPadding, vertical = MedicalMateSpace.s16),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(selected = selected)
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
            Text(text = title, style = MedicalMateTheme.typography.bodyLStrong, color = colors.fgDefault)
            Text(text = meta, style = MedicalMateTheme.typography.bodyS, color = colors.fgSubtle)
        }
    }
}

/** 고른 표시. 상자 규격은 `Checkbox` 마스터와 같다. 24 · 반경 8 · 미선택 테두리 1.5. */
@Composable
private fun CheckMark(selected: Boolean) {
    val colors = MedicalMateTheme.colors
    Box(
        modifier =
        Modifier
            .size(BoxSize)
            .background(
                color = if (selected) colors.bgPrimary else colors.bgSurface,
                shape = MedicalMateRadius.xs,
            )
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        width = BorderWidth,
                        color = colors.borderStrong,
                        shape = MedicalMateRadius.xs,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(MedicalMateIcons.Check),
                contentDescription = null,
                tint = colors.fgOnPrimary,
                modifier = Modifier.size(MedicalMateSize.iconSm),
            )
        }
    }
}

/** 마스터의 테두리 1.5. 미선택도 같은 굵기여야 고를 때 크기가 흔들리지 않는다. */
private val BorderWidth = 1.5.dp

/** 마스터의 좌우 여백 18. */
private val HorizontalPadding = 18.dp

/** `Checkbox` 마스터의 상자 24. */
private val BoxSize = 24.dp
