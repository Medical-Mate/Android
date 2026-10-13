package com.mist.medicalmate.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.calendar.ui.ScheduleAddDestination
import com.mist.medicalmate.card.ui.BriefCardListDestination
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.core.voice.LocalVoicePanelInset
import com.mist.medicalmate.core.voice.LocalVoiceTargets
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

    // 화면을 옮기기 전에 비운다. 옮기면서 이 효과가 취소되면 같은 명령이 다시 흘러나간다.
    LaunchedEffect(state.command) {
        val request = state.command ?: return@LaunchedEffect
        viewModel.onCommandHandled()
        navController.perform(request)
    }

    // 막대는 화면 위에 겹쳐 뜬다. 화면을 줄이지 않고 그 높이만 내려 주어 스크롤 끝에 빈자리를 붙인다(#284).
    var panelHeight by remember { mutableIntStateOf(0) }
    val inset = with(LocalDensity.current) { if (state.panel != null) panelHeight.toDp() else 0.dp }
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
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { panelHeight = it.height },
            )
        }
    }
}

/**
 * 명령을 화면 이동으로.
 *
 * 탭 셋은 탭을 누른 것과 같다. 뒤로는 시작 화면에서 멈춘다. 그 아래로 pop하면 그래프가
 * 빈다.
 *
 * 일정 추가는 말에 날짜나 병원이 들어 있으면 그 말을 예약 문자처럼 들고 간다. "10월 20일 오후
 * 3시 고려튼튼통증의학과 일정 추가해 줘"가 칸이 채워진 일정 추가가 된다(#282). 아무것도 없는 "일정
 * 추가해 줘"에 넘기면 "날짜를 찾지 못했어요"가 떠서 넘기지 않는다. 말로 연 것이라는 표시(`voice`)를
 * 함께 실어, 병원을 말했으면 병원 찾기가 하나로 좁혀질 때 알아서 고른다.
 */
private fun NavHostController.perform(request: VoiceCommandRequest) {
    when (request.command) {
        VoiceCommand.HOME -> selectTab(MedicalMateTab.HOME)
        VoiceCommand.CALENDAR -> selectTab(MedicalMateTab.CALENDAR)
        VoiceCommand.RECORD -> selectTab(MedicalMateTab.RECORD)
        VoiceCommand.PROFILE -> navigate(MyProfileDestination) { launchSingleTop = true }
        VoiceCommand.BACK -> if (previousBackStackEntry != null) popBackStack()
        // "오른쪽 어깨가 아파"의 부위를 증상 정리가 바로 고른다(#284).
        VoiceCommand.INTAKE -> navigate(IntakeDestination(null, spoken = request.text))
        VoiceCommand.BRIEF_CARDS -> navigate(BriefCardListDestination) { launchSingleTop = true }
        VoiceCommand.SCHEDULE_ADD ->
            navigate(ScheduleAddDestination(sharedText = request.text.takeIf(::mentionsSchedule), voice = true))
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
