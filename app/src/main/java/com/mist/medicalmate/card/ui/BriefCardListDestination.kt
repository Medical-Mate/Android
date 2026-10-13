package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** 와이어프레임 1j-4. 홈의 "전체 보기"에서 밀려 들어온다. */
@Serializable
internal data object BriefCardListDestination

internal fun NavGraphBuilder.briefCardListDestination(
    onCardClick: (cardId: String) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardListDestination> {
        BriefCardListRoute(
            onCardClick = onCardClick,
            onStartIntakeClick = onStartIntakeClick,
            onExit = onExit,
        )
    }
}

@Composable
private fun BriefCardListRoute(
    onCardClick: (cardId: String) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    // "두 번째", "두통 카드", "9월 4일 카드"로 찾아 연다(#286). 들어오면 무엇을 열지 묻는다.
    VoiceTargetEffect { text -> viewModel.onVoice(text, LocalDate.now()) { id -> onCardClick(id) } }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) { prompt(VoicePrompt(question = VoiceText(R.string.voice_card_ask))) }

    BriefCardListScreen(
        state = state,
        callbacks =
        BriefCardListCallbacks(
            onBackClick = onExit,
            onCardClick = { item -> onCardClick(item.id) },
            onStartIntakeClick = onStartIntakeClick,
            onEditStart = viewModel::onEditStart,
            onEditCancel = viewModel::onEditCancel,
            onSelectChange = viewModel::onSelectChange,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onRetryClick = viewModel::load,
        ),
        modifier = modifier,
    )
}
