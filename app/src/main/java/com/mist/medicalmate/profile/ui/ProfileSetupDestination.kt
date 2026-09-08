package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 1b-1~1b-3. 세 단계가 한 목적지 안에서 넘어간다. */
@Serializable
internal data object ProfileSetupDestination

/** 와이어프레임 1b-4. 신상정보 등록을 마친 화면. */
@Serializable
internal data object ProfileCompleteDestination

/**
 * 신상정보 입력 등록.
 *
 * [onCompleted]는 마지막 단계에서 완료를 눌렀을 때다. [onExit]는 첫 단계에서 뒤로 갈
 * 때다. 어디로 가는지는 그래프가 정한다.
 */
internal fun NavGraphBuilder.profileSetupDestination(onCompleted: () -> Unit, onExit: () -> Unit) {
    composable<ProfileSetupDestination> {
        ProfileSetupRoute(onCompleted = onCompleted, onExit = onExit)
    }
}

/** 신상정보 완료 등록. [onFinished]는 화면이 제 몫을 다한 뒤다. */
internal fun NavGraphBuilder.profileCompleteDestination(onFinished: () -> Unit) {
    composable<ProfileCompleteDestination> {
        ProfileCompleteScreen(onFinished = onFinished)
    }
}

/**
 * 상태 있는 진입점.
 *
 * 완료 신호를 상태로 들고 있다가 [LaunchedEffect]로 한 번만 흘려보낸다. 목적지 스코프라
 * 화면을 떠나면 ViewModel도 사라지므로 소비 표시를 따로 두지 않는다.
 */
@Composable
private fun ProfileSetupRoute(
    onCompleted: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    ProfileSetupScreen(
        state = state,
        onOptionToggle = viewModel::onOptionToggle,
        onNoteChange = viewModel::onNoteChange,
        onNextClick = viewModel::onNext,
        onBackClick = { if (state.canGoBack) viewModel.onBack() else onExit() },
        modifier = modifier,
    )
}
