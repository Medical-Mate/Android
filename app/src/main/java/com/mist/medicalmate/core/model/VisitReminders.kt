package com.mist.medicalmate.core.model

import com.mist.medicalmate.core.network.ApiResult

/**
 * 진료 하루 전 알림 예약을 지금 일정에 맞춘다.
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
 * 진료 하루 전 알림을 받을지. 계정 설정이다.
 *
 * 값은 `profile`의 설정에 있고 읽는 것은 `calendar`의 예약이다. [VisitReminders]와 같은
 * 이유로 `core`에 둔다.
 */
fun interface VisitReminderSetting {
    suspend fun enabled(): ApiResult<Boolean>
}
