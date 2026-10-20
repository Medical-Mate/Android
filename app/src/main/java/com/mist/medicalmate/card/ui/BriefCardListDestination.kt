package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.LocalVoiceTargets
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** 와이어프레임 1j-4. 홈의 "전체 보기"에서 밀려 들어온다. */
@Serializable
internal data object BriefCardListDestination

/**
 * [onResumeClick]은 카드 없는 문답 줄을 눌렀을 때다(#338). 홈의 이어서 하기와 같은 곳으로 간다. [tempSaved]가 true면
 * 질문 고르기까지 마친 임시 저장이다.
 */
internal fun NavGraphBuilder.briefCardListDestination(
    onCardClick: (cardId: String) -> Unit,
    onResumeClick: (sessionId: Long, tempSaved: Boolean) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardListDestination> {
        BriefCardListRoute(
            onCardClick = onCardClick,
            onResumeClick = onResumeClick,
            onStartIntakeClick = onStartIntakeClick,
            onExit = onExit,
        )
    }
}

@Composable
private fun BriefCardListRoute(
    onCardClick: (cardId: String) -> Unit,
    onResumeClick: (sessionId: Long, tempSaved: Boolean) -> Unit,
    onStartIntakeClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 돌아올 때마다 다시 읽는다(#338). 이어서 하기로 문답을 마치고 오면 그 줄이 카드가 돼 있다.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }

    // "두 번째", "두통 카드", "9월 4일 카드"로 찾아 연다(#286). 들어오면 무엇을 열지 묻는다.
    // 말로 열었는데 카드가 하나도 없으면 빈 목록을 남기지 않고 돌아가서 없다고 답한다(#320).
    val targets = LocalVoiceTargets.current
    VoiceTargetEffect(CARD_LIST_SUGGESTIONS) { text ->
        if (targets?.handingOff == true && viewModel.awaitEmpty()) {
            onExit()
            VoiceReply.Done(R.string.voice_cards_empty, question = VoiceText(R.string.voice_cards_ask_intake))
        } else {
            viewModel.onVoice(text, LocalDate.now()) { id -> onCardClick(id) }
        }
    }
    val prompt = rememberVoicePrompt()
    // 고를 카드가 있을 때만 무엇을 열지 묻는다.
    LaunchedEffect(Unit) {
        if (!viewModel.awaitEmpty()) prompt(VoicePrompt(question = VoiceText(R.string.voice_card_ask)))
    }

    BriefCardListScreen(
        state = state,
        callbacks =
        BriefCardListCallbacks(
            onBackClick = onExit,
            onCardClick = { item ->
                val session = intakeSessionOf(item.id)
                if (session != null) {
                    onResumeClick(session, item.status == RecordItem.Status.TEMP_SAVED)
                } else {
                    onCardClick(item.id)
                }
            },
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
