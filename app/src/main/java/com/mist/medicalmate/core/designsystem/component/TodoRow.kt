package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Todo Row`(`1129:9197`) 320x54.
 *
 * 진료 전 할 일 한 줄이다. 체크박스와 할 일, 그리고 편집 모드의 삭제 ×로 이뤄진다.
 *
 * 체크 부분은 [MedicalMateCheckbox]를 그대로 쓴다. 마스터의 상자 24 · 반경 8 · 간격 12 ·
 * 행 높이 54가 그 컴포넌트와 같은 값이고, 행 전체를 hit area로 삼는 규칙도 같다. 여기서
 * 다시 그리면 두 곳이 갈린다.
 *
 * [delete]를 주면 오른쪽에 ×가 붙는다. 문서가 이 ×를 화면 단위 액션으로 봐서 L 크기(48
 * 상자 · 24 아이콘)를 쓴다. 목록 안 항목이라 S로 보일 수 있지만, 문서의 삭제 × 배치
 * 기준이 진료 전 할 일을 L로 못박았다.
 *
 * 삭제에 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고, 편집 모드를 벗어나기 전이면
 * 취소가 실행 취소를 대신한다.
 *
 * [onLabelChange]를 주면 라벨이 그 자리에서 받는 입력이 된다. 시안
 * `1092:3935`~`1185:12839`가 할 일 추가를 빈 행 → 적는 중 → 완료된 행으로 그린다. 새 입력
 * 필드를 띄우는 대신 목록에 빈 항목이 하나 생기는 것이 문서의 추가 방식이고,
 * [MedicalMateAddRow]의 설명과 같은 규칙이다. 값 아래 `border/strong` 밑줄은
 * `KV Row`의 편집 상태와 같은 표시다.
 *
 * 적기로 들어가면 커서를 그 줄에 놓고 키보드를 띄운다. 목록에 빈 줄만 생기고 다시 눌러야
 * 적을 수 있으면 "그 자리에서 적는다"가 아니다. 끝내는 것은 [onEditDone]이 받는다. 키보드의
 * 완료를 누르거나 다른 곳으로 초점이 넘어가면 부른다.
 */
@Composable
fun MedicalMateTodoRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    delete: MedicalMateRowDelete? = null,
    onLabelChange: ((String) -> Unit)? = null,
    onEditDone: (() -> Unit)? = null,
    labelPlaceholder: String = "",
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onLabelChange == null) {
            MedicalMateCheckbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = label,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        } else {
            EditingLabel(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = label,
                placeholder = labelPlaceholder,
                onLabelChange = onLabelChange,
                onEditDone = onEditDone ?: {},
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        if (delete != null) {
            MedicalMateIconButton(
                onClick = delete.onClick,
                icon = MedicalMateIcons.Close,
                contentDescription = delete.contentDescription,
                style = MedicalMateIconButtonStyle.GHOST,
                size = MedicalMateIconButtonSize.L,
                enabled = enabled,
            )
        }
    }
}

/**
 * 적는 중인 할 일.
 *
 * 체크박스는 상자만 두고 글자는 오른쪽 입력이 맡는다. 라벨을 주면 그 라벨이 행 전체의
 * hit area를 먹어서 글자를 눌러도 커서가 아니라 체크가 바뀐다.
 *
 * 입력을 감싼 상자가 행 높이를 채운다. 밑줄은 글자 아래에만 그려서 시안(`1092:4042`)의
 * `Value Wrap` 29와 같은 자리에 오고, 누를 수 있는 높이는 48을 지킨다. 밑줄을 상자
 * 바닥에 그리면 둘 중 하나를 포기해야 한다.
 */
@Composable
private fun EditingLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    placeholder: String,
    onLabelChange: (String) -> Unit,
    onEditDone: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = null,
            enabled = enabled,
        )
        Box(
            modifier =
            Modifier
                .weight(1f)
                .heightIn(min = MedicalMateSize.touchMin),
            contentAlignment = Alignment.CenterStart,
        ) {
            UnderlinedField(
                label = label,
                placeholder = placeholder,
                onLabelChange = onLabelChange,
                onEditDone = onEditDone,
                enabled = enabled,
            )
        }
    }
}

/**
 * 밑줄 있는 한 줄 입력.
 *
 * 밑줄은 글자 아래에만 그린다. 바깥 상자가 행 높이를 채워서 누를 수 있는 높이는 48을
 * 지키고, 선은 시안(`1092:4042`)의 `Value Wrap` 29와 같은 자리에 온다.
 */
@Composable
private fun UnderlinedField(
    label: String,
    placeholder: String,
    onLabelChange: (String) -> Unit,
    onEditDone: () -> Unit,
    enabled: Boolean,
) {
    val colors = MedicalMateTheme.colors
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var everFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - UnderlineWidth.toPx() / 2
                drawLine(
                    color = colors.borderStrong,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = UnderlineWidth.toPx(),
                )
            }
            .padding(bottom = MedicalMateSpace.s2),
    ) {
        if (label.isEmpty()) {
            Text(
                text = placeholder,
                style = MedicalMateTheme.typography.bodyL,
                color = colors.fgSubtle,
            )
        }
        BasicTextField(
            value = label,
            onValueChange = onLabelChange,
            enabled = enabled,
            textStyle = MedicalMateTheme.typography.bodyL.copy(color = colors.fgDefault),
            cursorBrush = SolidColor(colors.borderFocus),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                // 처음 한 번은 초점이 오기 전이라 잃은 것이 아니다. 받은 적이 있는
                // 줄만 끝난 것으로 본다.
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        everFocused = true
                    } else if (everFocused) {
                        onEditDone()
                    }
                },
        )
    }
}

/** `KV Row`의 편집 밑줄과 같은 1이다. */
private val UnderlineWidth = 1.dp
