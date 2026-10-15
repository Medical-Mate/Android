package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.speech.rememberMicPermission
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import kotlinx.serialization.Serializable

/**
 * 증상 정리 세 단계(부위 · 1d · 1c)가 한 목적지 안에서 넘어간다(#310).
 *
 * 홈의 "증상 정리 시작하기"와 "이어서 하기"가 이 목적지로 들어온다.
 *
 * @param sessionId 이어서 할 문답. 홈의 `inProgressSession`에서 온다. `null`이면 새로
 *   시작하는 것이고, 강도를 고르고 다음을 누를 때 세션이 만들어진다.
 * @param spoken "오른쪽 어깨가 아파"처럼 증상 정리를 연 말(#284). 그 속의 부위를 바로 고른다.
 */
@Serializable
internal data class IntakeDestination(val sessionId: Long? = null, val spoken: String? = null)

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
 * [onCompleted]는 세 단계를 마쳤을 때다. 정리 완료(1c-5)로 간다. 전에는 브리핑 카드로 바로
 * 갔는데, 시안이 그 사이에 카드와 병원 찾기로 갈리는 화면을 뒀다. [onExit]는 첫 단계에서
 * 뒤로 갈 때다.
 */
internal fun NavGraphBuilder.intakeDestination(onCompleted: (Long?) -> Unit, onExit: () -> Unit) {
    composable<IntakeDestination> { entry ->
        val route = entry.toRoute<IntakeDestination>()
        IntakeRoute(
            sessionId = route.sessionId,
            spoken = route.spoken,
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
    onCardRequested: (sessionId: Long) -> Unit,
    onHospitalClick: (sessionId: Long) -> Unit,
    onExit: () -> Unit,
) {
    composable<IntakeDoneDestination> { entry ->
        // 문답 없이 이 화면에 올 길이 없다. 세션이 없으면 만들 카드도 고를 병원도 없어서
        // 두 버튼이 아무 일도 하지 않는다.
        val sessionId = entry.toRoute<IntakeDoneDestination>().sessionId ?: return@composable
        IntakeDoneVoice(onCard = { onCardRequested(sessionId) }, onHospital = { onHospitalClick(sessionId) })
        IntakeDoneScreen(
            onCardClick = { onCardRequested(sessionId) },
            onHospitalClick = { onHospitalClick(sessionId) },
            onBackClick = onExit,
        )
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
    sessionId: Long?,
    spoken: String?,
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

    IntakeVoice(state = state, spoken = spoken.takeIf { sessionId == null }, viewModel = viewModel)

    // 이 기기에서 음성을 쓸 수 있는지. 쓸 수 없으면 마이크를 그리지 않는다.
    LaunchedEffect(Unit) { viewModel.voice.check() }
    // 화면이 가려지면 받아쓰기를 멈춘다(#304). 이 엔트리는 백스택에 남아서, 멈추지 않으면 다른 화면에서도 마이크를
    // 잡고 있어 "헤이 메이트"가 듣히지 않는다.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.voice.onHidden() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.voice.onShown() }

    // 권한은 마이크를 누를 때 묻는다. 화면을 열자마자 묻지 않는다.
    val askMic = rememberMicPermission(onGranted = viewModel.voice::onMicClick, onDenied = viewModel.voice::onDenied)
    val askVoiceMode =
        rememberMicPermission(onGranted = viewModel.voice::onVoiceMode, onDenied = viewModel.voice::onDenied)

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
            onBodyMap3dToggle = viewModel.bodyMap::onMap3dToggle,
            onBodyReset = viewModel.bodyMap::onReset,
            onBodySearchChange = viewModel.bodyMap::onSearchChange,
            onDraftChange = viewModel::onDraftChange,
            onSendClick = viewModel::onSend,
            onVoiceClick = askVoiceMode,
            onMicClick = askMic,
            onTypeInsteadClick = { viewModel.onInputModeChange(IntakeInputMode.TEXT) },
            onSeverityChange = viewModel::onSeverityChange,
            onQuestionToggle = viewModel.question::onToggle,
            onQuestionAddClick = viewModel.question::onAddClick,
        ),
        modifier = modifier,
    )
}

/**
 * "헤이 메이트" 다음의 말을 이 화면이 받는다(#284).
 *
 * 단계에 들어설 때 그 단계에서 할 말을 막대에 묻는다. 문답에서는 AI의 질문이 막대 윗줄에 선다 —
 * 막대가 문답을 맡아서, 화면을 내려다보지 않아도 무엇을 묻는지 알 수 있어야 한다. 막대가 닫혀
 * 있으면 묻는 말은 버려진다.
 *
 * [spoken]은 증상 정리를 연 말이다. 그 속에 부위가 있으면 바로 고른다.
 */
@Composable
private fun IntakeVoice(state: IntakeUiState, spoken: String?, viewModel: IntakeViewModel) {
    val labels = MedicalMateSeverity.entries.associateWith { stringResource(it.labelRes) }
    val routes =
        IntakeVoiceRoutes(
            next = { viewModel.onNext(labels.getValue(viewModel.uiState.value.severity)) },
            severityLabel = { labels.getValue(it) },
        )
    VoiceTargetEffect(state.voiceSuggestions()) { text -> viewModel.voiceCommands.handle(text, routes) }

    val prompt = rememberVoicePrompt()
    // 손으로 큰 부위를 짚어 들어가도 그 안에 고를 수 있는 곳을 읽어 준다.
    LaunchedEffect(state.step, state.bodyMap.focus) {
        state.voiceQuestion { labels.getValue(it) }?.let { prompt(VoicePrompt(question = it)) }
    }

    val asked = state.messages.lastOrNull()?.takeIf { it.sender == IntakeMessage.Sender.AI }
    val chatting = state.step == IntakeStep.SYMPTOM_CHAT && !state.awaitingReply
    LaunchedEffect(asked?.id, chatting, state.chatFinished) {
        if (asked == null || !chatting) return@LaunchedEffect
        val next = if (state.chatFinished) R.string.voice_intake_ask_questions else R.string.voice_intake_ask_answer
        prompt(
            VoicePrompt(
                message = VoiceText(R.string.voice_intake_ai_says, listOf(asked.text)),
                question = VoiceText(next),
            ),
        )
    }

    LaunchedEffect(Unit) {
        val reply =
            spoken?.let { viewModel.voiceCommands.handle(it, routes) } as? VoiceReply.Done ?: return@LaunchedEffect
        prompt(VoicePrompt(message = VoiceText(reply.message, reply.args), question = reply.question))
    }
}

/** 1c-5를 말로 고른다(#284). "카드 만들어 줘", "병원부터 찾을래". */
@Composable
private fun IntakeDoneVoice(onCard: () -> Unit, onHospital: () -> Unit) {
    VoiceTargetEffect(INTAKE_DONE_SUGGESTIONS) { text ->
        when (parseDoneVoice(text)) {
            IntakeDoneChoice.CARD -> {
                onCard()
                VoiceReply.Done(R.string.voice_intake_done_card)
            }

            IntakeDoneChoice.HOSPITAL -> {
                onHospital()
                VoiceReply.Done(R.string.voice_intake_done_hospital)
            }

            null -> VoiceReply.NotMine
        }
    }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) { prompt(VoicePrompt(question = VoiceText(R.string.voice_intake_done_ask))) }
}
