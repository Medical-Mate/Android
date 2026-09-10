package com.mist.medicalmate.calendar.ui

import androidx.annotation.Keep
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일정 추가 화면의 상태. Figma 1r-4 `1062:3031`.
 *
 * 병원 · 날짜 · 시간은 골라 채우는 값이고, 카드와 할 일은 이 일정에 딸리는 목록이다.
 *
 * [hospital]이 없어도 저장할 수 있다. 시안에 `1r-4 · 병원 미등록`이 정상 상태로 있고,
 * 병원 찾기(1m-B)도 건너뛰기를 준다. 반대로 날짜와 시간은 없으면 캘린더에 놓을 자리가
 * 없어서 [canSave]가 둘을 요구한다. 시안에 부분만 채운 프레임이 없어 그 선은 여기서
 * 정했다.
 */
data class ScheduleAddUiState(
    val cards: List<ScheduleAddCard> = emptyList(),
    val todos: List<ScheduleAddTodo> = emptyList(),
    val hospital: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val sheet: ScheduleAddSheet = ScheduleAddSheet.NONE,
) {
    /** 고른 카드 수. 섹션 머리 오른쪽에 적는다. */
    val pickedCardCount: Int get() = cards.count { it.picked }

    val canSave: Boolean get() = date != null && time != null
}

/** 가져갈 카드 후보 한 장. */
data class ScheduleAddCard(val id: String, val title: String, val meta: String, val picked: Boolean = false)

/**
 * 이 일정에 딸린 할 일 한 줄.
 *
 * [editing]이면 그 자리에서 글자를 받는다. 문서의 추가 방식이 새 입력 필드를 띄우는 대신
 * 목록에 빈 항목을 하나 만드는 것이고, 시안 `1092:3935`~`1185:12839`가 빈 행 → 적는 중 →
 * 완료된 행의 순서다.
 */
data class ScheduleAddTodo(val id: String, val label: String, val done: Boolean = false, val editing: Boolean = false)

/** 떠 있는 시트. 날짜(1r-4-D)와 시간(1r-4-T)이 같은 자리를 쓴다. */
@Keep
enum class ScheduleAddSheet {
    NONE,
    DATE,
    TIME,
}
