package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowSpec

/*
 * 홈 맨 위 사진 배너(#312). Figma `Home Banner`(`3317:5085`)와 그 안의 `Link / On Image`(`3313:4925`)다.
 *
 * 배너 한 장([MedicalMateHomeBannerPage])과 헤더([MedicalMateHomeBannerHeader])를 나눠 둔다. 장이 넘어가도
 * 헤더(로고 · 알림 · 아바타)는 그대로 있다(문서의 모션 규칙).
 *
 * 사진 위 글자와 아이콘에는 `Shadow/On Image`를 건다. 사진이 밝아도 흰 글자가 읽혀야 한다.
 */

/** 배너 아래 링크. 없으면 `CTA=None`(처음 홈, 높이 334)이다. [page]는 "1 / 5"다. */
class MedicalMateBannerLink(val label: String, val page: String, val onClick: () -> Unit)

/**
 * 배너 한 장. 높이는 `CTA=Link`면 400, `None`이면 334다. 폭은 화면을 채운다.
 *
 * 사진은 칸을 채우고 아래에 맞춘다(마스터의 `object-bottom`). 기기 폭이 360보다 넓으면 위아래가 조금 더
 * 잘린다.
 *
 * [extendTop]만큼 위로 사진을 더 그린다. 홈이 상태바 뒤까지 사진을 잇는 자리다. 글과 링크는 아래에 붙어 있어
 * 자리가 그대로이고, 상태바 아래의 배너는 시안 높이 그대로다.
 */
@Composable
fun MedicalMateHomeBannerPage(
    @DrawableRes image: Int,
    title: String,
    body: String,
    link: MedicalMateBannerLink?,
    modifier: Modifier = Modifier,
    extendTop: Dp = 0.dp,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height((if (link == null) BannerHeightWelcome else BannerHeight) + extendTop)
            .background(MedicalMateTheme.colors.bgInverse),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier =
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = MedicalMateSize.gutter, end = MedicalMateSize.gutter, bottom = ContentBottom),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        ) {
            OnImageText(text = title, style = MedicalMateTheme.typography.displayM)
            OnImageText(text = body, style = MedicalMateTheme.typography.bodyM)
            link?.let { LinkOnImage(it, modifier = Modifier.padding(top = MedicalMateSpace.s8)) }
        }
    }
}

/**
 * 배너 헤더. 높이 56 · 좌우 20. 로고 락업(Mono Light, 마스터의 0.82배) · 알림 `Icon Button` · 아바타.
 *
 * 아바타는 36 원에 `bg/on-dark` 면과 `border/on-dark` 테두리이고, 바깥에서 48을 눌리게 한다.
 */
@Composable
fun MedicalMateHomeBannerHeader(
    userInitial: String,
    notificationLabel: String,
    profileLabel: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(MedicalMateSize.navBarHeight)
            .padding(horizontal = MedicalMateSize.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BannerLockup()
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier =
            Modifier
                .size(MedicalMateSize.touchMin)
                .clickable(role = Role.Button, onClick = onNotificationClick)
                .semantics { contentDescription = notificationLabel },
            contentAlignment = Alignment.Center,
        ) {
            OnImageIcon(icon = MedicalMateIcons.Bell, size = MedicalMateSize.iconLg)
        }
        Box(
            modifier =
            Modifier
                .size(MedicalMateSize.touchMin)
                .clickable(role = Role.Button, onClick = onProfileClick)
                .semantics { contentDescription = profileLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                Modifier
                    .size(AvatarSize)
                    .background(color = MedicalMateTheme.colors.bgOnDark, shape = MedicalMateRadius.full)
                    .border(width = 1.dp, color = MedicalMateTheme.colors.borderOnDark, shape = MedicalMateRadius.full),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userInitial,
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = MedicalMateTheme.colors.fgOnPrimary,
                )
            }
        }
    }
}

/**
 * `Link / On Image`. 위 구분선 `border/on-dark`, 높이 48, 왼쪽 링크(`Body/M Strong` + chevron 18), 오른쪽 장 수
 * (`Label/M`). 글자와 화살표는 흰색이다.
 */
@Composable
private fun LinkOnImage(link: MedicalMateBannerLink, modifier: Modifier = Modifier) {
    val line = MedicalMateTheme.colors.borderOnDark
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(MedicalMateSize.touchMin)
            .drawBehind { drawLine(line, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .padding(top = MedicalMateSpace.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.heightIn(min = MedicalMateSize.touchMin - MedicalMateSpace.s4)
                .clickable(role = Role.Button, onClick = link.onClick),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = link.label,
                style = MedicalMateTheme.typography.bodyMStrong,
                color = MedicalMateTheme.colors.fgOnPrimary,
            )
            Icon(
                painter = painterResource(MedicalMateIcons.ChevronRight),
                contentDescription = null,
                tint = MedicalMateTheme.colors.fgOnPrimary,
                modifier = Modifier.size(MedicalMateSize.iconSm),
            )
        }
        Text(text = link.page, style = MedicalMateTheme.typography.labelM, color = MedicalMateTheme.colors.fgOnInverse)
    }
}

/** Logo Lockup · Mono Light를 0.82배로. 흰 마크 29.5 + 워드마크 Bold 18 · 간격 8.2. 그림자를 건다. */
@Composable
private fun BannerLockup() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LockupGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnImageIcon(icon = MedicalMateLogo.Mark, size = LockupMark, shadow = MedicalMateEffects.onImageTextWide)
        // 워드마크는 Pretendard Bold다. `Display/M`의 글꼴을 그대로 쓰고 크기만 줄인다.
        OnImageText(
            text = stringResource(R.string.app_name),
            style =
            MedicalMateTheme.typography.displayM.copy(
                fontSize = LockupWordmarkSize,
                lineHeight = LockupWordmarkLine,
                letterSpacing = LOCKUP_TRACKING.em,
            ),
        )
    }
}

/**
 * 사진 위 흰 글자. `Shadow/On Image · Text`의 두 겹을 글자를 두 번 겹쳐 그려 낸다 — `TextStyle`의 그림자가
 * 한 겹만 받는다. 아래 겹은 스크린 리더가 읽지 않는다.
 */
@Composable
private fun OnImageText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val color = MedicalMateTheme.colors.fgOnPrimary
    Box(modifier = modifier) {
        Text(
            text = text,
            style = style.copy(color = color, shadow = MedicalMateEffects.onImageTextWide.toTextShadow(density)),
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = text,
            style = style.copy(color = color, shadow = MedicalMateEffects.onImageTextTight.toTextShadow(density)),
        )
    }
}

/**
 * 사진 위 흰 아이콘. 그림자는 같은 아이콘을 그림자 색으로 칠해 내리고 흐린 것이다. 흐림은 API 31부터라 그
 * 아래에서는 내린 그림자만 남는다.
 */
@Composable
private fun OnImageIcon(@DrawableRes icon: Int, size: Dp, shadow: ShadowSpec = MedicalMateEffects.onImageIcon) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = shadow.color,
            modifier = Modifier.size(size).offset(y = shadow.y).blur(shadow.blur / 2),
        )
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MedicalMateTheme.colors.fgOnPrimary,
            modifier = Modifier.size(size),
        )
    }
}

private fun ShadowSpec.toTextShadow(density: Density): Shadow =
    with(density) { Shadow(color = color, offset = Offset(0f, y.toPx()), blurRadius = blur.toPx()) }

/** 배너 높이. `CTA=Link` 400, `CTA=None` 334. */
/** `CTA=Link` 배너 높이. 홈이 상태바 아이콘 색을 언제 바꿀지 이 값으로 잰다. */
val MedicalMateHomeBannerHeight = 400.dp

private val BannerHeight = MedicalMateHomeBannerHeight
private val BannerHeightWelcome = 334.dp

/** 글 묶음 아래 여백 56. 시트가 28 겹쳐 올라오는 자리를 비운다. */
private val ContentBottom = 56.dp

private val AvatarSize = 36.dp

/** 마스터 락업(심볼 36 · 간격 10 · 워드마크 22)의 0.82배. */
private val LockupMark = 29.5.dp
private val LockupGap = 8.2.dp
private val LockupWordmarkSize = 18.sp
private val LockupWordmarkLine = 23.sp

/** 워드마크 자간 -3%. 한글 다섯 자가 심볼보다 넓어지지 않게 조인다(마스터 설명). */
private const val LOCKUP_TRACKING = -0.03f
