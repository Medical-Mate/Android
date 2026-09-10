package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateGlass
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Tab Bar`의 목적지.
 *
 * 세 개다. Figma 마스터의 variant가 셋이고, 와이어프레임에서 Tab Bar가 붙은 화면도
 * 홈(1n), 기록(1j), 캘린더(1r) 셋뿐이다. 문서의 컴포넌트 규격이 `내 정보`를 넣어 넷으로 적은 것은
 * 문서 오류다. `user`·`user-filled` 아이콘은 아이콘 세트에 있지만 탭에 쓰이지 않는다.
 *
 * **선언 순서가 화면 순서다.** `캘린더 · 홈 · 기록`이고 홈이 가운데다. Figma 마스터
 * `319:1026`의 variant 셋과 Tab Bar가 붙은 화면 다섯(1n-1 · 1n-2 · 1j-1 · 1j-2 · 1r-1)이
 * 모두 이 순서라 순서를 바꾸려면 양쪽을 함께 봐야 한다.
 */
enum class MedicalMateTab(@StringRes val labelRes: Int, @DrawableRes val icon: Int, @DrawableRes val activeIcon: Int) {
    CALENDAR(R.string.tab_calendar, MedicalMateIcons.Calendar, MedicalMateIcons.CalendarFilled),
    HOME(R.string.tab_home, MedicalMateIcons.Home, MedicalMateIcons.HomeFilled),
    RECORD(R.string.tab_record, MedicalMateIcons.Note, MedicalMateIcons.NoteFilled),
}

/**
 * DESIGN.md의 `Tab Bar`.
 *
 * 1Depth에서만 노출한다. 문답이나 카드 작성처럼 흐름 안에 들어간 화면에서는 감춘다.
 *
 * 활성은 세 가지로 함께 표시한다. 채움 아이콘, 브랜드 색, 라벨 굵기다. 색만 바꾸면
 * 색각 이상에서 어느 탭에 있는지 알 수 없다(문서의 D11).
 *
 * 높이 79는 위 여백 8 + 탭 46 + 아래 안전 여백 24 + 경계선 1이다. 문서 3.0이
 * `layout/tabbar-h`를 79로 확정했다.
 *
 * 아래 24는 Figma 컴포넌트 안쪽 여백이다. 기기의 홈 인디케이터 inset과 중복 적용하지
 * 않도록 호출자가 `Scaffold`나 `WindowInsets` 처리를 함께 정한다(문서의 치수 토큰).
 */
@Composable
fun MedicalMateTabBar(
    selected: MedicalMateTab,
    onSelect: (MedicalMateTab) -> Unit,
    modifier: Modifier = Modifier,
    surface: MedicalMateSurfaceStyle = MedicalMateSurfaceStyle.OPAQUE,
) {
    val colors = MedicalMateTheme.colors
    val background =
        when (surface) {
            MedicalMateSurfaceStyle.OPAQUE -> colors.bgSurface
            MedicalMateSurfaceStyle.GLASS -> colors.bgSurface.copy(alpha = MedicalMateGlass.TAB_BAR_ALPHA)
        }

    Column(modifier = modifier.fillMaxWidth().background(background)) {
        // 문서의 고도 규칙이 Glass에 테두리를 두지 말라고 하지만, Tab Bar는 콘텐츠와 맞닿는
        // 경계라 마스터에 1px 선이 있다. 목록 구분선과 같은 성격이다.
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(HairlineHeight)
                .background(colors.borderSubtle),
            content = {},
        )
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TabRowHeight)
                .padding(top = MedicalMateSpace.s8),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicalMateTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                )
            }
        }
        Column(modifier = Modifier.height(MedicalMateSize.safeBottom), content = {})
    }
}

@Composable
private fun TabItem(tab: MedicalMateTab, selected: Boolean, onClick: () -> Unit) {
    val colors = MedicalMateTheme.colors
    val tint = if (selected) colors.fgPrimary else colors.fgMuted
    val label = stringResource(tab.labelRes)

    Column(
        modifier =
        Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = MedicalMateSpace.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
    ) {
        Icon(
            painter = painterResource(if (selected) tab.activeIcon else tab.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MedicalMateSize.iconLg),
        )
        Text(
            text = label,
            style = MedicalMateTheme.typography.labelS,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else null,
        )
    }
}

/** 마스터 실제 높이 79 = 1 + 8 + 46 + 24. */
private val TabRowHeight = 54.dp

private val HairlineHeight = 1.dp
