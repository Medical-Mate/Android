package com.mist.medicalmate.core.speech

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/** 호출어 모델이 이 기기에 어디까지 와 있는지. */
sealed interface WakeWordModelState {
    data object Absent : WakeWordModelState

    /** @param progress 0~1. 크기를 모르면 0에 머문다. */
    data class Downloading(val progress: Float) : WakeWordModelState

    data object Ready : WakeWordModelState

    /** 받다가 끊겼거나 받은 것이 어긋났다. 다시 받을 수 있다. */
    data object Failed : WakeWordModelState
}

/**
 * Vosk 영어 소형 모델(`vosk-model-small-en-us-0.15`, Apache 2.0)을 받아 둔다.
 *
 * **앱에 넣지 않고 켠 사람만 받는다.** 압축 41MB에 풀면 68MB다. 쓰지도 않을 사람의 저장
 * 공간을 차지할 수 없다. 받아쓰기 모델을 처음 쓸 때 받는 것과 같은 생각이다.
 *
 * 처음에는 한국어 모델(`vosk-model-small-ko-0.22`, 풀면 264MB)이었다(#278). 그것을 받아 둔
 * 기기에서는 같은 폴더의 다른 모델을 지운다. 그대로 두면 아무도 쓰지 않는 264MB가 남는다.
 *
 * 받는 곳은 Vosk 배포처(alphacephei.com)다. 출시 전에 Play Asset Delivery나 우리 저장소로
 * 옮길지 정한다(#273). 옮겨도 체크섬이 같으면 이 파일의 주소만 바꾸면 된다.
 *
 * **받은 것은 체크섬으로 확인한다.** 중간에 바뀐 파일을 그대로 풀면 원인을 알 수 없는 오인식이
 * 된다. 압축은 임시 폴더에 풀고 다 풀린 뒤에 제자리로 옮긴다. 풀다가 앱이 죽으면 반쯤 풀린
 * 모델이 "있는 것"으로 남기 때문이다.
 *
 * 받기는 화면이 아니라 이 객체의 범위에서 돈다. 토글을 켜고 내 정보를 나가도 끊기지 않아야 한다.
 */
@Singleton
internal class WakeWordModelStore
@Inject
constructor(
    @ApplicationContext context: Context,
    private val trace: VoiceWakeTrace,
) {
    private val root = File(context.noBackupFilesDir, ROOT_DIR)
    private val modelDir = File(root, MODEL_NAME)
    private val cacheDir = context.cacheDir

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val mutableState =
        MutableStateFlow(if (modelDir.isDirectory) WakeWordModelState.Ready else WakeWordModelState.Absent)
    val state: StateFlow<WakeWordModelState> = mutableState.asStateFlow()

    init {
        scope.launch { removeOtherModels() }
    }

    /** 받아 둔 모델의 자리. 아직 없으면 null이다. */
    fun directory(): File? = modelDir.takeIf { mutableState.value == WakeWordModelState.Ready }

    /** 받는다. 이미 있거나 받는 중이면 아무 일도 하지 않는다. */
    fun download() {
        val current = mutableState.value
        if (current == WakeWordModelState.Ready || current is WakeWordModelState.Downloading) return
        mutableState.value = WakeWordModelState.Downloading(0f)
        trace.log { "모델: 받기 시작 ($MODEL_URL)" }
        job = scope.launch {
            mutableState.value =
                try {
                    fetch()
                    trace.log { "모델: 받아서 풀었다 (${modelDir.path})" }
                    WakeWordModelState.Ready
                } catch (e: IOException) {
                    trace.log { "모델: 받지 못했다 (${e.message})" }
                    WakeWordModelState.Failed
                }
        }
    }

    /** 받던 것을 멈추고 지운다. "음성 모델 지우기"를 누르면 68MB를 돌려준다. */
    suspend fun delete() {
        job?.cancel()
        job?.join()
        job = null
        withContext(Dispatchers.IO) { root.deleteRecursively() }
        mutableState.value = WakeWordModelState.Absent
        trace.log { "모델: 지웠다" }
    }

    /** 지금 모델이 아닌 폴더를 지운다. 이전 한국어 모델과 그것을 풀다 만 것이다. */
    private fun removeOtherModels() {
        root.listFiles()
            ?.filter { !it.name.startsWith(MODEL_NAME) }
            ?.forEach { stale ->
                trace.log { "모델: 쓰지 않는 ${stale.name}을 지운다" }
                stale.deleteRecursively()
            }
    }

    private suspend fun fetch() {
        val archive = File(cacheDir, "$MODEL_NAME.zip")
        try {
            download(archive)
            val staging = File(root, "$MODEL_NAME.partial")
            staging.deleteRecursively()
            unzip(archive, staging)
            modelDir.deleteRecursively()
            if (!staging.renameTo(modelDir)) throw IOException("모델 폴더를 옮기지 못했다")
        } finally {
            archive.delete()
        }
    }

    private suspend fun download(target: File) {
        val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("모델 응답 ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: MODEL_BYTES
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    copy(input, total) { buffer, read ->
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != MODEL_SHA256) throw IOException("모델 체크섬이 다르다")
        } finally {
            connection.disconnect()
        }
    }

    /** 백분율이 바뀔 때만 상태를 고친다. 조각마다 고치면 화면이 초당 수백 번 다시 그린다. */
    private suspend fun copy(input: InputStream, total: Long, sink: (ByteArray, Int) -> Unit) {
        val buffer = ByteArray(BUFFER_BYTES)
        var done = 0L
        var percent = -1
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            sink(buffer, read)
            done += read
            val now = (done * PERCENT / total).toInt().coerceAtMost(PERCENT)
            if (now != percent) {
                percent = now
                mutableState.value = WakeWordModelState.Downloading(now / PERCENT.toFloat())
                if (now % TRACE_EVERY_PERCENT == 0) trace.log { "모델: $now%" }
            }
        }
    }

    /**
     * 압축을 푼다. 맨 앞 폴더(`vosk-model-small-en-us-0.15/`)는 떼고 [target] 바로 아래에 둔다.
     *
     * 항목 이름이 `../`로 [target] 밖을 가리키면 멈춘다. 받은 파일이 앱의 다른 파일을 덮지
     * 않게 한다.
     */
    private suspend fun unzip(archive: File, target: File) {
        val base = target.canonicalPath + File.separator
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            for (entry in generateSequence { zip.nextEntry }) {
                currentCoroutineContext().ensureActive()
                extract(zip, entry, target, base)
            }
        }
    }

    private fun extract(zip: ZipInputStream, entry: ZipEntry, target: File, base: String) {
        val relative = entry.name.substringAfter('/', missingDelimiterValue = "")
        if (relative.isEmpty()) return
        val file = File(target, relative)
        if (!file.canonicalPath.startsWith(base)) throw IOException("압축 밖을 가리킨다: ${entry.name}")
        if (entry.isDirectory) {
            file.mkdirs()
        } else {
            file.parentFile?.mkdirs()
            file.outputStream().use { zip.copyTo(it) }
        }
    }

    private companion object {
        const val ROOT_DIR = "wake-word"
        const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        const val MODEL_URL = "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"

        /** 2026-10-12에 받은 파일의 값이다. 배포처가 같은 이름으로 다른 파일을 올리면 받기가 실패한다. */
        const val MODEL_SHA256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"

        /** 응답에 길이가 없을 때 진행률을 셀 기준. 위 파일의 크기다. */
        const val MODEL_BYTES = 41_205_931L

        const val TIMEOUT_MS = 30_000
        const val BUFFER_BYTES = 64 * 1024
        const val PERCENT = 100
        const val TRACE_EVERY_PERCENT = 10
    }
}
