package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.2 `Text Field`.
 *
 * Figma의 `State=Default/Focus/Filled/Error/Disabled`를 파라미터로 받지 않는다. Focus는
 * `interactionSource`가, Filled는 [value]가, Disabled는 [enabled]가, Error는 [errorText]가
 * 각각 결정한다. 상태를 따로 받으면 실제 값과 어긋날 수 있다(문서 10.2의 매핑 지침).
 *
 * Default는 테두리 없는 subtle 면이다. Focus와 Filled는 surface 면에 테두리가 생긴다.
 * 모든 칸에 테두리를 두르면 화면이 선으로 가득 찬다(문서 5절).
 *
 * [errorText]는 무엇이 잘못됐는지가 아니라 어떻게 고치는지를 적는다. 문서 8.2가 "행동
 * 지침을 함께 표시"하라고 하고, 9절은 비활성만으로 필수 행동을 숨기지 말라고 한다.
 */
@Composable
fun MedicalMateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = MedicalMateTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val style =
        fieldStyle(
            enabled = enabled,
            hasError = errorText != null,
            focused = focused,
            filled = value.isNotEmpty(),
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
    ) {
        label?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyS,
                color = if (enabled) colors.fgSubtle else colors.fgDisabled,
            )
        }
        FieldSurface(style = style, minHeight = MedicalMateSize.controlLg) {
            FieldText(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                enabled = enabled,
                singleLine = true,
                interactionSource = interactionSource,
                keyboardType = keyboardType,
            )
        }
        SupportText(errorText = errorText, helperText = helperText)
    }
}

/**
 * DESIGN.md 8.2 `Text Area`.
 *
 * 자유 서술이다. 본문은 `Body/L`이고 내용에 따라 높이가 늘어난다. 최소 120이다.
 *
 * 안내 placeholder는 `fg/subtle`이다. `fg/muted`를 쓰면 안 된다. 문서 9절이 `fg/muted`를
 * 텍스트에 쓰지 말라고 한다.
 *
 * 글자 크기 확대에서 잘리지 않도록 고정 높이를 주지 않는다(문서 3절).
 */
@Composable
fun MedicalMateTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val style =
        fieldStyle(
            enabled = enabled,
            hasError = false,
            focused = focused,
            filled = value.isNotEmpty(),
        )

    FieldSurface(style = style, minHeight = TextAreaMinHeight, modifier = modifier) {
        FieldText(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            enabled = enabled,
            singleLine = false,
            interactionSource = interactionSource,
            keyboardType = KeyboardType.Text,
        )
    }
}

private class FieldStyle(val container: Color, val border: BorderStroke?)

/**
 * 면과 테두리 결정.
 *
 * 상태가 겹칠 때 우선순위가 있다. 비활성이 가장 세고, 그다음이 오류, 포커스, 채워짐
 * 순이다. 오류가 난 칸에 포커스가 있으면 오류를 먼저 보여야 한다.
 */
@Composable
private fun fieldStyle(enabled: Boolean, hasError: Boolean, focused: Boolean, filled: Boolean): FieldStyle {
    val colors = MedicalMateTheme.colors
    return when {
        !enabled -> FieldStyle(colors.bgSubtle, null)
        hasError -> FieldStyle(colors.bgDanger, BorderStroke(FocusBorderWidth, colors.fgDanger))
        focused -> FieldStyle(colors.bgSurface, BorderStroke(FocusBorderWidth, colors.borderFocus))
        filled -> FieldStyle(colors.bgSurface, BorderStroke(BorderWidth, colors.borderDefault))
        else -> FieldStyle(colors.bgSubtle, null)
    }
}

@Composable
private fun FieldSurface(
    style: FieldStyle,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = FieldShape,
        color = style.container,
        border = style.border,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
            Modifier
                .heightIn(min = minHeight)
                .padding(horizontal = MedicalMateSpace.s16, vertical = MedicalMateSpace.s14),
            content = { content() },
        )
    }
}

@Composable
private fun FieldText(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    enabled: Boolean,
    singleLine: Boolean,
    interactionSource: MutableInteractionSource,
    keyboardType: KeyboardType,
) {
    val colors = MedicalMateTheme.colors
    if (value.isEmpty() && placeholder != null) {
        Text(
            text = placeholder,
            style = MedicalMateTheme.typography.bodyL,
            color = colors.fgSubtle,
        )
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle =
        MedicalMateTheme.typography.bodyL.copy(
            color = if (enabled) colors.fgDefault else colors.fgDisabled,
        ),
        cursorBrush = SolidColor(colors.borderFocus),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 오류 문구가 있으면 도움말을 대신한다. 둘을 함께 보여주면 무엇을 고쳐야 할지 흐려진다. */
@Composable
private fun SupportText(errorText: String?, helperText: String?) {
    val text = errorText ?: helperText ?: return
    Text(
        text = text,
        style = MedicalMateTheme.typography.bodyS,
        color =
        if (errorText != null) {
            MedicalMateTheme.colors.fgDanger
        } else {
            MedicalMateTheme.colors.fgSubtle
        },
    )
}

/** 문서 8.2가 지정한 radius 14. Scale에 없는 값이라 Button M과 같은 상수를 쓴다. */
private val FieldShape = MedicalMateRadius.buttonM

private val BorderWidth = 1.dp

/** 포커스 링은 문서 9절이 2px로 정했다. 오류 테두리도 같은 두께로 둔다. */
private val FocusBorderWidth = 2.dp

private val TextAreaMinHeight = 120.dp
