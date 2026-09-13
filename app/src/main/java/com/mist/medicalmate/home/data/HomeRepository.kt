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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 홈이 쓰는 값을 모은다.
 *
 * 두 곳에서 온다. `GET /api/me/home`이 네 덩어리를 주지만 사람 이름이 없어서 헤더의 첫
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
                ApiResult.Success(home.value.toSnapshot(name, today))
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

private fun HomeResponse.toSnapshot(name: String?, today: LocalDate): HomeSnapshot {
    val appointmentAt = nextAppointment?.let { OffsetDateTime.parse(it.scheduledAt) }
    return HomeSnapshot(
        userInitial = name?.take(1).orEmpty(),
        todayLine = todayLine(today, appointmentAt?.toLocalDate()),
        resume = inProgressSession?.toResume(),
        savedCards = recentCards.map { it.toSummary() },
        upcoming = upcoming(appointmentAt),
    )
}

/**
 * 마지막 진료일이 없으면 아직 진료 기록이 없는 사람이다. 문서가 "신규 사용자는 전부
 * null이고 404가 아닙니다"라고 적었고 그것이 1n-2 화면이다.
 */
private fun HomeResponse.todayLine(today: LocalDate, nextVisit: LocalDate?): HomeTodayLine {
    val last = lastVisitedOn?.let(LocalDate::parse) ?: return HomeTodayLine.FirstVisit
    return HomeTodayLine.SinceLastVisit(
        daysSinceLastVisit = (today.toEpochDay() - last.toEpochDay()).toInt(),
        nextVisit = nextVisit,
    )
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

/** 서버는 다음 일정 하나만 준다. 화면은 목록으로 받으므로 0개나 1개가 된다. */
private fun HomeResponse.upcoming(at: OffsetDateTime?): List<HomeSchedule> = listOfNotNull(
    nextAppointment?.let { appointment ->
        at?.let { scheduledAt ->
            HomeSchedule(
                id = appointment.appointmentId.toString(),
                title = appointment.cardTitle ?: appointment.clinicName.orEmpty(),
                date = scheduledAt.toLocalDate(),
                time = scheduledAt.toLocalTime().format(TIME_FORMAT),
            )
        }
    },
)

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
