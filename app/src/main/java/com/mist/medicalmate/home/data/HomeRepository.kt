package com.mist.medicalmate.home.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.home.ui.HomeCardStatus
import com.mist.medicalmate.home.ui.HomeLead
import com.mist.medicalmate.home.ui.HomeResume
import com.mist.medicalmate.home.ui.HomeSchedule
import com.mist.medicalmate.home.ui.HomeVisitLink
import com.mist.medicalmate.home.ui.SavedCardSummary
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

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
    /** @param today 배너 첫 장과 D-day를 가르는 기준일. 화면이 열린 날을 넘긴다. */
    suspend fun load(today: LocalDate): ApiResult<HomeSnapshot>
}

internal class DefaultHomeRepository
@Inject
constructor(
    private val api: HomeApi,
    private val currentUser: CurrentUserProvider,
    private val json: Json,
) : HomeRepository {
    override suspend fun load(today: LocalDate): ApiResult<HomeSnapshot> = coroutineScope {
        val nameCall = async { currentUser.displayName() }

        when (val home = apiCall(json) { api.home() }) {
            is ApiResult.Success -> {
                val name = (nameCall.await() as? ApiResult.Success)?.value
                val lead = home.value.lead(today) ?: cardReady(home.value) ?: home.value.rest()
                ApiResult.Success(home.value.toSnapshot(name = name, lead = lead))
            }

            is ApiResult.Rejected -> home
            is ApiResult.NetworkUnavailable -> home
        }
    }

    /**
     * 가장 최근 카드가 아직 진료 전이고 그 카드로 잡은 일정이 없으면 "카드는 준비됐어요"다. 일정이 있는지는
     * 홈 응답의 카드 요약에 없어 그 카드를 한 번 읽는다. 못 읽으면 이 갈래를 건너뛴다 — 배너 한 장 때문에
     * 홈을 실패로 만들지 않는다.
     */
    private suspend fun cardReady(home: HomeResponse): HomeLead? {
        val latest = home.recentCards.firstOrNull()?.takeUnless { it.visited }
        val card = latest?.let { (apiCall(json) { api.card(it.cardId) } as? ApiResult.Success)?.value }
        return if (latest != null && card != null && card.appointment == null) {
            HomeLead.CardReady(cardId = latest.cardId.toString(), symptom = latest.symptom())
        } else {
            null
        }
    }
}

/** 홈 화면이 그릴 것. */
data class HomeSnapshot(
    val userInitial: String,
    val lead: HomeLead,
    val resume: HomeResume?,
    val savedCards: List<SavedCardSummary>,
    val upcoming: HomeSchedule?,
)

private fun HomeResponse.toSnapshot(name: String?, lead: HomeLead): HomeSnapshot = HomeSnapshot(
    userInitial = name?.take(1).orEmpty(),
    lead = lead,
    resume = inProgressSession?.toResume(),
    // 시안 1n-1의 최근 브리핑 카드가 두 장이다(#312). 서버는 세 장을 준다.
    savedCards = recentCards.take(RECENT_CARDS).map { it.toSummary() },
    upcoming = nextAppointment?.toSchedule(),
)

/**
 * 오늘 · 내일 진료(#312). 카드가 걸린 일정만 본다 — 진료 후 기록은 카드에 붙고 "카드 다시 보기"도 카드가
 * 있어야 갈 곳이 있다. 오늘 진료를 이미 기록했으면(마지막 진료일이 오늘) 오늘 배너를 띄우지 않는다.
 *
 * 서버가 주는 다음 일정은 날짜로만 견줘서 오늘 일정이 하루 종일 남는다. 그래서 시각이 지났는지는 보지
 * 않는다 — 오늘 진료가 있으면 "진료 잘 다녀오셨나요?"다(사용자 결정).
 */
private fun HomeResponse.lead(today: LocalDate): HomeLead? {
    val appointment = nextAppointment
    val card = appointment?.cards?.firstOrNull()
    if (appointment == null || card == null) return null
    val on = LocalDate.parse(appointment.scheduledOn)
    val clinic = appointment.clinicLabel()
    return when {
        on == today && lastVisitedOn?.let(LocalDate::parse) != today ->
            HomeLead.TodayVisit(
                clinic = clinic,
                visit =
                HomeVisitLink(
                    cardId = card.cardId.toString(),
                    cardTitle = card.title.orEmpty(),
                    clinic = appointment.clinicName?.takeIf { it.isNotBlank() },
                    date = on,
                ),
            )

        on == today.plusDays(1) ->
            HomeLead.TomorrowVisit(
                time = appointment.scheduledTime?.let(::parseTime),
                clinic = clinic,
                cardId = card.cardId.toString(),
            )

        else -> null
    }
}

/** 처음 온 사람이면 환영 배너, 아니면 나머지(무작위 첫 장)다. */
private fun HomeResponse.rest(): HomeLead =
    if (recentCards.isEmpty() && nextAppointment == null && lastVisitedOn == null) {
        HomeLead.FirstVisit
    } else {
        HomeLead.Other
    }

/** "서울OO병원 내과". 병원과 진료과를 합친다. 둘 다 없으면 null이다. */
private fun AppointmentResponse.clinicLabel(): String? =
    listOfNotNull(clinicName, department).filter { it.isNotBlank() }.joinToString(" ").ifEmpty { null }

/**
 * 임시저장된 문답.
 *
 * **서버의 진행도를 그대로 옮기지 않는다**(#176). `progressCurrent` / `progressTotal`은 문답
 * 왕복을 센 것이고 상한이 20이다. 그대로 찍으면 "20단계 중 0단계까지 답했어요"가 되는데
 * 환자가 보는 증상 정리는 세 단계다.
 *
 * 세션은 부위와 강도를 고른 뒤에 생기므로(#310) 홈에 뜨는 문답은 늘 3단계에 있다.
 */
private fun InProgressSessionResponse.toResume() = HomeResume(
    intakeId = sessionId.toString(),
    symptomTitle = siteText.orEmpty(),
    step = IntakeStep.SYMPTOM_CHAT,
)

/** 시간 미정이면 [HomeSchedule.time]이 없다(#202). */
private fun AppointmentResponse.toSchedule() = HomeSchedule(
    id = appointmentId.toString(),
    date = LocalDate.parse(scheduledOn),
    time = scheduledTime?.let(::parseTime),
    clinic = clinicName?.takeIf { it.isNotBlank() },
    department = department?.takeIf { it.isNotBlank() },
    followUp = origin == FOLLOW_UP_ORIGIN,
)

/** 진료 후 기록에서 잡힌 재방문. 줄 제목이 "재진"으로 끝난다. */
private const val FOLLOW_UP_ORIGIN = "VISIT_FOLLOW_UP"

/** 시안 1n-1의 최근 브리핑 카드 수. */
private const val RECENT_CARDS = 2

/** 확정한 카드. 진료 전 배지의 기준이다. */
private const val STATUS_CONFIRMED = "CONFIRMED"

/** 못 읽으면 시간 미정으로 본다. 일정 하나 때문에 홈 전체가 죽는 것보다 낫다. */
private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

/**
 * 병원 이름은 진료를 마쳤어도 비어 있을 수 있다. 선택 입력이라서다.
 *
 * 배지는 브리핑 카드 전체(1j-4)와 같은 규칙이다. 진료를 다녀왔는지(`visited`)가 먼저고, 아니면 확정했는지로
 * 가른다.
 */
private fun CardSummaryResponse.toSummary() = SavedCardSummary(
    id = cardId.toString(),
    title = title.orEmpty(),
    status =
    when {
        visited -> HomeCardStatus.VISITED
        status == STATUS_CONFIRMED -> HomeCardStatus.BEFORE_VISIT
        else -> HomeCardStatus.DRAFT
    },
    writtenOn = OffsetDateTime.parse(createdAt).toLocalDate(),
    clinic = clinicName,
)

/** 배너가 부르는 카드 이름. 주 호소가 없으면 제목이다. */
private fun CardSummaryResponse.symptom(): String = chiefComplaint?.takeIf { it.isNotBlank() } ?: title.orEmpty()
