package com.mist.medicalmate.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.calendar.ui.CalendarDayDestination
import com.mist.medicalmate.calendar.ui.CalendarDestination
import com.mist.medicalmate.calendar.ui.ScheduleAddDestination
import com.mist.medicalmate.card.ui.BriefCardListDestination
import com.mist.medicalmate.card.ui.RecordDestination
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.voice.LocalVoicePanelInset
import com.mist.medicalmate.core.voice.LocalVoiceTargets
import com.mist.medicalmate.core.voice.VoiceTargets
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.intake.ui.IntakeDestination
import com.mist.medicalmate.profile.ui.MyProfileDestination
import com.mist.medicalmate.voice.ui.VoiceCommand
import com.mist.medicalmate.voice.ui.VoiceCommandPanel
import com.mist.medicalmate.voice.ui.VoiceCommandRequest
import com.mist.medicalmate.voice.ui.VoiceCommandViewModel

/**
 * "헤이 메이트"로 부르고 말한 대로 화면을 옮긴다(#273·#282).
 *
 * 그래프([content])를 감싼다. 화면들이 "이 화면에서 알아듣는 말"을 등록할 자리
 * ([LocalVoiceTargets])를 내려 주고, 막대를 그래프 위 아래쪽에 띄운다.
 *
 * 그래프 밖의 층이라 [VoiceCommandViewModel]은 Activity 스코프다. 이 함수는 `NavHost`
 * 목적지 밖에서 불리므로 `hiltViewModel()`이 Activity의 스토어에서 꺼낸다.
 *
 * **앱이 화면에 있을 때만 듣는다.** 이 자리의 생명주기가 Activity의 것이라 `RESUMED`가 곧
 * 화면에 있다는 뜻이다. 권한 창이 떠도 멈춘다. 뒤에서 마이크를 잡는 앱이 되지 않는다.
 *
 * 홈에 갈 수 있는 세션이 아니면 듣지 않는다. 로그인 화면이나 온보딩 중에 화면을 옮기면
 * 그 흐름을 건너뛰는 길이 된다. 마이크 권한은 화면에 돌아올 때마다 다시 본다 — 설정에서
 * 거둘 수 있다.
 */
@Composable
internal fun VoiceCommandHost(
    navController: NavHostController,
    session: SessionUiState,
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(session, onboardingCompleted) {
        val micGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        viewModel.setAllowed(
            signedIn = session.destination(onboardingCompleted) == HomeDestination,
            micGranted = micGranted,
        )
        viewModel.setForeground(true)
        onPauseOrDispose { viewModel.setForeground(false) }
    }
    // 앱을 나가거나 홈 화면으로 가면 막대를 다시 열지 않는다(#324). 시스템 창은 여기로 오지 않는다.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setForeground(false, leftApp = true) }

    // 돌아갈 화면이 있는지 알려 준다(#298). 첫 화면의 "뒤로"에 "돌아갔어요"라고 하지 않으려는 것이다.
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry) { viewModel.targets.canGoBack = navController.previousBackStackEntry != null }

    // 화면을 옮기기 전에 비운다. 옮기면서 이 효과가 취소되면 같은 명령이 다시 흘러나간다.
    LaunchedEffect(state.command) {
        val request = state.command ?: return@LaunchedEffect
        viewModel.onCommandHandled()
        navController.perform(request, viewModel.targets)
    }

    // 막대는 화면 위에 겹쳐 뜬다. 화면을 줄이지 않고 그 높이만 내려 주어 스크롤 끝에 빈자리를 붙인다(#284).
    // Mate Card는 탭바 위 12에 뜬다(#312). 탭바가 없는 화면은 바닥 위 12다. 잰 높이에 탭바를 넣지 않는다 —
    // 탭바 화면의 스크롤은 탭바 위에서 끝나서 그만큼 비우면 너무 많이 빈다.
    val gap = MedicalMateSpace.s12
    var panelHeight by remember { mutableIntStateOf(0) }
    val inset = with(LocalDensity.current) { if (state.panel != null) panelHeight.toDp() + gap else 0.dp }
    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalVoiceTargets provides viewModel.targets,
            LocalVoicePanelInset provides inset,
            content = content,
        )
        state.panel?.let { panel ->
            VoiceCommandPanel(
                panel = panel,
                onDismiss = viewModel::dismiss,
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (entry.onTabRoot()) MedicalMateSize.tabBarHeight + gap else gap)
                    .onSizeChanged { panelHeight = it.height },
            )
        }
    }
}

/**
 * 명령을 화면 이동으로.
 *
 * **손으로 들어갈 때와 같은 스택을 만든다**(#298). 처음에는 지금 화면 위에 그냥 쌓아서, 일정 추가나 병원 찾기
 * 같은 깊은 화면에서 "배가 아파"라고 하면 그 위에 증상 정리가 쌓이고, 끝나고 뒤로 가면 엉뚱한 화면이 나왔다.
 * 이제 그 화면에 손으로 들어가는 탭의 첫 화면으로 돌아간 뒤 그 위에 연다([voiceTabOf]). 증상 정리·브리핑 카드
 * 목록·내 정보는 홈 위에, 일정 추가·일자 화면은 캘린더 위에 선다. 같은 화면이 이미 맨 위에 있으면 쌓지 않는다.
 *
 * 탭 셋은 탭을 누른 것과 같다. 다만 날짜를 말한 "캘린더"는 그 탭에 저장해 둔 화면을 되살리지 않는다. 예전에
 * 열어 둔 일자 화면이 되살아나면 날짜를 여는 말이 그 화면에 잘못 넘어간다.
 *
 * 캘린더·카드 목록·일자 화면은 옮겨 간 화면이 그 말을 받아 날짜나 카드를 연다(#286·#288). 일정 추가는 말에
 * 날짜나 병원이 들어 있으면 그 말을 예약 문자처럼 들고 간다(#282).
 */
private fun NavHostController.perform(request: VoiceCommandRequest, targets: VoiceTargets) {
    val command = request.command
    val tab = voiceTabOf(command)
    val text = request.text
    when (command) {
        VoiceCommand.HOME -> selectTab(tab)
        // "10월 2일 기록 보여 줘", "최근 기록 열어 줘"는 기록 탭이 그 말로 기록을 연다(#324).
        VoiceCommand.RECORD -> {
            targets.handOff(text)
            selectTab(tab)
        }
        VoiceCommand.CALENDAR ->
            if (DATE_HINT.containsMatchIn(text)) {
                targets.handOff(text)
                openTabRoot(tab)
            } else {
                selectTab(tab)
            }

        VoiceCommand.BACK -> if (previousBackStackEntry != null) popBackStack()
        VoiceCommand.INTAKE -> openOn(tab, IntakeDestination(null, spoken = text), targets)
        // 캘린더가 그 날 진료가 있는지 보고 일자 화면을 연다(#320). 바로 열면 진료가 없는 날도 빈 화면이 떴다.
        VoiceCommand.VISIT_RECORD -> {
            targets.handOff(text)
            openTabRoot(tab)
        }

        VoiceCommand.BRIEF_CARDS -> {
            targets.handOff(text)
            openOn(tab, BriefCardListDestination, targets)
        }

        // 내 정보가 그 말로 알림을 켜고 끄거나 알림 시간을 연다(#320).
        VoiceCommand.PROFILE -> {
            targets.handOff(text)
            openOn(tab, MyProfileDestination, targets)
        }

        VoiceCommand.SCHEDULE_ADD ->
            openOn(tab, ScheduleAddDestination(sharedText = text.takeIf(::mentionsSchedule), voice = true), targets)

        // 하던 정리와 다음 진료는 홈이 안다(#320). 홈이 그 말을 받아 답한다.
        VoiceCommand.RESUME, VoiceCommand.NEXT_VISIT -> {
            targets.handOff(text)
            openTabRoot(tab)
        }
    }
}

/**
 * [tab]의 첫 화면으로 돌아간 뒤 [destination]을 연다. 이미 [destination]과 같은 화면이 맨 위에 있으면 그대로
 * 둔다 — 쓰던 것을 버리고 새로 열지 않는다.
 */
private fun NavHostController.openOn(tab: MedicalMateTab, destination: Any, targets: VoiceTargets) {
    // 일자 화면은 날짜가 다르면 다른 화면이다(#320). 10월 15일을 보다가 "어제 진료 기록할래"라고 하면 어제를 연다.
    val sameDay =
        destination !is CalendarDayDestination ||
            currentBackStackEntry?.toRoute<CalendarDayDestination>()?.date == destination.date
    if (currentDestination?.hasRoute(destination::class) == true && sameDay) {
        // 옮겨 가지 않으니 넘긴 말을 거둔다. 남겨 두면 나중에 등록하는 엉뚱한 화면이 받는다.
        targets.takeHandoff()
        return
    }
    openTabRoot(tab)
    navigate(destination) { launchSingleTop = true }
}

/**
 * [tab]의 첫 화면으로 간다. 탭을 누를 때와 달리 그 탭에 저장해 둔 화면을 되살리지 않는다. 떠나는 탭의 화면은
 * 탭을 누를 때처럼 저장해 둔다.
 */
private fun NavHostController.openTabRoot(tab: MedicalMateTab) {
    navigate(tab.destination()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
    }
}

/**
 * 말에 일정 칸으로 읽을 것이 있는지. 날짜("내일"·"다음 주"·요일·숫자)나 병원 이름의 끝("병원"·"의원"·
 * "○○과")을 본다. 읽는 것은 일정 추가가 한다. "OO병원 일정 추가해 줘"도 병원을 들고 가야 한다.
 */
private fun mentionsSchedule(text: String): Boolean =
    DATE_HINT.containsMatchIn(text) || HOSPITAL_HINT.containsMatchIn(text)

private val HOSPITAL_HINT = Regex("""병원|의원|치과|한의원|클리닉|과\s""")

private val DATE_HINT = Regex("""\d|오늘|내일|모레|글피|다음\s?주|이번\s?주|다음\s?달|[월화수목금토일]요일""")

/** 탭바가 있는 화면인지. 탭의 첫 화면 셋만 탭바를 단다. */
private fun NavBackStackEntry?.onTabRoot(): Boolean {
    val destination = this?.destination ?: return false
    return destination.hasRoute<HomeDestination>() ||
        destination.hasRoute<CalendarDestination>() ||
        destination.hasRoute<RecordDestination>()
}
