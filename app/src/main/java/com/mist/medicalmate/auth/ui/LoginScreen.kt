package com.mist.medicalmate.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
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
            .padding(horizontal = MedicalMateSize.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.login_title),
            // DESIGN.md 3절이 화면 제목을 Heading/L로 규정한다.
            style = MedicalMateTheme.typography.headingL,
        )
        Spacer(Modifier.height(MedicalMateSpace.s8))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        if (state is LoginUiState.Failed) {
            Text(
                text = stringResource(state.reason.messageRes()),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgDanger,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MedicalMateSpace.s12))
        }

        KakaoLoginButton(
            enabled = !state.isBusy(),
            inProgress = state.isBusy(),
            onClick = onKakaoLoginClick,
        )

        Spacer(Modifier.height(MedicalMateSpace.s16))
        Text(
            text = stringResource(R.string.login_terms),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MedicalMateSpace.s32))
    }
}

/**
 * 카카오 로그인 버튼.
 *
 * 색은 카카오 디자인 가이드가 변경을 금지한다. 컨테이너 #FEE500, 레이블 #191600이다.
 *
 * 컨테이너 규격은 DESIGN.md 8.2의 `Social Login Button`을 따라 높이 56, radius 16이다.
 * 카카오 가이드는 radius 12를 제시하는데, 문서가 소셜 로그인 버튼을 브랜드 가이드
 * 예외로 명시하면서도 컨테이너 규격은 자기 값으로 정해 두었다. 색만 가이드를 따르고
 * 크기는 문서를 따른다. 어긋나면 디자인 트랙과 정리한다.
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
        // 테마 shapes가 바뀌어도 흔들리지 않게 직접 준다.
        shape = MedicalMateRadius.md,
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
            .height(MedicalMateSize.controlLg)
            .semantics { contentDescription = label },
    ) {
        if (inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.height(MedicalMateSize.iconMd),
                color = KakaoLabel,
                strokeWidth = 2.dp,
            )
        } else {
            // DESIGN.md 3절이 높이 56 버튼 라벨을 Label/L로 규정한다.
            Text(text = label, style = MedicalMateTheme.typography.labelL)
        }
    }
}

/** 실패 갈래별 문구. 카피는 디자인 소관이라 서버가 준 메시지를 그대로 쓰지 않는다. */
private fun LoginFailure.messageRes(): Int = when (this) {
    LoginFailure.KAKAO -> R.string.login_failed_kakao
    LoginFailure.NETWORK -> R.string.login_failed_network
    LoginFailure.SERVER -> R.string.login_failed_server
}

@Composable
private fun LoginScreenPreview(state: LoginUiState) {
    MedicalMateTheme {
        LoginScreen(state = state, onKakaoLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenIdlePreview() {
    LoginScreenPreview(LoginUiState.Idle)
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenExchangingPreview() {
    LoginScreenPreview(LoginUiState.ExchangingToken)
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenNetworkFailedPreview() {
    LoginScreenPreview(LoginUiState.Failed(LoginFailure.NETWORK))
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenServerFailedPreview() {
    LoginScreenPreview(LoginUiState.Failed(LoginFailure.SERVER))
}
