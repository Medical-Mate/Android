package com.mist.medicalmate.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate

/**
 * debug 전용. 진료 알림 네 가지(진료 전 · 진료 후 기록 × 시각 있음 · 미정)를 지금 띄운다.
 *
 * 예약을 기다리지 않고 앱이 쓰는 일꾼([VisitReminderWorker])에 견본 값을 넣어 바로 돌린다. 문구를 기기에서
 * 확인하는 용도다.
 *
 * ```
 * adb shell am broadcast -n com.mist.medicalmate/.calendar.reminder.ReminderPreviewReceiver
 * ```
 */
class ReminderPreviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val today = LocalDate.now()
        val samples =
            listOf(
                Sample(PREVIEW_ID, today.plusDays(1), "10:30", VisitReminderKind.BEFORE_VISIT),
                Sample(PREVIEW_ID + 1, today.plusDays(1), null, VisitReminderKind.BEFORE_VISIT),
                Sample(PREVIEW_ID + 2, today, "10:30", VisitReminderKind.AFTER_VISIT),
                Sample(PREVIEW_ID + 3, today, null, VisitReminderKind.AFTER_VISIT),
            )
        val workManager = WorkManager.getInstance(context)
        samples.forEach { sample ->
            val request =
                OneTimeWorkRequestBuilder<VisitReminderWorker>()
                    .setInputData(
                        workDataOf(
                            VisitReminderWorker.KEY_APPOINTMENT_ID to sample.id,
                            VisitReminderWorker.KEY_DATE to sample.on.toString(),
                            VisitReminderWorker.KEY_TIME to sample.time,
                            VisitReminderWorker.KEY_KIND to sample.kind.name,
                            VisitReminderWorker.KEY_CLINIC to "서울OO병원 내과",
                        ),
                    ).build()
            workManager.enqueue(request)
        }
    }

    private class Sample(val id: Long, val on: LocalDate, val time: String?, val kind: VisitReminderKind)

    private companion object {
        const val PREVIEW_ID = 900_001L
    }
}
