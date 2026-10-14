package com.mist.medicalmate.core.ondevice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 폰 안 모델로 뽑는다.
 *
 * 부르는 쪽은 결과가 폰에서 왔는지 몰라도 된다. [OnDeviceResult.Unavailable]이면 지금처럼
 * 서버로 보내면 된다.
 *
 * 발화 원문은 폰을 떠나지 않는다. 이 인터페이스가 그 경계다. 여기로 들어간 말은 밖으로
 * 나가지 않고 뽑은 결과만 서버로 간다.
 *
 * 진료 전 문답([extractTurn])은 아직 화면에 붙지 않았다. 프롬프트에 "지금 묻는 축"이 들어가야
 * 하는데 백엔드 응답에 그 값이 없다(#289).
 */
interface OnDeviceExtractor {
    /** 이 기기에서 쓸 수 있는지. 모델이 없거나 엔진이 없으면 false다. */
    suspend fun available(): Boolean

    /**
     * 진료 전 한 턴.
     *
     * @param user 벡터의 `user` 형식 그대로. 질문 맥락과 환자 답을 합친 한 덩어리다.
     */
    suspend fun extractTurn(user: String): OnDeviceResult<TurnExtraction>

    /**
     * 진료 후 메모 분류.
     *
     * 문장은 서버가 나눈 것을 그대로 받는다. 폰이 나누면 폰과 서버가 다른 번호를 가리킨다.
     */
    suspend fun labelMemo(sentences: List<String>): OnDeviceResult<Map<Int, MemoLabel>>
}

/**
 * 추출 결과.
 *
 * 실패를 예외가 아니라 값으로 다룬다. 네트워크와 같은 이유다. 부르는 쪽이 무엇이 날아올지
 * 타입으로 알아야 폴백을 빠뜨리지 않는다.
 */
sealed interface OnDeviceResult<out T> {
    data class Success<out T>(val value: T) : OnDeviceResult<T>

    /**
     * 폰에서 못 돌렸다. 서버 추출로 넘어간다.
     *
     * 미지원 기기·모델 미다운로드·시간 초과가 모두 여기다. 앱은 실패 판단만 하고 이유를
     * 화면에 적지 않는다. 사용자가 고를 수 있는 것이 아니다.
     */
    data object Unavailable : OnDeviceResult<Nothing>

    /**
     * 모델이 스키마를 어겼다.
     *
     * 이것은 폴백이 아니라 계약 위반이다. 온도 0에 스키마를 강제하는데도 어긋났다면 묶음
     * 버전이 맞지 않거나 엔진이 다른 것이다. AI 트랙에 알릴 일이라 따로 나눈다.
     */
    data class Malformed(val raw: String, val cause: Throwable) : OnDeviceResult<Nothing>
}

/**
 * 엔진으로 돌리는 구현.
 *
 * 프롬프트와 문법은 자산에서 한 번 읽어 둔다. 꺼져 있거나 모델을 다 받지 않았으면 엔진을 열지
 * 않고 [OnDeviceResult.Unavailable]이다.
 */
internal class LlamaOnDeviceExtractor
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val model: OnDeviceModel,
    private val engine: OnDeviceEngine,
) : OnDeviceExtractor {
    private val settings = OnDeviceSettings()

    private val extractPrompt by lazy { asset(OnDeviceBundle.EXTRACT_PROMPT_ASSET) }
    private val extractGrammar by lazy { asset(OnDeviceBundle.EXTRACT_GRAMMAR_ASSET) }
    private val memoPrompt by lazy { asset(OnDeviceBundle.MEMO_PROMPT_ASSET) }

    override suspend fun available(): Boolean = model.readyFile() != null

    override suspend fun extractTurn(user: String): OnDeviceResult<TurnExtraction> =
        run(ChatPrompt.of(extractPrompt, user), extractGrammar) { parseTurnExtraction(it) }

    override suspend fun labelMemo(sentences: List<String>): OnDeviceResult<Map<Int, MemoLabel>> {
        if (sentences.isEmpty()) return OnDeviceResult.Success(emptyMap())
        return run(ChatPrompt.of(memoPrompt, memoUserMessage(sentences)), memoLabelsGrammar(sentences.size)) {
            parseMemoLabels(it, sentences.size)
        }
    }

    private suspend fun <T> run(
        prompt: ChatPrompt,
        grammar: String,
        parse: (String) -> OnDeviceResult<T>,
    ): OnDeviceResult<T> {
        val raw = model.readyFile()?.let { engine.generate(it, prompt, grammar, settings.maxTokens) }
        return raw?.let(parse) ?: OnDeviceResult.Unavailable
    }

    private fun asset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * 모델이 뱉은 글을 [TurnExtraction]으로 읽는다.
 *
 * 스키마를 강제해도 파싱은 해야 한다. 강제가 걸리는 것은 모양이고, 그것을 우리 타입으로
 * 옮기다 어긋나면 여기서 잡힌다.
 */
internal fun parseTurnExtraction(raw: String): OnDeviceResult<TurnExtraction> = runCatching {
    onDeviceJson.decodeFromString<TurnExtraction>(raw)
}.fold(
    onSuccess = { OnDeviceResult.Success(it) },
    onFailure = { OnDeviceResult.Malformed(raw, it) },
)

/**
 * 번호 키 객체를 라벨 표로 읽는다.
 *
 * 키가 `"0"`~`"N-1"` 문자열이라 그대로는 쓸 수 없다. 번호가 비거나 문장 수와 다르면 계약
 * 위반으로 본다. 서버가 그 번호로 문장을 찾기 때문에 하나라도 빠지면 조립이 어긋난다.
 */
internal fun parseMemoLabels(raw: String, sentenceCount: Int): OnDeviceResult<Map<Int, MemoLabel>> = runCatching {
    val labels = onDeviceJson.decodeFromString<JsonObject>(raw)
    val byIndex =
        (0 until sentenceCount).associateWith { index ->
            val name = requireNotNull(labels[index.toString()]) { "$index 번 라벨이 없다" }.jsonPrimitive.content
            requireNotNull(MemoLabel.entries.firstOrNull { it.wireName == name }) { "모르는 라벨 $name" }
        }
    require(labels.size == sentenceCount) { "문장 ${sentenceCount}개인데 라벨 ${labels.size}개" }
    byIndex
}.fold(
    onSuccess = { OnDeviceResult.Success(it) },
    onFailure = { OnDeviceResult.Malformed(raw, it) },
)
