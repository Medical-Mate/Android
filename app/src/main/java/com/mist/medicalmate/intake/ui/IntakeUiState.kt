package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.model.IntakeStep

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
/**
 * @param sessionId 서버가 들고 있는 문답 id. 부위를 고르고 다음을 누를 때 받는다.
 *   임시저장의 열쇠라서, 없으면 앱을 나갔다 와도 이어 할 자리가 없다. 세션 만들기가
 *   실패해도 문답은 그대로 진행하므로 `null`일 수 있다.
 * @param restoring 홈의 "이어서 하기"로 들어와 서버에서 불러오는 중인지.
 * @param restoreFailed 불러오지 못했는지. 부위 선택부터 다시 하게 두는 대신 이유를 알린다.
 * @param sendFailed 보낸 말이 서버에 닿지 못했는지.
 */
data class IntakeUiState(
    val step: IntakeStep = IntakeStep.BODY_PART,
    val bodyPart: String? = null,
    val bodyMap: BodyMapUiState = BodyMapUiState(),
    val messages: List<IntakeMessage> = emptyList(),
    val draft: String = "",
    val inputMode: IntakeInputMode = IntakeInputMode.TEXT,
    val voice: MedicalMateVoiceState = MedicalMateVoiceState.IDLE,
    /**
     * 이 기기에서 음성을 쓸 수 있는지.
     *
     * 쓸 수 없으면 입력칸의 마이크를 그리지 않는다. 눌러 봐야 안 되는 버튼을 두고 "이
     * 기기에서는 안 돼요"를 띄우는 것보다 낫다 — 그 문구는 시안에도 없다.
     */
    val voiceAvailable: Boolean = false,
    val awaitingReply: Boolean = false,
    val severity: MedicalMateSeverity = MedicalMateSeverity.LEVEL_3,
    val questionDraft: String = "",
    val questions: List<String> = emptyList(),
    val chatFinished: Boolean = false,
    val completed: Boolean = false,
    val sessionId: Long? = null,
    val restoring: Boolean = false,
    val restoreFailed: Boolean = false,
    /**
     * 보낸 말이 서버에 닿지 못했는지.
     *
     * 보낸 말은 화면에 남긴다. 지우면 다시 적어야 한다. 다시 보내는 조작은 아직 없다.
     */
    val sendFailed: Boolean = false,
) {
    /**
     * 첫 단계에서 뒤로 가면 흐름을 벗어난다. 그 판단은 호출자가 한다.
     *
     * 인체도의 구역 단계는 화면이 바뀌지 않고 같은 단계 안에서 깊어진다. 그래서 확대한
     * 상태에서 뒤로 가면 흐름을 벗어나는 대신 앵커 화면으로 돌아간다.
     */
    val canGoBack: Boolean
        get() = step != IntakeStep.entries.first() || bodyMap.focus != null

    /** 부위를 고르기 전에는 다음으로 갈 수 없다. */
    val canLeaveBodyPart: Boolean get() = bodyMap.selection != null

    /**
     * 주격 조사를 붙인 부위 이름. 부위를 부르는 물음이 이 값을 쓴다.
     *
     * 조사를 문자열 리소스에 박아 두면("%1$s가 얼마나 아프세요?") 부위가 25가지로 늘었을
     * 때 "무릎가"가 된다. 문구는 리소스에 두고 조사는 상태가 계산한다.
     */
    val bodyPartSubject: String get() = bodyPart?.let { withSubjectParticle(it) }.orEmpty()

    /** 보낼 것이 있는지. 빈 글이나 공백만 보내면 문답이 헛돈다. */
    val canSend: Boolean get() = draft.isNotBlank() && !awaitingReply

    val canAddQuestion: Boolean get() = questionDraft.isNotBlank()

    /**
     * 다음 마디. id를 지금 있는 것들에서 이어 붙인다.
     *
     * 세는 값을 따로 들면 서버에서 불러온 마디와 겹친다. 불러온 마디의 id는 서버가 매긴
     * `seq`라 0부터 시작하지 않고, 이어 답한 마디가 같은 번호를 받으면 목록의 key가 겹쳐
     * 화면이 엉뚱한 줄을 다시 쓴다.
     */
    internal fun newMessage(sender: IntakeMessage.Sender, text: String): IntakeMessage =
        IntakeMessage(id = (messages.maxOfOrNull { it.id } ?: 0L) + 1, sender = sender, text = text)
}

/**
 * 주격 조사를 붙인 낱말. 받침이 있으면 "이", 없으면 "가"다.
 *
 * 부위 이름 25개에 "무릎"과 "머리"가 함께 있어서 하나로 고정할 수 없다. 이름 끝에
 * 괄호가 붙는 것도 있어("가슴 옆(갈비)") 마지막 한글 음절을 찾아서 본다.
 */
internal fun withSubjectParticle(word: String): String {
    // 한글은 식별자에 쓸 수 있는 문자다. "$word이"로 쓰면 `word이`라는 이름을 찾는다.
    val syllable = word.lastOrNull { it.code in HANGUL_FIRST..HANGUL_LAST } ?: return "${word}이"
    val hasFinalConsonant = (syllable.code - HANGUL_FIRST) % HANGUL_FINAL_COUNT != 0
    return if (hasFinalConsonant) "${word}이" else "${word}가"
}

/** 한글 음절 영역과 종성 개수. 유니코드가 초성·중성·종성 순서로 음절을 나열한다. */
private const val HANGUL_FIRST = 0xAC00
private const val HANGUL_LAST = 0xD7A3
private const val HANGUL_FINAL_COUNT = 28
