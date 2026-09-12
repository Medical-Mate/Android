package com.mist.medicalmate.core.model

/**
 * 증상 정리의 네 단계. Figma의 진행 표시가 `증상 문답 n / 4`다.
 *
 * 1단계 아픈 부위는 인체도다(1l-1·1l-2·1l-3). 앵커를 짚고 구역까지 고르면 다음으로
 * 넘어간다. 문답이 "짚은 부위"를 전제로 시작하므로 이 단계를 건너뛸 수 없다.
 *
 * **`core/model`에 있는 이유는 홈도 이 정의를 쓰기 때문이다**(#176). 홈의 "이어서 하기"
 * 카드가 어디까지 답했는지를 이 단계로 적는다. 양쪽이 각자 숫자를 들면 단계가 바뀔 때
 * 한쪽만 고쳐진다.
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
