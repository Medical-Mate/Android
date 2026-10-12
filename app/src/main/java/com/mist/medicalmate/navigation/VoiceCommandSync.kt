package com.mist.medicalmate.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.calendar.ui.ScheduleAddDestination
import com.mist.medicalmate.card.ui.BriefCardListDestination
import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.intake.ui.IntakeDestination
import com.mist.medicalmate.profile.ui.MyProfileDestination
import com.mist.medicalmate.voice.ui.VoiceCommand
import com.mist.medicalmate.voice.ui.VoiceCommandPanel
import com.mist.medicalmate.voice.ui.VoiceCommandViewModel

/**
 * "메이트야"로 부르고 말한 대로 화면을 옮긴다(#273).
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
internal fun VoiceCommandSync(
    navController: NavHostController,
    session: SessionUiState,
    onboardingCompleted: Boolean,
    viewModel: VoiceCommandViewModel = hiltViewModel(),
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
        val command = state.command ?: return@LaunchedEffect
        viewModel.onCommandHandled()
        navController.perform(command)
    }

    state.panel?.let { panel -> VoiceCommandPanel(panel = panel, onDismiss = viewModel::dismiss) }
}

/**
 * 명령을 화면 이동으로.
 *
 * 탭 셋은 탭을 누른 것과 같다. 뒤로는 시작 화면에서 멈춘다 — 그 아래로 pop하면 그래프가
 * 빈다.
 */
private fun NavHostController.perform(command: VoiceCommand) {
    when (command) {
        VoiceCommand.HOME -> selectTab(MedicalMateTab.HOME)
        VoiceCommand.CALENDAR -> selectTab(MedicalMateTab.CALENDAR)
        VoiceCommand.RECORD -> selectTab(MedicalMateTab.RECORD)
        VoiceCommand.PROFILE -> navigate(MyProfileDestination) { launchSingleTop = true }
        VoiceCommand.BACK -> if (previousBackStackEntry != null) popBackStack()
        VoiceCommand.INTAKE -> navigate(IntakeDestination(null))
        VoiceCommand.BRIEF_CARDS -> navigate(BriefCardListDestination) { launchSingleTop = true }
        VoiceCommand.SCHEDULE_ADD -> navigate(ScheduleAddDestination())
    }
}
