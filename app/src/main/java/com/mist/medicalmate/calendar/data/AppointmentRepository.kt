package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 캘린더 일정 읽고 쓰기.
 *
 * 시각을 [ZoneId]로 맞춘다. 서버가 날짜 경계를 한국 시각으로 자르므로 앱도 같은 기준으로
 * 보여줘야 한다. 기기 시간대로 그리면 해외에서 같은 일정이 다른 날에 찍힌다.
 */
interface AppointmentRepository {
    suspend fun month(month: YearMonth): ApiResult<List<Appointment>>

    suspend fun day(date: LocalDate): ApiResult<List<Appointment>>

    suspend fun upcoming(): ApiResult<List<Appointment>>

    /**
     * 일정을 만든다.
     *
     * @param cardId 가져갈 카드. 선택이다. 카드 없이 "다음 주 치과"만 적을 수 있어야 한다.
     */
    suspend fun create(
        clinicName: String?,
        department: String?,
        purpose: String?,
        at: LocalDateTime,
        cardId: Long?,
    ): ApiResult<Appointment>

    /**
     * 일정을 고친다.
     *
     * @param clearCard 카드 연결을 끊는다. `cardId = null`은 "안 바꿈"이라 이 플래그가 따로
     *   필요하다. 둘을 함께 보내지 않는다.
     */
    suspend fun update(
        id: Long,
        at: LocalDateTime? = null,
        purpose: String? = null,
        cardId: Long? = null,
        clearCard: Boolean = false,
    ): ApiResult<Appointment>

    suspend fun delete(id: Long): ApiResult<Unit>
}

internal class DefaultAppointmentRepository
@Inject
constructor(
    private val api: AppointmentApi,
    private val json: Json,
) : AppointmentRepository {
    override suspend fun month(month: YearMonth): ApiResult<List<Appointment>> =
        apiCall(json) { api.appointments(year = month.year, month = month.monthValue) }
            .map { list -> list.map { it.toAppointment() } }

    override suspend fun day(date: LocalDate): ApiResult<List<Appointment>> =
        apiCall(json) { api.appointments(date = date.toString()) }
            .map { list -> list.map { it.toAppointment() } }

    override suspend fun upcoming(): ApiResult<List<Appointment>> =
        apiCall(json) { api.upcoming() }.map { list -> list.map { it.toAppointment() } }

    override suspend fun create(
        clinicName: String?,
        department: String?,
        purpose: String?,
        at: LocalDateTime,
        cardId: Long?,
    ): ApiResult<Appointment> = apiCall(json) {
        api.create(
            CreateAppointmentRequest(
                clinicName = clinicName,
                department = department,
                purpose = purpose,
                scheduledAt = at.toServerTime(),
                cardId = cardId,
            ),
        )
    }.map { it.toAppointment() }

    override suspend fun update(
        id: Long,
        at: LocalDateTime?,
        purpose: String?,
        cardId: Long?,
        clearCard: Boolean,
    ): ApiResult<Appointment> = apiCall(json) {
        api.update(
            id,
            UpdateAppointmentRequest(
                purpose = purpose,
                scheduledAt = at?.toServerTime(),
                // 카드를 끊는 요청에는 id를 싣지 않는다. 서버가 둘을 함께 받으면 어느 쪽인지 모른다.
                cardId = cardId.takeUnless { clearCard },
                clearCard = true.takeIf { clearCard },
            ),
        )
    }.map { it.toAppointment() }

    override suspend fun delete(id: Long): ApiResult<Unit> = apiCall(json) { api.delete(id) }
}

/**
 * 일정 하나.
 *
 * [at]을 한국 시각으로 맞춰 들고 있다. 화면이 날짜와 시각을 따로 쓰고 D-day도 여기서 센다.
 *
 * [title]을 서버가 주지 않는다. 병원·진료과·목적 셋을 앱이 합친다. 문서가 "화면의
 * '서울OO병원 내과 재진'은 앱이 세 필드를 조합해 만드세요"라고 적었다.
 */
data class Appointment(
    val id: Long,
    val title: String,
    val at: LocalDateTime,
    val status: AppointmentStatus,
    val cardId: Long?,
    val cardTitle: String?,
)

enum class AppointmentStatus { SCHEDULED, DONE, CANCELED }

private fun AppointmentResponse.toAppointment() = Appointment(
    id = appointmentId,
    title = listOfNotNull(clinicName, department, purpose).joinToString(" ").ifEmpty { cardTitle.orEmpty() },
    at = OffsetDateTime.parse(scheduledAt).atZoneSameInstant(KST).toLocalDateTime(),
    status = statusOf(status),
    cardId = cardId,
    cardTitle = cardTitle,
)

/**
 * 모르는 값은 예정으로 본다.
 *
 * 서버가 상태를 늘렸을 때 일정을 숨기는 것보다 보여주는 편이 낫다. 캘린더에서 사라지면
 * 사용자가 일정을 잃었다고 본다.
 */
private fun statusOf(value: String?): AppointmentStatus = when (value) {
    "DONE" -> AppointmentStatus.DONE
    "CANCELED" -> AppointmentStatus.CANCELED
    else -> AppointmentStatus.SCHEDULED
}

/**
 * 서버에 보낼 시각.
 *
 * 초를 늘 적는다. `OffsetDateTime.toString()`은 초가 0이면 생략해서
 * `2026-09-30T10:00+09:00`을 만드는데, 정각으로 잡은 일정만 다른 모양으로 나가는 것이
 * 좋지 않다.
 *
 * **이것이 등록 실패의 원인은 아니었다.** 기기에서 `POST /api/me/appointments`가 400을
 * 돌려주는 것을 보고 형식을 의심해 고쳤는데, 초를 넣어도 같은 400이 온다. 원인은 아직
 * 모른다(#141).
 */
private fun LocalDateTime.toServerTime(): String = atZone(KST).toOffsetDateTime().format(SERVER_TIME)

private val SERVER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

/**
 * 서버가 날짜 경계를 자르는 기준.
 *
 * 기기 시간대를 쓰면 해외에서 같은 일정이 다른 날에 찍힌다. 월 격자의 점과 일자 조회가 서로
 * 어긋나는 것이 그 증상이다.
 */
private val KST: ZoneId = ZoneId.of("Asia/Seoul")
