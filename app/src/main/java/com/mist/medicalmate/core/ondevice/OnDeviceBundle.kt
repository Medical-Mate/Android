package com.mist.medicalmate.core.ondevice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AI 트랙이 넘긴 고정 묶음. `Medical-Mate/AI`의 `docs/android-ondevice-handoff.md` "묶음 v2".
 *
 * **앱이 모델·프롬프트·문법을 바꾸지 않는다.** 바뀌면 AI 쪽이 버전을 올려 다시 전달한다.
 * 프롬프트와 문법 파일은 `assets/ondevice/`에 그대로 두고 이름만 여기서 가리킨다.
 *
 * v1(Qwen3-1.7B)에서 v2(Qwen3.5-2B)로 바뀐 것은 2026-10-13 폰 실측 두 번으로 정해졌다(AI#35).
 * 같은 조건에서 턴 지연과 발열은 같고 메모 분류가 89.4%에서 93.9%로 올랐다.
 */
object OnDeviceBundle {
    /**
     * 모델 파일.
     *
     * unsloth의 Q4_0은 `ssm_out` 18개가 Q5_K인 혼합 양자화라 Hexagon이 계산하지 못한다. 그
     * 18개만 Q8_0으로 바꾼 파일이고 나머지 텐서는 바이트까지 같다(AI#35).
     */
    const val MODEL_FILE = "Qwen3.5-2B-Q4_0-ssmout-q8.gguf"

    /** 커밋으로 고정한 주소다. `main`이면 업스트림이 파일을 바꿀 때 벡터 검증이 조용히 깨진다. */
    const val MODEL_URL =
        "https://huggingface.co/hyos0415/Qwen3.5-2B-NPU-GGUF/resolve/" +
            "1c069ab6a03d5840d81ad29b2476752a13a407c0/$MODEL_FILE"

    const val MODEL_SHA256 = "c8e057dfc224b87b2fce69690ee6b53ca828f67543d01d088e409cb8315dc53f"

    const val MODEL_SIZE_BYTES = 1_243_185_536L

    /** 진료 전 추출. 물은 축 하나의 답에서 갱신을 뽑는다. */
    const val EXTRACT_PROMPT_ASSET = "ondevice/prompt-extract-small-2b-v1.system.txt"

    const val EXTRACT_GRAMMAR_ASSET = "ondevice/turn_extraction.small.gbnf"

    /**
     * 질문 후보. 문답이 끝난 카드로 "의사에게 물어볼 것"을 3~5개 만든다.
     *
     * 프롬프트 파일이 따로 없다. AI 쪽 벡터의 `messages`(시스템 프롬프트와 예시 두 쌍)를 그대로
     * 옮긴 것이 [QUESTIONS_HISTORY_ASSET]이고, 마지막 사용자 메시지만 [questionsUserMessage]가
     * 만든다. 문법은 질문 40자 상한이다 — 상한이 없으면 같은 말을 되풀이하다 1024토큰까지 간다.
     */
    const val QUESTIONS_HISTORY_ASSET = "ondevice/questions-small4.messages.json"

    const val QUESTIONS_GRAMMAR_ASSET = "ondevice/questions.gbnf"

    /** 진료 후 메모 분류. 문장마다 라벨 하나. 문법은 문장 수에 맞춰 [memoLabelsGrammar]가 만든다. */
    const val MEMO_PROMPT_ASSET = "ondevice/prompt-memo-small-v5.system.txt"

    /**
     * 서버에 "무엇으로 라벨을 붙였는지" 알리는 이름.
     *
     * 백엔드 `labelsMeta`로 가서 기록의 `extractedBy`에 남는다. 어느 버전이 이상하게 나눴는지
     * 되짚는 자리라 파일 이름 그대로 둔다.
     */
    const val MODEL_ID = "Qwen3.5-2B-Q4_0-ssmout-q8"

    const val MEMO_PROMPT_VERSION = "memo-small-v5"

    const val EXTRACT_PROMPT_VERSION = "extract-small-2b-v1"

    const val QUESTIONS_PROMPT_VERSION = "questions-small4"

    /**
     * 서버에 "무엇으로 만들었는지" 알리는 값. 진료 전 추출의 `extractionMeta`, 질문 후보의
     * `candidatesMeta`로 간다. 백엔드는 열어 보지 않고 AI에 넘기고, AI 계약이 뱀 표기다.
     */
    fun meta(promptVersion: String): JsonObject = buildJsonObject {
        put("model_id", MODEL_ID)
        put("prompt_version", promptVersion)
    }

    /** 묶음 버전. 불일치를 AI 쪽에 알릴 때 함께 적는다. */
    const val VERSION =
        "Qwen3.5-2B-Q4_0-ssmout-q8 · extract-small-2b-v1 · questions-small4 · memo-small-v5 (2026-10-13)"
}

/**
 * 추론 설정. AI 트랙이 고정한 값이다.
 *
 * 온도 0이라 샘플링이 greedy이고 시드는 쓰이지 않는다. 그래도 남겨 두는 이유는 벡터가 이 값으로
 * 만들어졌다는 것을 시험이 확인하기 때문이다.
 *
 * [sequences]가 1인 것은 2B에서 생긴 조건이다. 시퀀스를 여럿 잡으면 DeltaNet 18층의
 * `ssm_out` 행렬곱이 3차원이 되고 Hexagon이 받지 않아 CPU로 간다(graph splits 3 → 39, AI#35).
 */
data class OnDeviceSettings(
    val temperature: Double = 0.0,
    val seed: Int = 42,
    val maxTokens: Int = 1024,
    val reasoning: Boolean = false,
    val contextSize: Int = 4096,
    val sequences: Int = 1,
)

/**
 * 온디바이스 출력 전용 JSON.
 *
 * 모르는 키를 무시하지 않는다. 문법이 모양을 강제해도 우리 타입으로 옮기다 어긋나는 것은
 * 여기서 잡혀야 한다. 계약 위반을 조용히 넘기면 무엇이 틀렸는지 찾을 수 없다.
 *
 * 기본값이 있는 필드는 출력에서 빠질 수 있다. 벡터의 기대 출력도 `chief_complaint`가 없는
 * 줄이 많다.
 */
internal val onDeviceJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

/**
 * 메모 분류 문법을 문장 수에 맞춰 만든다.
 *
 * AI 쪽 파일(`memo_labels-memo-small-v5.n4.gbnf`)은 N=4 예시다. 문서가 "키 `0`…`N-1`이
 * 순서대로 전부 나오게 N마다 같은 모양으로 만든다"고 한다. 4개짜리를 그대로 쓰면 다섯 문장
 * 메모의 다섯 번째 라벨이 나오지 않는다.
 *
 * 규칙 이름 차례도 그 파일과 같게 둔다(문자열 순). 문법의 뜻은 차례와 상관없지만 같은 모양이면
 * 시험이 파일과 글자 그대로 견줄 수 있다.
 */
internal fun memoLabelsGrammar(sentenceCount: Int): String {
    require(sentenceCount > 0) { "문장이 없으면 라벨도 없다" }
    val labels = MemoLabel.entries.joinToString(" | ") { "\"\\\"${it.wireName}\\\"\"" }
    val keys = (0 until sentenceCount).map(Int::toString)
    val rules =
        keys.sorted().flatMap { key ->
            listOf(
                "$key ::= ($labels)",
                "$key-kv ::= \"\\\"$key\\\"\" space \":\" space $key",
            )
        }
    val root = "root ::= \"{\" space " + keys.joinToString(" \",\" space ") { "$it-kv" } + " space \"}\""
    val space = "space ::= | \" \" | \"\\n\"{1,2} [ \\t]{0,20}"
    return (rules + root + space).joinToString("\n", postfix = "\n")
}

/** 문법과 서버에 적히는 이름. 열거형 이름과 다르다. */
internal val MemoLabel.wireName: String
    get() = when (this) {
        MemoLabel.FINDINGS -> "findings"
        MemoLabel.TESTS -> "tests"
        MemoLabel.MEDICATION_INSTRUCTIONS -> "medication_instructions"
        MemoLabel.LIFESTYLE_INSTRUCTIONS -> "lifestyle_instructions"
        MemoLabel.FOLLOW_UP -> "follow_up"
        MemoLabel.NONE -> "none"
    }

/**
 * 메모 분류의 사용자 메시지. 벡터 `user` 형식 그대로다.
 *
 * 번호는 서버가 나눈 문장의 자리다. 폰이 문장을 나누지 않는다.
 */
internal fun memoUserMessage(sentences: List<String>): String = buildString {
    append("문장 ${sentences.size}개:\n")
    sentences.forEachIndexed { index, sentence -> append("$index: $sentence\n") }
    append("번호마다 라벨 하나. JSON:")
}

/**
 * 진료 전 추출의 사용자 메시지. 벡터 `user` 형식 그대로다.
 *
 * @param axisKey 백엔드가 준 `askingAxis.key`(`onset`).
 * @param axisLabel 같은 자리의 `label`(`언제부터·어떻게 시작`). AI가 폰 프롬프트와 같은 원천에서
 *   내므로 가공하지 않고 넣는다(Backend#157).
 */
internal fun extractUserMessage(axisKey: String, axisLabel: String, answer: String): String =
    "질문한 항목: $axisLabel (axis=$axisKey). 이 항목만 답한다.\n환자: $answer\nJSON:"

/**
 * 질문 후보의 카드. AI 쪽 `assist_prompts.questions_user`가 읽는 값 그대로다.
 *
 * @param axes 축 키 → 값. 키는 카드의 키(`exacerbating_relieving`)를 그대로 쓴다.
 * @param severity "3 (꽤 아파요)". 강도는 카드 축이 아니라 3단계에서 따로 고른다.
 * @param medications 비어 있으면 "(확인 안 됨)"이다. "없음"이 아니다 — 온보딩이 "없어요"와
 *   "모른다"를 가르지 못해 빈 답은 확인이 안 된 것이고, 알러지에서 둘은 처방이 갈린다.
 */
data class QuestionCard(
    val axes: Map<String, String>,
    val severity: String?,
    val medications: List<String>,
    val conditions: List<String>,
    val allergies: List<String>,
)

/**
 * 질문 후보의 사용자 메시지.
 *
 * 줄 차례와 이름이 AI 쪽 표(`_AXIS_KO`)와 같아야 한다. 값이 없는 칸은 "(안 답함)"이다 — 프롬프트가
 * 그 칸으로는 질문을 만들지 말라고 적는다.
 */
internal fun questionsUserMessage(card: QuestionCard): String {
    val axes = QUESTION_CARD_AXES.map { (key, label) ->
        val value = if (key == SEVERITY_AXIS) card.severity else card.axes[key]
        "$label: ${value?.takeIf { it.isNotBlank() } ?: NOT_ANSWERED}"
    }
    val profile =
        listOf("복용약" to card.medications, "기저질환" to card.conditions, "알러지" to card.allergies)
            .map { (label, items) -> "$label: ${items.joinToString(", ").ifEmpty { PROFILE_UNKNOWN }}" }
    return "카드:\n" + (axes + profile).joinToString("\n") + "\n\n의사에게 물어볼 것 후보. JSON:"
}

/** AI 쪽 `_AXIS_KO`의 차례. 그쪽은 악화·완화를 `exacerbating`으로 부르는데 카드 키는 다르다. */
private val QUESTION_CARD_AXES =
    listOf(
        "site" to "부위",
        "onset" to "시작",
        "character" to "느낌",
        SEVERITY_AXIS to "심각도",
        "time_course" to "경과",
        "exacerbating_relieving" to "악화·완화",
        "radiation" to "퍼짐",
        "associated" to "동반 증상",
    )

private const val SEVERITY_AXIS = "severity"
private const val NOT_ANSWERED = "(안 답함)"
private const val PROFILE_UNKNOWN = "(확인 안 됨)"
