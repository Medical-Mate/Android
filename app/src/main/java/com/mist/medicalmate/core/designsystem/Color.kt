package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * 카카오 로그인 버튼 색상. 카카오 디자인 가이드가 색상 변경을 금지한다.
 * 컨테이너 #FEE500, 심볼 #000000, 레이블 #000000 85%.
 *
 * 브랜드가 고정한 값이라 테마의 colorScheme에 넣지 않는다. 다크 모드에서도 그대로다.
 * https://developers.kakao.com/docs/ko/kakaologin/design-guide
 */
val KakaoContainer = Color(0xFFFEE500)
val KakaoSymbol = Color.Black
val KakaoLabel = Color.Black.copy(alpha = 0.85f)
