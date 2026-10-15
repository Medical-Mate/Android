package com.mist.medicalmate.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.home.data.HomeRepository
import com.mist.medicalmate.home.data.HomeSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/**
 * 홈 화면 상태 보유자.
 *
 * [Clock]을 받는다. 배너 첫 장과 D-day가 오늘이 며칠인지에 달려 있어서, 시스템 시계를 직접 읽으면 그 계산을
 * 테스트할 수 없다.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(private val repository: HomeRepository, private val clock: Clock) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    /** 첫 장을 무작위로 고를 때 쓴다. 시험이 바꿔 끼운다. */
    internal var random: Random = Random.Default

    /**
     * 상황에 맞는 첫 장이 없을 때 고른 장(2~5장의 차례). 이 화면이 살아 있는 동안은 같은 장을 둔다 — 홈으로
     * 돌아올 때마다 다시 읽는데 그때마다 첫 장이 바뀌면 방금 본 배너가 사라진 것처럼 보인다.
     */
    private var randomIndex: Int? = null

    /**
     * 홈을 다시 읽는다.
     *
     * 화면으로 돌아올 때마다 부른다. 문답을 하다 나오면 진행도가 달라져 있고, 카드를
     * 만들거나 일정을 넣어도 마찬가지다.
     */
    fun refresh() {
        mutableUiState.value = HomeUiState.Loading
        viewModelScope.launch {
            mutableUiState.value =
                when (val result = repository.load(LocalDate.now(clock))) {
                    is ApiResult.Success -> result.value.toContent()
                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> HomeUiState.Failed
                }
        }
    }

    /**
     * 읽지 않은 알림 표시는 늘 꺼 둔다.
     *
     * `HomeResponse`에 대응하는 값이 없고 알림 목록 API도 없다. 서버가 알림을 주기 시작하면 여기만 바꾼다.
     */
    private fun HomeSnapshot.toContent() = HomeUiState.Content(
        userInitial = userInitial,
        hasUnreadNotification = false,
        banners = banners(),
        resume = resume,
        savedCards = savedCards,
        upcoming = upcoming,
    )

    /**
     * 배너 다섯 장(#312). 첫 장이 상황에 따라 바뀌고 2~5장은 그대로다. 처음 온 사람은 환영 한 장뿐이다
     * (1n-2 — 장 수도 넘김도 없다).
     */
    private fun HomeSnapshot.banners(): List<HomeBanner> {
        val rest =
            listOf(
                HomeBanner.Message,
                HomeBanner.Schedule,
                HomeBanner.SaveCard(savedCards.firstOrNull()?.id),
                HomeBanner.Mate,
            )
        val first =
            when (val lead = lead) {
                HomeLead.FirstVisit -> return listOf(HomeBanner.Welcome)
                is HomeLead.TodayVisit -> HomeBanner.Today(lead.clinic, lead.visit)
                is HomeLead.TomorrowVisit -> HomeBanner.Tomorrow(lead.time, lead.clinic, lead.cardId)
                is HomeLead.CardReady -> HomeBanner.CardReady(lead.cardId, lead.symptom)
                HomeLead.Other -> rest[randomIndex ?: random.nextInt(rest.size).also { randomIndex = it }]
            }
        return listOf(first) + rest
    }
}
