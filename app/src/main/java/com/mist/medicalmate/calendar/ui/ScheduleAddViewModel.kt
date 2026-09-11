package com.mist.medicalmate.calendar.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
constructor() : ViewModel() {
    private val mutableUiState = MutableStateFlow(ScheduleAddUiState(cards = fixtureCards))
    val uiState: StateFlow<ScheduleAddUiState> = mutableUiState.asStateFlow()

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
}

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
