package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md 4.1 간격 12개.
 *
 * 임의의 간격값을 쓰지 않는다. 새 값이 필요하면 컴포넌트에 하드코딩하기 전에 여기에
 * 추가할지 검토한다. `space/16` → `MedicalMateSpace.s16`.
 */
object MedicalMateSpace {
    val s2: Dp = 2.dp
    val s4: Dp = 4.dp
    val s6: Dp = 6.dp
    val s8: Dp = 8.dp
    val s10: Dp = 10.dp
    val s12: Dp = 12.dp
    val s14: Dp = 14.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s40: Dp = 40.dp
}

/**
 * DESIGN.md 4.3 크기와 레이아웃.
 *
 * `size/touch-min` 48은 접근성 기준이다. 시각 규격이 더 작은 컨트롤(32 Icon Button,
 * 46 Date Cell)은 hit area를 따로 넓혀 48을 맞춘다(DESIGN.md 9절, 11.4).
 *
 * OS 상태바와 홈 인디케이터 높이는 여기 두지 않는다. 플랫폼 safe-area를 쓴다.
 * [safeBottom]은 Figma 컴포넌트 내부의 시각 여백이라 기기 inset과 중복 적용하지 않는다.
 */
object MedicalMateSize {
    val touchMin: Dp = 48.dp
    val iconSm: Dp = 18.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val controlSm: Dp = 40.dp
    val controlMd: Dp = 48.dp
    val controlLg: Dp = 56.dp

    /** 주 음성 입력 버튼. */
    val mic: Dp = 88.dp

    /** 기준 화면 폭. 이 값으로 레이아웃을 고정하지 않는다. 거터를 유지하고 콘텐츠를 Fill한다. */
    val screenWidth: Dp = 390.dp
    val gutter: Dp = 20.dp

    /** 390 기준 콘텐츠 폭. 모든 기기에서 강제하지 않는다(DESIGN.md 4.4). */
    val contentWidth: Dp = 350.dp
    val safeBottom: Dp = 24.dp
    val navBarHeight: Dp = 56.dp

    /**
     * Tab Bar 높이.
     *
     * DESIGN.md 4.3의 `layout/tabbar-h` 토큰은 82이지만 Figma의 Tab Bar variant 3개는
     * 실제로 390x79다. 문서 11.2가 토큰과 마스터 중 하나를 확정하라고 남긴 항목이고,
     * 확정 전까지는 활성 마스터 값을 쓴다.
     */
    val tabBarHeight: Dp = 79.dp
}
