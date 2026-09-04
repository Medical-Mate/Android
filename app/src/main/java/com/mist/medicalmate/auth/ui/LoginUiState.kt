package com.mist.medicalmate.auth.ui

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object InProgress : LoginUiState

    /** 카카오 토큰을 받은 상태. 서버 교환은 다음 단계에서 붙인다. */
    data class Authenticated(val kakaoAccessToken: String) : LoginUiState

    data object Failed : LoginUiState
}
