package com.mist.medicalmate.home.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.home.ui.HomeResume
import com.mist.medicalmate.home.ui.HomeSchedule
import com.mist.medicalmate.home.ui.HomeTodayLine
import com.mist.medicalmate.home.ui.SavedCardSummary
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 홈이 쓰는 값을 모은다.
 *
 * 두 곳에서 온다. `GET /api/me/home`이 홈에 필요한 것을 주지만 사람 이름이 없어서 헤더의 첫
 * 글자는 [CurrentUserProvider]가 채운다. 그 구현은 `profile` 도메인에 있고 Hilt가 연결한다.
 *
 * 둘을 나란히 보낸다. 이어 부르면 화면이 두 번 기다린다.
 *
 * 이름을 못 받아도 홈은 그린다. 아바타 한 글자 때문에 화면 전체를 실패로 만들지 않는다.
 * 홈 본문이 실패한 경우만 실패다.
 */
interface HomeRepository {
    /**
     * @param today "며칠 지났어요"를 세는 기준일. 화면이 열린 날을 넘긴다. 기본값으로 두면
     *   자정을 넘겨 다시 그릴 때 옛 날짜가 남는다.
     */
    suspend fun load(today: LocalDate, now: LocalTime): ApiResult<HomeSnapshot>
}

internal class DefaultHomeRepository
@Inject
constructor(
    private val api: HomeApi,
    private val currentUser: CurrentUserProvider,
    private val json: Json,
) : HomeRepository {
    override suspend fun load(today: LocalDate, now: LocalTime): ApiResult<HomeSnapshot> = coroutineScope {
        val nameCall = async { currentUser.displayName() }

        when (val home = apiCall(json) { api.home() }) {
            is ApiResult.Success -> {
                val name = (nameCall.await() as? ApiResult.Success)?.value
                ApiResult.Success(home.value.toSnapshot(name = name, today = today, now = now))
            }

            is ApiResult.Rejected -> home
            is ApiResult.NetworkUnavailable -> home
        }
    }
}

/** 홈 화면이 그릴 것. */
data class HomeSnapshot(
    val userInitial: String,
    val todayLine: HomeTodayLine,
    val resume: HomeResume?,
    val savedCards: List<SavedCardSummary>,
    val upcoming: List<HomeSchedule>,
)

private fun HomeResponse.toSnapshot(name: String?, today: LocalDate, now: LocalTime): HomeSnapshot {
    val nextOn = nextAppointment?.let { LocalDate.parse(it.scheduledOn) }
    return HomeSnapshot(
        userInitial = name?.take(1).orEmpty(),
        todayLine = todayLine(today = today, now = now),
        resume = inProgressSession?.toResume(),
        savedCards = recentCards.map { it.toSummary() },
        upcoming = upcoming(nextOn),
    )
}

/**
 * 오늘의 한 줄을 고른다. 규칙은 디자인 트랙이 확정했다(#235).
 *
 * **차례가 곧 우선순위다.** 오늘 일정이 가장 세고, 그다음이 기록이 빠진 지난 일정, 다음 진료,
 * 지난 진료, 카드만 있는 상태다. 지난 진료와 다음 진료가 둘 다 있으면 다음 진료가 이긴다 —
 * 앞으로 할 일이 지나간 일보다 급하다.
 *
 * **경과일과 남은 날은 이틀 이상일 때만 숫자로 적는다.** 하루는 "어제"·"내일"이고 0일은 오늘
 * 갈래다. 앞날 진료에 경과일을 적어 "-2일이 지났어요"가 나오던 것이 이 규칙으로 사라진다
 * (#224).
 *
 * 마지막 진료일이 없고 카드도 일정도 없으면 아직 아무것도 안 한 사람이다. 문서가 "신규
 * 사용자는 전부 null이고 404가 아닙니다"라고 적었고 그것이 1n-2 화면이다.
 */
private fun HomeResponse.todayLine(today: LocalDate, now: LocalTime): HomeTodayLine {
    val last = lastVisitedOn?.let(LocalDate::parse)
    val appointment = nextAppointment
    val visit =
        NextVisit(
            on = appointment?.let { LocalDate.parse(it.scheduledOn) },
            at = appointment?.scheduledTime?.let(::parseTime),
            clinic = appointment?.clinicName?.takeIf { it.isNotBlank() },
        )

    return todayVisit(today, last, now, visit)
        ?: pendingRecordOn?.let(LocalDate::parse)?.let(HomeTodayLine::RecordMissing)
        ?: nextVisit(today, visit)
        ?: lastVisit(today, last)
        ?: if (recentCards.isNotEmpty()) HomeTodayLine.CardReady else HomeTodayLine.FirstVisit
}

/**
 * ① 오늘.
 *
 * 기록이 있으면 끝난 것이고, 없으면 시각이 지났는지로 갈린다. 시각이 없는 일정은 아직 앞둔
 * 것으로 본다 — 시간 미정이라 지났다고 말할 근거가 없다.
 */
private fun todayVisit(today: LocalDate, last: LocalDate?, now: LocalTime, visit: NextVisit): HomeTodayLine? = when {
    last == today -> HomeTodayLine.TodayRecorded
    visit.on != today -> null
    visit.at == null || !visit.at.isBefore(now) -> HomeTodayLine.TodayAhead(visit.clinic, visit.at)
    else -> HomeTodayLine.TodayDone
}

/** 홈 응답이 준 다음 일정. 날짜·시각·병원을 함께 나른다. */
private data class NextVisit(val on: LocalDate?, val at: LocalTime?, val clinic: String?)

/** ③ 다음 진료. 지난 진료와 둘 다 있으면 이쪽이 이긴다 — 앞으로 할 일이 급하다. */
private fun nextVisit(today: LocalDate, visit: NextVisit): HomeTodayLine? {
    val on = visit.on
    if (on == null || !on.isAfter(today)) return null
    val days = (on.toEpochDay() - today.toEpochDay()).toInt()
    return if (days == 1) {
        HomeTodayLine.NextTomorrow(visit.clinic, visit.at)
    } else {
        HomeTodayLine.NextInDays(days, on, visit.clinic)
    }
}

/** ④ 지난 진료. 경과일은 기록이 저장된 진료에만 적는다. */
private fun lastVisit(today: LocalDate, last: LocalDate?): HomeTodayLine? {
    if (last == null || !last.isBefore(today)) return null
    val days = (today.toEpochDay() - last.toEpochDay()).toInt()
    return if (days == 1) HomeTodayLine.LastYesterday else HomeTodayLine.LastDaysAgo(days)
}

/**
 * 임시저장된 문답.
 *
 * **서버의 진행도를 그대로 옮기지 않는다**(#176). `progressCurrent` / `progressTotal`은 문답
 * 왕복을 센 것이고 상한이 20이다. 그대로 찍으면 "20단계 중 0단계까지 답했어요"가 되는데
 * 환자가 보는 증상 정리는 네 단계다.
 *
 * 부위를 짚어야 세션이 생기므로 1단계는 이미 끝난 것이고, 문답에 한 마디라도 답했으면
 * 2단계다. 응답에 강도도 질문도 없어서 3·4단계는 여기서 가려낼 수 없다. 이름대로
 * `IN_PROGRESS`인 세션만 담긴다면 그 카드가 가리키는 것은 늘 문답 중인 세션이라 이 규칙으로
 * 맞는다. 어느 쪽인지 백엔드에 확인 중이다.
 */
private fun InProgressSessionResponse.toResume() = HomeResume(
    intakeId = sessionId.toString(),
    symptomTitle = siteText.orEmpty(),
    step = if (progressCurrent > 0) IntakeStep.SYMPTOM_CHAT else IntakeStep.BODY_PART,
)

/**
 * 서버는 다음 일정 하나만 준다. 화면은 목록으로 받으므로 0개나 1개가 된다.
 *
 * 시각이 없으면 시간 미정이다(#202). 그때 [HomeSchedule.time]이 없고 화면이 그 자리에
 * "시간 미정"을 적는다.
 */
private fun HomeResponse.upcoming(on: LocalDate?): List<HomeSchedule> = listOfNotNull(
    nextAppointment?.let { appointment ->
        on?.let { date ->
            HomeSchedule(
                id = appointment.appointmentId.toString(),
                // 병원과 진료과를 합친다. 카드 제목을 쓰던 것을 시안(`1n-1`)에 맞춰 바꿨다 —
                // 캘린더도 같은 자리를 병원으로 적고, 무엇을 가져가는지는 아래 줄이 말한다.
                // `purpose`는 앱이 채우지 않으므로 합치지 않는다.
                title =
                listOfNotNull(appointment.clinicName, appointment.department)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifEmpty { appointment.cards.firstOrNull()?.title.orEmpty() },
                date = date,
                time = appointment.scheduledTime?.let(::parseTime)?.format(TIME_FORMAT),
                followUp = appointment.origin == FOLLOW_UP_ORIGIN,
            )
        }
    },
)

/** 진료 후 기록에서 잡힌 재방문. 줄 제목이 "재진"으로 끝난다. */
private const val FOLLOW_UP_ORIGIN = "VISIT_FOLLOW_UP"

/** 못 읽으면 시간 미정으로 본다. 일정 하나 때문에 홈 전체가 죽는 것보다 낫다. */
private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

/**
 * 병원 이름은 진료를 마쳤어도 비어 있을 수 있다. 선택 입력이라서다.
 *
 * `status`를 쓰지 않는다. 카드를 확정했는지와 진료를 다녀왔는지는 다른 축이고, "진료 완료"
 * 배지는 `visited`로 판단하라고 문서가 적었다.
 */
private fun CardSummaryResponse.toSummary() = SavedCardSummary(
    id = cardId.toString(),
    title = title.orEmpty(),
    visited = visited,
    writtenOn = OffsetDateTime.parse(createdAt).toLocalDate(),
    clinic = clinicName,
)

/**
 * 시안의 "오전 10:30".
 *
 * 로케일을 고정한다. 기기 언어가 한국어가 아니면 `a`가 AM/PM으로 나오는데, 화면의 다른
 * 글자는 문자열 리소스라 한국어로 남아 한 줄 안에서 언어가 갈린다.
 */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
