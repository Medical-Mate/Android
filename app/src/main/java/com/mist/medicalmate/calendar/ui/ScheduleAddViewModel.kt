package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentTodo
import com.mist.medicalmate.calendar.data.NewAppointment
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
) : ViewModel() {
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
     */
    fun load() {
        viewModelScope.launch {
            val cards = (cardRepository.cards() as? ApiResult.Success)?.value.orEmpty()
            mutableUiState.update { state ->
                val picked = state.cards.filter { it.picked }.map { it.id }.toSet()
                state.copy(cards = cards.map { card -> card.toPick().copy(picked = card.id in picked) })
            }
        }
    }

    private var nextTodo = 1

    val todo =
        ScheduleAddTodoActions(
            nextId = { "todo-new-${nextTodo++}" },
            update = { change -> mutableUiState.update { it.copy(todos = change(it.todos)) } },
        )

    /** 병원 찾기에서 돌아왔다. 라우트가 이름을 실어 온다. */
    fun onHospitalPicked(name: String?) {
        if (name.isNullOrBlank()) return
        mutableUiState.update { it.copy(hospital = name) }
    }

    /**
     * 열릴 때 이미 정해져 있던 날. 캘린더에서 고른 날이거나 일자 화면의 그 날이다.
     *
     * 이미 날이 들어 있으면 덮지 않는다. 병원을 고르러 나갔다 돌아오는 길에 이 화면이 다시
     * 조합되는데, 그때 덮으면 시트에서 고쳐 둔 날이 처음 값으로 되돌아간다.
     */
    fun onDatePrefilled(date: LocalDate?) {
        if (date == null) return
        mutableUiState.update { if (it.date == null) it.copy(date = date) else it }
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

    fun onCardPickChange(id: String, picked: Boolean) {
        mutableUiState.update { state ->
            state.copy(cards = state.cards.map { if (it.id == id) it.copy(picked = picked) else it })
        }
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
            val result =
                repository.create(
                    NewAppointment(
                        clinicName = state.hospital,
                        on = date,
                        // 안 골랐으면 시각 없이 보낸다. 서버가 "시간 미정"으로 만든다(#202).
                        time = state.time,
                        cardIds = state.cards.filter { it.picked }.mapNotNull { it.id.toLongOrNull() },
                        // 적어 둔 할 일도 함께 보낸다. 비운 줄은 빼고 보낸다 — 적지 않은 것과
                        // 빈 줄은 다르다.
                        todos =
                        state.todos.mapNotNull { todo ->
                            todo.label.takeIf { it.isNotBlank() }
                                ?.let { AppointmentTodo(text = it, done = todo.done) }
                        },
                    ),
                )
            saving = false
            if (result is ApiResult.Success) onSaved()
        }
    }
}

/**
 * 고를 수 있는 카드 한 줄.
 *
 * 보조 문구는 작성일과 병원이다. 병원은 확정 전 카드에 없어서 그때는 작성일만 적는다.
 */
private fun CardListItem.toPick() = ScheduleAddCard(
    id = id,
    title = title,
    meta = listOfNotNull(writtenOn.format(CARD_DATE) + " 작성", clinic).joinToString(" · "),
)

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
