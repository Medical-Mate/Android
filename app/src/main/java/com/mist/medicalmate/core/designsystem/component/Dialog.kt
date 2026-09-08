package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.4 `Dialog`.
 *
 * 되돌릴 수 없는 동작을 확인받는 자리다. 되돌릴 수 있는 동작은 [MedicalMateToast]의
 * action으로 충분하고, 확인 대화상자를 남발하면 사용자가 내용을 읽지 않고 누른다.
 *
 * 취소를 왼쪽 Outline, 실행을 오른쪽 Danger로 둔다(문서 8.4). `AlertDialog`가
 * dismissButton을 왼쪽, confirmButton을 오른쪽에 놓으므로 순서가 그대로 맞는다.
 *
 * [onDismissRequest]는 바깥을 눌렀을 때도 불린다. 되돌릴 수 없는 선택 중에는 바깥 누름을
 * 막아야 하므로, 그런 자리에서는 호출자가 빈 람다를 주고 취소 버튼만 남긴다(문서 8.5의
 * Overlay Scrim 규칙과 같은 이유다).
 */
@Composable
fun MedicalMateDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MedicalMateRadius.xl,
        containerColor = MedicalMateTheme.colors.bgSurface,
        titleContentColor = MedicalMateTheme.colors.fgDefault,
        textContentColor = MedicalMateTheme.colors.fgSubtle,
        title = {
            Text(text = title, style = MedicalMateTheme.typography.headingM)
        },
        text = {
            Text(text = message, style = MedicalMateTheme.typography.bodyM)
        },
        confirmButton = {
            MedicalMateButton(
                onClick = onConfirm,
                label = confirmLabel,
                type = MedicalMateButtonType.DANGER,
                size = MedicalMateButtonSize.M,
            )
        },
        dismissButton = {
            MedicalMateButton(
                onClick = onDismissRequest,
                label = dismissLabel,
                type = MedicalMateButtonType.OUTLINE,
                size = MedicalMateButtonSize.M,
            )
        },
        modifier = modifier.width(DialogWidth),
    )
}

/** 문서 8.4가 지정한 폭. 높이는 내용에 따라 늘어난다. */
private val DialogWidth = 320.dp
