package com.mist.medicalmate.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType

/**
 * 계정에서 나가는 두 가지. Figma 1s-1의 하단.
 *
 * 홈 화면에 임시로 붙어 있던 것을 여기로 옮겼다. 설정 화면이 생기면 옮기기로 한 것이고
 * 1s-1이 그 자리다.
 *
 * **회원탈퇴는 시안에 없다.** 1s-1 하단에는 로그아웃만 있다. 그렇다고 홈에 남겨 두면
 * 로그아웃과 탈퇴가 다른 화면에 흩어지므로 로그아웃 아래에 텍스트로 뒀다. 자리를 정하면
 * 그대로 옮긴다(#85).
 *
 * 탈퇴는 되돌릴 수 없어 확인 대화상자를 거친다. 실수로 한 번 눌러서 계정이 사라지면
 * 복구 수단이 없다.
 */
@Composable
internal fun AccountActions(accountActions: AccountActionCallbacks, modifier: Modifier = Modifier) {
    var confirmingWithdraw by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
    ) {
        MedicalMateButton(
            onClick = accountActions.onLogoutClick,
            label = stringResource(R.string.account_logout),
            type = MedicalMateButtonType.OUTLINE,
            enabled = accountActions.enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        MedicalMateButton(
            onClick = { confirmingWithdraw = true },
            label = stringResource(R.string.account_withdraw),
            type = MedicalMateButtonType.GHOST,
            enabled = accountActions.enabled,
            modifier = Modifier.fillMaxWidth(),
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
