package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState

/**
 * 증상 정리의 네 단계. Figma의 진행 표시가 `증상 문답 n / 4`다.
 *
 * 1단계 아픈 부위는 인체도다(1l-1·1l-2·1l-3). 앵커를 짚고 구역까지 고르면 다음으로
 * 넘어간다. 문답이 "짚은 부위"를 전제로 시작하므로 이 단계를 건너뛸 수 없다.
 */
enum class IntakeStep {
    BODY_PART,
    SYMPTOM_CHAT,
    SEVERITY,
    QUESTIONS,
    ;

    val number: Int get() = ordinal + 1

    val isLast: Boolean get() = this == entries.last()

    companion object {
        val total: Int = entries.size
    }
}

/**
 * 문답의 한 마디. Figma `313:999` Bubble.
 *
 * AI는 면색도 테두리도 없이 라벨과 본문만, 환자는 유색 면에 우하 꼬리다. 그 차이는
 * 컴포넌트가 [sender]로 그린다.
 *
 * [id]를 두는 이유는 목록의 key로 쓰기 위해서다. 본문이 같은 마디가 두 번 올 수 있다.
 */
data class IntakeMessage(val id: Long, val sender: Sender, val text: String) {
    enum class Sender { AI, PATIENT }
}

/** 문답의 입력 방식. Figma가 글(1c-2)과 음성(1c-3·1c-4)을 나눠 그렸다. */
enum class IntakeInputMode { TEXT, VOICE }

/**
 * 증상 정리 화면의 상태.
 *
 * 네 단계가 한 상태에 모여 있다. 단계를 오가도 앞 단계의 답이 남아 있어야 하고, 마지막에
 * 카드 한 장으로 묶여야 한다.
 */
data class IntakeUiState(
    val step: IntakeStep = IntakeStep.BODY_PART,
    val bodyPart: String? = null,
    val bodyMap: BodyMapUiState = BodyMapUiState(),
    val messages: List<IntakeMessage> = emptyList(),
    val draft: String = "",
    val inputMode: IntakeInputMode = IntakeInputMode.TEXT,
    val voice: MedicalMateVoiceState = MedicalMateVoiceState.IDLE,
    val awaitingReply: Boolean = false,
    val severity: MedicalMateSeverity = MedicalMateSeverity.LEVEL_3,
    val questionDraft: String = "",
    val questions: List<String> = emptyList(),
    val chatFinished: Boolean = false,
    val completed: Boolean = false,
) {
    /**
     * 첫 단계에서 뒤로 가면 흐름을 벗어난다. 그 판단은 호출자가 한다.
     *
     * 인체도의 구역 단계는 화면이 바뀌지 않고 같은 단계 안에서 깊어진다. 그래서 앵커를
     * 고른 상태에서 뒤로 가면 흐름을 벗어나는 대신 앵커 선택으로 돌아간다.
     */
    val canGoBack: Boolean
        get() = step != IntakeStep.entries.first() || bodyMap.selection != null

    /** 부위를 다 고르기 전에는 다음으로 갈 수 없다. */
    val canLeaveBodyPart: Boolean get() = bodyMap.selection?.isComplete == true

    /** 보낼 것이 있는지. 빈 글이나 공백만 보내면 문답이 헛돈다. */
    val canSend: Boolean get() = draft.isNotBlank() && !awaitingReply

    val canAddQuestion: Boolean get() = questionDraft.isNotBlank()
}
