package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 와이어프레임 1l·1c·1d·1i. 증상 정리 네 단계가 한 목적지 안에서 넘어간다.
 *
 * 홈의 "증상 정리 시작하기"와 "이어서 하기"가 이 목적지로 들어온다. 이어서 하기는 서버에
 * 저장된 진행 상태를 불러와야 해서 아직 시작과 같게 동작한다(#69).
 */
@Serializable
internal data object IntakeDestination

/**
 * 그래프 등록.
 *
 * [onCompleted]는 네 단계를 마쳤을 때다. 브리핑 카드(1e-1)로 가야 하는데 그 화면이 없어서
 * 호출자가 홈으로 돌려보낸다. [onExit]는 첫 단계에서 뒤로 갈 때다.
 */
internal fun NavGraphBuilder.intakeDestination(onCompleted: () -> Unit, onExit: () -> Unit) {
    composable<IntakeDestination> {
        IntakeRoute(onCompleted = onCompleted, onExit = onExit)
    }
}

/**
 * 상태 있는 진입점.
 *
 * 완료 신호를 [LaunchedEffect]로 한 번만 흘려보낸다. 목적지 스코프라 화면을 떠나면
 * ViewModel도 사라지므로 소비 표시를 따로 두지 않는다.
 */
@Composable
private fun IntakeRoute(
    onCompleted: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    IntakeScreen(
        state = state,
        callbacks =
        IntakeCallbacks(
            onBackClick = { if (state.canGoBack) viewModel.onBack() else onExit() },
            onNextClick = viewModel::onNext,
            onBodyViewChange = viewModel.bodyMap::onViewChange,
            onBodyDotClick = viewModel.bodyMap::onDotClick,
            onBodySideAnchorClick = viewModel.bodyMap::onSideAnchorSelect,
            onBodyPartSelect = viewModel.bodyMap::onPartSelect,
            onBodyAnchorFocus = viewModel.bodyMap::onAnchorFocus,
            onBodyFocusClear = viewModel.bodyMap::onFocusClear,
            onBodyListModeToggle = viewModel.bodyMap::onListModeToggle,
            onDraftChange = viewModel::onDraftChange,
            onSendClick = viewModel::onSend,
            onVoiceClick = { viewModel.onInputModeChange(IntakeInputMode.VOICE) },
            onMicClick = viewModel::onMicClick,
            onTypeInsteadClick = { viewModel.onInputModeChange(IntakeInputMode.TEXT) },
            onSeverityChange = viewModel::onSeverityChange,
            onQuestionDraftChange = viewModel::onQuestionDraftChange,
            onAddQuestionClick = viewModel::onAddQuestion,
            onRemoveQuestionClick = viewModel::onRemoveQuestion,
        ),
        modifier = modifier,
    )
}
