package com.mist.medicalmate.home.ui

import com.mist.medicalmate.core.model.IntakeStep
import java.time.LocalDate
import java.time.LocalTime

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /**
     * Figma `1n-1`과 `1n-2`가 같은 상태의 두 갈래다. [savedCards]와 [upcoming]이 비어
     * 있으면 빈 상태 화면이 된다. 별도 상태로 두지 않는 이유는 헤더와 오늘의 한 줄,
     * 시작 버튼이 두 화면에서 같기 때문이다.
     */
    data class Content(
        val userInitial: String,
        val hasUnreadNotification: Boolean,
        val todayLine: HomeTodayLine,
        val resume: HomeResume?,
        val savedCards: List<SavedCardSummary>,
        val upcoming: List<HomeSchedule>,
    ) : HomeUiState

    data object Failed : HomeUiState
}

/**
 * 홈 맨 위 "오늘의 한 줄" 카드의 내용.
 *
 * 문구를 담지 않고 상황만 갖는다. 카피는 `strings.xml`에 있고 화면이 조립한다. 첫 방문과
 * 상황마다 문구가 아예 다르므로 갈래로 나눴다. 하나의 문자열로 받으면 어느 상황인지 알 수
 * 없어 테스트할 것이 없어진다.
 *
 * **아홉 갈래다**(#235). 디자인 트랙이 여덟 상태와 고르는 규칙을 확정했다. 차례가 곧
 * 우선순위다 — 오늘 일정이 가장 세고, 그다음이 기록이 빠진 지난 일정, 다음 진료, 지난 진료,
 * 카드만 있는 상태 순이다.
 *
 * 규칙 둘이 경계를 정한다. "지났어요"는 기록이 저장된 진료에만 이틀 이상일 때 쓰고,
 * "남았어요"는 내일 이후 일정에만 이틀 이상일 때 쓴다. 하루는 "어제"·"내일"이고 0일은
 * 오늘 갈래다. 앞날 진료에 경과일을 적어 음수가 나오던 것이 이 규칙으로 사라진다(#224).
 */
sealed interface HomeTodayLine {
    /** 진료도 카드도 일정도 없는 사용자. Figma `1n-2`의 "처음 오셨네요". */
    data object FirstVisit : HomeTodayLine

    /** ①-a 오늘 진료가 있고 아직 시각 전이다. */
    data class TodayAhead(val clinic: String?, val time: LocalTime?) : HomeTodayLine

    /** ①-b 오늘 진료 시각이 지났는데 기록이 없다. */
    data object TodayDone : HomeTodayLine

    /** ①-c 오늘 진료를 기록해 뒀다. */
    data object TodayRecorded : HomeTodayLine

    /** ② 지난 일정에 기록이 없다. */
    data class RecordMissing(val on: LocalDate) : HomeTodayLine

    /** ③-a 다음 진료가 내일이다. */
    data class NextTomorrow(val clinic: String?, val time: LocalTime?) : HomeTodayLine

    /** ③-b 다음 진료가 [days]일 남았다. 이틀 이상일 때만 쓴다. */
    data class NextInDays(val days: Int, val on: LocalDate, val clinic: String?) : HomeTodayLine

    /** ④-a 어제 진료를 다녀왔다. */
    data object LastYesterday : HomeTodayLine

    /** ④-b 지난 진료 이후 [days]일이 지났다. 이틀 이상일 때만 쓴다. */
    data class LastDaysAgo(val days: Int) : HomeTodayLine

    /** ⑤ 카드만 있고 일정도 기록도 없다. */
    data object CardReady : HomeTodayLine
}

/**
 * 작성 중이던 증상 정리.
 *
 * Figma `1n-1`의 "이어서 하기" 카드다. 어디까지 답했는지를 [step]으로 갖는다. "2단계까지
 * 답했어요"를 문자열로 받으면 진행률을 다시 계산할 수 없다.
 *
 * **서버의 왕복 수가 아니라 우리 네 단계다**(#176). 서버는 문답 왕복을 세어 `0 / 20`처럼
 * 주는데 그것은 2단계 안에서 몇 마디 오갔는지이고 환자가 보는 단계가 아니다.
 */
data class HomeResume(val intakeId: String, val symptomTitle: String, val step: IntakeStep)

/**
 * 저장된 브리핑 카드 요약.
 *
 * **[visited]가 "진료 완료" 배지의 기준이다.** 카드의 확정 여부(`DRAFT`/`CONFIRMED`)와 다른
 * 축이다. 확정은 카드를 더 고치지 않겠다는 뜻이고, 진료를 다녀왔는지는 진료 기록이 붙었는지로
 * 정해진다. 문서가 그 둘을 섞지 말라고 적었다.
 *
 * [clinic]은 Figma의 "서울OO병원 내과"처럼 병원과 진료과를 합친 표시용 문자열이다. 병원명이
 * 선택 입력이라 진료를 마쳤어도 비어 있을 수 있어서, 이 값으로 진료 여부를 판단하면 안 된다.
 */
data class SavedCardSummary(
    val id: String,
    val title: String,
    val visited: Boolean,
    val writtenOn: LocalDate,
    val clinic: String?,
)

/**
 * 다가오는 진료 일정.
 *
 * D-day를 문자열로 받지 않고 [date]에서 계산한다. 화면이 열린 날에 따라 값이 달라지므로
 * 미리 만들어 두면 날짜가 바뀐 뒤에도 옛 값이 남는다.
 */
/** [time]이 없으면 시간 미정이다. 화면이 그 자리에 "시간 미정"을 적는다(#202). */
/**
 * 다가오는 일정 한 줄.
 *
 * @param followUp 진료 후 기록에서 잡힌 재방문인지. 시안 1n-1이 병원 이름 뒤에 `재진`·`초진`을
 *   붙이는데 그 둘을 가르는 값이 서버의 `origin`이다.
 */
data class HomeSchedule(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: String?,
    val followUp: Boolean = false,
)
