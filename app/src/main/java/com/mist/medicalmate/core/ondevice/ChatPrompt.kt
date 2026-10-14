package com.mist.medicalmate.core.ondevice

/**
 * 모델에 넣는 글 전체를 앱이 직접 만든다.
 *
 * **`llama_chat_apply_template`을 쓰지 않는다.** 그 함수는 chatml을 `<|im_start|>assistant\n`에서
 * 끝내고 생각 끄기 블록(`<think>\n\n</think>\n\n`)을 붙이지 않는다(AI#35 대응표). 붙이지 않으면
 * 모델이 생각부터 늘어놓고 문법이 그것을 막아 출력이 깨진다. 여기서 만들면 AI 쪽 렌더 전문
 * (`rendered/2b-A01-0.enable_thinking_false.txt`)과 글자까지 같은지를 JVM 시험이 확인할 수 있다.
 *
 * 둘로 나눠 넘긴다. [prefix]는 시스템 프롬프트까지이고 매 호출이 같다. 엔진이 그 상태를 떠
 * 두었다가 되돌려 쓴다 — 2B는 DeltaNet 상태라 뒤쪽만 지울 수 없어서, 약 2,000토큰의 시스템
 * 프롬프트를 매 호출 다시 처리하지 않으려면 이 방법뿐이다.
 */
internal data class ChatPrompt(val prefix: String, val suffix: String) {
    val full: String get() = prefix + suffix

    companion object {
        fun of(system: String, user: String): ChatPrompt = ChatPrompt(
            prefix = "<|im_start|>system\n$system<|im_end|>\n",
            suffix = "<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n",
        )
    }
}
