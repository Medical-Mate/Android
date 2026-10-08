package com.mist.medicalmate.calendar.share

import android.content.Intent

/**
 * 다른 앱에서 공유받은 예약 문자(#270).
 *
 * 문자 앱의 공유 메뉴에서 진료메이트를 고르면 `MainActivity`가 이 값을 꺼내 그래프에 넘기고,
 * 그래프가 일정 추가(1r-4)를 이 글과 함께 연다. 진료 알림의 `VisitReminderLink`와 같은 길이다.
 */
internal data class SharedAppointmentText(val text: String) {
    companion object {
        /**
         * 라우트에 실어 나르는 글의 상한.
         *
         * 예약 문자는 길어야 수백 자이고 날짜와 병원은 앞쪽에 있다. 긴 글을 통째로 공유받아도
         * 라우트가 그만큼 불어나지 않게 자른다.
         */
        private const val MAX_LENGTH = 1_000

        /** 글 공유가 아니거나 글이 비었으면 null이다. */
        fun from(intent: Intent?): SharedAppointmentText? {
            if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.take(MAX_LENGTH)
            return text?.takeIf { it.isNotEmpty() }?.let(::SharedAppointmentText)
        }
    }
}
