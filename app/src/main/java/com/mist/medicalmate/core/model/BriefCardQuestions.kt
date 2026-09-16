package com.mist.medicalmate.core.model

/**
 * 의사에게 물어볼 질문의 상한.
 *
 * 서버가 `PUT /api/sessions/{id}/questions`와 카드 편집 요청에서 같은 수를 넘기면 거절한다
 * (Backend#132). 문답 4단계(1i)와 카드 편집(1e-1-E)이 같은 값을 들어야 한 쪽에서 만든 목록이
 * 다른 쪽에서 거절되지 않는다 — 그래서 두 도메인이 함께 읽는 `core/model`에 둔다.
 *
 * 앱이 이 수에서 막는 이유가 있다(#264). 넘치는 목록을 보내면 서버가 400을 주고 세션의
 * 질문이 빈 채로 남아, 만든 카드에 질문이 하나도 뜨지 않았다.
 */
const val MAX_BRIEF_CARD_QUESTIONS: Int = 5
