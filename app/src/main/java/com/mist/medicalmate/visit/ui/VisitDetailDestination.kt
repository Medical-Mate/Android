package com.mist.medicalmate.visit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * 저장한 진료 후 기록 하나를 보는 목적지(#256).
 *
 * 1q-1의 [VisitRecordDestination]과 다르다. 그쪽은 메모를 들고 들어와 나누고 저장하는 흐름의
 * 끝이고, 여기는 저장된 것을 id로 읽는다. 진료 후 기록 흐름 바깥에서 들어오는 유일한 자리라
 * 흐름의 목적지 파일에 섞지 않았다.
 */
@Serializable
internal data class VisitDetailDestination(val visitId: String)

internal fun NavGraphBuilder.visitDetailDestination(onExit: () -> Unit) {
    composable<VisitDetailDestination> { entry ->
        VisitDetailRoute(visitId = entry.toRoute<VisitDetailDestination>().visitId, onExit = onExit)
    }
}

@Composable
private fun VisitDetailRoute(
    visitId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(visitId) { viewModel.load(visitId) }

    VisitDetailScreen(
        state = state,
        onBackClick = onExit,
        onRetryClick = { viewModel.load(visitId) },
        modifier = modifier,
    )
}
