package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 와이어프레임 1s-1. 홈 헤더의 아바타에서 들어온다. */
@Serializable
internal data object MyProfileDestination

/** 와이어프레임 1s-2. */
@Serializable
internal data object HealthEditDestination

internal fun NavGraphBuilder.myProfileDestination(
    accountActions: AccountActionCallbacks,
    onHealthEdit: () -> Unit,
    onExit: () -> Unit,
) {
    composable<MyProfileDestination> {
        MyProfileRoute(accountActions = accountActions, onHealthEdit = onHealthEdit, onExit = onExit)
    }
}

internal fun NavGraphBuilder.healthEditDestination(onSaved: () -> Unit, onExit: () -> Unit) {
    composable<HealthEditDestination> {
        HealthEditRoute(onSaved = onSaved, onExit = onExit)
    }
}

@Composable
private fun MyProfileRoute(
    accountActions: AccountActionCallbacks,
    onHealthEdit: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 1s-2에서 고치고 돌아오는 자리라 들어올 때 한 번이 아니라 보일 때마다 읽는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    MyProfileScreen(
        state = state,
        accountActions = accountActions,
        onBackClick = onExit,
        onHealthEditClick = onHealthEdit,
        onSettingChange = viewModel::onSettingChange,
        modifier = modifier,
    )
}

/**
 * 상태 있는 진입점.
 *
 * 저장하면 화면을 나간다. 저장 자체는 아직 없고, 나가는 판단은 그래프가 한다.
 */
@Composable
private fun HealthEditRoute(
    onSaved: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    HealthEditScreen(
        state = state,
        callbacks =
        HealthEditCallbacks(
            onCloseClick = onExit,
            onOptionClick = viewModel::onOptionClick,
            onAddClick = viewModel::onAddClick,
            onDraftChange = viewModel::onDraftChange,
            onDraftSubmit = viewModel::onDraftSubmit,
            onSaveClick = { viewModel.onSaveClick(onSaved) },
        ),
        modifier = modifier,
    )
}
