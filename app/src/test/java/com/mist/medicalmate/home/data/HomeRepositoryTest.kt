package com.mist.medicalmate.home.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.home.ui.HomeCardStatus
import com.mist.medicalmate.home.ui.HomeLead
import com.mist.medicalmate.home.ui.HomeVisitLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/**
 * 응답을 화면 값으로 옮기는 부분.
 *
 * 홈 배너 첫 장(#312)은 오늘 진료 → 내일 진료 → 일정 없는 카드 → 처음 온 사람 차례로 고르고, 어디에도 안
 * 맞으면 나머지([HomeLead.Other])다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {
    @Test
    fun `카드도 일정도 진료도 없으면 처음 온 사람이다`() = runTest {
        // 문서: "신규 사용자는 전부 null 또는 빈 배열입니다. 404가 아닙니다."
        assertEquals(HomeLead.FirstVisit, load(HomeResponse()).lead)
    }

    @Test
    fun `오늘 카드가 걸린 진료가 있으면 진료 후 기록으로 이끈다`() = runTest {
        val snapshot = load(HomeResponse(nextAppointment = appointment.copy(scheduledOn = TODAY)))

        assertEquals(
            HomeLead.TodayVisit(
                clinic = "서울OO병원 내과",
                visit =
                HomeVisitLink(
                    cardId = "1",
                    cardTitle = "복부 통증 · 3주",
                    clinic = "서울OO병원",
                    date = LocalDate.parse(TODAY),
                ),
            ),
            snapshot.lead,
        )
    }

    @Test
    fun `오늘 진료는 시각 전이어도 진료 후 기록이다`() = runTest {
        // 서버가 날짜로만 견줘 오늘 일정이 하루 종일 남는다. 시각을 보지 않는다(사용자 결정).
        val snapshot =
            load(HomeResponse(nextAppointment = appointment.copy(scheduledOn = TODAY, scheduledTime = "18:00:00")))

        assertTrue(snapshot.lead is HomeLead.TodayVisit)
    }

    @Test
    fun `오늘 진료를 이미 기록했으면 오늘 배너를 띄우지 않는다`() = runTest {
        val snapshot =
            load(HomeResponse(lastVisitedOn = TODAY, nextAppointment = appointment.copy(scheduledOn = TODAY)))

        assertEquals(HomeLead.Other, snapshot.lead)
    }

    @Test
    fun `카드가 걸리지 않은 오늘 일정은 진료 후 기록으로 이끌지 않는다`() = runTest {
        // 기록은 카드에 붙는다. 카드 없는 일정으로는 저장할 곳이 없다.
        val snapshot = load(HomeResponse(nextAppointment = appointment.copy(scheduledOn = TODAY, cards = emptyList())))

        assertEquals(HomeLead.Other, snapshot.lead)
    }

    @Test
    fun `병원 없는 오늘 일정은 병원 이름 없이 간다`() = runTest {
        val snapshot =
            load(
                HomeResponse(
                    nextAppointment = appointment.copy(scheduledOn = TODAY, clinicName = null, department = null),
                ),
            )

        val lead = snapshot.lead as HomeLead.TodayVisit
        assertNull(lead.clinic)
        assertNull(lead.visit.clinic)
    }

    @Test
    fun `내일 진료가 있으면 카드를 다시 보게 한다`() = runTest {
        val snapshot = load(HomeResponse(nextAppointment = appointment.copy(scheduledOn = TOMORROW)))

        assertEquals(
            HomeLead.TomorrowVisit(time = LocalTime.of(10, 30), clinic = "서울OO병원 내과", cardId = "1"),
            snapshot.lead,
        )
    }

    @Test
    fun `모레 진료는 내일 배너가 아니다`() = runTest {
        val snapshot = load(HomeResponse(nextAppointment = appointment.copy(scheduledOn = "2026-09-13")))

        assertEquals(HomeLead.Other, snapshot.lead)
    }

    @Test
    fun `가장 최근 카드에 일정이 없으면 일정을 잡게 한다`() = runTest {
        val snapshot = load(HomeResponse(recentCards = listOf(draftCard, visitedCard)), cardHasAppointment = false)

        assertEquals(HomeLead.CardReady(cardId = "2", symptom = "두통"), snapshot.lead)
    }

    @Test
    fun `주 호소가 없으면 카드 제목으로 부른다`() = runTest {
        val untitled = draftCard.copy(chiefComplaint = null)

        val snapshot = load(HomeResponse(recentCards = listOf(untitled)), cardHasAppointment = false)

        assertEquals(HomeLead.CardReady(cardId = "2", symptom = "두통 · 잦은 어지러움"), snapshot.lead)
    }

    @Test
    fun `가장 최근 카드에 일정이 있으면 일정을 잡게 하지 않는다`() = runTest {
        val snapshot = load(HomeResponse(recentCards = listOf(draftCard)), cardHasAppointment = true)

        assertEquals(HomeLead.Other, snapshot.lead)
    }

    @Test
    fun `가장 최근 카드가 진료를 마쳤으면 카드를 읽지 않는다`() = runTest {
        val api = FakeHomeApi(HomeResponse(recentCards = listOf(visitedCard)), cardHasAppointment = false)

        val snapshot = load(api)

        assertEquals(HomeLead.Other, snapshot.lead)
        assertEquals(emptyList<Long>(), api.readCards)
    }

    @Test
    fun `카드를 못 읽으면 일정 잡기를 건너뛴다`() = runTest {
        // 배너 한 장 때문에 홈을 실패로 만들지 않는다.
        val snapshot = load(FakeHomeApi(HomeResponse(recentCards = listOf(draftCard)), cardFails = true))

        assertEquals(HomeLead.Other, snapshot.lead)
    }

    @Test
    fun `오늘 진료가 일정 없는 카드보다 앞선다`() = runTest {
        val api =
            FakeHomeApi(
                HomeResponse(nextAppointment = appointment.copy(scheduledOn = TODAY), recentCards = listOf(draftCard)),
                cardHasAppointment = false,
            )

        val snapshot = load(api)

        assertTrue(snapshot.lead is HomeLead.TodayVisit)
        assertEquals(emptyList<Long>(), api.readCards)
    }

    @Test
    fun `작성 중이던 문답이 이어서 하기가 된다`() = runTest {
        val snapshot = load(
            HomeResponse(
                inProgressSession =
                InProgressSessionResponse(sessionId = 7, siteText = "복부", progressCurrent = 2, progressTotal = 20),
            ),
        )

        val resume = requireNotNull(snapshot.resume)
        assertEquals("7", resume.intakeId)
        assertEquals("복부", resume.symptomTitle)
        assertEquals(IntakeStep.SYMPTOM_CHAT, resume.step)
    }

    @Test
    fun `서버의 왕복 수를 단계로 쓰지 않는다`() = runTest {
        // 상한이 20이다. 그대로 찍으면 "20단계 중 2단계"가 된다. 세션은 강도를 고른 뒤에 생겨(#310) 두 단계가
        // 끝나 있다.
        val snapshot = load(
            HomeResponse(
                inProgressSession =
                InProgressSessionResponse(sessionId = 7, siteText = "복부", progressCurrent = 0, progressTotal = 20),
            ),
        )

        assertEquals(3, IntakeStep.total)
        assertEquals(2, requireNotNull(snapshot.resume).doneSteps)
    }

    @Test
    fun `작성 중이던 문답이 없으면 이어서 하기도 없다`() = runTest {
        assertNull(load(HomeResponse()).resume)
    }

    @Test
    fun `카드 배지는 다녀왔는지가 먼저고 아니면 확정했는지로 갈린다`() = runTest {
        // 브리핑 카드 전체(1j-4)와 같은 규칙이다(사용자 결정).
        val confirmed = draftCard.copy(cardId = 3, status = "CONFIRMED")

        val snapshot = load(HomeResponse(recentCards = listOf(visitedCard, confirmed, draftCard)))

        assertEquals(listOf(HomeCardStatus.VISITED, HomeCardStatus.BEFORE_VISIT), snapshot.savedCards.map { it.status })
    }

    @Test
    fun `최근 브리핑 카드는 두 장만 담는다`() = runTest {
        // 시안 1n-1이 두 장이다. 서버는 세 장을 준다.
        val snapshot = load(HomeResponse(recentCards = listOf(visitedCard, draftCard, draftCard.copy(cardId = 3))))

        assertEquals(listOf("1", "2"), snapshot.savedCards.map { it.id })
    }

    @Test
    fun `카드 작성일은 날짜 타입으로 옮기고 병원이 없으면 비운다`() = runTest {
        val snapshot = load(HomeResponse(recentCards = listOf(visitedCard, draftCard)))

        assertEquals(LocalDate.of(2026, 9, 4), snapshot.savedCards.first().writtenOn)
        assertNull(snapshot.savedCards.last().clinic)
    }

    @Test
    fun `다음 일정은 날짜 · 시각 · 병원 · 진료과로 갈라 담는다`() = runTest {
        val schedule = requireNotNull(load(HomeResponse(nextAppointment = appointment)).upcoming)

        assertEquals("1", schedule.id)
        assertEquals(LocalDate.of(2026, 9, 12), schedule.date)
        assertEquals(LocalTime.of(10, 30), schedule.time)
        assertEquals("서울OO병원", schedule.clinic)
        assertEquals("내과", schedule.department)
    }

    @Test
    fun `재방문으로 잡힌 일정은 재진이다`() = runTest {
        val schedule = load(HomeResponse(nextAppointment = appointment.copy(origin = "VISIT_FOLLOW_UP"))).upcoming

        assertTrue(requireNotNull(schedule).followUp)
    }

    @Test
    fun `다음 일정이 없으면 비어 있다`() = runTest {
        assertNull(load(HomeResponse()).upcoming)
    }

    @Test
    fun `이름 첫 글자를 아바타에 쓴다`() = runTest {
        assertEquals("김", load(HomeResponse(), name = "김지훈").userInitial)
    }

    @Test
    fun `이름을 못 받아도 홈은 그린다`() = runTest {
        // 아바타 한 글자 때문에 화면 전체를 실패로 만들지 않는다.
        val repository =
            DefaultHomeRepository(
                api = FakeHomeApi(HomeResponse()),
                currentUser = CurrentUserProvider { ApiResult.NetworkUnavailable(IOException()) },
                json = Json,
            )

        val result = repository.load(LocalDate.parse(TODAY))

        assertTrue(result is ApiResult.Success)
        assertEquals("", (result as ApiResult.Success).value.userInitial)
    }

    @Test
    fun `홈 본문이 실패하면 실패다`() = runTest {
        val repository =
            DefaultHomeRepository(
                api = FakeHomeApi(HomeResponse(), homeFails = true),
                currentUser = { ApiResult.Success("김지훈") },
                json = Json,
            )

        assertTrue(repository.load(LocalDate.parse(TODAY)) is ApiResult.NetworkUnavailable)
    }

    private suspend fun load(
        response: HomeResponse,
        name: String? = "김지훈",
        cardHasAppointment: Boolean = true,
    ): HomeSnapshot = load(FakeHomeApi(response, cardHasAppointment = cardHasAppointment), name)

    private suspend fun load(api: FakeHomeApi, name: String? = "김지훈"): HomeSnapshot {
        val repository = DefaultHomeRepository(api = api, currentUser = { ApiResult.Success(name) }, json = Json)
        return (repository.load(LocalDate.parse(TODAY)) as ApiResult.Success).value
    }

    /** 홈 응답을 그대로 주고, 카드를 읽으면 그 id를 남긴다. */
    private class FakeHomeApi(
        private val response: HomeResponse,
        private val cardHasAppointment: Boolean = true,
        private val cardFails: Boolean = false,
        private val homeFails: Boolean = false,
    ) : HomeApi {
        val readCards = mutableListOf<Long>()

        override suspend fun home(): HomeResponse = if (homeFails) throw IOException() else response

        override suspend fun card(cardId: Long): HomeCardResponse {
            readCards += cardId
            if (cardFails) throw IOException()
            return HomeCardResponse(
                cardId = cardId,
                appointment = HomeCardAppointment(appointmentId = 9, scheduledOn = "2026-09-20").takeIf {
                    cardHasAppointment
                },
            )
        }
    }

    private companion object {
        const val TODAY = "2026-09-11"
        const val TOMORROW = "2026-09-12"

        val appointment =
            AppointmentResponse(
                appointmentId = 1,
                clinicName = "서울OO병원",
                department = "내과",
                scheduledOn = "2026-09-12",
                scheduledTime = "10:30:00",
                cards = listOf(LinkedCardResponse(cardId = 1, title = "복부 통증 · 3주")),
            )

        val visitedCard =
            CardSummaryResponse(
                cardId = 1,
                title = "복부 통증 · 3주",
                chiefComplaint = "복부 통증",
                status = "CONFIRMED",
                visited = true,
                clinicName = "서울OO병원 내과",
                createdAt = "2026-09-04T09:00:00+09:00",
            )

        val draftCard =
            CardSummaryResponse(
                cardId = 2,
                title = "두통 · 잦은 어지러움",
                chiefComplaint = "두통",
                status = "DRAFT",
                createdAt = "2026-08-21T09:00:00+09:00",
            )
    }
}
