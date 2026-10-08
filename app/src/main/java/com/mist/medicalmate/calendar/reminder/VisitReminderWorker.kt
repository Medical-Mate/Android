package com.mist.medicalmate.calendar.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mist.medicalmate.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 진료 전날 알림을 띄운다.
 *
 * 띄울 내용을 예약할 때 입력으로 실어 둔다. 뜰 때 서버를 다시 읽지 않는 이유는 그 시각에
 * 연결이 있다는 보장이 없어서다. 예약 뒤에 일정이 바뀌면 그 자리에서 예약이 갈아끼워지므로
 * 실린 값이 낡지 않는다.
 *
 * Hilt를 쓰지 않는다. 주입받을 것이 없어 WorkManager의 기본 생성 방식으로 충분하다.
 *
 * **권한이 없으면 띄우지 않고 끝낸다.** Android 13부터 알림이 권한이고, 거절한 사람에게
 * 띄우려 하면 시스템이 버린다. 실패로 돌려 재시도하게 하면 같은 거절을 되풀이할 뿐이다.
 */
internal class VisitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appointmentId = inputData.getLong(KEY_APPOINTMENT_ID, 0L)
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val time = inputData.getString(KEY_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        show(appointmentId, title, time)
        return Result.success()
    }

    private fun show(appointmentId: Long, title: String, time: LocalTime?) {
        val context = applicationContext
        // lint가 권한 확인을 알아보려면 호출과 같은 함수 안에 있어야 한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(context.getString(R.string.visit_reminder_title))
                .setContentText(body(context, title, time))
                .setContentIntent(openApp(context))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        NotificationManagerCompat.from(context).notify(appointmentId.hashCode(), notification)
    }

    internal companion object {
        const val KEY_APPOINTMENT_ID = "appointmentId"
        const val KEY_TITLE = "title"
        const val KEY_TIME = "time"
    }
}

/**
 * 알림 본문. "서울OO병원 내과 재진 · 오전 10:30"이다.
 *
 * 제목이 비면 병원도 카드도 없이 만든 일정이다. 그때는 무엇인지 적을 수 없어 일정을 열어
 * 보라고만 적는다. 시간 미정이면 시각을 빼고 일정 이름만 적는다.
 */
private fun body(context: Context, title: String, time: LocalTime?): String {
    val name = title.ifBlank { context.getString(R.string.visit_reminder_untitled) }
    return if (time == null) name else context.getString(R.string.visit_reminder_body_timed, name, time.format(TIME))
}

/** 알림을 누르면 앱을 연다. 세션 확인을 거쳐 홈으로 간다. */
private fun openApp(context: Context): PendingIntent? {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}

/** 채널은 이미 있으면 그대로 둔다. 사용자가 시스템 설정에서 바꾼 중요도를 덮지 않는다. */
private fun ensureChannel(context: Context) {
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.visit_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.visit_reminder_channel_description) }
    context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
}

private const val CHANNEL_ID = "visit-reminder"

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
