package com.mist.medicalmate.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateSocialLoginButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateSocialProvider

/**
 * 와이어프레임 1o. Figma `397:1192`을 옮겼다.
 *
 * 상태를 갖지 않아 Preview로 모든 분기를 볼 수 있다.
 *
 * 구조는 위아래 여백이 같은 세 덩어리다. Figma에서 Spacer가 244로 둘 다 같아 `weight(1f)`로
 * 옮겼다. 고정값으로 두면 화면 높이가 다른 기기에서 아래가 잘린다.
 *
 * 왼쪽 정렬이다. 제목이 두 줄이라 가운데 정렬하면 줄 끝이 들쭉날쭉해진다.
 *
 * 카카오만 노출한다. 백엔드의 `User` 식별자가 `kakaoId` 단독이라 다른 제공사는 눌러도
 * 실패한다. `Social Login Stack`을 쓰지 않고 버튼 하나를 직접 두는 이유도 같다.
 *
 * Figma에는 로그인 실패 안내 자리가 없다. 기능상 필요해서 버튼 위에 유지한다.
 */
@Composable
fun LoginScreen(state: LoginUiState, onKakaoLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(horizontal = MedicalMateSize.gutter),
    ) {
        Spacer(Modifier.height(MedicalMateSpace.s24))
        Spacer(Modifier.weight(1f))
        Hero()
        Spacer(Modifier.weight(1f))
        Actions(state = state, onKakaoLoginClick = onKakaoLoginClick)
        Spacer(Modifier.height(MedicalMateSize.safeBottom))
    }
}

/** 로고, 제목, 설명. Figma Hero(`397:1195`) 350x192이고 세 요소 사이 간격이 20이다. */
@Composable
private fun Hero() {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20)) {
        Image(
            painter = painterResource(MedicalMateLogo.Lockup),
            // 워드마크가 앱 이름이라 그대로 읽힌다. 아래 제목은 다른 문장이다.
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(LockupHeight),
        )
        Text(
            text = stringResource(R.string.login_title),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 버튼과 하단 문구. Figma Actions(`397:1206`) 350x116이고 사이 간격이 20이다. */
@Composable
private fun Actions(state: LoginUiState, onKakaoLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20),
    ) {
        if (state is LoginUiState.Failed) {
            Text(
                text = stringResource(state.reason.messageRes()),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgDanger,
            )
        }
        MedicalMateSocialLoginButton(
            provider = MedicalMateSocialProvider.KAKAO,
            label = stringResource(R.string.login_kakao),
            contentDescription = stringResource(R.string.login_kakao_content_description),
            onClick = onKakaoLoginClick,
            enabled = !state.isBusy(),
            inProgress = state.isBusy(),
        )
        Text(
            text = stringResource(R.string.login_disclaimer),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 실패 갈래별 문구. 카피는 디자인 소관이라 서버가 준 메시지를 그대로 쓰지 않는다. */
private fun LoginFailure.messageRes(): Int = when (this) {
    LoginFailure.KAKAO -> R.string.login_failed_kakao
    LoginFailure.NETWORK -> R.string.login_failed_network
    LoginFailure.SERVER -> R.string.login_failed_server
}

/** Figma Logo Lockup 139x36. 폭은 그림 비율이 정한다. */
private val LockupHeight = 36.dp

@Composable
private fun LoginScreenPreview(state: LoginUiState) {
    MedicalMateTheme {
        LoginScreen(state = state, onKakaoLoginClick = {})
    }
}

@MedicalMateScreenPreviews
@Composable
private fun LoginScreenIdlePreview() {
    LoginScreenPreview(LoginUiState.Idle)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenExchangingPreview() {
    LoginScreenPreview(LoginUiState.ExchangingToken)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenNetworkFailedPreview() {
    LoginScreenPreview(LoginUiState.Failed(LoginFailure.NETWORK))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenServerFailedPreview() {
    LoginScreenPreview(LoginUiState.Failed(LoginFailure.SERVER))
}
