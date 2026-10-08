package com.mist.medicalmate.calendar.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 예약 문자에서 일정 칸을 고르는 규칙(#270). 추출기 자체는 기기에서만 돈다. */
class AppointmentTextTest {
    @Test
    fun `문자 머리의 대괄호에서 병원을 찾는다`() {
        assertEquals("서울OO병원", hospitalIn("[Web발신]\n[서울OO병원] 10월 15일 오후 2시 예약되었습니다."))
    }

    @Test
    fun `병원 뒤에 붙은 진료과는 함께 든다`() {
        assertEquals("서울OO병원 정형외과", hospitalIn("[서울OO병원 정형외과] 예약 안내"))
        assertEquals("마디병원 재활의학과", hospitalIn("마디병원 재활의학과 10/15 진료 예약"))
    }

    @Test
    fun `진료과 자리에 다른 말이 오면 떼어 낸다`() {
        assertEquals("서울OO병원", hospitalIn("[서울OO병원 예약안내] 10/15"))
        assertEquals("서울OO병원", hospitalIn("[서울OO병원 결과 안내]"))
    }

    @Test
    fun `대괄호가 없으면 본문에서 찾는다`() {
        assertEquals("연세이비인후과의원", hospitalIn("연세이비인후과의원에서 알려드립니다. 10월 15일 진료"))
        assertEquals("튼튼치과", hospitalIn("안녕하세요 튼튼치과입니다"))
        assertEquals("경희한의원", hospitalIn("경희한의원 예약 확인"))
    }

    @Test
    fun `과 이름으로 끝나는 의원도 병원이다`() {
        assertEquals("튼튼정형외과", hospitalIn("[Web발신]\n[튼튼정형외과] 10/15 오후 2시 예약"))
        assertEquals("밝은안과", hospitalIn("안녕하세요 밝은안과입니다. 10월 15일 진료 예약"))
        assertEquals("우리소아청소년과", hospitalIn("[우리소아청소년과] 예약 안내"))
        assertEquals("튼튼재활의학과", hospitalIn("튼튼재활의학과에서 알려드립니다"))
        assertEquals("서울내과", hospitalIn("서울내과 10월 15일 내원 예정"))
    }

    @Test
    fun `과 이름만 홀로 서면 병원이 아니다`() {
        assertNull(hospitalIn("[Web발신] 정형외과 진료가 10월 15일 오후 2시에 예약되었습니다."))
        assertNull(hospitalIn("[재활의학과] 10/15 예약"))
    }

    @Test
    fun `홀로 선 과를 건너뛰고 뒤의 병원을 찾는다`() {
        assertEquals("튼튼정형외과", hospitalIn("정형외과 진료 예약 안내 - 튼튼정형외과"))
    }

    @Test
    fun `병원 뒤의 세부 진료과도 함께 든다`() {
        assertEquals("서울OO병원 소화기내과", hospitalIn("[서울OO병원 소화기내과] 10/15 예약"))
    }

    @Test
    fun `병원 이름 꼴이 없으면 비운다`() {
        assertNull(hospitalIn("[Web발신] 10월 15일 오후 2시 예약되었습니다. 병원에 오실 때 신분증을 지참하세요."))
    }

    @Test
    fun `처음 나온 앞날을 고른다`() {
        val mentions =
            listOf(
                mention(0, 6, LocalDateTime.of(2026, 10, 15, 14, 30), hasTime = true),
                mention(30, 36, LocalDateTime.of(2026, 10, 14, 18, 0), hasTime = true),
            )

        assertEquals(LocalDate.of(2026, 10, 15) to LocalTime.of(14, 30), pickSchedule(mentions, TODAY))
    }

    @Test
    fun `지난 날은 건너뛴다`() {
        val mentions =
            listOf(
                mention(0, 5, LocalDateTime.of(2026, 9, 30, 0, 0), hasTime = false),
                mention(20, 26, LocalDateTime.of(2026, 10, 15, 0, 0), hasTime = false),
            )

        assertEquals(LocalDate.of(2026, 10, 15) to null, pickSchedule(mentions, TODAY))
    }

    @Test
    fun `날짜 바로 뒤의 시각은 같은 일정이다`() {
        // "10월 15일(목) 오후 2시"를 추출기가 둘로 나누어 준 경우다.
        val mentions =
            listOf(
                mention(0, 7, LocalDateTime.of(2026, 10, 15, 0, 0), hasTime = false),
                mention(11, 16, LocalDateTime.of(2026, 10, 8, 14, 0), hasTime = true),
            )

        assertEquals(LocalDate.of(2026, 10, 15) to LocalTime.of(14, 0), pickSchedule(mentions, TODAY))
    }

    @Test
    fun `떨어진 시각은 붙이지 않는다`() {
        // "10월 15일 진료 예약 ... 변경은 오후 6시까지"의 6시는 진료 시각이 아니다.
        val mentions =
            listOf(
                mention(0, 7, LocalDateTime.of(2026, 10, 15, 0, 0), hasTime = false),
                mention(30, 36, LocalDateTime.of(2026, 10, 8, 18, 0), hasTime = true),
            )

        assertEquals(LocalDate.of(2026, 10, 15) to null, pickSchedule(mentions, TODAY))
    }

    @Test
    fun `날짜가 없으면 둘 다 비운다`() {
        assertEquals(null to null, pickSchedule(emptyList(), TODAY))
    }

    @Test
    fun `하루까지 짚지 못한 것은 버린다`() {
        assertNull(dateTimeMention(0, 4, EPOCH_MILLIS, Granularity.COARSE, SEOUL))
    }

    @Test
    fun `시각까지 짚었으면 분 단위로 든다`() {
        val found = dateTimeMention(0, 4, EPOCH_MILLIS, Granularity.TIME, SEOUL)

        assertEquals(LocalDateTime.of(2026, 10, 15, 14, 30), found?.at)
        assertEquals(true, found?.hasTime)
    }

    private fun mention(start: Int, end: Int, at: LocalDateTime, hasTime: Boolean) =
        DateTimeMention(start, end, at, hasTime)

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 8)
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val EPOCH_MILLIS =
            ZonedDateTime.of(2026, 10, 15, 14, 30, 42, 0, SEOUL).toInstant().toEpochMilli()
    }
}

/** 다른 앱의 일정 추가 요청에서 받는 값(#270). 갤럭시 메시지의 "일정 추가"가 이 길이다. */
class SharedAppointmentLinkTest {
    @Test
    fun `보낸 앱이 읽은 날짜와 시각을 그대로 받는다`() {
        val begin = ZonedDateTime.of(2026, 10, 15, 14, 0, 0, 0, SEOUL).toInstant().toEpochMilli()

        val link =
            SharedAppointmentLink.fromEvent(begin, allDay = false, listOf("튼튼정형외과 예약", null, ""), SEOUL)

        assertEquals(LocalDate.of(2026, 10, 15), link?.date)
        assertEquals(LocalTime.of(14, 0), link?.time)
        assertEquals("튼튼정형외과 예약", link?.text)
    }

    @Test
    fun `하루 종일 일정이면 시각을 버리고 날짜는 UTC로 읽는다`() {
        // 하루 종일 일정의 시작은 그 날 0시 UTC다. 서울 시각으로 읽으면 오전 9시가 진료 시각으로 들어간다.
        val begin = ZonedDateTime.of(2026, 10, 15, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

        val link = SharedAppointmentLink.fromEvent(begin, allDay = true, emptyList(), ZoneId.of("America/New_York"))

        assertEquals(LocalDate.of(2026, 10, 15), link?.date)
        assertNull(link?.time)
        assertNull(link?.text)
    }

    @Test
    fun `날짜도 글도 없으면 넘길 것이 없다`() {
        assertNull(SharedAppointmentLink.fromEvent(-1L, allDay = false, listOf(null, " "), SEOUL))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

/** 이름 안에 특수문자가 든 병원(#270). 이름을 둘러싼 특수문자는 원래 잡힌다. */
class HospitalNameCharTest {
    @Test
    fun `이름을 둘러싼 특수문자는 떼고 든다`() {
        assertEquals("튼튼정형외과", hospitalIn("★튼튼정형외과★ 10/15 예약"))
        assertEquals("서울병원", hospitalIn("『서울병원』 예약 안내"))
        assertEquals("튼튼정형외과", hospitalIn("*튼튼정형외과* 예약"))
        assertEquals("서울OO병원", hospitalIn("(서울OO병원) 10/15 예약"))
    }

    @Test
    fun `가림 글자가 든 이름도 병원이다`() {
        assertEquals("서울○○병원", hospitalIn("[서울○○병원] 10월 15일 예약"))
        assertEquals("서울**병원", hospitalIn("서울**병원 예약 안내"))
    }

    @Test
    fun `전각 글자 이름도 병원이다`() {
        assertEquals("ＯＯ병원", hospitalIn("[ＯＯ병원] 예약"))
    }

    @Test
    fun `말 사이의 가운뎃점은 이름에 붙이지 않는다`() {
        assertEquals("튼튼정형외과", hospitalIn("예약 안내·튼튼정형외과"))
    }
}

/** 띄어 쓴 병원 이름(#270). 붙여 쓴 이름을 못 찾았을 때만 본다. */
class SpacedHospitalNameTest {
    @Test
    fun `머리의 띄어 쓴 이름을 든다`() {
        assertEquals("강남 세브란스 병원", hospitalIn("[Web발신]\n[강남 세브란스 병원] 10/15 14시 예약"))
        assertEquals("밝은 안과", hospitalIn("[밝은 안과] 예약 안내"))
        assertEquals("튼튼 정형외과", hospitalIn("[예약안내 튼튼 정형외과] 10/15"))
    }

    @Test
    fun `본문의 띄어 쓴 이름을 든다`() {
        assertEquals("강남 세브란스 병원", hospitalIn("안녕하세요 강남 세브란스 병원입니다. 10월 15일 예약"))
        assertEquals("튼튼 정형외과의원", hospitalIn("10/15 14시 튼튼 정형외과의원 진료 예약"))
        assertEquals("서울대 병원 소화기내과", hospitalIn("서울대 병원 소화기내과 10월 15일 예약"))
    }

    @Test
    fun `이름이 아닌 앞말은 받지 않는다`() {
        assertNull(hospitalIn("10월 15일 예약하신 병원에 오실 때 신분증을 지참하세요"))
        assertNull(hospitalIn("해당 병원으로 문의 바랍니다"))
        assertNull(hospitalIn("홍길동님 정형외과 진료가 10/15 예약되었습니다"))
        assertNull(hospitalIn("오후 2시 가까운 병원에 방문하세요"))
    }

    @Test
    fun `붙여 쓴 이름이 있으면 그것이 먼저다`() {
        assertEquals("튼튼정형외과", hospitalIn("해당 병원 안내 - 튼튼정형외과 10/15 예약"))
    }
}

/** 문자 앱이 넣어 보내는 보이지 않는 공백(#270). */
class InvisibleSpaceTest {
    @Test
    fun `줄바꿈 없는 공백과 전각 공백도 띄어쓰기다`() {
        assertEquals("한국튼튼 정형외과", hospitalIn("[Web발신]\n-한국튼튼 정형외과-\n10월 15일 14시"))
        assertEquals("한국튼튼 정형외과", hospitalIn("-한국튼튼　정형외과-"))
    }

    @Test
    fun `폭 없는 문자는 지운다`() {
        assertEquals("튼튼정형외과", hospitalIn("[튼튼​정형외과] 예약"))
    }
}
