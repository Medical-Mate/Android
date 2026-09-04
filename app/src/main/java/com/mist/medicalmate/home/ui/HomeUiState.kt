package com.mist.medicalmate.home.ui

import java.time.LocalDate

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(val userName: String, val notice: HomeNotice?, val savedCards: List<SavedCardSummary>) :
        HomeUiState

    data object Failed : HomeUiState
}

/**
 * 홈 상단 배너. 와이어프레임의 "9/3 검사 결과 확인일이에요"에 해당한다.
 *
 * 표시 문구를 담지 않고 종류와 날짜만 갖는다. 문구는 `strings.xml`에 있고
 * 화면단에서 조립한다. 카피가 바뀌어도 이 타입은 그대로다.
 */
data class HomeNotice(val date: LocalDate, val kind: Kind) {
    enum class Kind { TEST_RESULT, REVISIT }
}

/**
 * 저장된 브리핑 카드 요약.
 *
 * [status]는 백엔드의 카드 상태 `draft` / `confirmed`에 대응한다. 확정된 카드는
 * 작성일을, 진행 중인 카드는 "이어서 작성"을 보여준다.
 *
 * 카드 화면(1e)을 만들 때 `card` 도메인으로 옮기거나 그쪽 모델을 참조하게 된다.
 * 지금은 홈만 쓰므로 여기 둔다.
 */
data class SavedCardSummary(val id: String, val title: String, val status: Status, val writtenOn: LocalDate) {
    enum class Status { DRAFT, CONFIRMED }
}
