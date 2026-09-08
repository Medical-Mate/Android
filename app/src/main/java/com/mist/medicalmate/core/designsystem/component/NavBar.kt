package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateGlass
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.5 `Nav Bar`의 `Leading` variant. */
enum class MedicalMateNavLeading {
    BACK,
    CLOSE,
    NONE,
}

/**
 * DESIGN.md 8.5 `Nav Bar`.
 *
 * 390x56이고 좌우 slot이 각각 48이다. 제목은 가운데에 고정한다. slot 폭을 좌우 같게
 * 잡아야 제목이 화면 가운데에 온다. 오른쪽 액션 유무에 따라 제목이 움직이면 화면을
 * 넘길 때마다 눈이 따라가야 한다.
 *
 * 우측 주요 액션은 아이콘이 아니라 텍스트 라벨을 쓴다(문서 8.5). 저장이나 완료 같은
 * 동작은 아이콘만으로 뜻이 전달되지 않는다.
 *
 * [MedicalMateNavLeading.BACK]과 [MedicalMateNavLeading.CLOSE]는 뜻이 다르다. 뒤로는
 * 흐름을 한 단계 되돌리고, 닫기는 흐름 전체를 벗어난다. 문답 중간에서 닫기를 누르면
 * 작성 내용을 어떻게 할지 물어야 한다.
 */
@Composable
fun MedicalMateNavBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: MedicalMateNavLeading = MedicalMateNavLeading.BACK,
    onLeadingClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    surface: MedicalMateSurfaceStyle = MedicalMateSurfaceStyle.OPAQUE,
) {
    val colors = MedicalMateTheme.colors
    val background =
        when (surface) {
            MedicalMateSurfaceStyle.OPAQUE -> colors.bgSurface
            MedicalMateSurfaceStyle.GLASS -> colors.bgSurface.copy(alpha = MedicalMateGlass.NAV_BAR_ALPHA)
        }

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(MedicalMateSize.navBarHeight)
            .background(background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(MedicalMateSize.touchMin), contentAlignment = Alignment.Center) {
            LeadingSlot(leading = leading, onClick = onLeadingClick)
        }
        Text(
            text = title,
            style = MedicalMateTheme.typography.headingS,
            color = colors.fgDefault,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(MedicalMateSize.touchMin),
            contentAlignment = Alignment.Center,
        ) {
            if (actionLabel != null && onActionClick != null) {
                MedicalMateButton(
                    onClick = onActionClick,
                    label = actionLabel,
                    type = MedicalMateButtonType.GHOST,
                    size = MedicalMateButtonSize.S,
                    enabled = actionEnabled,
                )
            }
        }
    }
}

@Composable
private fun LeadingSlot(leading: MedicalMateNavLeading, onClick: (() -> Unit)?) {
    val icon =
        when (leading) {
            MedicalMateNavLeading.BACK -> MedicalMateIcons.ChevronLeft
            MedicalMateNavLeading.CLOSE -> MedicalMateIcons.Close
            MedicalMateNavLeading.NONE -> null
        }
    val descriptionRes =
        when (leading) {
            MedicalMateNavLeading.BACK -> R.string.nav_back
            MedicalMateNavLeading.CLOSE -> R.string.nav_close
            MedicalMateNavLeading.NONE -> null
        }

    if (icon != null && descriptionRes != null && onClick != null) {
        MedicalMateIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = stringResource(descriptionRes),
            size = MedicalMateIconButtonSize.M,
        )
    }
}
