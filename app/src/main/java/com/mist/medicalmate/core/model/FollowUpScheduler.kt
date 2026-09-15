package com.mist.medicalmate.core.model

import java.time.LocalDate

/**
 * 진료 후 기록의 재방문 날짜를 캘린더 일정으로 올린다.
 *
 * 기록은 `visit` 도메인이 저장하고 일정은 `calendar` 도메인이 만든다. 저장이 끝난 자리에서
 * 일정을 만들어야 하는데 한 도메인이 다른 도메인을 직접 부르지 않으므로 `core`에
 * 인터페이스를 두고 Hilt가 연결한다. [CurrentUserProvider]와 같은 방식이다.
 *
 * **서버는 기록에서 일정을 만들지 않는다.** 문서가 "환자가 보고 등록하는 흐름"으로 못
 * 박았고, AI가 날짜를 잘못 뽑아도 조용히 일정이 생기지 않아야 한다고 적는다. 그 확인은
 * 1q-1이 한다 — 재방문 줄에 날짜가 보이고 고칠 수 있으며, 그 화면의 저장이 곧 확인이다.
 * 그래서 앱이 저장 직후에 만든다(#245). 만들어진 일정은 돌아가는 일자 화면의 "다음 일정"에
 * 바로 서서 조용히 생기지 않는다.
 *
 * 결과를 돌려주지 않는다. 기록이 본체고 일정은 덧붙이는 것이라, 일정을 못 만들어도 저장은
 * 성공이다. 구현이 실패를 삼킨다.
 */
fun interface FollowUpScheduler {
    /**
     * @param clinic 진료받은 병원. 서버가 일정에 병원을 요구해서 없으면 만들지 않는다.
     * @param on 재방문 날짜.
     * @param cardId 그 진료의 브리핑 카드. 일정에 걸어 두어야 그 날 무엇을 들고 가는지가 남는다.
     */
    suspend fun schedule(clinic: String?, on: LocalDate, cardId: Long)
}
