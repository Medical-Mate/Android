package com.mist.medicalmate.profile.ui

import androidx.compose.material3.AlertDialog
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
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateSettingNavigationRow

/**
 * 계정 카드. 10-15 시안 1s-1의 `계정` 구역이다(#316). 로그아웃과 회원탈퇴가 `Setting Row` Navigation 두 줄이다.
 *
 * 탈퇴는 되돌릴 수 없어 확인 대화상자를 거친다. 실수로 한 번 눌러서 계정이 사라지면
 * 복구 수단이 없다.
 */
@Composable
internal fun AccountActions(accountActions: AccountActionCallbacks, modifier: Modifier = Modifier) {
    var confirmingWithdraw by remember { mutableStateOf(false) }

    MedicalMateSettingCard(modifier = modifier) {
        MedicalMateSettingNavigationRow(
            title = stringResource(R.string.account_logout),
            onClick = accountActions.onLogoutClick,
            enabled = accountActions.enabled,
        )
        MedicalMateSettingNavigationRow(
            title = stringResource(R.string.account_withdraw),
            onClick = { confirmingWithdraw = true },
            enabled = accountActions.enabled,
        )
    }

    if (confirmingWithdraw) {
        WithdrawConfirmDialog(
            onConfirm = {
                confirmingWithdraw = false
                accountActions.onWithdrawClick()
            },
            onDismiss = { confirmingWithdraw = false },
        )
    }
}

/**
 * 계정 동작의 진입점.
 *
 * [enabled]는 로그아웃이나 탈퇴가 진행 중일 때 꺼진다. 두 번 눌러 요청이 두 번 나가면
 * 두 번째가 이미 없는 토큰으로 나간다.
 */
data class AccountActionCallbacks(
    val enabled: Boolean = true,
    val onLogoutClick: () -> Unit = {},
    val onWithdrawClick: () -> Unit = {},
)

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
fun WithdrawFailedDialog(onDismiss: () -> Unit) {
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
