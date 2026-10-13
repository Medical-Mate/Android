package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentEdit
import com.mist.medicalmate.calendar.data.AppointmentOrigin
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentTodo
import com.mist.medicalmate.calendar.data.NewAppointment
import com.mist.medicalmate.calendar.share.AppointmentTextReader
import com.mist.medicalmate.calendar.share.SharedAppointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 일정 추가 상태 보유자.
 *
 * 카드 후보가 픽스처다. Figma 1r-4의 두 장을 그대로 옮겼다. `GET /api/cards`가 붙으면
 * 그 목록을 받는다. 저장도 아직 없어서 완료를 누르면 화면만 닫힌다.
 *
 * 할 일 목록 조작은 [ScheduleAddTodoActions]가 맡는다. 다섯 가지라 여기 얹으면 한 클래스가
 * 화면 조작과 목록 조작을 다 들고 있게 된다.
 */
@HiltViewModel
class ScheduleAddViewModel
@Inject
internal constructor(
    private val repository: AppointmentRepository,
    private val cardRepository: CardRepository,
    private val textReader: AppointmentTextReader,
) : ViewModel() {
    /**
     * "헤이 메이트"로 연 것인지(#282). 그러면 [load]가 받은 글은 예약 문자가 아니라 사람이 한 말이고,
     * 그 속의 병원은 병원 찾기가 하나로 좁혀질 때 알아서 고른다. 라우트가 [load] 전에 정한다.
     */
    internal var openedByVoice = false

    /** 두 번 눌러 일정이 둘 생기는 것을 막는다. */
    private var saving = false

    private val mutableUiState = MutableStateFlow(ScheduleAddUiState())
    val uiState: StateFlow<ScheduleAddUiState> = mutableUiState.asStateFlow()

    /**
     * 가져갈 카드로 고를 수 있는 것들. 저장된 카드 전부다.
     *
     * **고른 것을 지우지 않는다.** 이 호출은 화면이 조합될 때마다 온다. 병원을 고르러
     * 나갔다 돌아오면 이 화면이 컴포지션에 다시 들어오면서 한 번 더 오는데, 그때 목록을
     * 통째로 갈아끼우면 골라 둔 카드가 조용히 풀린다. 그대로 저장하면 카드가 안 걸린 일정이
     * 되고, 나중에 그 일정으로는 진료 후 기록을 남길 수 없다.
     *
     * [sharedText]는 문자 앱에서 공유받은 예약 문자다(#270). 읽은 값으로 **비어 있는 칸만**
     * 채운다. 한국어 모델을 처음 받으면 몇 초가 걸리고, 그 사이 사람이 고른 값이 나중의 뜻이다.
     * 읽기는 **한 번만** 한다. 다시 읽으면 시트에서 지운 시각이 문자 값으로 되살아난다.
     */
    fun load(
        appointmentId: Long? = null,
        date: LocalDate? = null,
        cardId: String? = null,
        followUp: Boolean = false,
        sharedText: String? = null,
    ) {
        if (!sharedText.isNullOrBlank() && mutableUiState.value.shareRead == ScheduleAddShareRead.NONE) {
            mutableUiState.update { it.copy(shareRead = ScheduleAddShareRead.READING) }
            viewModelScope.launch {
                val read = textReader.read(sharedText)
                mutableUiState.update { it.filledFrom(read, openedByVoice) }
            }
        }
        viewModelScope.launch {
            // 이미 한 번 담아 왔으면 다시 읽지 않는다. 고치는 중에 들어온 값을 덮어쓴다.
            val editing =
                if (mutableUiState.value.appointmentId == null) repository.find(appointmentId, date) else null
            val cards = (cardRepository.cards() as? ApiResult.Success)?.value.orEmpty()
            mutableUiState.update { state ->
                val picked =
                    editing?.cards?.map { it.id.toString() }?.toSet()
                        ?: state.cards.filter { it.picked }.map { it.id }.toSet()
                            .plus(listOfNotNull(cardId))
                val picks = cards.map { card -> card.toPick().copy(picked = card.id in picked) }
                state.copy(
                    appointmentId = editing?.id ?: state.appointmentId,
                    followUp = state.followUp || followUp,
                    // 골라 둔 카드의 병원을 채운다. 손으로 고를 때와 같은 규칙이다(1r-4-B).
                    hospital = state.hospital ?: editing?.title ?: picks.pickedClinic(),
                    date = state.date ?: editing?.on,
                    time = state.time ?: editing?.time,
                    cards = picks,
                    todos = editing?.todos?.toDrafts() ?: state.todos,
                )
            }
        }
    }

    /**
     * 문자에서 읽은 병원으로 병원 찾기를 열었다(#270). 다시 열리지 않게 지운다.
     *
     * 화면을 옮기기 전에 부른다. 옮기면서 효과가 취소되면 뒤에 둔 코드는 돌지 않아서, 병원을 고르고
     * 돌아왔을 때 같은 검색이 또 열린다.
     */
    fun onHospitalSearchOpened() {
        mutableUiState.update { it.copy(hospitalSearch = null, hospitalSearchByVoice = false) }
    }

    private var nextTodo = 1

    val todo =
        ScheduleAddTodoActions(
            nextId = { "todo-new-${nextTodo++}" },
            update = { change -> mutableUiState.update { it.copy(todos = change(it.todos)) } },
        )

    /** 받아쓴 말(#282). 화면의 칸과 버튼을 말로 부른다. */
    internal val voice =
        ScheduleAddVoiceActions(
            reader = textReader,
            state = { mutableUiState.value },
            update = { change -> mutableUiState.update(change) },
            nextTodoId = { "todo-new-${nextTodo++}" },
        )

    /** 병원 찾기에서 돌아왔다. 라우트가 이름을 실어 온다. */
    fun onHospitalPicked(name: String?) {
        if (name.isNullOrBlank()) return
        mutableUiState.update { it.copy(hospital = name) }
    }

    /**
     * 열릴 때 이미 정해져 있던 날. 캘린더에서 고른 날이거나 일자 화면의 그 날이다.
     *
     * [time]은 다른 앱이 일정 추가를 요청하면서 시각까지 넘겨준 경우에만 온다(#270).
     *
     * 이미 들어 있으면 덮지 않는다. 병원을 고르러 나갔다 돌아오는 길에 이 화면이 다시
     * 조합되는데, 그때 덮으면 시트에서 고쳐 둔 날이 처음 값으로 되돌아간다.
     */
    fun onDatePrefilled(date: LocalDate?, time: LocalTime? = null) {
        mutableUiState.update { it.copy(date = it.date ?: date, time = it.time ?: time) }
    }

    /** 날짜와 시간 필드가 같은 자리에 시트를 띄운다. 어느 쪽인지만 다르다. */
    fun onSheetOpen(sheet: ScheduleAddSheet) {
        mutableUiState.update { it.copy(sheet = sheet) }
    }

    fun onSheetDismiss() {
        mutableUiState.update { it.copy(sheet = ScheduleAddSheet.NONE) }
    }

    fun onDateConfirm(date: LocalDate) {
        mutableUiState.update { it.copy(date = date, sheet = ScheduleAddSheet.NONE) }
    }

    fun onTimeConfirm(time: LocalTime) {
        mutableUiState.update { it.copy(time = time, sheet = ScheduleAddSheet.NONE) }
    }

    /**
     * 가져갈 카드를 고르거나 풀었다.
     *
     * **고른 카드의 병원으로 병원 칸을 채운다**(1r-4-B). 그 카드로 갈 병원이 이미 정해져
     * 있는데 같은 값을 다시 찾게 하지 않는다.
     *
     * 이미 적힌 병원은 덮지 않는다. 손으로 고른 것이 카드에 적힌 것보다 나중의 뜻이다.
     * 카드를 풀어도 지우지 않는다 — 지우면 카드를 잘못 눌렀다 되돌린 사람의 병원까지
     * 사라진다.
     */
    fun onCardPickChange(id: String, picked: Boolean) {
        mutableUiState.update { it.withCardPicked(id, picked) }
    }

    private companion object {
        val fixtureCards =
            listOf(
                ScheduleAddCard(
                    id = "card-1",
                    title = "복부 통증 · 3주",
                    meta = "09.04 작성 · 서울OO병원 내과",
                ),
                ScheduleAddCard(
                    id = "card-2",
                    title = "두통 · 잦은 어지러움",
                    meta = "08.21 작성 · 병원 미정",
                ),
            )
    }

    /**
     * 하단 `저장하기`. 서버에 일정을 만든다.
     *
     * 고른 카드가 있으면 첫 장만 싣는다. 서버의 `cardId`가 단수이고 시안도 한 장이다.
     *
     * 병원과 날짜가 필수다. 비면 보내지 않고 그 칸에 안내를 띄운다. 시간은 안 골라도 되고 그때는
     * 기본 시각으로 저장한다.
     *
     * **할 일은 보내지 않는다.** `AppointmentResponse`에 자리가 없다. 여러 줄에 줄마다 체크
     * 상태까지 있어서 `purpose` 하나로 담을 수 없다. 화면에만 남는다(#141).
     *
     * [onSaved]는 저장이 끝난 뒤의 화면 이동이다. 실패하면 부르지 않는다.
     */
    fun onSaveClick(onSaved: () -> Unit) {
        val state = mutableUiState.value
        if (!state.canSave) {
            // 비활성 버튼으로 막지 않는다. 문서가 비활성만으로 필요한 행동을 숨기지 말라고 한다.
            // 무엇이 비었는지 그 칸에서 알린다.
            mutableUiState.update { it.copy(showErrors = true) }
            return
        }
        val date = state.date
        if (date == null || saving) return

        saving = true
        viewModelScope.launch {
            val cardIds = state.cards.filter { it.picked }.mapNotNull { it.id.toLongOrNull() }
            // 적어 둔 할 일도 함께 보낸다. 비운 줄은 빼고 보낸다 — 적지 않은 것과 빈 줄은 다르다.
            val todos =
                state.todos.mapNotNull { todo ->
                    todo.label.takeIf { it.isNotBlank() }?.let { AppointmentTodo(text = it, done = todo.done) }
                }
            val result =
                state.appointmentId?.let { id ->
                    // **병원은 보내지 않는다.** 서버의 수정 요청에 병원 자리가 없다. 고치러
                    // 들어온 화면에서 병원을 바꿔도 그 값은 나가지 않는다.
                    repository.update(
                        id,
                        AppointmentEdit(
                            on = date,
                            time = state.time,
                            // 시각을 지운 채 저장하면 "시간 미정"으로 되돌린다.
                            clearTime = state.time == null,
                            cardIds = cardIds,
                            todos = todos,
                        ),
                    )
                } ?: repository.create(
                    NewAppointment(
                        clinicName = state.hospital,
                        on = date,
                        // 안 골랐으면 시각 없이 보낸다. 서버가 "시간 미정"으로 만든다(#202).
                        time = state.time,
                        cardIds = cardIds,
                        // 재진인 일정은 출처를 남긴다. 홈과 일자 화면이 이 값으로 "재진"을 적는다.
                        origin = if (state.isRevisit) AppointmentOrigin.VISIT_FOLLOW_UP else AppointmentOrigin.MANUAL,
                        todos = todos,
                    ),
                )
            saving = false
            if (result is ApiResult.Success) onSaved()
        }
    }
}

/**
 * 고치러 들어온 일정.
 *
 * 서버에 일정 하나를 id로 읽는 경로가 없어서 그 날의 목록에서 찾는다. 날짜는 라우트가
 * 함께 실어 온다 — 그 일정이 선 날에서만 들어오는 길이라 늘 있다.
 */
private suspend fun AppointmentRepository.find(appointmentId: Long?, date: LocalDate?): Appointment? {
    if (appointmentId == null || date == null) return null
    return (day(date) as? ApiResult.Success)?.value?.firstOrNull { it.id == appointmentId }
}

/**
 * 문자에서 읽은 값으로 빈 칸을 채운다. 날짜를 찾았는지가 화면의 안내를 정한다.
 *
 * 병원을 문자에서 채웠으면 그 이름으로 병원 찾기를 연다([ScheduleAddUiState.hospitalSearch]).
 * 같은 이름의 다른 지점이 있을 수 있어 사람이 주소를 보고 고른다. 이미 고른 병원이 있으면 열지 않는다.
 */
private fun ScheduleAddUiState.filledFrom(read: SharedAppointment, fromVoice: Boolean) = copy(
    hospital = hospital ?: read.hospital,
    hospitalSearch = read.hospital.takeIf { hospital == null },
    hospitalSearchByVoice = fromVoice && hospital == null && read.hospital != null,
    date = date ?: read.date,
    time = time ?: read.time,
    // 날짜 칸이 찼는지로 본다. 다른 앱이 날짜를 넘겨준 경우에는 글에 날짜가 없어도 칸이 차 있다.
    shareRead = if ((date ?: read.date) != null) ScheduleAddShareRead.FILLED else ScheduleAddShareRead.MISSED,
)

/**
 * 재진인 일정인지.
 *
 * 진료 후 기록의 재방문을 확정하러 들어왔거나(#245), **이미 진료를 마친 카드를 골랐으면**
 * 재진이다(#272). 뒤의 것이 없으면 같은 카드로 다시 잡은 일정이 일자 화면과 홈에서 "초진"으로
 * 뜨고, 기록 상세(1j-3)는 같은 진료를 재방문으로 적어 두 화면이 어긋난다.
 *
 * 서버의 `origin`은 원래 일정이 어디서 왔는지인데 화면이 그 값 하나로 초진·재진을 가르므로 여기에
 * 싣는다. 서버가 초진·재진을 따로 내려 주면 그 값을 쓴다.
 */
private val ScheduleAddUiState.isRevisit: Boolean
    get() = followUp || cards.any { it.picked && it.visited }

/** 카드를 고르거나 푼다. 고른 카드의 병원으로 빈 병원 칸을 채운다. 버튼과 말(#282)이 함께 쓴다. */
internal fun ScheduleAddUiState.withCardPicked(id: String, picked: Boolean): ScheduleAddUiState {
    val cards = cards.map { if (it.id == id) it.copy(picked = picked) else it }
    val clinic = cards.firstOrNull { it.id == id && it.picked }?.clinic?.takeIf { it.isNotBlank() }
    return copy(cards = cards, hospital = hospital ?: clinic)
}

/** 저장할지 물을 때 무엇을 저장하는지. "10월 20일 (화) 오후 3:00 · 고려튼튼통증의학과". */
internal fun ScheduleAddUiState.voiceSummary(): String =
    listOfNotNull(date?.format(VOICE_DATE), time?.format(VOICE_TIME)).joinToString(" ") +
        hospital?.let { " · $it" }.orEmpty()

internal val VOICE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

internal val VOICE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/** 골라 둔 카드의 병원. 일정 추가가 병원 칸을 채울 때 쓴다. */
private fun List<ScheduleAddCard>.pickedClinic(): String? =
    firstOrNull { it.picked }?.clinic?.takeIf { it.isNotBlank() }

/** 서버의 할 일을 화면 줄로. 서버가 id를 매기지 않아 차례로 만든다. */
private fun List<AppointmentTodo>.toDrafts(): List<ScheduleAddTodo> =
    mapIndexed { index, todo -> ScheduleAddTodo(id = "todo-${index + 1}", label = todo.text, done = todo.done) }

/**
 * 고를 수 있는 카드 한 줄.
 *
 * 보조 문구는 작성일과 병원이다. 병원이 없으면 자리를 비우지 않고 "병원 미정"을 적는다 —
 * 시안 `1r-4-T`의 둘째 줄이 그렇다. 비워 두면 병원이 없는 것인지 아직 안 읽은 것인지가
 * 구별되지 않는다.
 */
private fun CardListItem.toPick() = ScheduleAddCard(
    id = id,
    title = title,
    meta = "${writtenOn.format(CARD_DATE)} 작성 · ${clinic?.takeIf { it.isNotBlank() } ?: CLINIC_UNSET}",
    clinic = clinic,
    visited = visited,
)

private const val CLINIC_UNSET = "병원 미정"

private val CARD_DATE: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("MM.dd", java.util.Locale.KOREAN)

/** Preview용 일정 추가 상태. 시안 1r-4-C와 같은 지점이다. */
internal val previewScheduleAddState =
    ScheduleAddUiState(
        cards =
        listOf(
            ScheduleAddCard(
                id = "card-1",
                title = "복부 통증 · 3주",
                meta = "09.04 작성 · 서울OO병원 내과",
                picked = true,
            ),
            ScheduleAddCard(id = "card-2", title = "두통 · 잦은 어지러움", meta = "08.21 작성 · 병원 미정"),
        ),
        todos =
        listOf(
            ScheduleAddTodo(id = "todo-1", label = "달라진 증상 있으면 카드 수정", done = true),
            ScheduleAddTodo(id = "todo-2", label = "복용 중인 약 챙기기"),
        ),
        hospital = "서울OO병원 내과",
        date = LocalDate.of(2026, 9, 26),
        time = LocalTime.of(10, 30),
    )
