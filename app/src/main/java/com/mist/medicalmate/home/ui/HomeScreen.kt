package com.mist.medicalmate.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateActionCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateResumeCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.designsystem.component.MedicalMateTabBar
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.voice.rememberVoiceScrollState
import com.mist.medicalmate.core.voice.voicePanelSpace
import java.time.LocalDate

/**
 * 홈. Figma `Wireframe 10-15`의 1n-1(기록 있음)과 1n-2(처음)다(#312).
 *
 * 맨 위 사진 배너(400, 처음 홈은 334) 아래로 흰 시트가 28 겹쳐 올라온다. 시트에는 증상 정리 시작하기 · 이어서
 * 하기 · 최근 브리핑 카드 · 다가오는 일정이 차례로 선다. 탭바만 바닥에 고정되고 배너와 시트가 함께 스크롤한다.
 *
 * 처음 온 사람(1n-2)은 스크롤하지 않는다. 시트가 남은 높이를 전부 받고 그 안에서 빈 상태가 가운데에 선다.
 *
 * [today]를 받는 이유는 D-day를 화면이 열린 날 기준으로 계산해야 하기 때문이다. 기본값을 `LocalDate.now()`로
 * 두면 Preview와 테스트에서 값을 고정할 수 없다.
 *
 * 화면을 이루는 조각들은 `HomeComponents.kt`, Preview는 `HomePreviews.kt`에 있다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    today: LocalDate,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    onTabSelect: (MedicalMateTab) -> Unit = {},
) {
    Column(modifier = modifier.background(MedicalMateTheme.colors.bgSurface)) {
        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                HomeUiState.Loading -> LoadingContent()
                HomeUiState.Failed -> FailedContent(onRetryClick = callbacks.onRetryClick)
                is HomeUiState.Content ->
                    if (state.firstVisit) {
                        FirstHomeContent(content = state, callbacks = callbacks)
                    } else {
                        HomeContent(content = state, today = today, callbacks = callbacks)
                    }
            }
        }
        MedicalMateTabBar(selected = MedicalMateTab.HOME, onSelect = onTabSelect)
    }
}

/**
 * 홈에서 나가는 길들.
 *
 * 파라미터로 하나씩 받으면 목적지가 늘 때마다 서명이 길어진다. 아직 대상 화면이 없는 것은 호출자가 빈 동작을
 * 준다.
 */
data class HomeCallbacks(
    val onStartIntakeClick: () -> Unit = {},
    val onResumeClick: (String?) -> Unit = {},
    val onSavedCardClick: (cardId: String) -> Unit = {},
    val onAllCardsClick: () -> Unit = {},
    val onScheduleClick: (date: LocalDate, appointmentId: String) -> Unit = { _, _ -> },
    val onCalendarClick: () -> Unit = {},
    val onNotificationClick: () -> Unit = {},
    val onProfileClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
    /** 배너 1 · 오늘 진료의 "진료 후 기록하기". 병원 확인(1m-12)부터 간다. */
    val onRecordVisitClick: (HomeVisitLink) -> Unit = {},
    /** 일정 추가(1r-4). 카드가 있으면 그 카드가 골라진 채 열린다. */
    val onScheduleAddClick: (cardId: String?) -> Unit = {},
    /** 배너 2 · 예약 문자의 "방법 보기". 일정 추가와 방법 안내 시트(1r-4-S)가 함께 열린다. */
    val onMessageGuideClick: () -> Unit = {},
    /** 배너 4의 "카드 저장하기". 카드가 없으면 카드 목록이다. */
    val onSaveCardClick: (cardId: String?) -> Unit = {},
    /** 배너 5의 "메이트에게 말해보기". 홈 위에 메이트 카드가 뜬다(1n-1-M). */
    val onMateClick: () -> Unit = {},
) {
    /** 배너 링크가 가는 곳. 시안의 "흐름" 메모를 따른다. */
    fun onBannerClick(banner: HomeBanner) {
        when (banner) {
            HomeBanner.Welcome -> Unit
            is HomeBanner.Today -> onRecordVisitClick(banner.visit)
            is HomeBanner.Tomorrow -> onSavedCardClick(banner.cardId)
            is HomeBanner.CardReady -> onScheduleAddClick(banner.cardId)
            HomeBanner.Message -> onMessageGuideClick()
            HomeBanner.Schedule -> onScheduleAddClick(null)
            is HomeBanner.SaveCard -> onSaveCardClick(banner.cardId)
            HomeBanner.Mate -> onMateClick()
        }
    }
}

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

/** 1n-1. 배너와 시트가 함께 스크롤한다. */
@Composable
private fun HomeContent(content: HomeUiState.Content, today: LocalDate, callbacks: HomeCallbacks) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberVoiceScrollState()).voicePanelSpace()) {
        HomeBannerPager(content = content, callbacks = callbacks)
        HomeSheet {
            SheetTop(content = content, callbacks = callbacks)
            if (content.savedCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
                    MedicalMateSectionHeader(
                        title = stringResource(R.string.home_saved_cards),
                        actionLabel = stringResource(R.string.home_saved_cards_all),
                        onActionClick = callbacks.onAllCardsClick,
                    )
                    BriefingList(cards = content.savedCards, onCardClick = callbacks.onSavedCardClick)
                }
            }
            content.upcoming?.let { schedule ->
                Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
                    MedicalMateSectionHeader(
                        title = stringResource(R.string.home_upcoming),
                        actionLabel = stringResource(R.string.home_calendar),
                        onActionClick = callbacks.onCalendarClick,
                    )
                    ScheduleCard(
                        schedule = schedule,
                        today = today,
                        onClick = { callbacks.onScheduleClick(schedule.date, schedule.id) },
                    )
                }
            }
        }
    }
}

/**
 * 1n-2. 처음 온 사람. 배너가 짧고(334) 시트가 남은 높이를 받아 빈 상태를 가운데에 둔다.
 *
 * **빈 상태에 행동 버튼을 두지 않는다.** 시안의 인스턴스가 그 자리를 꺼 두었다. 바로 위에 "증상 정리
 * 시작하기"가 있다.
 */
@Composable
private fun FirstHomeContent(content: HomeUiState.Content, callbacks: HomeCallbacks) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeBannerPager(content = content, callbacks = callbacks)
        HomeSheet(modifier = Modifier.weight(1f)) {
            SheetTop(content = content, callbacks = callbacks)
            MedicalMateEmptyState(
                type = MedicalMateEmptyStateType.NO_RECORD,
                title = stringResource(R.string.home_empty_title),
                description = stringResource(R.string.home_empty_description),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 시트 맨 위. `Card / Action` 다음에 진행 중인 문답이 있으면 `Card / Resume`가 선다. */
@Composable
private fun SheetTop(content: HomeUiState.Content, callbacks: HomeCallbacks) {
    MedicalMateActionCard(
        label = stringResource(R.string.home_action_label),
        title = stringResource(R.string.home_start_intake),
        onClick = callbacks.onStartIntakeClick,
    )
    content.resume?.let { resume ->
        MedicalMateResumeCard(
            done = resume.doneSteps,
            total = IntakeStep.total,
            label = stringResource(R.string.home_resume_label),
            title = stringResource(R.string.home_resume_step, resume.symptomTitle, resume.doneSteps),
            onClick = { callbacks.onResumeClick(resume.intakeId) },
        )
    }
}
