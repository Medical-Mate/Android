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
import com.mist.medicalmate.R
import com.mist.medicalmate.core.speech.rememberMicPermission
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.rememberVoicePrompt
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
    /**
     * 열자마자 찾을 이름(#270).
     *
     * 일정 추가가 예약 문자에서 읽은 병원 이름이거나 병원 칸에 이미 적힌 이름이다. 그 이름으로
     * 찾은 목록에서 같은 이름의 여러 지점을 주소로 가려 고르게 한다.
     */
    val query: String? = null,
    /**
     * [query]가 사람이 말한 병원인지(#282). 그러면 하나로 좁혀질 때 알아서 고르고, 여럿이면 막대로
     * 몇 번째인지나 동네를 되묻는다. 예약 문자에서 온 이름은 지금처럼 사람이 주소를 보고 고른다.
     */
    val voicePick: Boolean = false,
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
/**
 * 와이어프레임 1m-12. 일정 상세의 "진료 후 기록하기"가 먼저 닿는 곳이다(#246).
 *
 * 일정에 등록한 병원을 들고 들어와 그 병원이 맞는지 묻는다. 맞으면 [clinic]을 그대로 1p로
 * 나르고, 다른 곳이면 1m으로 가서 다시 고른다. [address]는 일정 응답에 없어 카드에 남은
 * 것을 받는다 — 없으면 이름만 적는다.
 */
@Serializable
internal data class ClinicConfirmDestination(
    val clinic: String,
    val address: String? = null,
    val cardId: String,
    val cardTitle: String,
    val visitedOn: String,
)

internal fun NavGraphBuilder.clinicConfirmDestination(
    onConfirmed: (clinic: String, cardId: String, cardTitle: String, visitedOn: String) -> Unit,
    onOther: (cardId: String, cardTitle: String, visitedOn: String) -> Unit,
    onExit: () -> Unit,
) {
    composable<ClinicConfirmDestination> { entry ->
        val route = entry.toRoute<ClinicConfirmDestination>()
        val confirm = { onConfirmed(route.clinic, route.cardId, route.cardTitle, route.visitedOn) }
        val other = { onOther(route.cardId, route.cardTitle, route.visitedOn) }
        // "네, 맞아요" / "다른 병원이에요"(#288).
        VoiceTargetEffect { text ->
            when (parseClinicConfirm(text)) {
                true -> {
                    confirm()
                    VoiceReply.Done(R.string.voice_clinic_confirmed)
                }

                false -> {
                    other()
                    VoiceReply.Done(R.string.voice_clinic_other)
                }

                null -> VoiceReply.NotMine
            }
        }
        val prompt = rememberVoicePrompt()
        LaunchedEffect(Unit) {
            prompt(VoicePrompt(question = VoiceText(R.string.voice_clinic_ask, listOf(route.clinic))))
        }
        ClinicConfirmScreen(
            clinic = route.clinic,
            address = route.address,
            scheduledOn = LocalDate.parse(route.visitedOn),
            onConfirmClick = confirm,
            onOtherClick = other,
            onBackClick = onExit,
        )
    }
}

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
            query = route.query,
            voicePick = route.voicePick,
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
    query: String?,
    voicePick: Boolean,
    onPicked: (cardId: String?, cardTitle: String?, visitedOn: String?, hospital: Hospital) -> Unit,
    onCardRequested: (cardId: String?, sessionId: Long?, hospital: Hospital?) -> Unit,
    onScheduleRequested: (Hospital?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HospitalPickViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(purpose, cardId, query) {
        viewModel.load(purpose, forExistingCard = cardId != null, initialQuery = query, voicePick = voicePick)
    }

    // 목적마다 돌아가는 자리가 다르다. 진료 후에는 고른 병원의 id만 다음 화면으로 가고,
    // 진료 전에는 카드가 이름과 주소를 바로 그려야 해서 병원을 그대로 넘긴다. 일정
    // 추가는 필드에 이름만 채우고 돌아간다. 버튼과 말(#282)이 같은 길로 간다.
    val submit: (Hospital?) -> Unit = { selected ->
        when (purpose) {
            HospitalPickPurpose.AFTER_VISIT -> selected?.let { onPicked(cardId, cardTitle, visitedOn, it) }
            HospitalPickPurpose.BEFORE_VISIT -> onCardRequested(cardId, sessionId, selected)
            HospitalPickPurpose.SCHEDULE -> onScheduleRequested(selected)
        }
    }

    VoiceTargetEffect { text -> viewModel.onVoice(text, submit) }

    // 말로 찾은 결과(#282). 하나로 좁혀졌으면 고르고 마치며 무엇을 골랐는지 알린다. 여럿이거나 없으면
    // 막대로 되묻는다. 사용자는 몇 번째인지, 동네나 지점 이름, 다른 병원 이름으로 답한다.
    val prompt = rememberVoicePrompt()
    LaunchedEffect(state.voiceDecision) {
        val decision = state.voiceDecision ?: return@LaunchedEffect
        viewModel.onVoiceDecisionHandled()
        when (decision) {
            is HospitalVoiceDecision.Pick -> {
                prompt(
                    VoicePrompt(
                        message = VoiceText(R.string.voice_hospital_auto_picked, listOf(decision.hospital.name)),
                    ),
                )
                submit(decision.hospital)
            }

            is HospitalVoiceDecision.Ask -> prompt(decision.toPrompt())
        }
    }

    HospitalPickScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onHospitalClick = viewModel::onHospitalClick,
        onSubmitClick = { submit(state.selected) },
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

    // 막대가 메모를 받아 적는다(#288). "다 했어"면 적은 원문을 들고 1q-1로 간다.
    VoiceTargetEffect { text -> viewModel.onVoiceNote(text, onSaved) }
    val prompt = rememberVoicePrompt()
    LaunchedEffect(Unit) { prompt(VoicePrompt(question = VoiceText(R.string.voice_note_ask))) }

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

    // 항목 고치기·지우기, "재방문 11월 3일로", "저장"(#288). 저장은 확인을 받는다.
    VoiceTargetEffect { text ->
        viewModel.voice.handle(text, LocalDate.now()) {
            viewModel.onSaveClick(cardId = route.cardId, onSaved = onSaved)
        }
    }
    val prompt = rememberVoicePrompt()
    val loaded = state is VisitRecordUiState.Content
    LaunchedEffect(loaded) { if (loaded) prompt(VoicePrompt(question = VoiceText(R.string.voice_record_ask))) }

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
