package com.mist.medicalmate.home.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.home.ui.HomeTodayLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/**
 * 응답을 화면 값으로 옮기는 부분.
 *
 * 전에는 이 단언들이 `HomeViewModelTest`에 픽스처 검사로 있었다. 픽스처가 서버로 바뀌면서
 * 실제 판단이 여기로 왔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {
    @Test
    fun `마지막 진료일이 없으면 첫 방문이다`() = runTest {
        // 문서: "신규 사용자는 전부 null 또는 빈 배열입니다. 404가 아닙니다."
        val snapshot = load(HomeResponse())

        assertEquals(HomeTodayLine.FirstVisit, snapshot.todayLine)
    }

    @Test
    fun `카드만 있으면 날짜를 잡으라고 한다`() = runTest {
        val snapshot = load(HomeResponse(recentCards = listOf(confirmedCard)))

        assertEquals(HomeTodayLine.CardReady, snapshot.todayLine)
    }

    @Test
    fun `오늘 일정은 시각 전이면 앞두고 있는 것이다`() = runTest {
        val snapshot = load(
            HomeResponse(nextAppointment = appointment.copy(scheduledOn = "2026-09-11")),
            today = LocalDate.of(2026, 9, 11),
            now = LocalTime.of(9, 0),
        )

        assertEquals(
            HomeTodayLine.TodayAhead(clinic = "서울OO병원 내과", time = LocalTime.of(10, 30)),
            snapshot.todayLine,
        )
    }

    @Test
    fun `오늘 일정의 시각이 지났는데 기록이 없으면 묻는다`() = runTest {
        val snapshot = load(
            HomeResponse(nextAppointment = appointment.copy(scheduledOn = "2026-09-11")),
            today = LocalDate.of(2026, 9, 11),
            now = LocalTime.of(14, 0),
        )

        assertEquals(HomeTodayLine.TodayDone, snapshot.todayLine)
    }

    @Test
    fun `오늘 기록이 있으면 다른 것보다 앞선다`() = runTest {
        // 시각이 지났는지도, 다음 일정도 보지 않는다. 오늘 남긴 것이 가장 최근의 일이다.
        val snapshot = load(
            HomeResponse(lastVisitedOn = "2026-09-11", nextAppointment = appointment),
            today = LocalDate.of(2026, 9, 11),
        )

        assertEquals(HomeTodayLine.TodayRecorded, snapshot.todayLine)
    }

    @Test
    fun `다음 진료가 내일이면 날수를 적지 않는다`() = runTest {
        val snapshot = load(
            HomeResponse(nextAppointment = appointment.copy(scheduledOn = "2026-09-12")),
            today = LocalDate.of(2026, 9, 11),
        )

        assertEquals(
            HomeTodayLine.NextTomorrow(clinic = "서울OO병원 내과", time = LocalTime.of(10, 30)),
            snapshot.todayLine,
        )
    }

    @Test
    fun `다음 진료가 이틀 이상 남으면 날수를 적는다`() = runTest {
        val snapshot = load(
            HomeResponse(nextAppointment = appointment.copy(scheduledOn = "2026-09-16")),
            today = LocalDate.of(2026, 9, 11),
        )

        assertEquals(
            HomeTodayLine.NextInDays(days = 5, on = LocalDate.of(2026, 9, 16), clinic = "서울OO병원 내과"),
            snapshot.todayLine,
        )
    }

    @Test
    fun `다음 진료가 지난 진료를 이긴다`() = runTest {
        // 앞으로 할 일이 지나간 일보다 급하다. 경과일은 다음 진료가 없을 때만 적는다.
        val snapshot = load(
            HomeResponse(lastVisitedOn = "2026-08-30", nextAppointment = appointment.copy(scheduledOn = "2026-09-16")),
            today = LocalDate.of(2026, 9, 11),
        )

        assertTrue(snapshot.todayLine is HomeTodayLine.NextInDays)
    }

    @Test
    fun `어제 다녀왔으면 날수를 적지 않는다`() = runTest {
        val snapshot = load(HomeResponse(lastVisitedOn = "2026-09-10"), today = LocalDate.of(2026, 9, 11))

        assertEquals(HomeTodayLine.LastYesterday, snapshot.todayLine)
    }

    @Test
    fun `지난 진료가 이틀 이상 지나면 날수를 적는다`() = runTest {
        val snapshot = load(HomeResponse(lastVisitedOn = "2026-08-30"), today = LocalDate.of(2026, 9, 11))

        assertEquals(HomeTodayLine.LastDaysAgo(12), snapshot.todayLine)
    }

    @Test
    fun `앞날 진료에 경과일을 적지 않는다`() = runTest {
        // 진료 후 기록의 날짜가 앞날이면 뺄셈이 음수가 됐다(#224). 이제 그 값은 오늘 갈래나
        // 다음 진료 갈래로 간다.
        val snapshot = load(HomeResponse(lastVisitedOn = "2026-09-16"), today = LocalDate.of(2026, 9, 14))

        assertTrue(snapshot.todayLine !is HomeTodayLine.LastDaysAgo)
    }

    @Test
    fun `기록이 빠진 지난 일정이 있으면 그 날을 알린다`() = runTest {
        val snapshot = load(
            HomeResponse(lastVisitedOn = "2026-09-02"),
            today = LocalDate.of(2026, 9, 11),
            month = listOf(appointment.copy(scheduledOn = "2026-09-08")),
        )

        assertEquals(HomeTodayLine.RecordMissing(LocalDate.of(2026, 9, 8)), snapshot.todayLine)
    }

    @Test
    fun `기록보다 앞선 지난 일정은 알리지 않는다`() = runTest {
        // 그 날 것은 이미 적었다. 기록은 진료일로 남는다.
        val snapshot = load(
            HomeResponse(lastVisitedOn = "2026-09-08"),
            today = LocalDate.of(2026, 9, 11),
            month = listOf(appointment.copy(scheduledOn = "2026-09-02")),
        )

        assertEquals(HomeTodayLine.LastDaysAgo(3), snapshot.todayLine)
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
        // 상한이 20이다. 그대로 찍으면 "20단계 중 2단계"가 된다.
        val snapshot = load(
            HomeResponse(
                inProgressSession =
                InProgressSessionResponse(sessionId = 7, siteText = "복부", progressCurrent = 2, progressTotal = 20),
            ),
        )

        assertEquals(IntakeStep.total, 4)
        assertEquals(2, requireNotNull(snapshot.resume).step.number)
    }

    @Test
    fun `한 마디도 answer 안 했으면 부위까지만 답한 것이다`() = runTest {
        // 세션은 부위를 짚어야 생긴다. 그래서 왕복이 0이어도 1단계는 끝나 있다.
        val snapshot = load(
            HomeResponse(
                inProgressSession =
                InProgressSessionResponse(sessionId = 7, siteText = "오른쪽 손목", progressCurrent = 0, progressTotal = 20),
            ),
        )

        assertEquals(IntakeStep.BODY_PART, requireNotNull(snapshot.resume).step)
    }

    @Test
    fun `작성 중이던 문답이 없으면 이어서 하기도 없다`() = runTest {
        assertNull(load(HomeResponse()).resume)
    }

    @Test
    fun `진료 완료 배지는 확정이 아니라 다녀왔는지로 갈린다`() = runTest {
        // 확정은 카드를 더 안 고친다는 뜻이고, 진료를 다녀왔는지는 다른 축이다.
        // 확정만 한 카드에 "진료 완료"가 붙으면 안 된다.
        val snapshot = load(HomeResponse(recentCards = listOf(confirmedCard, draftCard)))

        assertEquals(listOf(true, false), snapshot.savedCards.map { it.visited })
    }

    @Test
    fun `확정했지만 안 다녀온 카드는 배지가 없다`() = runTest {
        val confirmedNotVisited = draftCard.copy(status = "CONFIRMED", visited = false)

        val snapshot = load(HomeResponse(recentCards = listOf(confirmedNotVisited)))

        assertFalse(snapshot.savedCards.single().visited)
    }

    @Test
    fun `확정 전 카드에는 병원이 없다`() = runTest {
        val snapshot = load(HomeResponse(recentCards = listOf(draftCard)))

        assertNull(snapshot.savedCards.single().clinic)
    }

    @Test
    fun `카드 작성일은 날짜 타입으로 옮긴다`() = runTest {
        // 포맷은 화면 몫이다. 서버가 만든 문자열을 그대로 쓰면 날짜 계산을 다시 할 수 없다.
        val snapshot = load(HomeResponse(recentCards = listOf(confirmedCard)))

        assertEquals(LocalDate.of(2026, 9, 4), snapshot.savedCards.single().writtenOn)
    }

    @Test
    fun `다음 일정은 날짜와 시각으로 갈라 담는다`() = runTest {
        val snapshot = load(HomeResponse(nextAppointment = appointment))

        val schedule = snapshot.upcoming.single()
        assertEquals("1", schedule.id)
        assertEquals(LocalDate.of(2026, 9, 12), schedule.date)
        assertEquals("오전 10:30", schedule.time)
    }

    @Test
    fun `다음 일정이 없으면 목록이 빈다`() = runTest {
        assertTrue(load(HomeResponse()).upcoming.isEmpty())
    }

    @Test
    fun `이름 첫 글자를 아바타에 쓴다`() = runTest {
        assertEquals("김", load(HomeResponse(), name = "김지훈").userInitial)
    }

    @Test
    fun `이름을 못 받아도 홈은 그린다`() = runTest {
        // 아바타 한 글자 때문에 화면 전체를 실패로 만들지 않는다.
        val repository = repository(HomeResponse(), nameResult = ApiResult.NetworkUnavailable(IOException()))

        val result = repository.load(LocalDate.of(2026, 9, 11), LocalTime.of(9, 0))

        assertTrue(result is ApiResult.Success)
        assertEquals("", (result as ApiResult.Success).value.userInitial)
    }

    @Test
    fun `홈 본문이 실패하면 실패다`() = runTest {
        val repository =
            DefaultHomeRepository(
                api = object : HomeApi {
                    override suspend fun home(): HomeResponse = throw IOException()

                    override suspend fun appointments(year: Int, month: Int) = emptyList<AppointmentResponse>()
                },
                currentUser = { ApiResult.Success("김지훈") },
                json = Json,
            )

        assertTrue(repository.load(LocalDate.of(2026, 9, 11), LocalTime.of(9, 0)) is ApiResult.NetworkUnavailable)
    }

    private suspend fun load(
        response: HomeResponse,
        today: LocalDate = LocalDate.of(2026, 9, 11),
        name: String? = "김지훈",
        now: LocalTime = LocalTime.of(9, 0),
        month: List<AppointmentResponse> = emptyList(),
    ): HomeSnapshot {
        val result = repository(response, ApiResult.Success(name), month).load(today, now)
        return (result as ApiResult.Success).value
    }

    private fun repository(
        response: HomeResponse,
        nameResult: ApiResult<String?>,
        monthly: List<AppointmentResponse> = emptyList(),
    ) = DefaultHomeRepository(
        api = object : HomeApi {
            override suspend fun home(): HomeResponse = response

            override suspend fun appointments(year: Int, month: Int): List<AppointmentResponse> = monthly
        },
        currentUser = CurrentUserProvider { nameResult },
        json = Json,
    )

    private companion object {
        val appointment =
            AppointmentResponse(
                appointmentId = 1,
                clinicName = "서울OO병원 내과",
                scheduledOn = "2026-09-12",
                scheduledTime = "10:30:00",
                cards = listOf(LinkedCardResponse(cardId = 1, title = "서울OO병원 내과 재진")),
            )

        val confirmedCard =
            CardSummaryResponse(
                cardId = 1,
                title = "복부 통증 · 3주",
                status = "CONFIRMED",
                visited = true,
                clinicName = "서울OO병원 내과",
                createdAt = "2026-09-04T09:00:00+09:00",
            )

        val draftCard =
            CardSummaryResponse(
                cardId = 2,
                title = "두통 · 잦은 어지러움",
                status = "DRAFT",
                createdAt = "2026-08-21T09:00:00+09:00",
            )
    }
}
