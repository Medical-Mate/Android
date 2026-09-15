package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Button`의 `Type` variant.
 *
 * 브랜드 채움([PRIMARY])은 실제 행동에만 쓴다. 버튼 두 개를 나란히 둘 때는 왼쪽에
 * [OUTLINE], 오른쪽에 [PRIMARY]를 둔다.
 *
 * [DANGER]는 채움이 아니라 danger 면과 danger 글자 조합이다. 문서의 원시 팔레트가 상태색의 기본
 * 조합을 50 배경과 700 텍스트로 정했고, 채움에 쓸 red/500급 시맨틱 토큰이 없다. 파괴
 * 동작의 최종 확인은 Dialog가 담당하므로 버튼 자체를 더 세게 만들 이유도 적다.
 */
enum class MedicalMateButtonType {
    PRIMARY,
    TONAL,
    OUTLINE,
    GHOST,
    DANGER,
}

/**
 * DESIGN.md의 `Button`의 `Size` variant. 높이 56/48/40, radius 16/14/12다.
 *
 * 라벨은 [L]이 `Label/L`, [M]과 [S]가 `Label/M`이다. 문서의 타이포 규칙이 `Label/L`을 높이 56
 * 버튼 라벨, `Label/M`을 작은 버튼으로 규정한다.
 */
enum class MedicalMateButtonSize {
    L,
    M,
    S,
}

/**
 * DESIGN.md의 `Button`.
 *
 * Figma의 `State=Default/Pressed/Disabled`를 파라미터로 받지 않는다. Compose가 [enabled]와
 * press indication으로 이미 다루고, 상태를 따로 받으면 [enabled]와 중복돼 어긋날 수 있다.
 * 매핑은 이렇다. Default는 기본, Disabled는 `enabled = false`,
 * Pressed는 `interactionSource`가 누름을 알려줄 때다. Primary만 누름에
 * `bg/primary-pressed` 토큰이 있어 색을 바꾸고, 나머지는 ripple로 표현한다.
 *
 * 아이콘은 라벨이 의미를 가지므로 `contentDescription`을 주지 않는다. 라벨 없이 아이콘만
 * 쓰는 자리는 [MedicalMateIconButton]이다.
 */
@Composable
fun MedicalMateButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    type: MedicalMateButtonType = MedicalMateButtonType.PRIMARY,
    size: MedicalMateButtonSize = MedicalMateButtonSize.L,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val colors = buttonColors(type = type, enabled = enabled, pressed = pressed)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = size.shape,
        color = colors.container,
        contentColor = colors.content,
        border = colors.border?.let { BorderStroke(width = 1.dp, color = it) },
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = size.height),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MedicalMateSpace.s20),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { ButtonIcon(it, size) }
            Text(text = label, style = size.labelStyle)
            trailingIcon?.let { ButtonIcon(it, size) }
        }
    }
}

@Composable
private fun ButtonIcon(@DrawableRes icon: Int, size: MedicalMateButtonSize) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(size.iconSize),
    )
}

private val MedicalMateButtonSize.height: Dp
    get() = when (this) {
        MedicalMateButtonSize.L -> MedicalMateSize.controlLg
        MedicalMateButtonSize.M -> MedicalMateSize.controlMd
        MedicalMateButtonSize.S -> MedicalMateSize.controlSm
    }

private val MedicalMateButtonSize.shape: RoundedCornerShape
    get() = when (this) {
        MedicalMateButtonSize.L -> MedicalMateRadius.md
        MedicalMateButtonSize.M -> MedicalMateRadius.buttonM
        MedicalMateButtonSize.S -> MedicalMateRadius.sm
    }

/** 버튼 안 아이콘은 인라인이라 20, S에서는 좁은 자리라 18이다(DESIGN.md의 아이콘 규격). */
private val MedicalMateButtonSize.iconSize: Dp
    get() = when (this) {
        MedicalMateButtonSize.S -> MedicalMateSize.iconSm
        else -> MedicalMateSize.iconMd
    }

private val MedicalMateButtonSize.labelStyle: TextStyle
    @Composable get() = when (this) {
        MedicalMateButtonSize.L -> MedicalMateTheme.typography.labelL
        else -> MedicalMateTheme.typography.labelM
    }

@Composable
private fun buttonColors(type: MedicalMateButtonType, enabled: Boolean, pressed: Boolean): ButtonColorsSpec {
    val colors = MedicalMateTheme.colors
    if (!enabled) {
        val filled = type == MedicalMateButtonType.PRIMARY || type == MedicalMateButtonType.TONAL
        return ButtonColorsSpec(
            container = if (filled) colors.bgSubtle else Color.Transparent,
            content = colors.fgDisabled,
            border = if (type == MedicalMateButtonType.OUTLINE) colors.borderSubtle else null,
        )
    }
    return when (type) {
        MedicalMateButtonType.PRIMARY ->
            ButtonColorsSpec(
                container = if (pressed) colors.bgPrimaryPressed else colors.bgPrimary,
                content = colors.fgOnPrimary,
                border = null,
            )

        MedicalMateButtonType.TONAL ->
            ButtonColorsSpec(colors.bgPrimarySubtle, colors.fgPrimary, null)

        MedicalMateButtonType.OUTLINE ->
            ButtonColorsSpec(Color.Transparent, colors.fgDefault, colors.borderDefault)

        // 마스터(`291:670`)의 Ghost는 세 크기 모두 `fg/subtle`이다. 브랜드색으로 두면 링크와
        // 구별되지 않는다 — 문서가 "링크와 버튼은 색이 아니라 면 유무로 갈린다"고 적는데, Ghost는
        // 면이 없어서 색까지 링크와 같으면 둘이 한 가지로 보인다.
        MedicalMateButtonType.GHOST ->
            ButtonColorsSpec(Color.Transparent, colors.fgSubtle, null)

        MedicalMateButtonType.DANGER ->
            ButtonColorsSpec(colors.bgDanger, colors.fgDanger, null)
    }
}

/** 컨테이너, 글자, 경계선 묶음. 버튼 계열이 함께 쓴다. */
internal class ButtonColorsSpec(val container: Color, val content: Color, val border: Color?)
