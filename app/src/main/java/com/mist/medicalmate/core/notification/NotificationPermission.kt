package com.mist.medicalmate.core.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 알림 권한을 묻는 자리를 돌려준다(#268).
 *
 * **알림이 필요해지는 순간에 묻는다.** 일정을 저장할 때와 진료 알림 토글을 켤 때다. 앱을
 * 열자마자 물으면 무엇을 알리겠다는 것인지 모르는 채로 거절하게 된다. 마이크 권한과 같은
 * 원칙이다.
 *
 * 결과를 기다리지 않는다. 거절해도 일정은 저장되고 토글도 켜진 채다 — 그 사람이 나중에
 * 시스템 설정에서 알림을 켜면 걸어 둔 예약이 그대로 뜬다. 뜰 때 권한을 다시 보므로 거절한
 * 사람에게 억지로 띄우지도 않는다.
 *
 * Android 13 미만은 권한이 없어 아무것도 하지 않는다. 두 번 거절하면 시스템이 창을 더 띄우지
 * 않으므로 같은 자리에서 여러 번 불러도 귀찮게 하지 않는다.
 */
@Composable
fun rememberNotificationPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
