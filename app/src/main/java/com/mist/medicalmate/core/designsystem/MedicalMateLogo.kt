package com.mist.medicalmate.core.designsystem

import com.mist.medicalmate.R

/**
 * DESIGN.md 7절 로고.
 *
 * 맞물린 두 조각으로 환자의 말과 의사의 진료를 잇는 제품 역할을 표현한다.
 *
 * 기본 크기는 심볼 48, 최소 24다. 보호 여백은 심볼 높이의 1/4을 사방에 확보한다.
 * 비율을 고정하고 찌그러뜨리거나 회전하지 않는다. [SymbolGradient]는 앱 아이콘과
 * 스플래시에만 쓴다.
 *
 * 락업은 가로 조합만 쓴다. 별도 레터링이나 세로 조합을 만들지 않는다.
 *
 * Figma의 `Mark` 마스터에는 기존 로고 자산의 파란 gradient(`#1769F7` 계열)도 들어
 * 있는데, 일반 UI에서 그 값을 새 브랜드 컬러처럼 재사용하지 않는다.
 *
 * `Subtle`(옅은 면 위)과 `Mono Light`(어두운 면 위)는 따로 두지 않았다. [Mark]에
 * tint를 주고 뒤에 면을 깔면 같은 결과가 된다. Subtle은 `bg/primary-subtle` 면에
 * `fg/primary` 마크, Mono Light는 흰 마크다.
 */
object MedicalMateLogo {
    /** UI 안 기본형. `primary/600` 면에 흰 마크. */
    val Symbol = R.drawable.ic_logo_symbol

    /** 앱 아이콘과 스플래시 전용. */
    val SymbolGradient = R.drawable.ic_logo_symbol_gradient

    /** 배경 없는 마크. 원본 색은 `fg/primary`이고 tint로 바꾼다. */
    val Mark = R.drawable.ic_logo_mark

    /** 심볼 36 + 워드마크. 139x36. */
    val Lockup = R.drawable.ic_logo_lockup
}
