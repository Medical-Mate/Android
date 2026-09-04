package com.mist.medicalmate.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.KakaoContainer
import com.mist.medicalmate.core.designsystem.KakaoLabel
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 와이어프레임 1o. 상태를 갖지 않아 Preview로 모든 분기를 볼 수 있다.
 *
 * 와이어프레임에는 Apple·전화번호 버튼도 있지만 백엔드가 카카오만 지원한다.
 * 동작하지 않는 버튼을 그려두지 않기로 하고 카카오 하나만 뒀다.
 */
@Composable
fun LoginScreen(state: LoginUiState, onKakaoLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        if (state == LoginUiState.Failed) {
            Text(
                text = stringResource(R.string.login_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }

        KakaoLoginButton(
            enabled = state != LoginUiState.InProgress,
            inProgress = state == LoginUiState.InProgress,
            onClick = onKakaoLoginClick,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.login_terms),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * 카카오 디자인 가이드를 따른다. 컨테이너 #FEE500, 레이블 검정 85%, radius 12.
 * 색상과 문구를 임의로 바꿀 수 없다.
 *
 * 말풍선 심볼은 아직 없다. 콘솔의 도구 > 리소스 다운로드에서 받아 레이블 왼쪽에 넣어야
 * 가이드를 완전히 충족한다.
 */
@Composable
private fun KakaoLoginButton(enabled: Boolean, inProgress: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.login_kakao)
    Button(
        onClick = onClick,
        enabled = enabled,
        // 가이드가 radius 12를 지정한다. 테마 shapes가 바뀌어도 흔들리지 않게 직접 준다.
        shape = RoundedCornerShape(12.dp),
        colors =
        ButtonDefaults.buttonColors(
            containerColor = KakaoContainer,
            contentColor = KakaoLabel,
            disabledContainerColor = KakaoContainer,
            disabledContentColor = KakaoLabel,
        ),
        modifier =
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = label },
    ) {
        if (inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = KakaoLabel,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenIdlePreview() {
    MedicalMateTheme {
        LoginScreen(state = LoginUiState.Idle, onKakaoLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenInProgressPreview() {
    MedicalMateTheme {
        LoginScreen(state = LoginUiState.InProgress, onKakaoLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenFailedPreview() {
    MedicalMateTheme {
        LoginScreen(state = LoginUiState.Failed, onKakaoLoginClick = {})
    }
}
