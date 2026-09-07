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
