package com.mist.medicalmate.core.ondevice

import kotlinx.serialization.Serializable

/**
 * 모델에 넣는 글 전체를 앱이 직접 만든다.
 *
 * **`llama_chat_apply_template`을 쓰지 않는다.** 그 함수는 chatml을 `<|im_start|>assistant\n`에서
 * 끝내고 생각 끄기 블록(`<think>\n\n</think>\n\n`)을 붙이지 않는다(AI#35 대응표). 붙이지 않으면
 * 모델이 생각부터 늘어놓고 문법이 그것을 막아 출력이 깨진다. 여기서 만들면 AI 쪽 렌더 전문
 * (`rendered/2b-*.enable_thinking_false.txt`)과 글자까지 같은지를 JVM 시험이 확인할 수 있다.
 *
 * 각 말은 앞뒤 공백을 걷는다. 모델 안 템플릿이 그렇게 한다 — 질문 후보의 시스템 프롬프트가
 * 줄바꿈으로 끝나는데 렌더 전문에는 그 줄바꿈이 없다. 지난 어시스턴트 말 앞에는 생각 끄기
 * 블록을 붙이지 않는다. 렌더 전문이 그렇다.
 *
 * 둘로 나눠 넘긴다. [prefix]는 사용자 메시지 앞까지(시스템 프롬프트와 예시)이고 같은 기능이면
 * 매 호출이 같다. 엔진이 그 상태를 떠 두었다가 되돌려 쓴다 — 2B는 DeltaNet 상태라 뒤쪽만 지울
 * 수 없어서, 약 2,000토큰의 시스템 프롬프트를 매 호출 다시 처리하지 않으려면 이 방법뿐이다.
 */
internal data class ChatPrompt(val prefix: String, val suffix: String) {
    val full: String get() = prefix + suffix

    companion object {
        fun of(system: String, user: String): ChatPrompt = of(listOf(ChatMessage(ROLE_SYSTEM, system)), user)

        /** @param history 시스템 프롬프트와 예시 대화. 마지막 사용자 메시지는 [user]다. */
        fun of(history: List<ChatMessage>, user: String): ChatPrompt = ChatPrompt(
            prefix = history.joinToString("") { it.rendered() },
            suffix = ChatMessage(ROLE_USER, user).rendered() + "<|im_start|>assistant\n<think>\n\n</think>\n\n",
        )
    }
}

/** 대화의 말 하나. 질문 후보의 예시 대화를 자산에서 이 모양으로 읽는다. */
@Serializable
internal data class ChatMessage(val role: String, val content: String) {
    fun rendered(): String = "<|im_start|>$role\n${content.trim()}<|im_end|>\n"
}

private const val ROLE_SYSTEM = "system"
private const val ROLE_USER = "user"
