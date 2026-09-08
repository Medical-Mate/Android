package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.tooling.preview.Preview

/**
 * 화면 Preview에 붙이는 공용 애노테이션.
 *
 * Figma 캔버스인 390 하나만 보면 실제 기기에서 넘치는 것을 못 본다. 가장 좁은 360과 넓은
 * 412를 함께 세워 두고, 글꼴 배율을 올린 경우까지 같은 자리에서 확인한다.
 *
 * 360은 갤럭시 A 계열과 화면 크기를 키운 기기가 들어가는 폭이다. 412는 픽셀 계열이다.
 * 그 사이(390·393·411)는 두 끝이 통과하면 함께 통과한다.
 *
 * 글꼴 배율은 200%만 본다. 접근성 설정의 상한이고, 여기서 견디면 중간 배율은 문제가 없다.
 * 배율을 올리면 높이가 함께 늘어나므로 세로도 키워 잡았다.
 *
 * 컴포넌트 Preview에는 붙이지 않는다. 조각 하나를 네 번 그리면 확인할 것이 늘지 않는다.
 */
@Preview(name = "390 · 기준", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "360 · 좁은 기기", widthDp = 360, heightDp = 780, showBackground = true)
@Preview(name = "412 · 넓은 기기", widthDp = 412, heightDp = 892, showBackground = true)
@Preview(
    name = "360 · 글꼴 200%",
    widthDp = 360,
    heightDp = 1400,
    fontScale = 2f,
    showBackground = true,
)
annotation class MedicalMateScreenPreviews
