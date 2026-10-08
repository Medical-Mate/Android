package com.mist.medicalmate.calendar.reminder

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.time.Clock
import java.time.Duration

/**
 * 알림을 기기에 건다.
 *
 * 무엇을 걸지는 [planVisitReminders]가 정하고 이쪽은 거는 일만 한다. 그 경계로 나눈 이유는
 * 거는 쪽이 Android에 묶여 JVM 시험이 닿지 않기 때문이다.
 */
internal interface ReminderScheduler {
    /** 걸린 것을 모두 지우고 [reminders]만 남긴다. */
    fun replaceAll(reminders: List<VisitReminder>)

    fun cancelAll()
}

/**
 * WorkManager로 건다.
 *
 * **재부팅해도 남는다.** WorkManager가 예약을 자기 저장소에 두고 부팅 뒤 다시 건다.
 * `AlarmManager`로 걸면 부팅 수신자를 따로 두고 예약을 앱이 어딘가에 들고 있어야 한다.
 *
 * **분 단위로 정확하지 않다.** 기기가 잠들어 있으면 시스템이 깨우는 창에 맞춰 늦게 돈다.
 * 진료 전날 저녁 알림이라 몇 분 늦어도 할 일은 같고, 정확한 알람은 Android 14부터 사용자가
 * 따로 허락해야 하는 권한이라 그 값을 치를 이유가 없다.
 *
 * 지우고 다시 거는 두 요청은 WorkManager가 받은 차례대로 처리한다.
 */
internal class WorkManagerReminderScheduler
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    override fun replaceAll(reminders: List<VisitReminder>) {
        workManager.cancelAllWorkByTag(VISIT_REMINDER_TAG)
        val now = clock.instant()
        reminders.forEach { reminder ->
            val fireAt = reminder.fireAt.atZone(clock.zone).toInstant()
            val request =
                OneTimeWorkRequestBuilder<VisitReminderWorker>()
                    .setInitialDelay(Duration.between(now, fireAt))
                    .setInputData(
                        workDataOf(
                            VisitReminderWorker.KEY_APPOINTMENT_ID to reminder.appointmentId,
                            VisitReminderWorker.KEY_TITLE to reminder.title,
                            VisitReminderWorker.KEY_TIME to reminder.time?.toString(),
                        ),
                    ).addTag(VISIT_REMINDER_TAG)
                    .build()
            workManager.enqueue(request)
        }
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(VISIT_REMINDER_TAG)
    }

    private companion object {
        const val VISIT_REMINDER_TAG = "visit-reminder"
    }
}
