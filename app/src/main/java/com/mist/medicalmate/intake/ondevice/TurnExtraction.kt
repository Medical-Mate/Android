package com.mist.medicalmate.intake.ondevice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 폰 안 모델이 한 턴에서 뽑은 것. AI 트랙의 `turn_extraction.small.schema.json`.
 *
 * **스키마를 앱에서 바꾸지 않는다.** 고정된 묶음이고 바뀌면 AI 쪽이 버전을 올려 다시
 * 전달한다. 이 타입은 그 스키마를 그대로 옮긴 것이라 필드를 더하거나 빼지 않는다.
 *
 * 스키마의 `additionalProperties: false`가 모델이 `diagnosis` 같은 필드를 덧붙이는 것을
 * 막는다. 앱 쪽에서도 모르는 키를 무시하지 않고 실패로 본다. 계약이 어긋난 것을 조용히
 * 넘기면 무엇이 틀렸는지 찾을 수 없다.
 */
@Serializable
data class TurnExtraction(
    @SerialName("chief_complaint")
    val chiefComplaint: String? = null,
    val updates: List<AxisUpdate> = emptyList(),
    val notes: List<String> = emptyList(),
    @SerialName("wants_to_stop")
    val wantsToStop: Boolean = false,
)

/**
 * 축 하나의 갱신.
 *
 * [evidence]는 환자가 실제로 한 말이다. 스키마가 `minLength: 1`로 빈 값을 막는다. 모델이
 * 지어낸 값과 들은 값을 가르는 유일한 자리라 비어 있으면 안 된다.
 */
@Serializable
data class AxisUpdate(
    val axis: ExtractionAxis,
    val status: FieldStatus,
    /** `filled`면 정리한 값, 아니면 빈 문자열. */
    val value: String,
    val evidence: String,
)

/**
 * 진료 전 SOCRATES 8축과 진료 후 6축.
 *
 * 한 열거형에 담는다. 스키마가 `axis`를 두 열거형의 `anyOf`로 두고 있어 실제로 오는 값이
 * 섞인다. 나눠 두면 파싱할 때 어느 쪽인지 먼저 알아야 한다.
 *
 * 부위(`site`)는 환자 표현 그대로 둔다. 온톨로지 매핑은 부위가 확정된 뒤의 일이다.
 */
@Serializable
enum class ExtractionAxis {
    @SerialName("site")
    SITE,

    @SerialName("onset")
    ONSET,

    @SerialName("character")
    CHARACTER,

    @SerialName("radiation")
    RADIATION,

    @SerialName("associated")
    ASSOCIATED,

    @SerialName("time_course")
    TIME_COURSE,

    @SerialName("exacerbating_relieving")
    EXACERBATING_RELIEVING,

    @SerialName("severity")
    SEVERITY,

    @SerialName("heard_diagnosis")
    HEARD_DIAGNOSIS,

    @SerialName("medication")
    MEDICATION,

    @SerialName("tests_procedures")
    TESTS_PROCEDURES,

    @SerialName("follow_up")
    FOLLOW_UP,

    @SerialName("instructions")
    INSTRUCTIONS,

    @SerialName("open_questions")
    OPEN_QUESTIONS,
}

/**
 * 축의 상태.
 *
 * `unknown`("잘 모르겠다")과 `skipped`("넘어간다")를 같게 다루지 않는다. 앞은 환자가 답을
 * 못 한 것이고 뒤는 묻지 않기로 한 것이다. 카드에 적히는 말이 달라진다.
 */
@Serializable
enum class FieldStatus {
    @SerialName("not_asked")
    NOT_ASKED,

    @SerialName("filled")
    FILLED,

    @SerialName("unknown")
    UNKNOWN,

    @SerialName("skipped")
    SKIPPED,

    @SerialName("ambiguous")
    AMBIGUOUS,
}

/**
 * 진료 후 메모 한 문장의 라벨. AI 트랙의 `memo_labels.schema.json`.
 *
 * 폰은 문장에 라벨만 붙인다. 분리·조립·날짜 계산은 서버가 한다. 문장을 나누는 것도 서버라
 * 폰과 서버가 같은 번호를 가리킨다.
 */
@Serializable
enum class MemoLabel {
    @SerialName("findings")
    FINDINGS,

    @SerialName("tests")
    TESTS,

    @SerialName("medication_instructions")
    MEDICATION_INSTRUCTIONS,

    @SerialName("follow_up")
    FOLLOW_UP,

    @SerialName("none")
    NONE,
}
