package com.mist.medicalmate.card.ui

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
 * 와이어프레임 1e-1. 문답을 마치거나 홈·기록에서 카드를 눌러 들어온다.
 *
 * [cardId]를 라우트에 담는다. 어느 카드를 여는지가 목적지의 일부이고, 화면이 다시
 * 만들어질 때 살아 있어야 한다. 서버 연동 전에는 쓰이지 않고 픽스처가 나온다.
 */
@Serializable
internal data class BriefCardDestination(val cardId: String)

/** 와이어프레임 1f-1. 폰을 의사에게 건네는 화면. */
@Serializable
internal data class HandoffDestination(val cardId: String)

internal fun NavGraphBuilder.briefCardDestination(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHandoff: (String) -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardDestination> {
        BriefCardRoute(onSaved = onSaved, onDeleted = onDeleted, onHandoff = onHandoff, onExit = onExit)
    }
}

internal fun NavGraphBuilder.handoffDestination(onDone: () -> Unit) {
    composable<HandoffDestination> {
        HandoffRoute(onDone = onDone)
    }
}

/**
 * 상태 있는 진입점.
 *
 * 편집 모드에서는 하단에 저장하기가 없다. 그 자리가 `브리핑 카드 삭제`이고, 사본을 카드에
 * 옮기는 것은 Nav 우측 `확인`이 한다. 그래서 저장하기는 읽는 중에만 나오고 항상 화면을
 * 나간다. 나가는 판단을 여기서 하는 이유는 목적지 이동이 그래프의 일이기 때문이다.
 *
 * 삭제는 대화상자의 `삭제`를 누른 뒤 화면을 나간다. 지운 카드의 화면에 남을 수 없다.
 */
@Composable
private fun BriefCardRoute(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHandoff: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val content = state as? BriefCardUiState.Content

    BriefCardScreen(
        state = state,
        callbacks =
        BriefCardCallbacks(
            onBackClick = onExit,
            onEditClick = viewModel::onEditClick,
            onEditDoneClick = viewModel::onEditDoneClick,
            onCancelClick = viewModel::onCancelClick,
            edit = viewModel.editActions,
            onSaveClick = onSaved,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = {
                viewModel.onDeleteConfirm()
                onDeleted()
            },
            onHandoffClick = { content?.card?.id?.let(onHandoff) },
            onRetryClick = viewModel::load,
        ),
        modifier = modifier,
    )
}

/**
 * 진료실 화면의 진입점.
 *
 * 카드를 다시 불러온다. 브리핑 카드 화면과 목적지가 달라 ViewModel도 다른 것을 쓴다.
 * 서버 연동에서는 같은 카드를 두 번 부르지 않도록 저장된 카드를 넘겨받는 편이 낫다.
 */
@Composable
private fun HandoffRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val card = (state as? BriefCardUiState.Content)?.card ?: return

    HandoffScreen(card = card, onCloseClick = onDone, onDoneClick = onDone, modifier = modifier)
}
