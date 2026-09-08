package com.mist.medicalmate.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateToast
import com.mist.medicalmate.core.designsystem.component.MedicalMateToastTone
import java.time.LocalDate

/**
 * 와이어프레임 1n. Figma `399:1339`(기록 있음)과 `399:1720`(기록 없음)을 옮겼다.
 *
 * 두 화면이 같은 상태의 두 갈래다. 헤더, 오늘의 한 줄, 시작 버튼까지 같고 그 아래가
 * 목록이거나 빈 상태다.
 *
 * 블록 사이 간격이 모두 16이다. `Section Header`는 자체 여백(위 24 아래 10)을 갖고
 * 있어 추가 간격을 주지 않는다.
 *
 * [today]를 받는 이유는 D-day를 화면이 열린 날 기준으로 계산해야 하기 때문이다. 기본값을
 * `LocalDate.now()`로 두면 Preview와 테스트에서 값을 고정할 수 없다.
 *
 * **Tab Bar는 붙이지 않았다.** 컴포넌트는 있지만 네비게이션 그래프에 기록·캘린더 목적지가
 * 없다. 세 탭 중 둘이 눌러도 아무 일이 없으면 사용자가 눌러본다. 목적지가 생기면 붙인다.
 *
 * 화면을 이루는 조각들은 `HomeComponents.kt`, Preview는 `HomePreviews.kt`에 있다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    today: LocalDate,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    accountActions: AccountActionCallbacks = AccountActionCallbacks(),
    registeredToastVisible: Boolean = false,
) {
    Box(modifier = modifier) {
        when (state) {
            HomeUiState.Loading -> LoadingContent()
            HomeUiState.Failed -> FailedContent(onRetryClick = callbacks.onRetryClick)
            is HomeUiState.Content ->
                HomeContent(
                    content = state,
                    today = today,
                    callbacks = callbacks,
                    accountActions = accountActions,
                )
        }
        if (registeredToastVisible) {
            RegisteredToast()
        }
    }
}

/**
 * 신상정보를 등록하고 홈에 도착했을 때의 토스트. Figma `1b-4 · 홈 · 등록 완료 토스트`
 * (`681:3700`)의 `681:3768`이다.
 *
 * 헤더 아래 68에 얹는다. 헤더를 가리지 않고 첫 카드 위에 뜬다.
 *
 * **문서 8.4는 Toast를 화면 아래에 두라고 한다.** Figma의 이 인스턴스는 위에 있다. 값의
 * 정본이 Figma라서 그대로 뒀고 디자인 트랙에 넘길 항목으로 남겼다(#67).
 */
@Composable
private fun BoxScope.RegisteredToast() {
    MedicalMateToast(
        message = stringResource(R.string.home_registered_toast),
        tone = MedicalMateToastTone.POSITIVE,
        modifier =
        Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = MedicalMateSize.gutter)
            .padding(top = ToastTop),
    )
}

/** Figma가 토스트를 헤더 아래 68에 놓았다. */
private val ToastTop = 68.dp

/**
 * 홈에서 나가는 길들.
 *
 * 파라미터로 하나씩 받으면 목적지가 늘 때마다 서명이 길어진다. 아직 대상 화면이 없는
 * 것은 호출자가 빈 동작을 준다.
 */
data class HomeCallbacks(
    val onStartIntakeClick: () -> Unit = {},
    val onResumeClick: () -> Unit = {},
    val onSavedCardClick: (String) -> Unit = {},
    val onAllCardsClick: () -> Unit = {},
    val onScheduleClick: (String) -> Unit = {},
    val onCalendarClick: () -> Unit = {},
    val onNotificationClick: () -> Unit = {},
    val onProfileClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

/**
 * 계정 관련 임시 동작. Figma에 없는 개발용 진입점이라 한 덩어리로 묶어 기본값을 준다.
 * 설정 화면이 생기면 이 파라미터는 사라진다.
 *
 * 지금 지우면 로그아웃할 방법이 없어진다. 헤더의 아바타가 프로필로 가야 하는데 그 화면이
 * 아직 없다.
 *
 * 홈이 `auth` 도메인을 직접 참조하지 않도록 콜백만 받는다. 실제 수행은
 * `SessionViewModel`이 하고 `MainActivity`가 연결한다.
 */
data class AccountActionCallbacks(
    val enabled: Boolean = true,
    val onLogoutClick: () -> Unit = {},
    val onWithdrawClick: () -> Unit = {},
)

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FailedContent(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.home_failed_title),
            description = stringResource(R.string.home_failed_description),
            actionLabel = stringResource(R.string.home_retry),
            onActionClick = onRetryClick,
        )
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    today: LocalDate,
    callbacks: HomeCallbacks,
    accountActions: AccountActionCallbacks,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
        PaddingValues(
            start = MedicalMateSize.gutter,
            end = MedicalMateSize.gutter,
            top = MedicalMateSpace.s8,
            bottom = MedicalMateSize.gutter,
        ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        item {
            HomeHeader(
                userInitial = content.userInitial,
                hasUnreadNotification = content.hasUnreadNotification,
                onNotificationClick = callbacks.onNotificationClick,
                onProfileClick = callbacks.onProfileClick,
            )
        }
        item { TodayLineCard(content.todayLine) }
        item { StartIntakeButton(onClick = callbacks.onStartIntakeClick) }

        content.resume?.let { resume ->
            item { ResumeCard(resume = resume, onClick = callbacks.onResumeClick) }
        }

        savedCardsSection(content = content, callbacks = callbacks)
        upcomingSection(content = content, today = today, callbacks = callbacks)

        item {
            AccountActions(
                enabled = accountActions.enabled,
                onLogoutClick = accountActions.onLogoutClick,
                onWithdrawClick = accountActions.onWithdrawClick,
            )
        }
    }
}

/**
 * 최근 브리핑 카드 구역.
 *
 * 카드가 없으면 목록 대신 빈 상태를 둔다. Figma `1n-2`가 그 화면이다. 제목만 남기고 빈
 * 목록을 두면 무엇을 해야 하는지 알 수 없다.
 */
private fun LazyListScope.savedCardsSection(content: HomeUiState.Content, callbacks: HomeCallbacks) {
    if (content.savedCards.isEmpty()) {
        item {
            MedicalMateEmptyState(
                type = MedicalMateEmptyStateType.NO_RECORD,
                title = stringResource(R.string.home_empty_title),
                description = stringResource(R.string.home_empty_description),
                actionLabel = stringResource(R.string.home_empty_action),
                onActionClick = callbacks.onStartIntakeClick,
            )
        }
        return
    }
    item {
        MedicalMateSectionHeader(
            title = stringResource(R.string.home_saved_cards),
            actionLabel = stringResource(R.string.home_saved_cards_all),
            onActionClick = callbacks.onAllCardsClick,
        )
    }
    items(items = content.savedCards, key = { it.id }) { card ->
        SavedCardRow(card = card, onClick = { callbacks.onSavedCardClick(card.id) })
    }
}

/** 다가오는 일정 구역. 일정이 없으면 구역 자체를 두지 않는다. Figma `1n-2`가 그렇다. */
private fun LazyListScope.upcomingSection(content: HomeUiState.Content, today: LocalDate, callbacks: HomeCallbacks) {
    if (content.upcoming.isEmpty()) return

    item {
        MedicalMateSectionHeader(
            title = stringResource(R.string.home_upcoming),
            actionLabel = stringResource(R.string.home_calendar),
            onActionClick = callbacks.onCalendarClick,
        )
    }
    items(items = content.upcoming, key = { it.id }) { schedule ->
        ScheduleRow(
            schedule = schedule,
            today = today,
            onClick = { callbacks.onScheduleClick(schedule.id) },
        )
    }
}
