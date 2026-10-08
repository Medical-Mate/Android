package com.mist.medicalmate.calendar.reminder

import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.core.model.VisitReminderSetting
import com.mist.medicalmate.core.model.VisitReminders
import com.mist.medicalmate.core.network.ApiResult
import dagger.Lazy
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDateTime

/**
 * [VisitReminders]의 구현.
 *
 * **다가오는 일정 전체를 읽어 통째로 갈아끼운다.** 바뀐 일정 하나만 고치려면 무엇이 바뀌었는지
 * 알아야 하는데, 다른 기기에서 고친 일정은 그 사실이 이 기기에 오지 않는다. 다시 읽는 것이
 * 맞추는 유일한 길이고 `GET /api/me/appointments/upcoming` 한 번이면 된다.
 *
 * **못 읽으면 손대지 않는다.** 연결이 끊긴 순간에 예약을 지우면 오프라인에서 연 앱이 알림을
 * 모두 날린다. 토글을 못 읽은 경우도 같다 — 끈 것인지 모르는 채로 지우지 않는다.
 *
 * 한 번에 하나만 돈다. 일정 저장과 화면 진입이 겹쳐 둘이 동시에 갈아끼우면 늦게 읽은 쪽이
 * 먼저 쓴 쪽을 덮을 수도, 그 반대일 수도 있다. 로그아웃의 [clear]도 같은 줄에 선다 —
 * 진행 중인 [refresh]가 지운 뒤에 다시 걸면 다음 계정에 이전 계정의 알림이 남는다.
 *
 * [AppointmentRepository]를 [Lazy]로 받는 이유는 그쪽이 일정을 저장할 때마다 이 객체를
 * 부르기 때문이다. 바로 받으면 둘이 서로를 만들어야 해서 그래프가 닫히지 않는다.
 */
@Singleton
internal class DefaultVisitReminders
@Inject
constructor(
    private val appointments: Lazy<AppointmentRepository>,
    private val setting: VisitReminderSetting,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : VisitReminders {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun refresh() {
        scope.launch { sync() }
    }

    override fun clear() {
        scope.launch { mutex.withLock { scheduler.cancelAll() } }
    }

    /** [refresh]의 본체. 시험이 기다릴 수 있게 따로 둔다. */
    internal suspend fun sync() = mutex.withLock {
        val enabled = (setting.enabled() as? ApiResult.Success)?.value ?: return@withLock
        if (!enabled) {
            scheduler.cancelAll()
            return@withLock
        }
        val upcoming = (appointments.get().upcoming() as? ApiResult.Success)?.value ?: return@withLock
        scheduler.replaceAll(planVisitReminders(upcoming, LocalDateTime.now(clock)))
    }
}
