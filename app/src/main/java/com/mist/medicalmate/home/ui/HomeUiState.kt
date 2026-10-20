package com.mist.medicalmate.home.ui

import com.mist.medicalmate.core.model.IntakeStep
import java.time.LocalDate
import java.time.LocalTime

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /**
     * 홈(1n-1 · 1n-2).
     *
     * @param banners 맨 위 사진 배너. 처음 온 사람은 [HomeBanner.Welcome] 한 장이고 넘기지 않는다(1n-2). 그
     *   밖에는 다섯 장이고 첫 장이 상황에 따라 바뀐다.
     * @param upcoming 다가오는 일정. 서버가 하나만 준다.
     */
    data class Content(
        val userInitial: String,
        val hasUnreadNotification: Boolean,
        val banners: List<HomeBanner>,
        val resume: HomeResume?,
        val savedCards: List<SavedCardSummary>,
        val upcoming: HomeSchedule?,
    ) : HomeUiState {
        /** 처음 온 사람(1n-2). 배너가 짧고 시트에 빈 상태가 선다. */
        val firstVisit: Boolean get() = banners.singleOrNull() == HomeBanner.Welcome
    }

    data object Failed : HomeUiState
}

/**
 * 홈 배너 첫 장을 고를 재료(#312). 서버 응답에서 저장소가 정한다.
 *
 * 차례가 곧 우선순위다. 오늘 진료 → 내일 진료 → 일정 없는 카드 → 처음 온 사람이고, 어디에도 안 맞으면
 * [Other]다. 그때 첫 장은 2~5장 가운데 하나를 무작위로 둔다(사용자 결정). 문구는 담지 않는다 — 리소스에
 * 있고 화면이 조립한다.
 */
sealed interface HomeLead {
    /** 카드도 일정도 진료도 없는 사람. */
    data object FirstVisit : HomeLead

    /** 오늘 카드가 걸린 진료가 있고 아직 기록하지 않았다. [clinic]은 "서울OO병원 내과"처럼 합친 이름이다. */
    data class TodayVisit(val clinic: String?, val visit: HomeVisitLink) : HomeLead

    /** 내일 카드가 걸린 진료가 있다. */
    data class TomorrowVisit(val time: LocalTime?, val clinic: String?, val cardId: String) : HomeLead

    /** 가장 최근 카드가 아직 진료 전이고 그 카드로 잡은 일정이 없다. [symptom]은 "복부 통증"이다. */
    data class CardReady(val cardId: String, val symptom: String) : HomeLead

    data object Other : HomeLead
}

/**
 * 진료 후 기록으로 가는 데 필요한 것. 기록이 카드에 붙고(서버), 병원 확인(1m-12)이 병원 이름을 보인다.
 *
 * @param clinic 일정에 등록한 병원 이름. 비어 있으면 병원 찾기부터 간다.
 */
data class HomeVisitLink(val cardId: String, val cardTitle: String, val clinic: String?, val date: LocalDate)

/** Figma `Home Banner`의 한 장. 앞의 넷이 첫 장 자리, 뒤의 넷이 2~5장이다. */
sealed interface HomeBanner {
    /** Banner=1 · CTA=None. 처음 온 사람. */
    data object Welcome : HomeBanner

    /** Banner=1. 오늘 진료 — "진료 후 기록하기". */
    data class Today(val clinic: String?, val visit: HomeVisitLink) : HomeBanner

    /** Banner=1. 내일 진료 — "카드 다시 보기". */
    data class Tomorrow(val time: LocalTime?, val clinic: String?, val cardId: String) : HomeBanner

    /** Banner=1. 일정 없는 카드 — "진료 일정 잡기". */
    data class CardReady(val cardId: String, val symptom: String) : HomeBanner

    /** Banner=2. 예약 문자. */
    data object Message : HomeBanner

    /** Banner=3. 일정 알림. */
    data object Schedule : HomeBanner

    /** Banner=4. 카드 이미지 저장. [cardId]는 가장 최근 카드이고 없으면 카드 목록으로 간다(사용자 결정). */
    data class SaveCard(val cardId: String?) : HomeBanner

    /** Banner=5. 헤이 메이트. */
    data object Mate : HomeBanner
}

/**
 * 작성 중이던 증상 정리. Figma `Card / Resume`.
 *
 * 어디까지 했는지를 [step]으로 갖는다. 진행 원은 끝낸 단계 수를 세 칸 중에 채운다.
 */
data class HomeResume(val intakeId: String, val symptomTitle: String, val step: IntakeStep) {
    /** 끝낸 단계 수. 지금 있는 단계 앞까지다. */
    val doneSteps: Int get() = step.number - 1
}

/**
 * 저장된 브리핑 카드 요약.
 *
 * [clinic]은 "서울OO병원 내과"처럼 병원과 진료과를 합친 표시용 문자열이다. 병원명이 선택 입력이라 진료를
 * 마쳤어도 비어 있을 수 있어서, 이 값으로 진료 여부를 판단하면 안 된다.
 */
data class SavedCardSummary(
    val id: String,
    val title: String,
    val status: HomeCardStatus,
    val writtenOn: LocalDate,
    val clinic: String?,
)

/**
 * 카드 줄의 배지. 브리핑 카드 전체(1j-4)와 같은 규칙이다(사용자 결정) — 진료 기록이 붙었으면 진료 완료, 아니면
 * 진료 전이다. 카드는 만들 때부터 확정돼 있어(Backend#181) 저장하지 않은 카드가 없다(#338).
 */
enum class HomeCardStatus { BEFORE_VISIT, VISITED }

/**
 * 다가오는 일정. Figma `Card / Schedule`.
 *
 * D-day를 문자열로 받지 않고 [date]에서 계산한다. 화면이 열린 날에 따라 값이 달라지므로 미리 만들어 두면
 * 날짜가 바뀐 뒤에도 옛 값이 남는다.
 *
 * @param time 없으면 시간 미정이다(#202).
 * @param followUp 진료 후 기록에서 잡힌 재방문인지. 제목이 "내과 재진"처럼 끝난다.
 */
data class HomeSchedule(
    val id: String,
    val date: LocalDate,
    val time: LocalTime?,
    val clinic: String?,
    val department: String?,
    val followUp: Boolean = false,
)
