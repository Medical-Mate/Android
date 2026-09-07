package com.mist.medicalmate.core.designsystem

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md 5절 고도.
 *
 * 이 시스템은 모든 요소에 테두리를 두르지 않는다. 선은 목록 구분, 선택, 포커스,
 * 접근성 경계에만 쓰고 나머지는 그림자로 층을 표현한다. 그림자는 순수 검정이 아니라
 * 브랜드 틴트 [ShadowTint]를 쓴다.
 *
 * Figma 원본 값(2026-09-07 확인):
 *
 * ```
 * Elevation/Card   #1B255A14 (0, 3)  blur 10  +  #1B255A0D (0, 1) blur 2
 * Elevation/Float  #1B255A17 (0, 4)  blur 14  +  #1B255A0F (0, 1) blur 3
 * Elevation/Sheet  #1B255A1A (0, -4) blur 24  +  #1B255A0D (0, 0) blur 2
 * ```
 *
 * **Compose로 이 값을 그대로 옮길 수 없다.** `Modifier.shadow`는 elevation dp와 shape만
 * 받는다. offset과 blur를 따로 지정할 수 없고, 2단 그림자도 표현하지 못한다. Sheet의
 * 위쪽(-4) 방향은 특히 재현할 수 없다.
 *
 * 아래 값은 각 단계의 주 레이어 y-offset을 그대로 옮긴 출발점이다. 컴포넌트를 만들 때
 * 눈으로 맞추고, 어긋나면 이 파일에서 조정한다. 정확히 맞춰야 하는 자리는 직접 그리는
 * 쪽을 검토한다.
 *
 * 브랜드 틴트는 API 28부터 적용된다(`View.setOutlineSpotShadowColor` since 28).
 * minSdk 24라서 API 24~27에서는 그림자가 검정으로 나온다.
 */
object MedicalMateElevation {
    /** Card, 선택 행 */
    val card: Dp = 3.dp

    /** 하단 고정 바, Toast, 플로팅 */
    val float: Dp = 4.dp

    /** Bottom Sheet, Dialog */
    val sheet: Dp = 8.dp
}

/** 그림자 틴트. `Modifier.shadow`의 `ambientColor`와 `spotColor`에 함께 넘긴다. */
val ShadowTint = Color(0xFF1B255A)

/**
 * DESIGN.md 5절 Glass surface.
 *
 * 콘텐츠 위에 떠 있다는 의미가 있을 때만 쓴다. 본문 Card와 Button에는 쓰지 않는다.
 * Glass에는 테두리를 추가하지 않는다. 한 화면에서 떠 있는 층은 최대 두 단계다.
 *
 * **Android에서 블러는 API 31부터다.** `android.graphics.RenderEffect`가 API 31에
 * 추가됐고 Compose의 `Modifier.blur`도 그 위에서만 동작한다. minSdk 24라서 Android
 * 7.0~11에서는 블러가 걸리지 않는다. 문서가 말하는 Opaque 변형은 선택이 아니라 필수
 * 경로다. 저사양 기기와 절전 모드에서도 Opaque로 내린다.
 *
 * Opaque 변형은 같은 레이아웃과 대비를 유지해야 한다. 알파를 1로 올리는 것으로 끝내지
 * 말고 뒤에 `bg/surface`를 깐다.
 */
object MedicalMateGlass {
    val blurRadius: Dp = 24.dp

    const val BOTTOM_CTA_ALPHA: Float = 0.78f
    const val NAV_BAR_ALPHA: Float = 0.82f
    const val TAB_BAR_ALPHA: Float = 0.86f

    /** 블러가 걸리는 최소 SDK. 아래에서는 Opaque 변형을 쓴다. */
    const val MIN_BLUR_SDK: Int = Build.VERSION_CODES.S

    val isBlurSupported: Boolean
        get() = Build.VERSION.SDK_INT >= MIN_BLUR_SDK
}
