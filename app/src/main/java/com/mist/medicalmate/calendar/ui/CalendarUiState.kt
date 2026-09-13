package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.core.designsystem.component.MedicalMateDateMarker
import java.time.LocalDate
import java.time.YearMonth

/**
 * 캘린더 월 화면의 상태. Figma 1r-1 `406:2310`.
 *
 * [recordDays]는 기록이 있는 날, [plannedDays]는 앞으로 일정이 있는 날이다. 날짜 칸이 각각
 * 채운 점과 빈 원을 찍는다. 지난 기록과 앞으로의 일정을 색만으로 가르면 구별되지 않아서
 * 두 집합으로 나눠 갖는다. 어떤 일정인지는 그 날을 눌러야 나오므로 여기서는 날짜만 갖는다.
 *
 * [selected]와 [today]를 나눠 갖는다. 오늘은 옅은 테두리, 고른 날은 채움이라 표시가 다르고,
 * 오늘이 아닌 날을 골라 볼 수 있어야 한다.
 *
 * [cardSheet]가 있으면 카드만 있는 날 시트(1r-1-S `1226:4669`)가 떠 있다. 일정이 있는
 * 날은 일자 화면으로 넘어가고, 카드만 쓴 날은 갈 화면이 없어서 그 자리에서 시트로 보여준다.
 *
 * [appointments]는 그 달에서 받아 온 원본이다. 날을 고를 때마다 서버를 다시 부르지 않으려고
 * 들고 있는다. 화면이 그리는 것은 [schedules]이고 그것은 고른 날 것만이다.
 */
data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    val recordDays: Set<Int> = emptySet(),
    val plannedDays: Set<Int> = emptySet(),
    val schedules: List<CalendarSchedule> = emptyList(),
    val cardSheet: DayCard? = null,
    val appointments: List<Appointment> = emptyList(),
    val cards: List<CardListItem> = emptyList(),
)

/**
 * 고른 날의 일정 한 줄.
 *
 * [dday]는 오늘로부터 남은 일수다. 화면이 열린 날 기준이라 저장해 두지 않고 계산해서
 * 넣는다. 지난 일정은 음수가 되고 화면이 표시를 감춘다.
 */
/** [time]이 없으면 시간 미정이다. 화면이 그 자리에 "시간 미정"을 적는다(#202). */
data class CalendarSchedule(val id: String, val title: String, val time: String?, val detail: String, val dday: Long)

/**
 * 캘린더 일자 화면의 상태. Figma 1r-2 `406:2514`, 1r-2-A `1060:2879`, 1r-2-A2 `1060:2998`.
 *
 * 진료 전과 후로 화면이 갈린다. [record]가 있으면 다녀온 날이다.
 *
 * 진료 전이면 일정·가져갈 카드·진료 전 할 일·빈 기록 넷이다. 다녀오면 할 일이 없어지고
 * (챙길 일이 끝났다) 기록 자리가 채워지며 다음 일정이 붙는다. 일정 카드의 배지도 남은
 * 날수에서 "진료 완료"로 바뀐다.
 *
 * [nextEvent]는 진료 후 기록의 재방문에서 자동으로 만들어진다. 시간이 정해지기 전과 후가
 * 다르게 보인다.
 *
 * [todoDraft]가 있으면 편집 중이다(1r-2-E). 문서의 CRUD 규칙대로 사본을 고치고 취소하면
 * 버린다. 지울 수 있는 것은 할 일 줄과 이 날 일정 자체 둘이고, 일정 삭제는 하단 Danger
 * 버튼과 확인 대화상자를 거친다.
 */
data class CalendarDayUiState(
    val date: LocalDate,
    val schedule: CalendarSchedule?,
    val card: DayCard?,
    val todos: List<DayTodo> = emptyList(),
    val record: DayRecord? = null,
    val nextEvent: DayNextEvent? = null,
    val todoDraft: List<DayTodo>? = null,
    val deleteRequested: Boolean = false,
) {
    /** 다녀온 날인지. 기록이 있으면 진료가 끝난 것이다. */
    val visited: Boolean get() = record != null

    /** 편집 중인지(1r-2-E). 사본이 있으면 편집이다. */
    val editing: Boolean get() = todoDraft != null

    /** 화면에 그릴 할 일. 편집 중이면 사본, 아니면 본값이다. */
    val shownTodos: List<DayTodo> get() = todoDraft ?: todos

    /**
     * 편집에서 바뀐 것이 있는지. Nav 우측이 `취소`와 `확인`으로 갈린다.
     *
     * 체크는 세지 않는다. 체크는 편집 밖에서도 누를 수 있는 조작이라 편집으로 바꾼 것이
     * 아니다. 여기서 세는 것은 지운 줄이다.
     */
    val changed: Boolean get() = todoDraft != null && todoDraft.size != todos.size
}

/**
 * 그 진료에 가져갈 카드.
 *
 * [status]는 다녀온 뒤에 없다. 진료 전에는 "진료 전"처럼 언제 쓸 카드인지가 배지로 붙는데,
 * 끝난 뒤에는 알릴 상태가 없고 언제 보여줬는지가 [meta]로 간다.
 */
/**
 * 그 날 일정에 걸린 카드.
 *
 * [id]는 서버 카드 id다. 눌러서 카드로 가는 길이고, "진료 후 기록하기"가 기록을 붙이는
 * 자리이기도 하다. 서버가 확정한 카드 하나에 기록 하나를 받는다.
 *
 * [meta]와 [status]는 일정 응답에 없다. 카드를 따로 읽어야 나오는 값이라 비어 있을 수 있다.
 */
data class DayCard(val id: String, val title: String, val meta: String? = null, val status: String? = null)

/** 진료 전 할 일 한 줄. */
data class DayTodo(val id: String, val label: String, val done: Boolean)

/** 그 날 남긴 진료 후 기록. 눌러서 기록 상세로 간다. */
data class DayRecord(val id: String, val title: String, val meta: String)

/**
 * 다음 일정. Figma의 `Next Event`.
 *
 * [at]이 없으면 아직 시간을 정하지 않은 상태다(1r-2-A). 날짜만 있고 무엇을 더 해야 하는지
 * 알리는 문구와 확정 버튼이 붙는다. 정해지면(1r-2-A2) 칩이 남은 날수로 바뀌고 그 자리에
 * 날짜와 시간이 온다.
 *
 * [chip]을 문장으로 받는다. 확정 전에는 날짜이고 확정 후에는 D-day라 종류가 달라서, 무엇을
 * 적을지는 데이터가 정한다.
 */
data class DayNextEvent(
    val chip: String,
    val title: String,
    val at: String? = null,
    /**
     * 재방문할 병원.
     *
     * [at]이 없을 때 "시간 정하고 확정하기"가 일정 추가(1r-4-B)를 여는데, 그 화면의 병원
     * 필드가 이 값으로 채워진 상태가 시안의 `1r-4-B`다. 제목에서 떼어내지 않는다. 제목은
     * "서울OO병원 내과 재방문"처럼 말이 붙어 있어 자르는 규칙이 생긴다.
     */
    val clinic: String? = null,
)

/** 그 날에 찍을 표시. 기록이 예정보다 앞선다. 이미 지난 일은 사실이고 예정은 계획이다. */
internal fun CalendarUiState.markerOn(day: Int): MedicalMateDateMarker = when (day) {
    in recordDays -> MedicalMateDateMarker.RECORD
    in plannedDays -> MedicalMateDateMarker.PLANNED
    else -> MedicalMateDateMarker.NONE
}
