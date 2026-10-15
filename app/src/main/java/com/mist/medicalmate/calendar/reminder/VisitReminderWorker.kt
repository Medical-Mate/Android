package com.mist.medicalmate.calendar.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mist.medicalmate.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 진료 알림을 띄운다. 진료 전 알림과 진료 후 기록 알림(#316)이 같은 일꾼이다.
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
        val date = inputData.getString(KEY_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val time = inputData.getString(KEY_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val kind =
            VisitReminderKind.entries.firstOrNull { it.name == inputData.getString(KEY_KIND) }
                ?: VisitReminderKind.BEFORE_VISIT
        show(appointmentId, date, kind, text(applicationContext, kind, title, time))
        return Result.success()
    }

    private fun show(appointmentId: Long, date: LocalDate?, kind: VisitReminderKind, text: Pair<String, String>) {
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
                .setContentTitle(text.first)
                .setContentText(text.second)
                .setContentIntent(openDay(context, appointmentId, date, notificationId(appointmentId, kind)))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        NotificationManagerCompat.from(context).notify(notificationId(appointmentId, kind), notification)
    }

    internal companion object {
        const val KEY_APPOINTMENT_ID = "appointmentId"
        const val KEY_DATE = "date"
        const val KEY_TITLE = "title"
        const val KEY_TIME = "time"
        const val KEY_KIND = "kind"
    }
}

/**
 * 알림 제목과 본문(#316).
 *
 * 진료 후 기록 알림은 홈 배너의 오늘 진료 문구(시안 1n-1 배너 1)를 쓴다. 알림 자체의 시안은 없다. 진료 전 알림은
 * 전과 같다 — 시각을 정한 일정은 24시간 전이라 "내일 진료가 있어요"가 그대로 맞는다.
 */
private fun text(context: Context, kind: VisitReminderKind, title: String, time: LocalTime?): Pair<String, String> =
    when (kind) {
        VisitReminderKind.BEFORE_VISIT -> context.getString(R.string.visit_reminder_title) to body(context, title, time)
        VisitReminderKind.AFTER_VISIT ->
            context.getString(R.string.visit_record_reminder_title) to
                context.getString(R.string.visit_record_reminder_body)
    }

/** 일정 하나에 알림이 둘이라 종류마다 번호를 달리한다. 같으면 뒤에 뜬 것이 앞의 것을 덮는다. */
private fun notificationId(appointmentId: Long, kind: VisitReminderKind): Int =
    (appointmentId * 2 + kind.ordinal).hashCode()

/**
 * 진료 전 알림 본문. "서울OO병원 내과 재진 · 오전 10:30"이다.
 *
 * 제목이 비면 병원도 카드도 없이 만든 일정이다. 그때는 무엇인지 적을 수 없어 일정을 열어
 * 보라고만 적는다. 시간 미정이면 시각을 빼고 일정 이름만 적는다.
 */
private fun body(context: Context, title: String, time: LocalTime?): String {
    val name = title.ifBlank { context.getString(R.string.visit_reminder_untitled) }
    return if (time == null) name else context.getString(R.string.visit_reminder_body_timed, name, time.format(TIME))
}

/**
 * 알림을 누르면 그 일정의 일자 화면(1r-2)을 연다.
 *
 * 앱의 시작 인텐트에 [VisitReminderLink]를 싣는다. `MainActivity`를 이름으로 부르지 않는 것은
 * 도메인이 앱 진입점을 참조하지 않게 하려는 것이다. 열려 있던 앱이면 그 Activity가 새
 * 인텐트를 받도록 위에 쌓인 것을 걷어낸다 — 새 Activity가 하나 더 뜨면 세션 확인과
 * 스플래시를 다시 거친다.
 *
 * 요청 코드를 일정마다 다르게 둔다. 같으면 시스템이 앞서 만든 `PendingIntent`의 내용을
 * 덮어서, 알림이 둘일 때 어느 쪽을 눌러도 같은 날이 열린다.
 *
 * 날짜를 못 읽은 예약이면 앱만 연다. 세션 확인을 거쳐 홈으로 간다.
 */
private fun openDay(context: Context, appointmentId: Long, date: LocalDate?, requestCode: Int): PendingIntent? {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    val intent =
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).also { intent ->
            date?.let { VisitReminderLink(it, appointmentId).writeTo(intent) }
        }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
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
