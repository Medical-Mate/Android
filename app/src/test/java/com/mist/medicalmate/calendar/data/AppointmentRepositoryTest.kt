package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * 일정 요청과 응답을 옮기는 부분.
 *
 * 시각 형식과 "안 바꿈" 플래그가 여기 걸린다. 둘 다 값이 하나 어긋나면 서버가 요청을
 * 통째로 거절하는 자리다.
 *
 * **서버가 날짜와 시각을 갈랐다**(#202). 전에는 `scheduledAt` 하나를 한국 시각으로 맞춰
 * 보내고 받았는데, 이제 둘 다 지역 값이라 옮길 일이 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentRepositoryTest {
    @Test
    fun `날짜와 시각을 따로 보낸다`() = runTest {
        val api = RecordingApi()

        repository(api).create(
            NewAppointment(clinicName = null, on = LocalDate.of(2026, 9, 30), time = LocalTime.of(10, 0)),
        )

        assertEquals("2026-09-30", api.created?.scheduledOn)
        assertEquals("10:00:00", api.created?.scheduledTime)
    }

    @Test
    fun `시각을 안 고르면 빼고 보낸다`() = runTest {
        // 서버가 시간 미정으로 만든다. 전에는 자리가 없어 오전 9시로 박았다.
        val api = RecordingApi()

        repository(api).create(NewAppointment(clinicName = null, on = LocalDate.of(2026, 9, 30)))

        assertEquals("2026-09-30", api.created?.scheduledOn)
        assertNull(api.created?.scheduledTime)
    }

    @Test
    fun `보내는 시각에 초가 들어간다`() = runTest {
        // `LocalTime.toString()`은 초가 0이면 생략한다. 정각 일정만 다른 모양으로 나가지 않게 한다.
        val api = RecordingApi()

        repository(api).create(
            NewAppointment(clinicName = null, on = LocalDate.of(2026, 9, 30), time = LocalTime.of(10, 0)),
        )

        assertEquals("10:00:00", api.created?.scheduledTime)
    }

    @Test
    fun `재방문에서 만든 일정은 출처를 싣는다`() = runTest {
        val api = RecordingApi()

        repository(api).create(
            NewAppointment(
                clinicName = null,
                on = LocalDate.of(2026, 9, 30),
                origin = AppointmentOrigin.VISIT_FOLLOW_UP,
            ),
        )

        assertEquals("VISIT_FOLLOW_UP", api.created?.origin)
    }

    @Test
    fun `손으로 만든 일정은 출처를 싣지 않는다`() = runTest {
        // 안 보내면 서버가 MANUAL로 둔다. 기본값을 굳이 실어 보낼 이유가 없다.
        val api = RecordingApi()

        repository(api).create(NewAppointment(clinicName = null, on = LocalDate.of(2026, 9, 30)))

        assertNull(api.created?.origin)
    }

    @Test
    fun `시각을 지우는 요청에는 시각을 싣지 않는다`() = runTest {
        // 서버가 둘을 함께 받으면 어느 쪽인지 모른다.
        val api = RecordingApi()

        repository(api).update(id = 1, edit = AppointmentEdit(time = LocalTime.of(10, 0), clearTime = true))

        assertEquals(true, api.updated?.clearTime)
        assertNull(api.updated?.scheduledTime)
    }

    @Test
    fun `시각을 안 바꾸면 플래그도 안 보낸다`() = runTest {
        val api = RecordingApi()

        repository(api).update(id = 1, edit = AppointmentEdit(time = LocalTime.of(10, 0)))

        assertEquals("10:00:00", api.updated?.scheduledTime)
        assertNull(api.updated?.clearTime)
    }

    @Test
    fun `카드를 안 바꾸면 목록을 보내지 않는다`() = runTest {
        // null이 "안 바꿈", 빈 목록이 "전부 뗌"이다.
        val api = RecordingApi()

        repository(api).update(id = 1, edit = AppointmentEdit(purpose = "재진"))

        assertNull(api.updated?.cardIds)
    }

    @Test
    fun `카드를 전부 떼면 빈 목록을 보낸다`() = runTest {
        val api = RecordingApi()

        repository(api).update(id = 1, edit = AppointmentEdit(cardIds = emptyList()))

        assertEquals(emptyList<Long>(), api.updated?.cardIds)
    }

    @Test
    fun `제목은 병원 진료과 목적을 합쳐 만든다`() = runTest {
        // 서버가 세 필드를 따로 준다. "서울OO병원 내과 재진"은 앱이 만든다.
        val api = RecordingApi(
            listOf(
                response().copy(clinicName = "서울OO병원", department = "내과", purpose = "재진"),
            ),
        )

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals("서울OO병원 내과 재진", (result as ApiResult.Success).value.single().title)
    }

    @Test
    fun `병원이 없으면 첫 카드 제목을 쓴다`() = runTest {
        val api = RecordingApi(listOf(response(cards = listOf(LinkedCardResponse(7, "복부 통증 · 3주")))))

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals("복부 통증 · 3주", (result as ApiResult.Success).value.single().title)
    }

    @Test
    fun `카드를 여러 장 받는다`() = runTest {
        // 서버가 목록으로 준다. 화면은 아직 한 장만 그리지만 값은 잃지 않는다.
        val api = RecordingApi(
            listOf(response(cards = listOf(LinkedCardResponse(7, "복부 통증"), LinkedCardResponse(8, "두통")))),
        )

        val result = repository(api).day(LocalDate.of(2026, 9, 30))

        assertEquals(listOf(7L, 8L), (result as ApiResult.Success).value.single().cards.map { it.id })
    }

    @Test
    fun `시각이 없으면 시간 미정이다`() = runTest {
        val api = RecordingApi(listOf(response(scheduledTime = null)))

        val result = repository(api).day(LocalDate.of(2026, 9, 30))

        assertNull((result as ApiResult.Success).value.single().time)
    }

    @Test
    fun `못 읽는 시각도 시간 미정으로 본다`() = runTest {
        // 일정 하나 때문에 캘린더 전체가 죽는 것보다 낫다.
        val api = RecordingApi(listOf(response(scheduledTime = "열 시")))

        val result = repository(api).day(LocalDate.of(2026, 9, 30))

        assertNull((result as ApiResult.Success).value.single().time)
    }

    @Test
    fun `모르는 출처는 손으로 만든 것으로 본다`() = runTest {
        val api = RecordingApi(listOf(response().copy(origin = "SOMETHING_NEW")))

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals(AppointmentOrigin.MANUAL, (result as ApiResult.Success).value.single().origin)
    }

    @Test
    fun `모르는 상태는 예정으로 본다`() = runTest {
        // 캘린더에서 사라지면 사용자가 일정을 잃었다고 본다.
        val api = RecordingApi(listOf(response().copy(status = "SOMETHING_NEW")))

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals(AppointmentStatus.SCHEDULED, (result as ApiResult.Success).value.single().status)
    }

    private fun repository(api: AppointmentApi) = DefaultAppointmentRepository(api, Json)

    /**
     * 흔한 응답 하나.
     *
     * 병원·상태·출처를 여기서 받지 않는다. 그 셋을 보는 시험이 하나씩뿐이라 그 자리에서
     * 직접 만드는 편이 짧고, 인자를 늘리면 무엇을 보는 시험인지가 흐려진다.
     */
    private fun response(scheduledTime: String? = "10:00:00", cards: List<LinkedCardResponse> = emptyList()) =
        AppointmentResponse(
            appointmentId = 1,
            scheduledOn = "2026-09-30",
            scheduledTime = scheduledTime,
            status = "SCHEDULED",
            cards = cards,
        )

    private class RecordingApi(private val list: List<AppointmentResponse> = emptyList()) : AppointmentApi {
        var created: CreateAppointmentRequest? = null
        var updated: UpdateAppointmentRequest? = null

        override suspend fun appointments(year: Int?, month: Int?, date: String?) = list

        override suspend fun upcoming() = list

        override suspend fun create(request: CreateAppointmentRequest): AppointmentResponse {
            created = request
            return AppointmentResponse(appointmentId = 1, scheduledOn = request.scheduledOn)
        }

        override suspend fun update(appointmentId: Long, request: UpdateAppointmentRequest): AppointmentResponse {
            updated = request
            return AppointmentResponse(appointmentId = appointmentId, scheduledOn = "2026-09-30")
        }

        override suspend fun delete(appointmentId: Long) = Unit
    }
}
