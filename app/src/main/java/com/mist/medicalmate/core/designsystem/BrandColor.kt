package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 카카오 로그인 버튼 색상. 카카오 디자인 가이드가 색상 변경을 금지한다.
 * 컨테이너 #FEE500, 심볼 #000000, 레이블 #191600.
 *
 * 브랜드가 고정한 값이라 시맨틱 토큰이나 `colorScheme`에 넣지 않는다. 테마와 다크 모드에
 * 따라 바뀌면 안 된다. DESIGN.md 8.2도 소셜 로그인 버튼을 공식 브랜드 가이드 예외로 둔다.
 * https://developers.kakao.com/docs/ko/kakaologin/design-guide
 */
val KakaoContainer = Color(0xFFFEE500)
val KakaoSymbol = Color.Black
val KakaoLabel = Color(0xFF191600)

/**
 * 나머지 소셜 로그인 브랜드 색. DESIGN.md 8.2가 적은 값이다.
 *
 * 각 사 공식 가이드가 정한 값이라 변경할 수 없다. 시맨틱 토큰이나 `colorScheme`에 넣지
 * 않는다. 테마나 다크 모드에 따라 바뀌면 가이드 위반이다.
 *
 * Naver 조합(`#03C75A` 면에 흰 글자)은 공식 규격 때문에 WCAG 대비 기준을 벗어난다.
 * 문서 8.2가 그 사실을 밝히고 다른 안내 요소는 기준을 지키라고 한다.
 *
 * 백엔드가 지금 카카오만 지원하므로 화면에는 카카오만 노출한다. 값을 미리 둔 것은
 * 컴포넌트가 variant를 다 갖추게 하려는 것이고, 버튼을 화면에 그리라는 뜻이 아니다.
 */
val NaverContainer = Color(0xFF03C75A)
val NaverLabel = Color(0xFFFFFFFF)

val AppleContainer = Color(0xFF000000)
val AppleLabel = Color(0xFFFFFFFF)

val GoogleContainer = Color(0xFFFFFFFF)
val GoogleLabel = Color(0xFF1F1F1F)
val GoogleBorder = Color(0xFF747775)
