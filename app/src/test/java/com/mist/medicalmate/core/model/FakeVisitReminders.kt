package com.mist.medicalmate.core.model

/** 알림 예약 대역. 몇 번 맞추라고 했는지와 지우라고 했는지만 센다. */
class FakeVisitReminders : VisitReminders {
    var refreshes = 0
        private set

    var clears = 0
        private set

    override fun refresh() {
        refreshes++
    }

    override fun clear() {
        clears++
    }
}
