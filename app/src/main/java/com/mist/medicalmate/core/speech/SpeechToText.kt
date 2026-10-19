package com.mist.medicalmate.core.speech

import android.content.Context
import android.os.Build
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 기기 안에서 도는 음성 인식.
 *
 * ML Kit GenAI다. Android의 `SpeechRecognizer`가 아니고, **오디오가 기기 밖으로 나가지
 * 않는다.** 진료에서 들은 말을 다루는 앱이라 그 점이 이 선택의 이유다.
 *
 * 두 화면이 쓴다 — 증상 문답(1c-3·1c-4)과 진료 후 메모(1p). 한 도메인이 다른 도메인을
 * 참조하지 않으므로 `core`에 둔다.
 *
 * **마이크는 두 방식이다**(#328). 지금은 `AudioSource.fromMic()`에 맡긴다. debug 빌드에서 내 정보의 토글을 켜면
 * 앱이 마이크를 잡아 다듬은 소리([CleanMic])를 `AudioSource.fromPfd`로 넘긴다. 어느 쪽이 나은지 기기에서 비교한
 * 뒤 하나만 남긴다. 다듬기 효과는 문서가 보증하지 않는다 — 문서는 `fromMic()`을 권한다.
 */
interface SpeechToText {
    /**
     * 이 기기에서 쓸 수 있는지.
     *
     * Basic 모드가 API 31 이상이고, 그 위여도 AICore를 못 쓰는 기기가 있다. 쓸 수 없으면
     * 화면이 마이크를 그리지 않는다 — 눌러 봐야 안 되는 버튼을 두는 것보다 낫다.
     */
    suspend fun available(): Boolean

    /**
     * 지금 누군가 받아쓰고 있는지.
     *
     * 호출어 감지기(#273)가 이것을 보고 마이크를 내준다. 두 쪽이 한 마이크를 같이 잡으면
     * 기기에 따라 한쪽이 무음을 받는다.
     */
    val active: StateFlow<Boolean>

    /**
     * 받아쓰기를 시작한다. 흐름이 끝날 때까지 듣는다.
     *
     * 모델이 없으면 먼저 받는다. 흘러나오는 값은 [SpeechChunk]이고, 부분 결과는 여러 번
     * 최종 결과는 한 번 온다.
     */
    fun listen(): Flow<SpeechChunk>

    /** 듣기를 멈춘다. [listen]의 흐름이 끝난다. */
    suspend fun stop()
}

/**
 * 받아쓴 한 토막.
 *
 * [Preparing]은 모델을 받는 중이다. 처음 쓸 때 한 번 나오고 그 뒤로는 없다.
 *
 * [Partial]은 말하는 중에 계속 갱신되는 값이라 **앞선 것을 갈아끼워야 한다.** 이어 붙이면
 * 같은 말이 여러 번 쌓인다.
 */
sealed interface SpeechChunk {
    data object Preparing : SpeechChunk

    data class Partial(val text: String) : SpeechChunk

    data class Final(val text: String) : SpeechChunk

    /** 못 알아들었거나 기기가 거절했다. 화면은 적던 글을 그대로 두고 패널만 닫는다. */
    data object Failed : SpeechChunk
}

internal class MlKitSpeechToText
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val cleanMic: CleanMicSetting,
    private val trace: VoiceWakeTrace,
) : SpeechToText {
    private var recognizer: SpeechRecognizer? = null

    private val listening = MutableStateFlow(false)

    override val active: StateFlow<Boolean> = listening.asStateFlow()

    override suspend fun available(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching {
            when (client().checkStatus()) {
                FeatureStatus.AVAILABLE, FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> true
                else -> false
            }
        }.getOrDefault(false)
    }

    override fun listen(): Flow<SpeechChunk> = flow {
        // 화면이 available()을 보고 마이크를 그리지만 여기서도 막는다. 그 확인과 이 호출
        // 사이가 벌어져 있고, 막지 않으면 API 31 미만에서 없는 것을 부른다.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            emit(SpeechChunk.Failed)
            return@flow
        }
        val client = client()
        val ready =
            runCatching { client.checkStatus() == FeatureStatus.AVAILABLE }.getOrDefault(false)
        if (!ready) {
            // 처음 쓸 때 한 번이다. 화면을 열 때가 아니라 여기서 받는 이유는, 미리 받으면
            // 쓰지도 않을 사람의 데이터를 쓰기 때문이다.
            emit(SpeechChunk.Preparing)
            if (!download(client)) {
                emit(SpeechChunk.Failed)
                return@flow
            }
        }
        // 다듬는 방식이면 앱이 마이크를 잡는다(#328). 못 잡으면 지금 방식으로 듣는다.
        val mic = if (cleanMic.enabled.first()) CleanMic.open(context) else null
        trace.log { "받아쓰기: ${if (mic == null) "fromMic" else "다듬어 넘김"}" }
        val source = mic?.let { AudioSource.fromPfd(it.input) } ?: AudioSource.fromMic()
        val request = speechRecognizerRequest { audioSource = source }
        listening.value = true
        try {
            coroutineScope {
                val pump = mic?.let { launch(Dispatchers.IO) { it.pump() } }
                try {
                    client.startRecognition(request).collect { response -> response.toChunk()?.let { emit(it) } }
                } finally {
                    mic?.close()
                    pump?.cancel()
                }
            }
        } finally {
            mic?.let { trace.log { "받아쓰기: ${it.summary()}" } }
            listening.value = false
        }
    }

    override suspend fun stop() {
        runCatching { recognizer?.stopRecognition() }
    }

    /** 모델을 받는다. 받아 둔 것이 있으면 부르지 않는다. */
    private suspend fun download(client: SpeechRecognizer): Boolean = runCatching {
        var done = false
        client.download().collect { status ->
            if (status is DownloadStatus.DownloadCompleted) done = true
        }
        done
    }.getOrDefault(false)

    private fun client(): SpeechRecognizer = recognizer ?: SpeechRecognition
        .getClient(
            speechRecognizerOptions {
                locale = Locale.KOREA
                // Advanced는 일부 기기 전용이라 쓰지 않는다. Basic이 갤럭시에서 동작한다.
                preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
            },
        )
        .also { recognizer = it }
}

private fun SpeechRecognizerResponse.toChunk(): SpeechChunk? = when (this) {
    is SpeechRecognizerResponse.PartialTextResponse -> SpeechChunk.Partial(text)
    is SpeechRecognizerResponse.FinalTextResponse -> SpeechChunk.Final(text)
    is SpeechRecognizerResponse.ErrorResponse -> SpeechChunk.Failed
    else -> null
}
