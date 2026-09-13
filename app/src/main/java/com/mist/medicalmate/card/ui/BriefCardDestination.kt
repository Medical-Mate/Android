package com.mist.medicalmate.card.ui

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
 * 와이어프레임 1e-1. 문답을 마치거나 홈·기록에서 카드를 눌러 들어온다.
 *
 * [cardId]를 라우트에 담는다. 어느 카드를 여는지가 목적지의 일부이고, 화면이 다시
 * 만들어질 때 살아 있어야 한다. 서버 연동 전에는 쓰이지 않고 픽스처가 나온다.
 */
@Serializable
internal data class BriefCardDestination(
    val cardId: String? = null,
    /**
     * 아직 카드가 없을 때 그 카드를 만들 문답.
     *
     * 문답을 마치면(1c-5) 카드가 없다. 병원을 먼저 찾고 오든 바로 오든 이 화면에서 만든다.
     * 두 길이 여기서 만나고, 만드는 자리가 하나라 어느 쪽으로 와도 같은 카드가 나온다.
     */
    val sessionId: Long? = null,
    /**
     * 카드를 만들 때 함께 보낼 진료받을 병원. 1m-B에서 고른 것이다.
     *
     * **만들 때만 쓴다.** 만들고 나면 카드가 그 값을 들고 서버가 응답에 실어 준다
     * (Backend#101). 이미 있는 카드를 열 때는 비어 있다.
     */
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
)

internal fun NavGraphBuilder.briefCardDestination(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHospitalChange: (String) -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardDestination> { entry ->
        val route = entry.toRoute<BriefCardDestination>()
        BriefCardRoute(
            entry = entry,
            // 라우트는 문자열로 들고 다닌다. 서버 id는 숫자라 여기서 바꾼다.
            cardId = route.cardId?.toLongOrNull(),
            sessionId = route.sessionId,
            hospital = briefCardHospital(route),
            onSaved = onSaved,
            onDeleted = onDeleted,
            onHospitalChange = onHospitalChange,
            onExit = onExit,
        )
    }
}

/**
 * 상태 있는 진입점.
 *
 * 편집 모드에서는 하단에 저장하기가 없다. 그 자리가 `브리핑 카드 삭제`이고, 사본을 카드에
 * 옮기는 것은 Nav 우측 `확인`이 한다. 그래서 저장하기는 읽는 중에만 나오고 항상 화면을
 * 나간다. 나가는 판단을 여기서 하는 이유는 목적지 이동이 그래프의 일이기 때문이다.
 *
 * 삭제는 대화상자의 `삭제`를 누른 뒤 화면을 나간다. 지운 카드의 화면에 남을 수 없다.
 */
@Composable
private fun BriefCardRoute(
    entry: NavBackStackEntry,
    cardId: Long?,
    sessionId: Long?,
    hospital: BriefCardHospital?,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHospitalChange: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(cardId, sessionId, hospital) { viewModel.open(cardId, sessionId, hospital) }

    // 병원 `변경`에서 골라 돌아온 값. 엔트리가 살아 있어서 편집 중이던 값이 남는다.
    entry.ConsumeResult(NavResult.HOSPITAL_NAME, NavResult.HOSPITAL_ADDRESS, viewModel::onHospitalPicked)

    val content = state as? BriefCardUiState.Content

    BriefCardScreen(
        state = state,
        callbacks =
        BriefCardCallbacks(
            onBackClick = onExit,
            onEditClick = viewModel::onEditClick,
            onEditDoneClick = viewModel::onEditDoneClick,
            onCancelClick = viewModel::onCancelClick,
            edit = viewModel.editActions,
            onSaveClick = { viewModel.onSaveClick(onSaved) },
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = { viewModel.onDeleteConfirm(onDeleted) },
            onHospitalChangeClick = { content?.card?.id?.let(onHospitalChange) },
            onRetryClick = { viewModel.open(cardId, sessionId, hospital) },
        ),
        modifier = modifier,
    )
}

/**
 * 라우트가 들고 온 병원.
 *
 * 이름이 없으면 병원이 없는 것이다. 이름만 있고 주소가 없는 경우는 만들지 않는다. 병원
 * 찾기가 둘을 함께 넘긴다.
 */
private fun briefCardHospital(route: BriefCardDestination): BriefCardHospital? {
    val name = route.hospitalName ?: return null
    return BriefCardHospital(name = name, address = route.hospitalAddress)
}
