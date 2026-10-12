package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mist.medicalmate.core.notification.rememberNotificationPermission
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.navigation.ConsumeResult
import com.mist.medicalmate.navigation.NavResult
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/**
 * 와이어프레임 1r-4. 일정 하나를 새로 만든다.
 *
 * [date]는 열릴 때 이미 정해져 있는 날이다. 캘린더에서 그 달의 날을 고르고 들어왔거나,
 * 일자 화면에서 시간을 정하러 온 경우다. 없으면 이 화면에서 고른다(1r-4-D) — 다른 달로
 * 넘겨 아무 날도 고르지 않은 채 들어오는 길이 그것이다.
 *
 * 병원 찾기에서 골라 돌아오는 값은 라우트가 아니라 결과로 받는다. 이유는 `NavResult`에
 * 있다. 라우트의 [hospitalName]은 처음부터 병원이 정해진 채로 열리는 경우다. 시안
 * `1r-4-B`가 그 상태이고,
 * 병원 찾기에서 고르고 돌아올 때도 이 값을 달고 이 목적지를 다시 만든다. 라우트에 담는
 * 이유는 화면이 다시 만들어질 때 살아 있어야 하기 때문이다. 브리핑 카드가 병원을 받는
 * 방식과 같다.
 */
@Serializable
internal data class ScheduleAddDestination(
    val hospitalName: String? = null,
    val date: String? = null,
    /**
     * 고치러 들어온 일정.
     *
     * 있으면 새로 만들지 않고 그 일정을 고친다. 시간 미정으로 저장한 일정에 시각을 채우러
     * 들어오는 길이다. 없으면 새 일정이다.
     */
    val appointmentId: Long? = null,
    /**
     * 미리 골라 둘 카드.
     *
     * 캘린더의 "이 카드로 일정 만들기"로 들어오면 그 카드가 이미 골라져 있어야 한다(#239).
     * 그 문구가 카드를 정해 놓고 들어가는 길이라 화면에서 다시 고르게 하면 두 번 고른다.
     */
    val cardId: String? = null,
    /**
     * 진료 후 기록의 재방문을 확정하러 들어온 것인지.
     *
     * 그러면 일정이 `VISIT_FOLLOW_UP`으로 만들어진다. 홈과 일자 화면이 그 값으로 "재진"을 적어서,
     * 캘린더에 점만 남는 것이 아니라 무엇 하러 가는 날인지가 남는다(#245).
     */
    val followUp: Boolean = false,
    /**
     * 공유받은 예약 문자(#270).
     *
     * 있으면 화면이 열리면서 이 글에서 병원·날짜·시간을 읽어 빈 칸을 채운다. 라우트에 담는 이유는
     * 병원과 같다 — 화면이 다시 만들어져도 남아야 한다.
     */
    val sharedText: String? = null,
    /**
     * 열릴 때 이미 정해진 시각.
     *
     * 다른 앱이 일정 추가를 요청하면서 시각까지 넘겨준 경우다(#270). [date]와 같이 비어 있을 때만
     * 채운다.
     */
    val time: String? = null,
)

internal fun NavGraphBuilder.scheduleAddDestination(
    onHospitalPick: (query: String?) -> Unit,
    onCardNew: () -> Unit,
    onSaved: () -> Unit,
    onExit: () -> Unit,
) {
    composable<ScheduleAddDestination> { entry ->
        val route = entry.toRoute<ScheduleAddDestination>()
        ScheduleAddRoute(
            entry = entry,
            hospitalName = route.hospitalName,
            date = route.date?.let(LocalDate::parse),
            appointmentId = route.appointmentId,
            cardId = route.cardId,
            followUp = route.followUp,
            sharedText = route.sharedText,
            time = route.time?.let(LocalTime::parse),
            onHospitalPick = onHospitalPick,
            onCardNew = onCardNew,
            onSaved = onSaved,
            onExit = onExit,
        )
    }
}

/**
 * 일정 추가의 진입점.
 *
 * 저장은 아직 서버로 나가지 않는다. 누르면 화면만 닫힌다. `POST /api/visits`가 붙으면
 * ViewModel이 그 호출을 맡는다.
 */
@Composable
private fun ScheduleAddRoute(
    entry: NavBackStackEntry,
    hospitalName: String?,
    date: LocalDate?,
    appointmentId: Long?,
    cardId: String?,
    followUp: Boolean,
    sharedText: String?,
    time: LocalTime?,
    onHospitalPick: (query: String?) -> Unit,
    onCardNew: () -> Unit,
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestNotificationPermission = rememberNotificationPermission()

    // 캘린더에서 고른 날이나 일자 화면의 그 날. 없이 열리면 이 화면에서 고른다(1r-4-D).
    // 다른 앱의 일정 추가 요청이면 시각도 함께 온다(#270). 글을 읽기 전에 채워 둔다 — 읽은 결과는
    // 빈 칸만 채우고, 날짜 칸이 찼는지로 안내를 고른다.
    LaunchedEffect(date, time) { viewModel.onDatePrefilled(date, time) }

    // 문자 앱에서 공유받았으면 그 글로 빈 칸을 채운다(#270).
    LaunchedEffect(Unit) { viewModel.load(appointmentId, date, cardId, followUp, sharedText) }

    // 병원 찾기에서 골라 돌아온 값. 엔트리가 그대로 살아 있어서 적어 둔 날짜·시간·할 일이
    // 남는다. 라우트 인자는 처음부터 병원이 정해진 채로 열리는 경우(1r-4-B)에 쓴다.
    entry.ConsumeResult(NavResult.HOSPITAL_NAME, viewModel::onHospitalPicked)

    LaunchedEffect(hospitalName) { viewModel.onHospitalPicked(hospitalName) }

    // 예약 문자에서 병원 이름을 읽었으면 그 이름으로 찾은 목록을 바로 연다(#270). 같은 이름의 다른
    // 지점이 있을 수 있어 사람이 주소를 보고 고른다. 옮기기 전에 소비 표시를 남긴다.
    LaunchedEffect(state.hospitalSearch) {
        val query = state.hospitalSearch ?: return@LaunchedEffect
        viewModel.onHospitalSearchOpened()
        onHospitalPick(query)
    }

    val save = {
        // 일정을 잡는 순간이 진료 알림이 왜 필요한지 보이는 자리다.
        requestNotificationPermission()
        viewModel.onSaveClick(onSaved)
    }

    // "헤이 메이트" 다음의 말(#282). 화면의 칸과 버튼을 모두 말로 부른다. 저장과 나가기는 확인을 받는다.
    VoiceTargetEffect { text ->
        viewModel.voice.handle(
            text,
            ScheduleAddVoiceRoutes(
                save = save,
                exit = onExit,
                openHospital = { onHospitalPick(state.hospital) },
                newCard = onCardNew,
            ),
        )
    }

    ScheduleAddScreen(
        state = state,
        callbacks =
        ScheduleAddCallbacks(
            todo = viewModel.todo,
            onCloseClick = onExit,
            onHospitalClick = { onHospitalPick(state.hospital) },
            onSheetOpen = viewModel::onSheetOpen,
            onSheetDismiss = viewModel::onSheetDismiss,
            onDateConfirm = viewModel::onDateConfirm,
            onTimeConfirm = viewModel::onTimeConfirm,
            onCardPickChange = viewModel::onCardPickChange,
            onCardNewClick = onCardNew,
            onSaveClick = save,
        ),
        modifier = modifier,
    )
}
