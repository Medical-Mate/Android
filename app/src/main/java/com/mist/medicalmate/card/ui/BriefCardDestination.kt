package com.mist.medicalmate.card.ui

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
 * 와이어프레임 1e-1. 문답을 마치거나 홈·기록에서 카드를 눌러 들어온다.
 *
 * [cardId]를 라우트에 담는다. 어느 카드를 여는지가 목적지의 일부이고, 화면이 다시
 * 만들어질 때 살아 있어야 한다. 서버 연동 전에는 쓰이지 않고 픽스처가 나온다.
 */
/**
 * @param hospitalName 진료 전 병원 찾기(1m-B)에서 고른 병원. 고르지 않았으면 null이다.
 * @param hospitalAddress 같은 병원의 주소.
 *
 * **id가 아니라 이름과 주소를 받는다.** id로 받으면 카드가 병원 목록을 찾아봐야 하는데 그
 * 목록은 `visit` 도메인에 있다. 한 도메인이 다른 도메인을 직접 참조하지 않는다는 규칙이
 * 있고, 서버가 붙기 전에는 고른 병원이 사실 이 두 문자열뿐이다. 병원 조회 API가 생기면
 * id를 받고 여기서 조회한다.
 */
@Serializable
internal data class BriefCardDestination(
    val cardId: String,
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
)

/** 와이어프레임 1f-1. 폰을 의사에게 건네는 화면. */
@Serializable
internal data class HandoffDestination(val cardId: String)

internal fun NavGraphBuilder.briefCardDestination(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHospitalChange: (String) -> Unit,
    onHandoff: (String) -> Unit,
    onExit: () -> Unit,
) {
    composable<BriefCardDestination> { entry ->
        val route = entry.toRoute<BriefCardDestination>()
        BriefCardRoute(
            hospital = briefCardHospital(route),
            onSaved = onSaved,
            onDeleted = onDeleted,
            onHospitalChange = onHospitalChange,
            onHandoff = onHandoff,
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.handoffDestination(onDone: () -> Unit) {
    composable<HandoffDestination> {
        HandoffRoute(onDone = onDone)
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
    hospital: BriefCardHospital?,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onHospitalChange: (String) -> Unit,
    onHandoff: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hospital) { viewModel.load(hospital) }

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
            onSaveClick = onSaved,
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = {
                viewModel.onDeleteConfirm()
                onDeleted()
            },
            onHospitalChangeClick = { content?.card?.id?.let(onHospitalChange) },
            onHandoffClick = { content?.card?.id?.let(onHandoff) },
            onRetryClick = { viewModel.load(hospital) },
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
    return BriefCardHospital(name = name, address = route.hospitalAddress.orEmpty())
}

/**
 * 진료실 화면의 진입점.
 *
 * 카드를 다시 불러온다. 브리핑 카드 화면과 목적지가 달라 ViewModel도 다른 것을 쓴다.
 * 서버 연동에서는 같은 카드를 두 번 부르지 않도록 저장된 카드를 넘겨받는 편이 낫다.
 */
@Composable
private fun HandoffRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val card = (state as? BriefCardUiState.Content)?.card ?: return

    HandoffScreen(card = card, onCloseClick = onDone, onDoneClick = onDone, modifier = modifier)
}
