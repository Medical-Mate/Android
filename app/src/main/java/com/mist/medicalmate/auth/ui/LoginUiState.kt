package com.mist.medicalmate.auth.ui

sealed interface LoginUiState {
    data object Idle : LoginUiState

    /** 카카오 SDK 로그인 진행 중. */
    data object InProgress : LoginUiState

    /** 카카오 토큰을 서버 JWT로 교환하는 중. */
    data object ExchangingToken : LoginUiState

    data class Authenticated(val onboardingRequired: Boolean) : LoginUiState

    data class Failed(val reason: LoginFailure) : LoginUiState
}

/** 카카오 로그인이든 서버 교환이든 진행 중이면 버튼을 잠근다. */
fun LoginUiState.isBusy(): Boolean = this == LoginUiState.InProgress || this == LoginUiState.ExchangingToken

/**
 * 실패 원인. 화면 문구를 나누기 위해서만 존재한다.
 *
 * 네트워크가 닿지 않은 것과 서버가 거절한 것은 사용자가 취할 행동이 다르다.
 * 문구는 `strings.xml`에 두고 여기서는 갈래만 구분한다.
 */
enum class LoginFailure {
    /** 카카오 로그인 단계에서 실패. 키 해시 미등록도 여기로 온다. */
    KAKAO,

    /** 서버에 닿지 못함. */
    NETWORK,

    /** 서버가 거절함. 카카오 토큰 검증 실패나 앱 ID 불일치가 여기로 온다. */
    SERVER,
}
