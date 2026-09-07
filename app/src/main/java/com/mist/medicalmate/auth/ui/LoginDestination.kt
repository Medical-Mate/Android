package com.mist.medicalmate.auth.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 와이어프레임 1o.
 *
 * 라우트 타입과 그래프 등록을 기능 패키지에 둔다. `navigation`이 화면 컴포저블의
 * 파라미터까지 알 필요가 없고, 화면이 늘어도 그래프 파일이 비대해지지 않는다.
 *
 * 인자가 없어 `data object`다. 인자를 받는 화면은 `@Serializable data class`로 두고
 * `composable<T>`의 `toRoute<T>()`로 꺼낸다.
 */
@Serializable
internal data object LoginDestination

internal fun NavGraphBuilder.loginDestination(onAuthenticated: (onboardingRequired: Boolean) -> Unit) {
    composable<LoginDestination> {
        LoginRoute(onAuthenticated = onAuthenticated)
    }
}
