package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.network.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * 일정 요청과 응답을 옮기는 부분.
 *
 * 시각 형식과 카드 연결 플래그가 여기 걸린다. 둘 다 값이 하나 어긋나면 서버가 요청을
 * 통째로 거절하는 자리다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentRepositoryTest {
    @Test
    fun `보내는 시각에 초가 들어간다`() = runTest {
        // `OffsetDateTime.toString()`은 초가 0이면 생략한다. 정각 일정만 다른 모양으로
        // 나가지 않게 고정한다.
        val api = RecordingApi()

        repository(api).create(null, null, null, LocalDateTime.of(2026, 9, 30, 10, 0), null)

        assertEquals("2026-09-30T10:00:00+09:00", api.created?.scheduledAt)
    }

    @Test
    fun `시각을 한국 시각으로 보낸다`() = runTest {
        // 서버가 날짜 경계를 KST로 자른다. 기기 시간대로 보내면 경계 근처 일정이 다른 날로 간다.
        val api = RecordingApi()

        repository(api).create(null, null, null, LocalDateTime.of(2026, 9, 30, 0, 30), null)

        assertTrue(api.created!!.scheduledAt.endsWith("+09:00"))
    }

    @Test
    fun `카드를 끊는 요청에는 id를 싣지 않는다`() = runTest {
        // 서버가 둘을 함께 받으면 어느 쪽인지 모른다.
        val api = RecordingApi()

        repository(api).update(id = 1, cardId = 9, clearCard = true)

        assertEquals(true, api.updated?.clearCard)
        assertNull(api.updated?.cardId)
    }

    @Test
    fun `카드를 안 바꾸면 플래그도 안 보낸다`() = runTest {
        val api = RecordingApi()

        repository(api).update(id = 1, cardId = 9)

        assertEquals(9L, api.updated?.cardId)
        assertNull(api.updated?.clearCard)
    }

    @Test
    fun `제목은 병원 진료과 목적을 합쳐 만든다`() = runTest {
        // 서버가 세 필드를 따로 준다. "서울OO병원 내과 재진"은 앱이 만든다.
        val api = RecordingApi(
            listOf(response(clinic = Triple("서울OO병원", "내과", "재진"))),
        )

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals("서울OO병원 내과 재진", (result as ApiResult.Success).value.single().title)
    }

    @Test
    fun `병원이 없으면 카드 제목을 쓴다`() = runTest {
        val api = RecordingApi(listOf(response(cardTitle = "복부 통증 · 3주")))

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals("복부 통증 · 3주", (result as ApiResult.Success).value.single().title)
    }

    @Test
    fun `받은 시각을 한국 시각으로 맞춘다`() = runTest {
        // UTC로 온 값을 그대로 쓰면 오전 9시 이전 일정이 전날로 밀린다.
        val api = RecordingApi(listOf(response(scheduledAt = "2026-09-30T01:00:00Z")))

        val result = repository(api).day(LocalDate.of(2026, 9, 30))

        assertEquals(LocalDateTime.of(2026, 9, 30, 10, 0), (result as ApiResult.Success).value.single().at)
    }

    @Test
    fun `모르는 상태는 예정으로 본다`() = runTest {
        // 캘린더에서 사라지면 사용자가 일정을 잃었다고 본다.
        val api = RecordingApi(listOf(response(status = "SOMETHING_NEW")))

        val result = repository(api).month(YearMonth.of(2026, 9))

        assertEquals(AppointmentStatus.SCHEDULED, (result as ApiResult.Success).value.single().status)
    }

    private fun repository(api: AppointmentApi) = DefaultAppointmentRepository(api, Json)

    private fun response(
        clinic: Triple<String?, String?, String?> = Triple(null, null, null),
        scheduledAt: String = "2026-09-30T10:00:00+09:00",
        status: String? = "SCHEDULED",
        cardTitle: String? = null,
    ) = AppointmentResponse(
        appointmentId = 1,
        clinicName = clinic.first,
        department = clinic.second,
        purpose = clinic.third,
        scheduledAt = scheduledAt,
        status = status,
        cardId = null,
        cardTitle = cardTitle,
    )

    private class RecordingApi(private val list: List<AppointmentResponse> = emptyList()) : AppointmentApi {
        var created: CreateAppointmentRequest? = null
        var updated: UpdateAppointmentRequest? = null

        override suspend fun appointments(year: Int?, month: Int?, date: String?) = list

        override suspend fun upcoming() = list

        override suspend fun create(request: CreateAppointmentRequest): AppointmentResponse {
            created = request
            return AppointmentResponse(appointmentId = 1, scheduledAt = request.scheduledAt)
        }

        override suspend fun update(appointmentId: Long, request: UpdateAppointmentRequest): AppointmentResponse {
            updated = request
            return AppointmentResponse(appointmentId = appointmentId, scheduledAt = "2026-09-30T10:00:00+09:00")
        }

        override suspend fun delete(appointmentId: Long) = Unit
    }
}
