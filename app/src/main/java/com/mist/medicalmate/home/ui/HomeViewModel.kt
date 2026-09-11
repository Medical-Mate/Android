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

/**
 * 홈 화면 상태 보유자.
 *
 * [Clock]을 받는다. "지난 진료 후 12일이 지났어요"와 D-day가 오늘이 며칠인지에 달려 있어서,
 * 시스템 시계를 직접 읽으면 그 계산을 테스트할 수 없다.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(private val repository: HomeRepository, private val clock: Clock) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

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
}

/**
 * 읽지 않은 알림 표시는 늘 꺼 둔다.
 *
 * `HomeResponse`에 대응하는 값이 없고 알림 목록 API도 없다. `POST /api/me/devices`가
 * 푸시 토큰을 받지만 문서가 "현재 미사용"이라고 적었다. 서버가 알림을 주기 시작하면
 * 여기만 바꾼다.
 */
private fun HomeSnapshot.toContent() = HomeUiState.Content(
    userInitial = userInitial,
    hasUnreadNotification = false,
    todayLine = todayLine,
    resume = resume,
    savedCards = savedCards,
    upcoming = upcoming,
)
