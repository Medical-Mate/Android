package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Search Field`(590:1307). `06 · 추가` 섹션의 신규 3종 가운데 하나다.
 *
 * `State=Empty/Filled` 두 변이를 파라미터로 받지 않는다. [value]가 비었는지로 갈린다.
 * 상태를 따로 받으면 실제 값과 어긋날 수 있다.
 *
 * 채워지면 오른쪽에 지우기가 붙는다. 검색은 고쳐 쓰는 일이 잦아서, 글자를 하나씩 지우게
 * 두면 다시 찾기가 번거롭다.
 *
 * `Text Field`와 달리 테두리가 없다. 검색은 값을 남기는 입력이 아니라 목록을 좁히는
 * 조작이라, 채워져도 면 색만 유지한다.
 */
@Composable
fun MedicalMateSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = MedicalMateRadius.md,
        color = colors.bgSubtle,
        contentColor = colors.fgDefault,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier
                .heightIn(min = MedicalMateSize.controlLg)
                .padding(start = MedicalMateSpace.s20, end = MedicalMateSpace.s4),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(MedicalMateIcons.Search),
                contentDescription = null,
                tint = colors.fgSubtle,
                modifier = Modifier.size(MedicalMateSize.iconMd),
            )
            Query(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                interactionSource = interactionSource,
                onSearch = onSearch,
                modifier = Modifier.weight(1f),
            )
            if (value.isNotEmpty()) {
                MedicalMateIconButton(
                    onClick = { onValueChange("") },
                    icon = MedicalMateIcons.Close,
                    contentDescription = clearContentDescription,
                    style = MedicalMateIconButtonStyle.GHOST,
                )
            }
        }
    }
}

/** 안내 문구를 입력 위에 겹친다. 나란히 두면 글을 적기 시작할 때 자리가 밀린다. */
@Composable
private fun Query(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    interactionSource: MutableInteractionSource,
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MedicalMateTheme.colors

    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MedicalMateTheme.typography.bodyL,
                color = colors.fgSubtle,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MedicalMateTheme.typography.bodyL.copy(color = colors.fgDefault),
            cursorBrush = SolidColor(colors.borderFocus),
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // 키보드의 검색 키. 값이 바뀔 때마다 목록이 좁혀지는 화면에서는 줄 것이 없다.
            keyboardActions =
            onSearch?.let { KeyboardActions(onSearch = { it() }) } ?: KeyboardActions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
