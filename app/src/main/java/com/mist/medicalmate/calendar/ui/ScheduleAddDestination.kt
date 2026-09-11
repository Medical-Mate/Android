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

/**
 * 와이어프레임 1r-4. 일정 하나를 새로 만든다.
 *
 * 병원 찾기에서 골라 돌아오는 값은 라우트가 아니라 결과로 받는다. 이유는 `NavResult`에
 * 있다. 라우트의 [hospitalName]은 처음부터 병원이 정해진 채로 열리는 경우다. 시안
 * `1r-4-B`가 그 상태이고,
 * 병원 찾기에서 고르고 돌아올 때도 이 값을 달고 이 목적지를 다시 만든다. 라우트에 담는
 * 이유는 화면이 다시 만들어질 때 살아 있어야 하기 때문이다. 브리핑 카드가 병원을 받는
 * 방식과 같다.
 */
@Serializable
internal data class ScheduleAddDestination(val hospitalName: String? = null)

internal fun NavGraphBuilder.scheduleAddDestination(
    onHospitalPick: () -> Unit,
    onCardNew: () -> Unit,
    onSaved: () -> Unit,
    onExit: () -> Unit,
) {
    composable<ScheduleAddDestination> { entry ->
        ScheduleAddRoute(
            entry = entry,
            hospitalName = entry.toRoute<ScheduleAddDestination>().hospitalName,
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
    onHospitalPick: () -> Unit,
    onCardNew: () -> Unit,
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    // 병원 찾기에서 골라 돌아온 값. 엔트리가 그대로 살아 있어서 적어 둔 날짜·시간·할 일이
    // 남는다. 라우트 인자는 처음부터 병원이 정해진 채로 열리는 경우(1r-4-B)에 쓴다.
    entry.ConsumeResult(NavResult.HOSPITAL_NAME, viewModel::onHospitalPicked)

    LaunchedEffect(hospitalName) { viewModel.onHospitalPicked(hospitalName) }

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
