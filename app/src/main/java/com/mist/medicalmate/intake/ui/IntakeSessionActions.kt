package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionStatus
import com.mist.medicalmate.intake.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 문답 세션을 서버와 주고받는 부분.
 *
 * `IntakeViewModel`에서 떼어냈다. 붙여 두면 한 클래스의 함수가 열한 개를 넘어 detekt가
 * 잡는다. 나눠 두면 세션 쪽만 따로 볼 수도 있다.
 *
 * 상태를 직접 들지 않고 [update]로 넘긴다. 화면 상태는 하나뿐이어야 한다.
 */
internal class IntakeSessionActions(
    private val repository: SessionRepository,
    private val scope: CoroutineScope,
    private val update: ((IntakeUiState) -> IntakeUiState) -> Unit,
) {
    /**
     * 짚은 부위로 세션을 연다.
     *
     * 실패해도 문답은 그대로 진행한다. 세션 id가 없으면 임시저장이 안 될 뿐이고, 여기서
     * 막으면 답하던 사람이 아무것도 못 하게 된다. 카드를 만들 때 id가 없으면 그때 다시
     * 만들면 된다.
     */
    fun start(selection: BodyMapSelection, siteText: String) {
        scope.launch {
            val result = repository.start(siteCodes = listOf(selection.siteCode), siteText = siteText)
            if (result is ApiResult.Success) {
                update { it.copy(sessionId = result.value.id) }
            }
        }
    }

    /**
     * 홈의 "이어서 하기"로 들어왔을 때 서버에 남은 문답을 되살린다.
     *
     * 부위를 다시 고르게 하지 않는다. 세션이 있다는 것은 그 단계를 이미 지났다는 뜻이고,
     * 서버가 `siteText`를 들고 있다.
     *
     * 되돌아가는 자리는 문답이다. 서버의 `progress`는 문답 안의 물음 수이지 우리 네 단계가
     * 아니라서, 강도나 질문 단계까지 건너뛸 근거가 없다. 답한 마디는 그대로 다시 그린다.
     */
    /**
     * 고른 강도를 서버에 남긴다.
     *
     * 표시 문구를 함께 보낸다. 서버가 카피를 들고 있으면 문구를 바꿀 때마다 배포해야 하고,
     * 그것은 디자인 것이지 서버 것이 아니다.
     *
     * 실패해도 흐름을 막지 않는다. 다음 단계로 못 가게 하는 것이 더 나쁘다.
     *
     * **응답에서 질문 후보를 받아 온다.** 서버가 문답이 끝나면 AI가 고른 질문 셋을 세션에
     * 얹어 두고 모든 세션 응답에 함께 싣는다. 3단계를 지나는 이 호출이 4단계(1i) 직전의
     * 마지막 왕복이라, 여기서 받으면 그 화면이 열릴 때 목록이 차 있다.
     *
     * 환자가 이미 적은 것이 있으면 덮지 않는다. 덮으면 지운 질문이 되살아난다.
     */
    fun saveSeverity(state: IntakeUiState, label: String) {
        val sessionId = state.sessionId ?: return
        scope.launch {
            val result = repository.setSeverity(sessionId, state.severity.level, label)
            if (result !is ApiResult.Success) return@launch
            update { current ->
                if (current.questions.isNotEmpty()) current else current.copy(questions = result.value.questions)
            }
        }
    }

    /** 적어 둔 질문을 통째로 보낸다. 추가·삭제·순서가 한 번에 처리된다. */
    fun saveQuestions(state: IntakeUiState) {
        val sessionId = state.sessionId ?: return
        scope.launch { repository.setQuestions(sessionId, state.questions) }
    }

    fun restore(sessionId: Long) {
        update { it.copy(restoring = true) }
        scope.launch {
            val result = repository.load(sessionId)
            update { state ->
                when (result) {
                    is ApiResult.Success -> state.restoredWith(result.value)
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                        state.copy(restoring = false, restoreFailed = true)
                }
            }
        }
    }
}

/**
 * 불러온 세션을 화면 상태에 얹는다.
 *
 * 마디 id는 서버의 `seq`를 그대로 쓴다. 목록의 key라서 이어 답할 때 새로 붙는 마디와
 * 겹치지 않아야 하고, `seq`는 서버가 세션 안에서 유일하게 매긴 값이다.
 */
private fun IntakeUiState.restoredWith(session: IntakeSession): IntakeUiState {
    val restored =
        session.messages.map {
            IntakeMessage(
                id = it.seq,
                sender = if (it.fromPatient) IntakeMessage.Sender.PATIENT else IntakeMessage.Sender.AI,
                text = it.text,
            )
        }
    return copy(
        step = session.leftAt(),
        sessionId = session.id,
        bodyPart = session.siteText,
        messages = restored.ifEmpty { openingFor(session.siteText) },
        chatFinished = session.status != IntakeSessionStatus.IN_PROGRESS,
        severity = session.severityLevel?.let(::severityOf) ?: severity,
        // 적어 둔 질문이 있으면 그것이, 없으면 AI 후보가 들어 있다. 저장소가 그 규칙을 든다.
        questions = session.questions.ifEmpty { questions },
        restoring = false,
        restoreFailed = false,
    )
}

/**
 * 나갔던 단계.
 *
 * 시안 흐름 주석이 "문답 이탈 시 임시저장 UI 연결(홈에서 진입 → 이전 단계 복귀)"이라고
 * 적었다. 무조건 문답으로 열면 강도를 고르고 나간 사람이 다 끝낸 문답을 다시 지나야 한다.
 *
 * 강도는 3단계에서 4단계로 넘어갈 때 저장한다. 그래서 강도가 있으면 4단계까지 갔던 것이고,
 * 강도 없이 문답만 끝났으면 3단계다. 질문으로는 가릴 수 없다 — 적어 둔 것이 없어도 AI 후보가
 * 들어 있어서 환자가 그 단계를 봤는지 알 수 없다.
 */
private fun IntakeSession.leftAt(): IntakeStep = when {
    severityLevel != null -> IntakeStep.QUESTIONS
    status != IntakeSessionStatus.IN_PROGRESS -> IntakeStep.SEVERITY
    else -> IntakeStep.SYMPTOM_CHAT
}

/** 서버가 준 1~5를 눈금으로. 밖이면 화면의 기본값을 그대로 둔다. */
private fun severityOf(level: Int): MedicalMateSeverity? = MedicalMateSeverity.entries.firstOrNull { it.level == level }

/**
 * 서버에 마디가 하나도 없을 때 첫 물음을 다시 연다.
 *
 * 지금 계약에는 답을 보내는 경로가 없어서 세션을 만들어도 대화가 서버에 쌓이지 않는다.
 * 그대로 두면 이어서 하기로 들어온 화면이 빈 채로 열려 무엇을 해야 하는지 알 수 없다.
 * 첫 물음은 어차피 앱이 만들고 있으므로 여기서도 같은 문장을 쓴다. 답을 보내는 경로가
 * 생기면 사라진다(#137).
 */
private fun IntakeUiState.openingFor(siteText: String?): List<IntakeMessage> =
    siteText?.let { listOf(newMessage(IntakeMessage.Sender.AI, intakeOpeningLine(it))) }.orEmpty()

/**
 * 서버에 보낼 부위 코드.
 *
 * 구역까지 골랐으면 그것이 더 자세하다. 앵커까지만 고른 경우는 구역이 없는 부위다.
 */
private val BodyMapSelection.siteCode: String get() = zoneId ?: anchorId
