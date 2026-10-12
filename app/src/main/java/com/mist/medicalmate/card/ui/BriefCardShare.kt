package com.mist.medicalmate.card.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.core.content.FileProvider
import com.mist.medicalmate.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 찍어 둔 브리핑 카드를 공유 시트로 보낸다(#275).
 *
 * 이미지를 앱 캐시(`cache/shared/`)에 쓰고 `FileProvider`로 내준다. 저장소 권한을 쓰지 않고 갤러리에도
 * 남지 않는다. 받는 앱에만 그 한 장을 읽을 권한을 준다.
 *
 * **카드에는 복용약·기저질환·알러지가 들어 있다.** 앱이 어디로 보내지 않는다. 사람이 공유 시트에서 고른
 * 앱으로만 나간다.
 *
 * 매번 같은 파일 이름에 덮어쓴다. 공유할 때마다 캐시에 카드가 쌓이지 않게 한다.
 *
 * 파일을 못 쓰면 아무것도 띄우지 않는다. 캐시가 가득 찬 드문 경우다.
 */
internal suspend fun shareBriefCard(context: Context, image: ImageBitmap) {
    val file = withContext(Dispatchers.IO) { runCatching { writePng(context, image) }.getOrNull() } ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = PNG_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            // 공유 시트의 미리보기와 받는 앱이 읽을 권한. ClipData에 실어야 시트가 그림을 보여 준다.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.brief_card_share_chooser)))
}

/**
 * 그리는 것을 [layer]에도 남긴다. 공유할 때 그 층을 이미지로 뽑는다(#275).
 *
 * 면색을 함께 담는다. 투명한 바탕으로 두면 받는 앱이 검게 그린다.
 */
internal fun Modifier.recordInto(layer: GraphicsLayer, surface: Color): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}.background(surface)

private fun writePng(context: Context, image: ImageBitmap): File {
    val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
    val file = File(dir, FILE_NAME)
    // 층에서 뽑은 그림은 하드웨어 비트맵일 수 있다. 픽셀을 읽어 파일로 쓰려면 메모리에 옮겨야 한다.
    val bitmap =
        image.asAndroidBitmap().let {
            if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
        }
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    return file
}

/** `res/xml/file_paths.xml`의 `cache-path`와 같아야 한다. */
private const val SHARED_DIR = "shared"
private const val FILE_NAME = "brief_card.png"
private const val PNG_TYPE = "image/png"

/** 매니페스트의 `FileProvider` authority 뒤쪽. 앞은 패키지 이름이다. */
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/** PNG는 무손실이라 이 값을 보지 않는다. API가 받는 자리라 둔다. */
private const val PNG_QUALITY = 100
