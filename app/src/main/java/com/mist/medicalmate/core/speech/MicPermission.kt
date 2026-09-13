package com.mist.medicalmate.core.speech

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 마이크 권한을 묻고 누를 자리를 돌려준다.
 *
 * **누를 때 묻는다.** 화면을 열자마자 물으면 글로 적고 나가는 사람에게 이유 없는 권한 창이
 * 뜬다. 마이크를 누른 것이 곧 왜 묻는지에 대한 답이다.
 *
 * 권한 요청은 Activity가 있어야 해서 화면이 맡는다. ViewModel에 `Context`를 넣지 않는다는
 * 규칙과 같은 이유다.
 */
@Composable
fun rememberMicPermission(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val granted = rememberUpdatedState(onGranted)
    val denied = rememberUpdatedState(onDenied)

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            if (allowed) granted.value() else denied.value()
        }

    return remember(context) {
        {
            val already =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            if (already) granted.value() else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
