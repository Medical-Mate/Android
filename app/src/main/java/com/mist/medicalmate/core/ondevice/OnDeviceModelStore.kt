package com.mist.medicalmate.core.ondevice

import android.content.Context
import android.util.Log
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** 모델 파일이 이 기기에 어디까지 와 있는지. */
sealed interface OnDeviceModelState {
    data object Absent : OnDeviceModelState

    /** @param progress 0~1. 다 받고 해시를 맞추는 동안은 1에 머문다. */
    data class Downloading(val progress: Float) : OnDeviceModelState

    data object Ready : OnDeviceModelState

    /** 받다가 끊겼거나 받은 것이 어긋났거나 자리가 모자랐다. 다시 받을 수 있다. */
    data object Failed : OnDeviceModelState
}

/**
 * 온디바이스 모델(1.24GB)을 받아 둔다.
 *
 * **앱에 넣지 않고 켠 사람만 받는다.** 쓰지 않을 사람의 저장 공간을 1GB 넘게 차지할 수 없다.
 * 음성 호출 모델(`WakeWordModelStore`)과 같은 생각이고 크기만 다르다.
 *
 * **끊긴 자리부터 이어 받는다.** 1GB가 넘어 휴대폰에서는 한 번에 받지 못하는 일이 흔하다. 받는
 * 곳이 이어받기(Range)를 지원한다(AI 문서). 받던 파일은 `.partial`로 두고, 다 받으면 해시를 맞춘
 * 뒤에야 제 이름으로 옮긴다. 어긋난 파일을 "있는 것"으로 두면 원인 모를 오분류가 된다.
 *
 * `noBackupFilesDir`에 둔다. 클라우드 백업으로 1GB가 올라가면 안 된다.
 *
 * 받기는 화면이 아니라 이 객체의 범위에서 돈다. 토글을 켜고 내 정보를 나가도 끊기지 않아야 한다.
 */
@Singleton
class OnDeviceModelStore
@Inject
constructor(@ApplicationContext context: Context) {
    private val root = File(context.noBackupFilesDir, ROOT_DIR)
    private val modelFile = File(root, OnDeviceBundle.MODEL_FILE)
    private val partialFile = File(root, OnDeviceBundle.MODEL_FILE + PARTIAL_SUFFIX)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val mutableState =
        MutableStateFlow(if (modelFile.isFile) OnDeviceModelState.Ready else OnDeviceModelState.Absent)
    val state: StateFlow<OnDeviceModelState> = mutableState.asStateFlow()

    init {
        scope.launch { removeOtherModels() }
    }

    /** 받아 둔 모델 파일. 아직 없으면 null이다. */
    fun file(): File? = modelFile.takeIf { mutableState.value == OnDeviceModelState.Ready }

    /** 받는다. 이미 있거나 받는 중이면 아무 일도 하지 않는다. */
    fun download() {
        val current = mutableState.value
        if (current == OnDeviceModelState.Ready || current is OnDeviceModelState.Downloading) return
        mutableState.value = OnDeviceModelState.Downloading(0f)
        job = scope.launch {
            mutableState.value =
                try {
                    fetch()
                    OnDeviceModelState.Ready
                } catch (e: IOException) {
                    Log.w(TAG, "모델을 받지 못했다: ${e.message}")
                    OnDeviceModelState.Failed
                }
        }
    }

    /** 받던 것을 멈추고 지운다. 받다 만 것도 지운다. */
    suspend fun delete() {
        job?.cancel()
        job?.join()
        job = null
        withContext(Dispatchers.IO) { root.deleteRecursively() }
        mutableState.value = OnDeviceModelState.Absent
    }

    /** 묶음이 바뀌어 이름이 달라진 옛 모델을 지운다. 1GB씩 남으면 안 된다. */
    private fun removeOtherModels() {
        root.listFiles()
            ?.filter { it.name != modelFile.name && it.name != partialFile.name }
            ?.forEach { stale ->
                Log.i(TAG, "쓰지 않는 ${stale.name}을 지운다")
                stale.deleteRecursively()
            }
    }

    private suspend fun fetch() {
        ensureSpace()
        fetchRemainder()
        mutableState.value = OnDeviceModelState.Downloading(1f)
        verifyAndPlace()
    }

    private fun ensureSpace() {
        root.mkdirs()
        val needed = OnDeviceBundle.MODEL_SIZE_BYTES - partialFile.length()
        if (root.usableSpace < needed + SPARE_BYTES) throw IOException("자리가 모자라다")
    }

    /** 해시가 맞아야 제 이름으로 옮긴다. 이어 붙인 것이 어긋났으면 다음에 처음부터 받도록 지운다. */
    private suspend fun verifyAndPlace() {
        if (digestOf(partialFile) != OnDeviceBundle.MODEL_SHA256) {
            partialFile.delete()
            throw IOException("모델 해시가 다르다")
        }
        if (!partialFile.renameTo(modelFile)) throw IOException("모델 파일을 옮기지 못했다")
    }

    /**
     * 받은 만큼 뒤부터 이어 받는다.
     *
     * 서버가 이어받기를 거절하고 처음부터 보내면(200) 받던 것을 버리고 새로 쓴다.
     */
    private suspend fun fetchRemainder() {
        val offset = partialFile.length()
        if (offset >= OnDeviceBundle.MODEL_SIZE_BYTES) return
        val connection = URL(OnDeviceBundle.MODEL_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
            val append =
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_PARTIAL -> true
                    HttpURLConnection.HTTP_OK -> false
                    else -> throw IOException("모델 응답 ${connection.responseCode}")
                }
            val start = if (append) offset else 0L
            connection.inputStream.use { input ->
                FileOutputStream(partialFile, append).use { output ->
                    copy(input, start) { buffer, read -> output.write(buffer, 0, read) }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (partialFile.length() != OnDeviceBundle.MODEL_SIZE_BYTES) throw IOException("받다가 끊겼다")
    }

    /** 백분율이 바뀔 때만 상태를 고친다. 조각마다 고치면 화면이 초당 수백 번 다시 그린다. */
    private suspend fun copy(input: InputStream, start: Long, sink: (ByteArray, Int) -> Unit) {
        val buffer = ByteArray(BUFFER_BYTES)
        var done = start
        var percent = -1
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            sink(buffer, read)
            done += read
            val now = (done * PERCENT / OnDeviceBundle.MODEL_SIZE_BYTES).toInt().coerceAtMost(PERCENT - 1)
            if (now != percent) {
                percent = now
                mutableState.value = OnDeviceModelState.Downloading(now / PERCENT.toFloat())
            }
        }
    }

    private suspend fun digestOf(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "OnDeviceModelStore"
        const val ROOT_DIR = "ondevice"
        const val PARTIAL_SUFFIX = ".partial"

        /** 다 받고도 남겨야 하는 자리. 꽉 찬 저장소에서 1GB를 채우면 다른 앱이 멈춘다. */
        const val SPARE_BYTES = 500L * 1024 * 1024

        const val TIMEOUT_MS = 30_000
        const val BUFFER_BYTES = 256 * 1024
        const val PERCENT = 100
    }
}
