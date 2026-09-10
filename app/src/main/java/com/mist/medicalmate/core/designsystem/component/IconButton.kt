package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md의 `Icon Button`의 `Style` variant. */
enum class MedicalMateIconButtonStyle {
    GHOST,
    TONAL,
    SOLID,
    OUTLINE,
}

/** DESIGN.md의 `Icon Button`의 `Size` variant. 시각 규격 48/40/32, 아이콘 24/20/18이다. */
enum class MedicalMateIconButtonSize {
    L,
    M,
    S,
}

/**
 * DESIGN.md의 `Icon Button`.
 *
 * 뒤로, 닫기, 더보기처럼 의미가 자명한 동작에만 쓴다. 그 외에는 라벨이 있는
 * [MedicalMateButton]을 쓴다.
 *
 * [contentDescription]을 필수로 받는다. 문서의 접근성 기준이 아이콘 단독 버튼에 접근성 이름을
 * 반드시 제공하라고 하고, 기본값을 두면 빠뜨리기 쉽다.
 *
 * 시각 규격이 48보다 작아도 hit area는 48을 유지한다. 문서의 접근성 기준이 `size/touch-min` 48을
 * 기준으로 정리했고, 시각 크기를 그대로 터치 영역으로 쓰면 [MedicalMateIconButtonSize.S]가
 * 32로 내려가 접근성 기준에 못 미친다.
 */
@Composable
fun MedicalMateIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: MedicalMateIconButtonStyle = MedicalMateIconButtonStyle.GHOST,
    size: MedicalMateIconButtonSize = MedicalMateIconButtonSize.L,
    enabled: Boolean = true,
) {
    val colors = iconButtonColors(style = style, enabled = enabled)
    Box(
        modifier =
        modifier.sizeIn(
            minWidth = MedicalMateSize.touchMin,
            minHeight = MedicalMateSize.touchMin,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = MedicalMateRadius.full,
            color = colors.container,
            contentColor = colors.content,
            border = colors.border?.let { BorderStroke(width = 1.dp, color = it) },
            modifier = Modifier.size(size.box),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size.iconSize),
                )
            }
        }
    }
}

private val MedicalMateIconButtonSize.box: Dp
    get() = when (this) {
        MedicalMateIconButtonSize.L -> MedicalMateSize.controlMd
        MedicalMateIconButtonSize.M -> MedicalMateSize.controlSm
        MedicalMateIconButtonSize.S -> 32.dp
    }

private val MedicalMateIconButtonSize.iconSize: Dp
    get() = when (this) {
        MedicalMateIconButtonSize.L -> MedicalMateSize.iconLg
        MedicalMateIconButtonSize.M -> MedicalMateSize.iconMd
        MedicalMateIconButtonSize.S -> MedicalMateSize.iconSm
    }

@Composable
private fun iconButtonColors(style: MedicalMateIconButtonStyle, enabled: Boolean): ButtonColorsSpec {
    val colors = MedicalMateTheme.colors
    if (!enabled) {
        return ButtonColorsSpec(
            container = if (style == MedicalMateIconButtonStyle.SOLID) colors.bgSubtle else Color.Transparent,
            content = colors.fgDisabled,
            border = if (style == MedicalMateIconButtonStyle.OUTLINE) colors.borderSubtle else null,
        )
    }
    return when (style) {
        MedicalMateIconButtonStyle.GHOST ->
            ButtonColorsSpec(Color.Transparent, colors.fgDefault, null)

        MedicalMateIconButtonStyle.TONAL ->
            ButtonColorsSpec(colors.bgPrimarySubtle, colors.fgPrimary, null)

        MedicalMateIconButtonStyle.SOLID ->
            ButtonColorsSpec(colors.bgPrimary, colors.fgOnPrimary, null)

        MedicalMateIconButtonStyle.OUTLINE ->
            ButtonColorsSpec(Color.Transparent, colors.fgDefault, colors.borderDefault)
    }
}
