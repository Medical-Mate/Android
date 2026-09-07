package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 계정 관련 임시 진입점.
 *
 * 와이어프레임에 없는 화면이다. 테스트용으로 계정을 바꿀 수 있게 하려고 홈에 붙였고,
 * 설정 화면이 생기면 그쪽으로 옮긴다.
 *
 * 탈퇴는 되돌릴 수 없어 확인 대화상자를 거친다. 실수로 한 번 눌러서 계정이 사라지면
 * 복구 수단이 없다.
 */
@Composable
internal fun AccountActions(
    enabled: Boolean,
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingWithdraw by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        Text(
            text = stringResource(R.string.account_section),
            style = MedicalMateTheme.typography.labelM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
            OutlinedButton(
                onClick = onLogoutClick,
                enabled = enabled,
                shape = MedicalMateRadius.buttonM,
                modifier =
                Modifier
                    .weight(1f)
                    .height(MedicalMateSize.controlMd),
            ) {
                Text(
                    text = stringResource(R.string.account_logout),
                    style = MedicalMateTheme.typography.labelL,
                )
            }
            OutlinedButton(
                onClick = { confirmingWithdraw = true },
                enabled = enabled,
                shape = MedicalMateRadius.buttonM,
                modifier =
                Modifier
                    .weight(1f)
                    .height(MedicalMateSize.controlMd),
            ) {
                Text(
                    text = stringResource(R.string.account_withdraw),
                    style = MedicalMateTheme.typography.labelL,
                    color = MedicalMateTheme.colors.fgDanger,
                )
            }
        }
    }

    if (confirmingWithdraw) {
        WithdrawConfirmDialog(
            onConfirm = {
                confirmingWithdraw = false
                onWithdrawClick()
            },
            onDismiss = { confirmingWithdraw = false },
        )
    }
}

@Composable
private fun WithdrawConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.account_withdraw_confirm_title)) },
        text = { Text(text = stringResource(R.string.account_withdraw_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.account_withdraw_confirm),
                    color = MedicalMateTheme.colors.fgDanger,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.account_cancel))
            }
        },
    )
}

/** 탈퇴 실패 안내. 계정이 남아 있다는 사실을 분명히 알린다. */
@Composable
internal fun WithdrawFailedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(R.string.account_withdraw_failed)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.account_confirm))
            }
        },
    )
}
