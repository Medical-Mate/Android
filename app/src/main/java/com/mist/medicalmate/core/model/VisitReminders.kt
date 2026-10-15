package com.mist.medicalmate.core.model

import com.mist.medicalmate.core.network.ApiResult
import java.time.LocalTime

/**
 * 진료 알림 예약(진료 전 · 진료 후 기록)을 지금 일정에 맞춘다.
 *
 * 예약은 `calendar`가 하는데, 맞춰야 하는 순간은 여러 도메인에 흩어져 있다. 로그인과
 * 로그아웃은 `auth`, 토글은 `profile`, 일정 저장은 `calendar`다. 서로 직접 부르지 않도록
 * `core`에 인터페이스를 두고 Hilt가 연결한다. [FollowUpScheduler]와 같은 방식이다.
 *
 * 둘 다 기다리지 않는다. 일정을 저장한 화면은 곧바로 닫히는데, 그 화면의 코루틴에 예약을
 * 걸어 두면 화면과 함께 취소된다. 구현이 앱 수명의 자리에서 돈다.
 */
interface VisitReminders {
    /** 일정과 토글을 다시 읽어 예약을 갈아끼운다. 못 읽으면 걸린 예약을 그대로 둔다. */
    fun refresh()

    /** 걸린 예약을 모두 지운다. 로그아웃하면 다음 계정에 이전 계정의 진료 알림이 가면 안 된다. */
    fun clear()
}

/**
 * 진료 알림 설정(#316, Backend#176). 계정 값이다.
 *
 * 진료 전 알림은 시각이 있는 일정이면 24시간 전, 시간 미정이면 전날 [beforeVisitTime]이다. 진료 후 기록 알림은
 * 시각이 있으면 2시간 뒤, 미정이면 당일 [afterVisitTime]이다. 두 시각은 서버에 따로 있지만 알림 시간
 * 화면(1s-3)의 휠 하나가 둘을 함께 정한다(사용자 결정).
 *
 * 값은 `profile`의 설정에 있고 읽는 것은 `calendar`의 예약이다. [VisitReminders]와 같은 이유로 `core`에 둔다.
 */
data class ReminderSettings(
    val beforeVisit: Boolean = true,
    val afterVisit: Boolean = true,
    val beforeVisitTime: LocalTime = DEFAULT_UNTIMED_REMINDER,
    val afterVisitTime: LocalTime = DEFAULT_UNTIMED_REMINDER,
)

/** 서버의 기본값. 시간 미정 일정의 알림 시각 오후 7시다. */
val DEFAULT_UNTIMED_REMINDER: LocalTime = LocalTime.of(DEFAULT_UNTIMED_REMINDER_HOUR, 0)

private const val DEFAULT_UNTIMED_REMINDER_HOUR = 19

/** 진료 알림 설정을 읽는다. 구현은 `profile`이다. */
fun interface VisitReminderSetting {
    suspend fun settings(): ApiResult<ReminderSettings>
}
