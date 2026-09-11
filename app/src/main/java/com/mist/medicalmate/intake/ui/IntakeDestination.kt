package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * 와이어프레임 1l·1c·1d·1i. 증상 정리 네 단계가 한 목적지 안에서 넘어간다.
 *
 * 홈의 "증상 정리 시작하기"와 "이어서 하기"가 이 목적지로 들어온다.
 *
 * @param sessionId 이어서 할 문답. 홈의 `inProgressSession`에서 온다. `null`이면 새로
 *   시작하는 것이고, 부위를 고르고 다음을 누를 때 세션이 만들어진다.
 */
@Serializable
internal data class IntakeDestination(val sessionId: Long? = null)

/**
 * 와이어프레임 1c-5. 문답을 마친 뒤의 갈림길.
 *
 * @param sessionId 방금 마친 문답. 카드를 만들 때 `POST /api/sessions/{id}/card`에 쓴다.
 *   세션 만들기가 실패했으면 `null`이고, 그때는 카드를 만들 수 없다.
 */
@Serializable
internal data class IntakeDoneDestination(val sessionId: Long? = null)

/**
 * 그래프 등록.
 *
 * [onCompleted]는 네 단계를 마쳤을 때다. 정리 완료(1c-5)로 간다. 전에는 브리핑 카드로 바로
 * 갔는데, 시안이 그 사이에 카드와 병원 찾기로 갈리는 화면을 뒀다. [onExit]는 첫 단계에서
 * 뒤로 갈 때다.
 */
internal fun NavGraphBuilder.intakeDestination(onCompleted: (Long?) -> Unit, onExit: () -> Unit) {
    composable<IntakeDestination> { entry ->
        IntakeRoute(
            sessionId = entry.toRoute<IntakeDestination>().sessionId,
            onCompleted = onCompleted,
            onExit = onExit,
        )
    }
}

/**
 * 1c-5 그래프 등록.
 *
 * 상태가 없어서 Route를 두지 않는다. 두 버튼과 뒤로가 전부다.
 */
internal fun NavGraphBuilder.intakeDoneDestination(
    onCardCreated: (String) -> Unit,
    onHospitalClick: () -> Unit,
    onExit: () -> Unit,
) {
    composable<IntakeDoneDestination> { entry ->
        IntakeDoneRoute(
            sessionId = entry.toRoute<IntakeDoneDestination>().sessionId,
            onCardCreated = onCardCreated,
            onHospitalClick = onHospitalClick,
            onExit = onExit,
        )
    }
}

/**
 * 1c-5의 진입점.
 *
 * "브리핑 카드 만들기"를 누르면 문답을 카드로 만든다. 만들어진 카드 id로 카드 화면을 연다.
 * 카드는 `DRAFT`로 만들어지고 확정은 진료실에서 보여줄 때 한다.
 */
@Composable
private fun IntakeDoneRoute(
    sessionId: Long?,
    onCardCreated: (String) -> Unit,
    onHospitalClick: () -> Unit,
    onExit: () -> Unit,
    viewModel: IntakeCardViewModel = hiltViewModel(),
) {
    val createdId by viewModel.createdCardId.collectAsStateWithLifecycle()

    LaunchedEffect(createdId) {
        createdId?.let(onCardCreated)
    }

    IntakeDoneScreen(
        onCardClick = { sessionId?.let(viewModel::create) },
        onHospitalClick = onHospitalClick,
        onBackClick = onExit,
    )
}

/**
 * 상태 있는 진입점.
 *
 * 완료 신호를 [LaunchedEffect]로 한 번만 흘려보낸다. 목적지 스코프라 화면을 떠나면
 * ViewModel도 사라지므로 소비 표시를 따로 두지 않는다.
 */
@Composable
private fun IntakeRoute(
    sessionId: Long?,
    onCompleted: (Long?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 강도의 표시 문구는 문자열 리소스라 ViewModel이 읽을 수 없다. 서버가 그 문구를 함께
    // 받으므로 화면이 풀어서 넘긴다.
    val severityLabel = stringResource(state.severity.labelRes)

    // 목적지 스코프라 화면을 떠나면 ViewModel도 사라진다. 다시 들어오면 다시 불러온다.
    LaunchedEffect(sessionId) {
        if (sessionId != null) viewModel.session.restore(sessionId)
    }

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted(state.sessionId)
    }

    IntakeScreen(
        state = state,
        callbacks =
        IntakeCallbacks(
            onBackClick = { if (state.canGoBack) viewModel.onBack() else onExit() },
            onNextClick = { viewModel.onNext(severityLabel) },
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
