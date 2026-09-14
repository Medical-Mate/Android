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
import com.mist.medicalmate.navigation.ConsumeResult
import com.mist.medicalmate.navigation.NavResult
import kotlinx.serialization.Serializable
import java.time.LocalDate

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
)

internal fun NavGraphBuilder.scheduleAddDestination(
    onHospitalPick: () -> Unit,
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
    onHospitalPick: () -> Unit,
    onCardNew: () -> Unit,
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load(appointmentId, date) }

    // 병원 찾기에서 골라 돌아온 값. 엔트리가 그대로 살아 있어서 적어 둔 날짜·시간·할 일이
    // 남는다. 라우트 인자는 처음부터 병원이 정해진 채로 열리는 경우(1r-4-B)에 쓴다.
    entry.ConsumeResult(NavResult.HOSPITAL_NAME, viewModel::onHospitalPicked)

    LaunchedEffect(hospitalName) { viewModel.onHospitalPicked(hospitalName) }

    // 캘린더에서 고른 날이나 일자 화면의 그 날. 없이 열리면 이 화면에서 고른다(1r-4-D).
    LaunchedEffect(date) { viewModel.onDatePrefilled(date) }

    ScheduleAddScreen(
        state = state,
        callbacks =
        ScheduleAddCallbacks(
            todo = viewModel.todo,
            onCloseClick = onExit,
            onHospitalClick = onHospitalPick,
            onSheetOpen = viewModel::onSheetOpen,
            onSheetDismiss = viewModel::onSheetDismiss,
            onDateConfirm = viewModel::onDateConfirm,
            onTimeConfirm = viewModel::onTimeConfirm,
            onCardPickChange = viewModel::onCardPickChange,
            onCardNewClick = onCardNew,
            onSaveClick = { viewModel.onSaveClick(onSaved) },
        ),
        modifier = modifier,
    )
}
