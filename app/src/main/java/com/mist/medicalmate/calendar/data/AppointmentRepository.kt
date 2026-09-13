package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 캘린더 일정 읽고 쓰기.
 *
 * **시간대를 다루지 않는다**(#202). 서버가 날짜와 시각을 따로 주고 둘 다 지역 값이라, 앱이
 * 시각을 옮길 일이 없어졌다. 전에는 `scheduledAt`을 한국 시각으로 맞춰 들고 있었다.
 */
interface AppointmentRepository {
    suspend fun month(month: YearMonth): ApiResult<List<Appointment>>

    suspend fun day(date: LocalDate): ApiResult<List<Appointment>>

    suspend fun upcoming(): ApiResult<List<Appointment>>

    suspend fun create(appointment: NewAppointment): ApiResult<Appointment>

    suspend fun update(id: Long, edit: AppointmentEdit): ApiResult<Appointment>

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

    override suspend fun create(appointment: NewAppointment): ApiResult<Appointment> = apiCall(json) {
        api.create(
            CreateAppointmentRequest(
                clinicName = appointment.clinicName,
                department = appointment.department,
                purpose = appointment.purpose,
                scheduledOn = appointment.on.toString(),
                scheduledTime = appointment.time?.format(SERVER_TIME),
                cardIds = appointment.cardIds,
                origin = appointment.origin.takeIf { it != AppointmentOrigin.MANUAL }?.name,
                todos = appointment.todos.map { TodoResponse(text = it.text, done = it.done) },
            ),
        )
    }.map { it.toAppointment() }

    override suspend fun update(id: Long, edit: AppointmentEdit): ApiResult<Appointment> = apiCall(json) {
        api.update(
            id,
            UpdateAppointmentRequest(
                purpose = edit.purpose,
                scheduledOn = edit.on?.toString(),
                // 시각을 지우는 요청에는 시각을 싣지 않는다. 서버가 둘을 함께 받으면 어느 쪽인지 모른다.
                scheduledTime = edit.time?.format(SERVER_TIME).takeUnless { edit.clearTime },
                clearTime = true.takeIf { edit.clearTime },
                cardIds = edit.cardIds,
                todos = edit.todos?.map { TodoResponse(text = it.text, done = it.done) },
            ),
        )
    }.map { it.toAppointment() }

    override suspend fun delete(id: Long): ApiResult<Unit> = apiCall(json) { api.delete(id) }
}

/**
 * 일정 하나.
 *
 * **날짜와 시각을 따로 든다**(#202). 서버가 그렇게 주고, [time]이 없으면 시간 미정이다.
 * 전에는 `LocalDateTime` 하나였는데 미정을 담을 자리가 없어 오전 9시로 박고 있었다.
 * D-day는 [on]으로 센다 — 날짜로 견주므로 시간대 때문에 하루가 밀리지 않는다.
 *
 * [title]을 서버가 주지 않는다. 병원·진료과·목적 셋을 앱이 합친다. 문서가 "화면의
 * '서울OO병원 내과 재진'은 앱이 세 필드를 조합해 만드세요"라고 적었다.
 *
 * [cards]는 가져갈 카드다. 여러 장 붙을 수 있다. 지금 화면은 한 장만 고르게 하지만
 * 서버가 목록으로 주므로 목록으로 들고 있는다.
 */
data class Appointment(
    val id: Long,
    val title: String,
    val on: LocalDate,
    val time: LocalTime? = null,
    val status: AppointmentStatus,
    val cards: List<AppointmentCard> = emptyList(),
    val origin: AppointmentOrigin = AppointmentOrigin.MANUAL,
    /** 진료 전 할 일. 그 일정에 매달린다. */
    val todos: List<AppointmentTodo> = emptyList(),
)

/** 일정에 붙은 카드. */
data class AppointmentCard(val id: Long, val title: String?)

/**
 * 이 일정이 어디서 왔는지.
 *
 * 진료 후 기록의 재방문에서 만들었으면 [VISIT_FOLLOW_UP]이다. 모르는 값은 [MANUAL]로 본다 —
 * 서버가 갈래를 늘렸을 때 일정을 잃는 것보다 손으로 만든 것으로 보는 편이 낫다.
 */
enum class AppointmentOrigin { MANUAL, VISIT_FOLLOW_UP }

/**
 * 만들 일정.
 *
 * @param on 날짜. 이것만 필수다.
 * @param time 시각. 없으면 시간 미정으로 만들어진다.
 * @param cardIds 가져갈 카드. 비워도 된다. 카드 없이 "다음 주 치과"만 적을 수 있어야 한다.
 */
data class NewAppointment(
    val clinicName: String?,
    val on: LocalDate,
    val time: LocalTime? = null,
    val department: String? = null,
    val purpose: String? = null,
    val cardIds: List<Long> = emptyList(),
    val origin: AppointmentOrigin = AppointmentOrigin.MANUAL,
    val todos: List<AppointmentTodo> = emptyList(),
)

/**
 * 고칠 것.
 *
 * null은 "안 바꿈"이다.
 *
 * @param clearTime 시각을 미정으로 되돌린다. [time]이 null인 것은 "안 바꿈"이라 이 플래그가
 *   따로 필요하다. 둘을 함께 보내지 않는다.
 * @param cardIds 통째로 갈아끼운다. `null`이 "안 바꿈", 빈 목록이 "전부 뗌"이다.
 * @param todos 통째로 갈아끼운다. 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다.
 */
data class AppointmentEdit(
    val on: LocalDate? = null,
    val time: LocalTime? = null,
    val clearTime: Boolean = false,
    val purpose: String? = null,
    val cardIds: List<Long>? = null,
    val todos: List<AppointmentTodo>? = null,
)

/**
 * 할 일 한 줄.
 *
 * 서버가 id를 매기지 않는다. 화면의 key는 앱이 차례로 만든다 — 같은 글이 두 줄 있을 수 있어
 * 글을 key로 쓸 수 없다.
 */
data class AppointmentTodo(val text: String, val done: Boolean = false)

enum class AppointmentStatus { SCHEDULED, DONE, CANCELED }

private fun AppointmentResponse.toAppointment() = Appointment(
    id = appointmentId,
    title =
    listOfNotNull(clinicName, department, purpose)
        .joinToString(" ")
        .ifEmpty { cards.firstOrNull()?.title.orEmpty() },
    on = LocalDate.parse(scheduledOn),
    time = scheduledTime?.let(::parseTime),
    status = statusOf(status),
    cards = cards.map { AppointmentCard(id = it.cardId, title = it.title) },
    origin = originOf(origin),
    todos = todos.map { AppointmentTodo(text = it.text, done = it.done) },
)

/**
 * 서버가 주는 시각.
 *
 * `HH:mm:ss`로 오는데 초를 생략한 판본도 있을 수 있어 [LocalTime.parse]에 맡긴다. 못 읽으면
 * 시간 미정으로 본다 — 일정 하나 때문에 캘린더 전체가 죽는 것보다 낫다.
 */
private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

private fun originOf(value: String?): AppointmentOrigin = if (value == AppointmentOrigin.VISIT_FOLLOW_UP.name) {
    AppointmentOrigin.VISIT_FOLLOW_UP
} else {
    AppointmentOrigin.MANUAL
}

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
 * 초를 늘 적는다. `LocalTime.toString()`은 초가 0이면 생략해서 `10:00`을 만드는데, 정각으로
 * 잡은 일정만 다른 모양으로 나가는 것이 좋지 않다.
 *
 * 시간대를 붙이지 않는다. 날짜와 시각이 갈리면서 서버가 둘 다 지역 값으로 받는다(#202).
 * 날짜로 견주므로 시간대 때문에 하루가 밀리는 일도 사라졌다.
 */
private val SERVER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
