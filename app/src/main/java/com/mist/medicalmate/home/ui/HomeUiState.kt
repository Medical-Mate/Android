package com.mist.medicalmate.home.ui

import com.mist.medicalmate.core.model.IntakeStep
import java.time.LocalDate

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
 * 재방문의 문구가 아예 다르므로 갈래로 나눴다. 하나의 문자열로 받으면 어느 상황인지
 * 알 수 없어 테스트할 것이 없어진다.
 */
sealed interface HomeTodayLine {
    /** 진료 기록이 아직 없는 사용자. Figma `1n-2`의 "처음 오셨네요". */
    data object FirstVisit : HomeTodayLine

    /**
     * 지난 진료 이후 [daysSinceLastVisit]일이 지났다.
     *
     * [nextVisit]이 있으면 다음 진료 날짜를 함께 알린다. 없으면 그 문장을 빼야 하므로
     * 화면이 문구를 갈라 쓴다.
     */
    data class SinceLastVisit(val daysSinceLastVisit: Int, val nextVisit: LocalDate?) : HomeTodayLine
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
data class HomeSchedule(val id: String, val title: String, val date: LocalDate, val time: String)
