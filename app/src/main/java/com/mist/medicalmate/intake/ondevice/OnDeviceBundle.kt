package com.mist.medicalmate.intake.ondevice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * AI 트랙이 넘긴 고정 묶음. `Medical-Mate/AI`의 `docs/android-ondevice-handoff.md`.
 *
 * **앱이 모델·프롬프트·스키마를 바꾸지 않는다.** 바뀌면 AI 쪽이 버전을 올려 다시 전달한다.
 * 프롬프트와 스키마 파일은 `assets/ondevice/`에 그대로 두고 이름만 여기서 가리킨다.
 *
 * 추론 설정도 그쪽이 정한 값이다. 온도 0과 고정 시드가 일치 확인의 전제라 화면이나 사용자
 * 설정으로 흔들면 안 된다.
 */
object OnDeviceBundle {
    /** 모델 파일. `unsloth/Qwen3-1.7B-GGUF`의 Q4_0. K-quant는 NPU 오프로드가 안 될 수 있다. */
    const val MODEL_FILE = "Qwen3-1.7B-Q4_0.gguf"

    const val MODEL_SIZE_BYTES = 1_000_000_000L

    /** 진료 전 추출. 한 턴의 발화에서 SOCRATES 축을 뽑는다. */
    const val EXTRACT_PROMPT_ASSET = "ondevice/prompt-small-v4.system.txt"

    const val EXTRACT_SCHEMA_ASSET = "ondevice/turn_extraction.small.schema.json"

    /** 진료 후 메모 분류. 문장마다 라벨 하나. */
    const val MEMO_PROMPT_ASSET = "ondevice/prompt-memo-small-v4.system.txt"

    const val MEMO_SCHEMA_ASSET = "ondevice/memo_labels.schema.json"

    /** 묶음 버전. 불일치를 AI 쪽에 알릴 때 함께 적는다. */
    const val VERSION = "Qwen3-1.7B-Q4_0 · extract-small-v4 · memo-small-v4 (2026-09-09)"
}

/**
 * 추론 설정. AI 트랙이 고정한 값이다.
 *
 * 온도 0과 시드 42가 일치 확인의 전제다. 같은 GGUF·같은 엔진이면 PC와 같은 출력이 나오는
 * 것이 그 둘 덕분이고, 하나라도 흔들면 88개 비교가 의미를 잃는다.
 */
data class OnDeviceSettings(
    val temperature: Double = 0.0,
    val seed: Int = 42,
    val maxTokens: Int = 1024,
    val reasoning: Boolean = false,
    val contextSize: Int = 4096,
    val threads: Int = 6,
)

/**
 * 온디바이스 출력 전용 JSON.
 *
 * 모르는 키를 무시하지 않는다. 스키마가 `additionalProperties: false`로 모델이 `diagnosis`
 * 같은 필드를 덧붙이는 것을 막고 있어서, 앱도 그것을 통과시키면 안 된다. 계약 위반을 조용히
 * 넘기면 무엇이 틀렸는지 찾을 수 없다.
 *
 * 기본값이 있는 필드는 출력에서 빠질 수 있다. 벡터의 기대 출력도 `chief_complaint`가 없는
 * 줄이 많다.
 */
internal val onDeviceJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

/**
 * 메모 분류 스키마를 문장 수에 맞춰 만든다.
 *
 * 파일(`memo_labels.schema.json`)은 N=4 예시일 뿐이다. 문서가 "요청마다 문장 수 N에 맞춰
 * 키 `0`~`N-1`을 전부 required로 생성"하라고 한다. 4개짜리 스키마를 그대로 쓰면 문장이
 * 다섯인 메모에서 다섯 번째 라벨이 강제되지 않는다.
 *
 * `additionalProperties: false`를 함께 둔다. 모델이 없는 번호를 지어내는 것을 막는다.
 */
internal fun memoLabelsSchema(sentenceCount: Int): JsonObject = buildJsonObject {
    put("type", "object")
    put("title", "MemoLabels")
    put("additionalProperties", false)
    putJsonObject("properties") {
        repeat(sentenceCount) { index ->
            putJsonObject(index.toString()) {
                put("type", "string")
                put(
                    "enum",
                    buildJsonArray {
                        MemoLabel.entries.forEach { add(JsonPrimitive(it.wireName)) }
                    },
                )
            }
        }
    }
    put(
        "required",
        buildJsonArray {
            repeat(sentenceCount) { index -> add(JsonPrimitive(index.toString())) }
        },
    )
}

/** 스키마에 적히는 이름. 열거형 이름과 다르다. */
internal val MemoLabel.wireName: String
    get() = when (this) {
        MemoLabel.FINDINGS -> "findings"
        MemoLabel.TESTS -> "tests"
        MemoLabel.MEDICATION_INSTRUCTIONS -> "medication_instructions"
        MemoLabel.FOLLOW_UP -> "follow_up"
        MemoLabel.NONE -> "none"
    }
