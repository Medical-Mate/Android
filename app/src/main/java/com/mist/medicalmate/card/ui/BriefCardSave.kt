package com.mist.medicalmate.card.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 찍어 둔 브리핑 카드를 갤러리에 저장하는 자리를 돌려준다(#306). 공유(#275)와 같은 이미지다.
 *
 * `Pictures/진료메이트` 앨범에 남긴다. 공유는 앱 캐시를 거쳐 받는 앱에만 내주지만 저장은 갤러리에 남는다 —
 * 카드에 복용약·기저질환·알러지가 들어 있어서 사람이 고른 때에만 저장한다.
 *
 * **Android 10부터는 권한이 없다.** `MediaStore`에 넣으면 앱이 만든 그림이라 묻지 않는다. 9 이하는
 * 저장소 쓰기 권한이 있어야 해서 그때만 묻고, 거절하면 저장하지 않는다.
 *
 * @param onResult 저장했는지. 화면이 결과를 알린다.
 */
@Composable
internal fun rememberBriefCardSaver(onResult: (Boolean) -> Unit): (ImageBitmap) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 권한을 묻는 동안 들고 있을 그림. 답이 오면 그것을 저장한다.
    var pending by remember { mutableStateOf<ImageBitmap?>(null) }
    val save = { image: ImageBitmap -> scope.launch { onResult(saveBriefCard(context, image)) } }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val image = pending
            pending = null
            when {
                image == null -> Unit
                granted -> save(image)
                else -> onResult(false)
            }
        }
    return { image ->
        if (needsStoragePermission(context)) {
            pending = image
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            save(image)
        }
    }
}

private fun needsStoragePermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
    PackageManager.PERMISSION_GRANTED

/** 저장한다. 못 쓰면 false다 — 공간이 모자라거나 권한이 없을 때다. */
private suspend fun saveBriefCard(context: Context, image: ImageBitmap): Boolean = withContext(Dispatchers.IO) {
    val bitmap = image.softwareBitmap()
    val name = "$FILE_PREFIX${LocalDateTime.now().format(FILE_TIME)}.png"
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertIntoMediaStore(context, name) { bitmap.writePng(it) }
        } else {
            writeToPictures(context, name) { bitmap.writePng(it) }
        }
    }.getOrDefault(false)
}

/**
 * Android 10 이상. 다 쓰기 전에는 갤러리에 보이지 않게 `IS_PENDING`으로 넣었다가 푼다. 쓰다 실패하면 빈 항목을
 * 지운다 — 남겨 두면 갤러리에 깨진 그림이 선다.
 */
private fun insertIntoMediaStore(context: Context, name: String, write: (OutputStream) -> Unit): Boolean {
    val resolver = context.contentResolver
    val values =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, PNG_MIME)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val written = resolver.openOutputStream(uri)?.use(write) != null
    if (written) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } else {
        resolver.delete(uri, null, null)
    }
    return written
}

/** Android 9 이하. 공용 사진 폴더에 쓰고 갤러리가 보도록 미디어 스캐너에 알린다. */
@Suppress("DEPRECATION") // getExternalStoragePublicDirectory는 10에서 막혔고 이 길은 9 이하에서만 탄다.
private fun writeToPictures(context: Context, name: String, write: (OutputStream) -> Unit): Boolean {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
    if (!dir.exists() && !dir.mkdirs()) return false
    val file = File(dir, name)
    file.outputStream().use(write)
    MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf(PNG_MIME), null)
    return true
}

/** 층에서 뽑은 그림은 하드웨어 비트맵일 수 있다. 픽셀을 읽어 쓰려면 메모리에 옮겨야 한다. */
private fun ImageBitmap.softwareBitmap(): Bitmap = asAndroidBitmap().let {
    if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
}

private fun Bitmap.writePng(out: OutputStream) {
    compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
}

/** 갤러리의 앨범 이름. 앱 이름과 같다. */
private const val ALBUM = "진료메이트"

/** 저장할 때마다 새 파일이다. 같은 카드를 두 번 저장해도 앞의 것을 덮지 않는다. */
private const val FILE_PREFIX = "브리핑카드_"
private val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
private const val PNG_MIME = "image/png"

/** PNG는 무손실이라 이 값을 보지 않는다. API가 받는 자리라 둔다. */
private const val PNG_QUALITY = 100
