package com.mist.medicalmate.calendar.ui

import androidx.annotation.Keep
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일정 추가 화면의 상태. Figma 1r-4 `1062:3031`.
 *
 * 병원 · 날짜 · 시간은 골라 채우는 값이고, 카드와 할 일은 이 일정에 딸리는 목록이다.
 *
 * [hospital]과 [date]가 필수다. 나머지는 선택이다.
 *
 * **시간은 필수가 아니다.** 진료 시각을 아직 모르고 날짜만 잡아 두는 일이 흔하다. 시안도
 * 그 칸에 필수 표시를 두지 않는다. 안 고르면 시각 없이 저장한다(#202) — 서버가 시간 미정을
 * 담는 자리를 열었다. 전에는 `scheduledAt`이 시각을 요구해 오전 9시로 박고 있었고, 환자가
 * 고르지 않은 시각이 일자 화면에 그대로 떴다.
 */
data class ScheduleAddUiState(
    /**
     * 고치고 있는 일정.
     *
     * 있으면 새로 만드는 것이 아니라 그 일정을 고친다. 시간 미정으로 저장한 일정에 시각을
     * 채우러 들어오는 길이 이 상태다(1r-2 · 1r-2-A). 없으면 새 일정이다.
     */
    val appointmentId: Long? = null,
    /** 진료 후 기록의 재방문을 확정하러 들어왔는지. 저장할 때 일정의 출처가 된다. */
    val followUp: Boolean = false,
    val cards: List<ScheduleAddCard> = emptyList(),
    val todos: List<ScheduleAddTodo> = emptyList(),
    val hospital: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    /**
     * 빠진 필수 칸을 알릴지.
     *
     * 화면을 열자마자 붉히지 않는다. 아직 채울 기회가 없었는데 틀렸다고 하는 셈이다.
     * 저장을 누른 뒤부터 선다.
     */
    val showErrors: Boolean = false,
    val sheet: ScheduleAddSheet = ScheduleAddSheet.NONE,
    /** 공유받은 예약 문자를 읽은 결과(#270). 문자 없이 열렸으면 [ScheduleAddShareRead.NONE]이다. */
    val shareRead: ScheduleAddShareRead = ScheduleAddShareRead.NONE,
    /**
     * 열어야 할 병원 검색(#270). 예약 문자에서 읽은 병원 이름이다.
     *
     * 문자 속 이름만으로는 어느 지점인지 모른다. 그 이름으로 찾은 목록을 띄워 주소를 보고 고르게 한다.
     * 화면이 병원 찾기를 열면서 지운다.
     */
    val hospitalSearch: String? = null,
    /**
     * [hospitalSearch]가 말에서 온 것인지(#282). 그러면 병원 찾기가 하나로 좁혀지는 경우 알아서
     * 고르고, 여럿이면 막대로 되묻는다. 예약 문자에서 온 것은 지금처럼 사람이 주소를 보고 고른다.
     */
    val hospitalSearchByVoice: Boolean = false,
) {
    /** 고른 카드 수. 섹션 머리 오른쪽에 적는다. */
    val pickedCardCount: Int get() = cards.count { it.picked }

    val canSave: Boolean get() = hospital != null && date != null

    /** 채우지 않은 필수 칸. [showErrors]가 서면 그 칸 아래에 안내가 붙는다. */
    val hospitalMissing: Boolean get() = showErrors && hospital == null

    val dateMissing: Boolean get() = showErrors && date == null
}

/**
 * 가져갈 카드 후보 한 장.
 *
 * [clinic]은 그 카드로 가기로 한 병원이다. [meta]에 이미 글로 들어 있지만 거기서 다시
 * 떼어낼 수 없어 따로 든다. 카드를 고르면 병원 칸을 이 값으로 채운다(1r-4-B).
 *
 * [visited]는 이 카드로 이미 진료를 받았는지다. 그 카드로 잡는 일정은 재진이다(#272).
 */
data class ScheduleAddCard(
    val id: String,
    val title: String,
    val meta: String,
    val clinic: String? = null,
    val picked: Boolean = false,
    val visited: Boolean = false,
)

/**
 * 이 일정에 딸린 할 일 한 줄.
 *
 * [editing]이면 그 자리에서 글자를 받는다. 문서의 추가 방식이 새 입력 필드를 띄우는 대신
 * 목록에 빈 항목을 하나 만드는 것이고, 시안 `1092:3935`~`1185:12839`가 빈 행 → 적는 중 →
 * 완료된 행의 순서다.
 */
data class ScheduleAddTodo(val id: String, val label: String, val done: Boolean = false, val editing: Boolean = false)

/**
 * 공유받은 예약 문자를 어디까지 읽었는지(#270).
 *
 * 읽은 값은 틀릴 수 있다. 채운 뒤에도 화면 위에 그 사실을 남겨 확인하고 저장하게 한다. 날짜를
 * 못 찾으면 그것도 알린다 — 말없이 빈 칸으로 두면 공유가 안 된 것으로 읽힌다.
 */
@Keep
enum class ScheduleAddShareRead {
    NONE,
    READING,

    /** 날짜를 찾아 채웠다. */
    FILLED,

    /** 날짜를 못 찾았다. 병원은 찾았으면 채워져 있다. */
    MISSED,
}

/** 떠 있는 시트. 날짜(1r-4-D)와 시간(1r-4-T)이 같은 자리를 쓴다. */
@Keep
enum class ScheduleAddSheet {
    NONE,
    DATE,
    TIME,
}
