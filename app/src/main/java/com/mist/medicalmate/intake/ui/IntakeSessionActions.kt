package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.intake.data.IntakeSession
import com.mist.medicalmate.intake.data.IntakeSessionStatus
import com.mist.medicalmate.intake.data.IntakeSeverity
import com.mist.medicalmate.intake.data.IntakeSide
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
     * 짚은 부위와 고른 강도로 세션을 연다(Backend#169). 강도 단계에서 다음을 누를 때다.
     *
     * **첫 질문은 서버 것이다**(#259). 세션을 만들면서 AI 첫 질문을 대화에 넣어 응답에 실어
     * 준다. 전에는 앱이 만든 문장을 먼저 세우고 응답의 마디를 버렸는데, 첫 답을 보내면
     * 대화 전체가 서버 것으로 갈아 끼워지면서 첫 질문이 다른 문장으로 바뀌어 보였다. 그래서
     * 응답이 오기 전에는 답을 기다리는 점 셋만 두고, 응답의 마디를 그대로 첫 마디로 세운다.
     *
     * 실패해도 문답은 그대로 진행한다. 세션 id가 없으면 임시저장이 안 될 뿐이고, 여기서
     * 막으면 답하던 사람이 아무것도 못 하게 된다. 그때만 앱이 만든 첫 물음을 세운다 —
     * 서버 문장이 없는데 빈 화면으로 둘 수는 없다. 카드를 만들 때 id가 없으면 그때 다시
     * 만들면 된다.
     */
    fun start(selection: BodyMapSelection, siteText: String, severity: IntakeSeverity) {
        scope.launch {
            val result = repository.start(selection.siteCode, selection.side.toIntakeSide(), siteText, severity)
            update { state ->
                when (result) {
                    is ApiResult.Success ->
                        state.copy(
                            sessionId = result.value.id,
                            messages = result.value.toMessages().ifEmpty { state.openingFor(siteText) },
                            awaitingReply = false,
                        )

                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                        state.copy(messages = state.openingFor(siteText), awaitingReply = false)
                }
            }
        }
    }

    /**
     * 문답을 연 뒤 강도 단계로 돌아가 다시 고른 값을 서버에 남긴다.
     *
     * 실패해도 흐름을 막지 않는다. 다음 단계로 못 가게 하는 것이 더 나쁘다.
     */
    fun saveSeverity(state: IntakeUiState, severity: IntakeSeverity) {
        val sessionId = state.sessionId ?: return
        scope.launch { repository.setSeverity(sessionId, severity) }
    }

    /** 체크한 질문을 화면 순서대로 통째로 보낸다. 체크를 푼 줄은 보내지 않는다. */
    fun saveQuestions(state: IntakeUiState) {
        val sessionId = state.sessionId ?: return
        scope.launch { repository.setQuestions(sessionId, state.questions) }
    }

    /**
     * 홈의 "이어서 하기"로 들어왔을 때 서버에 남은 문답을 되살린다.
     *
     * 부위와 강도를 다시 고르게 하지 않는다. 세션은 강도를 고른 뒤에 생기므로 세션이 있다는 것은
     * 그 두 단계를 지났다는 뜻이다. 되돌아가는 자리는 3단계다 — 문답이 끝났으면 추천 질문 고르기가
     * 이어서 선다(Backend#173의 `ended`).
     */
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
    val restored = session.toMessages()
    return copy(
        step = IntakeStep.SYMPTOM_CHAT,
        sessionId = session.id,
        bodyPart = session.siteText,
        messages = restored.ifEmpty { openingFor(session.siteText) },
        chatFinished = session.status != IntakeSessionStatus.IN_PROGRESS,
        severity = session.severityLevel?.let(::severityOf) ?: severity,
        questionRows = questionRowsOf(session.questionCandidates, saved = session.questions),
        restoring = false,
        restoreFailed = false,
    )
}

/**
 * 추천 질문 줄. 추천이 먼저 서고 저장해 둔 것 가운데 추천에 없는 것이 직접 적은 줄로 뒤에 붙는다.
 * 저장해 둔 것은 체크한 것이다 — 체크를 푼 줄은 저장하지 않는다.
 */
internal fun questionRowsOf(candidates: List<String>, saved: List<String> = emptyList()): List<IntakeQuestion> =
    candidates.map { IntakeQuestion(it, selected = it in saved) } +
        saved.filterNot { it in candidates }.map { IntakeQuestion(it, selected = true, custom = true) }

/** 서버가 준 1~5를 눈금으로. 밖이면 화면의 기본값을 그대로 둔다. */
private fun severityOf(level: Int): MedicalMateSeverity? = MedicalMateSeverity.entries.firstOrNull { it.level == level }

/** 서버의 마디를 화면 마디로. 마디 id는 서버의 `seq`다 — 세션 안에서 유일하고, 이어 붙는 마디와 겹치지 않는다. */
private fun IntakeSession.toMessages(): List<IntakeMessage> = messages.map {
    IntakeMessage(
        id = it.seq,
        sender = if (it.fromPatient) IntakeMessage.Sender.PATIENT else IntakeMessage.Sender.AI,
        text = it.text,
    )
}

/**
 * 서버 마디가 없을 때 앱이 세우는 첫 물음.
 *
 * 세션을 못 만들었거나(연결 없음) 응답에 마디가 비어 있을 때만 쓴다. 첫 질문은 서버가
 * 만드는 것이라(#259) 서버 마디가 있으면 이 문장은 화면에 나오지 않는다. 빈 화면으로 두면
 * 무엇을 해야 하는지 알 수 없어 그때만 이 문장으로 문답을 이어 간다.
 */
private fun IntakeUiState.openingFor(siteText: String?): List<IntakeMessage> =
    siteText?.let { listOf(newMessage(IntakeMessage.Sender.AI, intakeOpeningLine(it))) }.orEmpty()

/**
 * 서버에 보낼 부위 코드.
 *
 * 구역까지 골랐으면 그것이 더 자세하다. 앵커까지만 고른 경우는 구역이 없는 부위다.
 */
private val BodyMapSelection.siteCode: String get() = zoneId ?: anchorId

/** 서버에 보낼 좌우. 좌우가 없는 점(`CENTER`)과 팔·다리 기준점(`BASE`)은 보내지 않는다. */
private fun BodyMapSide.toIntakeSide(): IntakeSide? = when (this) {
    BodyMapSide.LEFT -> IntakeSide.LEFT
    BodyMapSide.RIGHT -> IntakeSide.RIGHT
    BodyMapSide.CENTER, BodyMapSide.BASE -> null
}
