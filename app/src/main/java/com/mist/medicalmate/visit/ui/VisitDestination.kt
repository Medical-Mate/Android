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
import com.mist.medicalmate.core.speech.rememberMicPermission
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * 진료 후 기록 플로우의 목적지들. Figma 흐름은 `1m → 1p → 1q-1`이고, 저장하면 흐름이
 * 시작된 캘린더 일자로 돌아간다.
 *
 * 세 화면이 한 흐름이지만 목적지가 각각이라 ViewModel을 공유하지 않는다. 그래서 앞 화면에서
 * 정한 것이 라우트를 타고 따라간다. 마지막 화면(1q-1)의 저장 한 번이
 * `POST /api/cards/{cardId}/visit`이고, 그 요청에 들어갈 값이 세 화면에 흩어져 있다.
 */
/**
 * 와이어프레임 1m과 1m-B.
 *
 * [purpose]가 두 자리를 가른다. 라우트에 담는 이유는 어느 목적으로 열린 화면인지가 목적지의
 * 일부이고, 화면이 다시 만들어질 때 살아 있어야 하기 때문이다.
 *
 * [cardId]는 브리핑 카드의 `변경`에서 들어온 경우에만 있다. 고른 뒤 그 카드로 돌아가야 해서
 * 어느 카드였는지를 들고 간다.
 *
 * [sessionId]는 진료 전(1m-B)에서만 채워진다. 고른 병원을 들고 카드 화면으로 갈 때 그
 * 문답으로 카드를 만들어야 해서 함께 나른다.
 *
 * [cardTitle]은 진료 후(1m)에서만 채워진다. 1p가 "무엇으로 진료받았는지"를 적는 데 쓰는
 * 값이고, 이 화면은 나르기만 한다. 카드를 다시 읽지 않는 이유는 그 제목이 이미 캘린더 일자에
 * 있기 때문이다. 병원 이름을 나르는 것과 같은 방식이다.
 */
@Serializable
internal data class HospitalPickDestination(
    val purpose: HospitalPickPurpose = HospitalPickPurpose.AFTER_VISIT,
    val cardId: String? = null,
    val cardTitle: String? = null,
    val sessionId: Long? = null,
    /**
     * 진료를 받은 날.
     *
     * 흐름이 시작된 캘린더 일자다. 오늘로 찍지 않는 이유는 어제 진료를 오늘 적을 수 있기
     * 때문이다. 그때 기록이 오늘 날짜로 남으면 그 일자 화면에는 영영 나오지 않는다.
     */
    val visitedOn: String? = null,
)

/**
 * 와이어프레임 1p.
 *
 * [clinic]은 1m에서 고른 병원의 **이름**이다. id를 나르지 않는 이유는 서버가 병원을 문자열
 * 하나(`clinicName`)로만 받기 때문이다. 심평원 키가 풀려 병원이 개체가 되면 id로 바꾼다.
 *
 * [cardId]는 기록을 붙일 카드다. 서버가 확정한 카드 하나에 기록 하나를 받는다. 캘린더 일자의
 * 일정에 걸린 카드에서 온다.
 *
 * [cardTitle]은 그 카드의 제목이다. 화면 맨 위에 "무엇으로 진료받았는지"를 적는 자리가 있고
 * 그 값이다. 카드를 다시 읽는 대신 캘린더에서부터 라우트로 따라온다.
 */
@Serializable
internal data class VisitNoteDestination(
    val clinic: String? = null,
    val cardId: String? = null,
    val cardTitle: String? = null,
    val visitedOn: String? = null,
)

/**
 * 와이어프레임 1q-1과 1q-1-E.
 *
 * [note]는 1p에서 적은 원문이다. 라우트로 나르는 이유는 이 화면이 그 글을 그대로 아래에
 * 보여주고, 저장할 때 `rawNote`로 함께 보내기 때문이다. 길어질 수 있는 값이지만 목적지가
 * 달라 다른 방법이 없다. 흐름을 한 목적지로 합치면 1p에서 뒤로 갈 자리가 사라진다.
 */
@Serializable
internal data class VisitRecordDestination(
    val clinic: String? = null,
    val cardId: String? = null,
    val note: String = "",
    val visitedOn: String? = null,
)

/**
 * @param onPicked 진료 후(1m)에서 병원을 고르고 완료했을 때. 고른 병원과, 이 흐름이 어느
 *   카드에 붙는지가(id와 제목) 함께 넘어간다.
 * @param onCardRequested 진료 전(1m-B)에서 카드로 넘어갈 때. 고른 병원이 없으면 null이
 *   넘어간다. 건너뛰기와 CTA가 같은 곳으로 가고, 다른 것은 병원을 들고 가는지뿐이다.
 */
internal fun NavGraphBuilder.hospitalPickDestination(
    onPicked: (cardId: String?, cardTitle: String?, visitedOn: String?, hospital: Hospital) -> Unit,
    onCardRequested: (cardId: String?, sessionId: Long?, hospital: Hospital?) -> Unit,
    onScheduleRequested: (Hospital?) -> Unit,
    onExit: () -> Unit,
) {
    composable<HospitalPickDestination> { entry ->
        val route = entry.toRoute<HospitalPickDestination>()
        HospitalPickRoute(
            purpose = route.purpose,
            cardId = route.cardId,
            cardTitle = route.cardTitle,
            sessionId = route.sessionId,
            visitedOn = route.visitedOn,
            onPicked = onPicked,
            onCardRequested = onCardRequested,
            onScheduleRequested = onScheduleRequested,
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.visitNoteDestination(
    onSaved: (clinic: String?, cardId: String?, note: String, visitedOn: String?) -> Unit,
    onExit: () -> Unit,
) {
    composable<VisitNoteDestination> { entry ->
        val route = entry.toRoute<VisitNoteDestination>()
        VisitNoteRoute(
            clinic = route.clinic,
            cardTitle = route.cardTitle,
            visitedOn = route.visitedOn,
            onSaved = { note -> onSaved(route.clinic, route.cardId, note, route.visitedOn) },
            onExit = onExit,
        )
    }
}

internal fun NavGraphBuilder.visitRecordDestination(onSaved: () -> Unit, onDeleted: () -> Unit, onExit: () -> Unit) {
    composable<VisitRecordDestination> { entry ->
        VisitRecordRoute(
            route = entry.toRoute<VisitRecordDestination>(),
            onSaved = onSaved,
            onDeleted = onDeleted,
            onExit = onExit,
        )
    }
}

@Composable
private fun HospitalPickRoute(
    purpose: HospitalPickPurpose,
    cardId: String?,
    cardTitle: String?,
    sessionId: Long?,
    visitedOn: String?,
    onPicked: (cardId: String?, cardTitle: String?, visitedOn: String?, hospital: Hospital) -> Unit,
    onCardRequested: (cardId: String?, sessionId: Long?, hospital: Hospital?) -> Unit,
    onScheduleRequested: (Hospital?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HospitalPickViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(purpose) { viewModel.load(purpose) }

    val selected = state.selected

    HospitalPickScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onHospitalClick = viewModel::onHospitalClick,
        // 목적마다 돌아가는 자리가 다르다. 진료 후에는 고른 병원의 id만 다음 화면으로 가고,
        // 진료 전에는 카드가 이름과 주소를 바로 그려야 해서 병원을 그대로 넘긴다. 일정
        // 추가는 필드에 이름만 채우고 돌아간다.
        onSubmitClick = {
            when (purpose) {
                HospitalPickPurpose.AFTER_VISIT -> selected?.let { onPicked(cardId, cardTitle, visitedOn, it) }
                HospitalPickPurpose.BEFORE_VISIT -> onCardRequested(cardId, sessionId, selected)
                HospitalPickPurpose.SCHEDULE -> onScheduleRequested(selected)
            }
        },
        onBackClick = onExit,
        modifier = modifier,
    )
}

@Composable
private fun VisitNoteRoute(
    clinic: String?,
    cardTitle: String?,
    visitedOn: String?,
    onSaved: (note: String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(clinic, cardTitle, visitedOn) {
        viewModel.load(clinic, cardTitle, visitedOn?.let(LocalDate::parse))
    }

    // 이 기기에서 음성을 쓸 수 있는지. 쓸 수 없으면 마이크를 그리지 않는다.
    LaunchedEffect(Unit) { viewModel.checkVoice() }

    // 권한은 마이크를 누를 때 묻는다. 화면을 열자마자 묻지 않는다.
    val askMic = rememberMicPermission(onGranted = viewModel::onMicClick, onDenied = viewModel::onMicDenied)
    val askVoice = rememberMicPermission(onGranted = viewModel::onVoiceClick, onDenied = viewModel::onMicDenied)

    VisitNoteScreen(
        state = state,
        callbacks =
        VisitNoteCallbacks(
            onMicClick = askMic,
            onVoiceClick = askVoice,
            onBackClick = onExit,
            onNoteChange = viewModel::onNoteChange,
            onTypeInsteadClick = viewModel::onTypeInsteadClick,
            // 적은 원문이 그대로 다음 화면으로 간다. 1q-1이 그 글을 보여주고 저장한다.
            onSaveClick = { onSaved(state.note) },
        ),
        modifier = modifier,
    )
}

/**
 * 상태 있는 진입점.
 *
 * 저장은 두 가지 일을 한다. 수정 중이면 초안을 옮기고 화면에 남고, 읽는 중이면 서버에
 * 보낸 뒤 흐름이 시작된 캘린더 일자로 돌아간다. 브리핑 카드 화면과 같은 구조다.
 */
@Composable
private fun VisitRecordRoute(
    route: VisitRecordDestination,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route) {
        viewModel.load(
            clinic = route.clinic,
            note = route.note,
            visitedOn = route.visitedOn?.let(LocalDate::parse),
        )
    }

    VisitRecordScreen(
        state = state,
        callbacks =
        VisitRecordCallbacks(
            onBackClick = onExit,
            onEditClick = viewModel::onEditClick,
            onEditDoneClick = viewModel::onEditDoneClick,
            onCancelClick = viewModel::onCancelClick,
            edit = viewModel.editActions,
            // 편집 중에는 하단에 저장하기가 없다. 그 자리가 삭제이고 사본을 옮기는 것은
            // Nav 우측 `확인`이 한다. 그래서 저장하기는 항상 화면을 나간다.
            onSaveClick = { viewModel.onSaveClick(cardId = route.cardId, onSaved = onSaved) },
            onDeleteClick = viewModel::onDeleteClick,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDeleteConfirm = {
                viewModel.onDeleteConfirm()
                onDeleted()
            },
            onRetryClick = {
                viewModel.load(
                    clinic = route.clinic,
                    note = route.note,
                    visitedOn = route.visitedOn?.let(LocalDate::parse),
                )
            },
        ),
        modifier = modifier,
    )
}
