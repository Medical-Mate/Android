package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateGlass
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md의 `Nav Bar`의 `Leading` variant. */
enum class MedicalMateNavLeading {
    BACK,
    CLOSE,
    NONE,
}

/**
 * DESIGN.md의 `Nav Bar`.
 *
 * 높이 56이고 좌우 slot이 각각 48이다. 바깥에 8을 두고 slot 사이는 4다 — 그래서 아이콘의
 * 왼쪽 끝이 화면에서 20에 온다(8 + 슬롯 48 안의 12).
 *
 * **제목은 바의 가운데다**(#226). 남은 폭의 가운데가 아니다. 전에는 좌우 슬롯 사이에 제목을
 * 끼워 넣어서, 오른쪽에 텍스트 액션이 붙으면 그 폭만큼 제목이 왼쪽으로 밀렸다. 마스터는
 * 좌우에 같은 값을 비우고 그 안에서 가운데에 둔다 — 액션이 없으면 60, 텍스트 액션이 있으면
 * 88이다. 화면을 넘길 때마다 제목이 자리를 지켜야 눈이 따라가지 않는다.
 *
 * 우측 주요 액션은 아이콘이 아니라 텍스트 라벨을 쓴다(문서의 컴포넌트 규격). 저장이나 완료 같은
 * 동작은 아이콘만으로 뜻이 전달되지 않는다. 그 라벨은 `Body/L Strong` 17이다 — 버튼 컴포넌트의
 * S 라벨(13)로 두면 같은 자리의 글자가 화면마다 다른 크기로 선다.
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

    val hasAction = actionLabel != null && onActionClick != null

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.navBarHeight)
            .background(background)
            .bottomBorder(colors.borderSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MedicalMateSpace.s8),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(MedicalMateSize.touchMin), contentAlignment = Alignment.Center) {
                LeadingSlot(leading = leading, onClick = onLeadingClick)
            }
            // 슬롯이 비어 있어도 자리를 지킨다. 마스터가 그렇게 두는 이유가 제목의 가운데를
            // 흔들지 않기 위해서인데, 제목을 따로 가운데에 두는 지금도 좌우 균형에 쓰인다.
            Box(
                modifier = Modifier.heightIn(min = MedicalMateSize.touchMin),
                contentAlignment = Alignment.Center,
            ) {
                if (hasAction) {
                    ActionSlot(
                        label = actionLabel,
                        onClick = onActionClick,
                        enabled = actionEnabled,
                    )
                }
            }
        }
        Text(
            text = title,
            style = MedicalMateTheme.typography.headingS,
            color = colors.fgDefault,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (hasAction) TitleInsetWithAction else TitleInset),
        )
    }
}

/**
 * 우측 텍스트 액션.
 *
 * `Button`이 아니다. 마스터가 이 자리에 `Body/L Strong` 17을 쓰는데 Ghost 버튼의 라벨은
 * 15·13이다. 대신 높이 48과 버튼 역할을 얹는다 — 글자 높이가 26뿐이라 그냥 두면 터치 하한에
 * 못 미친다. 온보딩의 건너뛰기도 같은 이유로 같은 모양이다.
 */
@Composable
private fun ActionSlot(label: String, onClick: () -> Unit, enabled: Boolean) {
    val colors = MedicalMateTheme.colors

    Box(
        modifier =
        Modifier
            .heightIn(min = MedicalMateSize.touchMin)
            .clip(MedicalMateRadius.full)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = MedicalMateSpace.s8, end = MedicalMateSpace.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = if (enabled) colors.fgPrimary else colors.fgMuted,
            maxLines = 1,
        )
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
        // 마스터의 슬롯이 48이고 그 안의 아이콘이 24다. M(40/20)으로 두면 아이콘이 작고
        // 왼쪽 끝이 화면에서 22에 와서 20과 어긋난다.
        MedicalMateIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = stringResource(descriptionRes),
            size = MedicalMateIconButtonSize.L,
        )
    }
}

/**
 * 하단 경계.
 *
 * Nav Bar v2가 더한 선이다. 스크롤할 때 크롬과 콘텐츠의 경계를 잡아준다. Glass 표면에서는
 * 면이 반투명해서 선이 없으면 밑의 글이 바 안으로 흘러 들어온 것처럼 보인다.
 *
 * `Modifier.border`는 네 변을 다 두르므로 아래만 그린다.
 */
private fun Modifier.bottomBorder(color: Color): Modifier = this.drawBehind {
    val stroke = BottomBorderWidth.toPx()
    val y = size.height - stroke / 2f
    drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
}

private val BottomBorderWidth = 1.dp

/** 제목 좌우로 비워 두는 폭. 바깥 8 + 슬롯 48 + 간격 4다. */
private val TitleInset = 60.dp

/** 텍스트 액션이 있을 때. 마스터가 88로 더 비운다. */
private val TitleInsetWithAction = 88.dp
