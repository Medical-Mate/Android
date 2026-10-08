package com.mist.medicalmate.calendar.share

import android.content.Intent
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 다른 앱에서 넘어온 예약(#270).
 *
 * 두 길로 온다. `MainActivity`가 인텐트에서 꺼내 그래프에 넘기고, 그래프가 일정 추가(1r-4)를
 * 이 값으로 연다. 진료 알림의 `VisitReminderLink`와 같은 길이다.
 *
 * - **글 공유**(`ACTION_SEND`). 문자 앱의 공유 메뉴다. 글만 오고 [date]·[time]은 없다 — 기기
 *   안에서 글을 읽어 채운다.
 * - **일정 추가**(`ACTION_INSERT`). 갤럭시 메시지가 날짜를 알아보면 띄우는 "일정 추가" 같은,
 *   캘린더에 일정을 넣어 달라는 요청이다. 보낸 앱이 이미 읽은 날짜와 시각이 숫자로 와서 그대로
 *   쓴다. 글(제목·설명·장소)은 병원을 찾는 데만 쓴다.
 */
internal data class SharedAppointmentLink(
    val text: String?,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
) {
    companion object {
        /**
         * 라우트에 실어 나르는 글의 상한.
         *
         * 예약 문자는 길어야 수백 자이고 날짜와 병원은 앞쪽에 있다. 긴 글을 통째로 받아도
         * 라우트가 그만큼 불어나지 않게 자른다.
         */
        private const val MAX_LENGTH = 1_000

        /** 넘겨받을 것이 없으면 null이다. */
        fun from(intent: Intent?): SharedAppointmentLink? = when (intent?.action) {
            Intent.ACTION_SEND -> fromShare(intent)
            Intent.ACTION_INSERT -> fromInsert(intent, ZoneId.systemDefault())
            else -> null
        }

        private fun fromShare(intent: Intent): SharedAppointmentLink? {
            if (intent.type != "text/plain") return null
            return clip(intent.getStringExtra(Intent.EXTRA_TEXT))?.let { SharedAppointmentLink(text = it) }
        }

        /**
         * 캘린더 일정 추가 요청. 키는 `CalendarContract`의 것이다.
         *
         * 하루 종일 일정이면 시각을 버린다. 그때 시작 시각은 그 날 0시(UTC)라 진료 시각이 아니다.
         */
        private fun fromInsert(intent: Intent, zone: ZoneId): SharedAppointmentLink? = fromEvent(
            beginMillis = intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1L),
            allDay = intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false),
            texts =
            listOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION,
            ).map(intent::getStringExtra),
            zone = zone,
        )

        /**
         * 일정 추가 요청의 값을 [SharedAppointmentLink]로. 인텐트를 걷어낸 부분이라 JVM에서 시험한다.
         *
         * 하루 종일 일정이면 시각을 버린다. 그때 시작 시각은 그 날 0시(UTC)라 진료 시각이 아니고,
         * 날짜도 UTC로 읽어야 하루 밀리지 않는다.
         *
         * @param beginMillis 시작 시각. 없으면 0 이하다.
         * @param texts 제목·장소·설명. 병원을 찾을 글로 잇는다.
         */
        fun fromEvent(beginMillis: Long, allDay: Boolean, texts: List<String?>, zone: ZoneId): SharedAppointmentLink? {
            val at = beginMillis.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(if (allDay) UTC else zone) }
            val text = texts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString("\n")
            val link =
                SharedAppointmentLink(
                    text = clip(text),
                    date = at?.toLocalDate(),
                    time = at?.takeUnless { allDay }?.toLocalTime()?.truncatedTo(ChronoUnit.MINUTES),
                )
            return link.takeIf { it.text != null || it.date != null }
        }

        private fun clip(text: String?): String? = text?.trim()?.take(MAX_LENGTH)?.takeIf { it.isNotEmpty() }

        private val UTC: ZoneId = ZoneId.of("UTC")
    }
}
